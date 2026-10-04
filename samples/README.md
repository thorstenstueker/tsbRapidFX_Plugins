# RapidFX samples

Real projects, not fragments. Each one stands on its own, each one runs as it is, and each one
exists to show **one thing**: a page changes to another page. That question has a different answer
in each of the three worlds, and the answer is the sample.

**Every form here is a designer form** — open one and the *Design* tab is there. That is not a
detail. These were hand-typed until 04.10.2026, and a typed `.rfx` carries no designer banner, so
the Design tab never appeared. Somebody who buys a product built around a designer, opens the
sample that comes with it and finds no designer does not conclude that the sample is plain. They
conclude the designer is broken.

So the forms are written by the designer's own generator now, the same path *Apply to code* takes.
`PagesSampleTest` in `desktop/designer` produces them and will not let them be anything else.

| | the page change | run it with |
|---|---|---|
| [`pages-desktop/`](pages-desktop/) | `New PageTwo(window).ShowIn(window)` — the window is yours, so there is nothing more to it | the green arrow, or `rfxc --project` |
| [`pages-web/`](pages-web/) | write into the session, **then** `rebuild()` — both halves, or nothing happens | `java -jar`, then `localhost:8099` |
| [`pages-mobile/`](pages-mobile/) | a stack, because the back gesture is not yours | `rfxmobile swing pages-mobile` |

**Two pages, one button on each.** Nothing else — no customer list, no sign-in, no table. The story
is one sentence long and the sample is the same length. An earlier version had four ideas in it and
taught the reader a customer file instead of a page change.

They are kept apart from the documentation on purpose: a snippet in a manual rots quietly, and a
project that is compiled on every build does not.

## Opening one

**File ▸ Open**, choose the directory. The desktop and web samples carry an `.rfxproj`; the mobile
one carries an `rfxmobile.properties` and is built with `rfxmobile` rather than from the IDE.

The web sample wants the three tsbWEB jars in its `lib/`, which come with the plugin. The desktop
one wants nothing: Swing is in the JDK.

## Take what you like

An example that may not be copied is decoration. Use anything here in your own programs, changed or
unchanged, with no condition and no attribution.

## What they are not

Not a framework, not a starting point to copy wholesale, and not styled. The shortest honest
version of one idea each, which is what makes them readable in a minute.
