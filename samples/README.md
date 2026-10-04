# RapidFX samples

Real projects, not fragments. Each directory stands on its own, each one runs as it is, and each
one exists to show **one thing** — mostly how you get from one screen to the next, which is the
question with a different answer in each of the three worlds.

They are kept apart from the documentation on purpose. A snippet in a manual rots quietly; a project
that is opened and run does not.

| | shows | run it with |
|---|---|---|
| [`pages-desktop/`](pages-desktop/) | Three screens in one window, a second window, a modal dialog | the green arrow, or `rfxc --project` |
| [`pages-web/`](pages-web/) | The same three screens as a server — session, sign-in, rebuild | `java -jar`, then `localhost:8099` |
| [`pages-mobile/`](pages-mobile/) | The same three screens on a phone — stack, back, a result handed back | `rfxmobile swing pages-mobile` |

**All three show the same program**, deliberately: a customer list, a detail screen and a picker
that hands a choice back. Open two side by side and the difference between the three models is
visible in about a minute.

## Opening one

**File ▸ Open**, choose the directory. The desktop and web samples have an `.rfxproj`; the mobile
one has an `rfxmobile.properties`, and is built with `rfxmobile` rather than from the IDE.

## What they are not

Not a framework, not a starting point to copy wholesale, and not styled. They are the shortest
honest version of one idea each, which is what makes them readable.
