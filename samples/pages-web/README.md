# Two pages, in a browser

The same two pages as the desktop sample next door, and the same designer — what
differs is the one thing worth seeing: **how a page changes when there is no window.**

## Running it

The three tsbWEB jars belong in `lib/`; they come with the plugin. Then:

```bash
rfxc --project PagesWeb.rfxproj --jar PagesWeb.jar
java -jar PagesWeb.jar
```

Open `http://localhost:8099/`. What the browser shows is Swing's own drawing, painted
on the server and sent — not HTML built from your form.

## What to look at

`PageOne.rfx` and `PageTwo.rfx` are **designer forms**: open either and the *Design*
tab is there, exactly as on the desktop.

There is **no navigation**. `createRoot` in `Main.rfx` is asked which page to draw, and
it is asked again on every rebuild. So changing page is two lines, and both are needed:

```basic
tsbWebSession.setValue("page", "two")
tsbWebSession.rebuild()
```

Without the rebuild, `createRoot` is never asked again and the browser goes on showing
the old page. That is the single most common surprise in a web program here, which is
why this sample is built around it.

## Why the session and not a field

A `Shared` field would be one value for **every** visitor at once: the second person to
click would move the first person's page. The session is per visitor. On a desktop that
difference is invisible; in a browser it is the whole game.
