# pages-web

The same three screens as a server. The forms are Swing forms — the server paints them and sends the
drawing.

```bash
rfxc -cp "lib/*" -o build src/*.rfx
java -cp "build:lib/*" Main
```

Then `http://localhost:8099/` — sign in with **anna / secret**.

## What it shows

**There is no navigation.** No router, no URL per screen, no navigation call. One method —
`createRoot` — decides what this user is looking at, and it is asked again whenever something
changes:

```basic
tsbWebSession.setValue("screen", "detail")
tsbWebSession.rebuild()
```

Both halves are needed. Writing into the session alone changes nothing on the screen.

**Why it works this way:** a browser can reload, go back, open a second tab, or sit for an hour and
then click. A navigation stack would have to answer all of those, and every answer would be a guess
about what the user meant. One question — *what should this user see now?* — has none of those
problems, and a reload produces the same screen because it produces the same answer.

**Passing something to the next screen** is the session, for the same reason: there is no parameter
because there is no call.

**`SessionStatic` and `Shared`** are declared together at the top of `Main.rfx`. One is per user;
the other is one value for everybody, which is right for a price list and a data leak for anything
belonging to a person.
