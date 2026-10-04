# Two pages

One window, two pages, and the change between them. That is the whole sample.

## Running it

**File ▸ Open**, choose this directory, then the green arrow. Or from a terminal:

```bash
rfxc --project PagesDesktop.rfxproj --jar PagesDesktop.jar
java -jar PagesDesktop.jar
```

## What to look at

`PageOne.rfx` and `PageTwo.rfx` are **designer forms** — open either one and the
*Design* tab is there. Drag the button somewhere else, save, and the generated half of
the file changes while the handler you wrote stays exactly as it was.

The page change is one line:

```basic
New PageTwo(window).ShowIn(window)
```

`ShowIn` puts a form into a window that already exists, so changing page is building the
other page and showing it. There is no router, no navigation stack and no framework —
on the desktop there does not need to be one, because the window is yours.

On a phone there *is* a stack, because the back gesture is not yours; in a browser there
is neither, because the server decides what to draw. Those are the mobile and web
samples beside this one.

## What it is not

Not styled, not a starting point to copy wholesale, and deliberately not useful. It is
the shortest honest version of one idea, which is what makes it readable in a minute.
