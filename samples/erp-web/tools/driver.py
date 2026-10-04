#!/usr/bin/env python3
"""A test driver for the running application.

It speaks the same protocol as app.js: an SSE stream for the tree, one POST per event. That is
enough to work the whole application without a browser - sign in, search, click - and it is the
only way to check it automatically.

Used by probe.py, which is the script that actually runs a round.
"""
import json
import sys
import threading
import time
import urllib.request
import urllib.error
import http.cookiejar

BASE = "http://localhost:8099"


class Session:
    def __init__(self):
        self.jar = http.cookiejar.CookieJar()
        self.opener = urllib.request.build_opener(
            urllib.request.HTTPCookieProcessor(self.jar))
        self.csrf = ""
        self.sid = ""
        self.tree = {}
        self.layers = {}
        self.counter = 0
        self.ready = threading.Event()

    # --- connecting ---------------------------------------------------------

    def start(self):
        self.opener.open(BASE + "/").read()
        threading.Thread(target=self._stream, daemon=True).start()
        if not self.ready.wait(10):
            raise SystemExit("no tree received")

    def _stream(self):
        answer = self.opener.open(BASE + "/tsb/stream")
        buffer = ""
        for line in answer:
            line = line.decode("utf-8")
            if line.startswith("data:"):
                buffer += line[5:].strip()
            elif line.strip() == "" and buffer:
                self._message(json.loads(buffer))
                buffer = ""

    def _message(self, m):
        kind = m.get("kind")
        if kind in ("hello", "session"):
            self.csrf = m.get("csrf", "")
            self.sid = m.get("sid", "")
        elif kind == "tree":
            name = m.get("layer") or "form"
            self.layers[name] = m
            if name == "form":
                self.tree = m
            self.counter += 1
            self.ready.set()
        elif kind == "patch":
            self._patch(m)
        elif kind == "close":
            self.layers.pop(m.get("layer") or "form", None)
        elif kind == "error":
            print("error from the server:", m.get("text"), file=sys.stderr)

    # A patch changes individual nodes instead of replacing the whole tree. Whoever does not
    # apply it reads the state from a moment ago afterwards - exactly the mistake a test driver
    # must not make.
    def _patch(self, m):
        name = m.get("layer") or "form"
        layer = self.layers.get(name)
        if not layer:
            return
        nodes = layer.setdefault("nodes", {})
        for id_, data in (m.get("add") or {}).items():
            nodes[str(id_)] = data
        for id_, changes in (m.get("set") or {}).items():
            entry = nodes.get(str(id_))
            if entry is None:
                continue
            for key, value in changes.items():
                if value is None:
                    entry.pop(key, None)
                else:
                    entry[key] = value
        for id_ in (m.get("del") or []):
            nodes.pop(str(id_), None)
        self.counter += 1

    # --- events -------------------------------------------------------------

    def send(self, id_, kind, value=""):
        body = f"{id_}\t{kind}\t{urllib.parse.quote(str(value))}".encode("utf-8")
        request = urllib.request.Request(BASE + "/tsb/event", data=body, method="POST")
        request.add_header("X-tsbWeb-Csrf", self.csrf)
        request.add_header("X-tsbWeb-Session", self.sid)
        request.add_header("Content-Type", "text/plain;charset=UTF-8")
        self.opener.open(request).read()
        time.sleep(0.35)

    # --- finding things -----------------------------------------------------
    #
    # The mirror sends a type, a position and a text - no field name. So things are found the
    # way a person points at a screen: by what a button says, or by where a field sits.

    def nodes(self, layer="form"):
        return (self.layers.get(layer) or {}).get("nodes", {})

    # Invisible components are skipped. The mirror sends them along ("vis": false), because a
    # patch has to be able to show them again later - but they are not on the screen, and a test
    # driver that finds one is checking something nobody can see.
    def every(self, type_=None, layer="form", hidden_too=False):
        for id_, d in sorted(self.nodes(layer).items(), key=lambda x: int(x[0])):
            if not hidden_too and d.get("vis") is False:
                continue
            if type_ is None or d.get("t") == type_:
                yield int(id_), d

    def button(self, caption, layer="form"):
        for id_, d in self.every("JButton", layer):
            if (d.get("text") or "").strip() == caption:
                return id_
        raise AssertionError(f"no button {caption!r} on screen: {self.button_texts(layer)}")

    def button_texts(self, layer="form"):
        return [d.get("text") for _, d in self.every("JButton", layer)]

    def texts(self, layer="form"):
        return [d.get("text") for _, d in self.every("JLabel", layer) if d.get("text")]

    def at(self, type_, x, y, layer="form"):
        for id_, d in self.every(type_, layer):
            if d.get("x") == x and d.get("y") == y:
                return id_
        raise AssertionError(f"no {type_} at {x},{y}")

    def first(self, type_, layer="form"):
        for id_, _ in self.every(type_, layer):
            return id_
        raise AssertionError(f"no {type_} on screen")

    def table(self, layer="form"):
        for id_, d in self.every("JTable", layer):
            return id_, d
        raise AssertionError("no table on screen")

    def rows(self, layer="form"):
        _, d = self.table(layer)
        return d.get("rows") or []

    # --- working it ---------------------------------------------------------

    def click(self, caption, layer="form"):
        self.send(self.button(caption, layer), "click")

    def type_in(self, id_, value):
        self.send(id_, "text", value)

    def choose(self, id_, index):
        self.send(id_, "select", str(index))

    def select_row(self, number, layer="form"):
        id_, _ = self.table(layer)
        self.send(id_, "rows", str(number))
