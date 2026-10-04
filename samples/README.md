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

## And one real application, three times

The question the small samples cannot answer is whether the tool carries something real. So the
same business application is here as well — customers, articles, quotations, delivery notes and
invoices in a SQLite database:

| | |
|---|---|
| [`erp-desktop/`](erp-desktop/) | 23 files, 8 designer forms, one `JFrame` |
| [`erp-web/`](erp-web/) | 17 files, 8 designer forms, a server and a port |
| [`erp-mobile/`](erp-mobile/) | 19 files, 7 designer forms, compiled ahead of time for both phones |

**How much is shared is measured, not claimed.** Desktop and web have the same data layer *byte
for byte* — `Article`, `ArticleFile`, `CustomerFile`, `DocumentFile`, `Document`, `Util` are the
same files. The phone is close: the records differ by two lines, the file classes by ten or twelve,
and all of that for one cause — iOS's built-in SQLite driver declines JDBC's generated keys, so the
phone asks `last_insert_rowid()` instead.

**Which files are designer forms, and which are not.** The ones ending in `Form`, plus
`MainWindow`. `Customer`, `Article`, `Database`, `Util`, `Main` are **not** forms and are not meant
to be — a customer record has no screen. Worth saying because mistaking the second group for the
first makes it look as though the designer could not open the application. It can: every form
round-trips through the designer unchanged, which `tools/CheckForms.java` in the two desktop
projects checks from a terminal.

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
