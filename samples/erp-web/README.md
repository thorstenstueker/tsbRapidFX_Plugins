# RapidX ERP

A small business application, served to a browser: customers, articles, quotations, delivery
notes and invoices in a SQLite database. Written in **RapidFX**, every screen drawn in
**tsbDesignerSWX**, delivered by **tsbWEB**.

There is no HTML page, no template language and no JavaScript in this project. The server paints
the same Swing forms that would sit in a `JFrame` on a desktop, and sends the drawing.

The program is English throughout — identifiers, screen texts, comments, the database schema and
every word of the sample data. It is the project people are shown first, so there is nothing on
screen that has to be translated for a reader.

Amounts read `1,405.39 EUR` and dates `2026-09-08`, both written with an explicit English locale
rather than the machine's own: a demonstration that says `1.405,39` on one laptop and `1,405.39` on
the next is a demonstration of the wrong thing. The company is in Germany — the euro and 19 % VAT
are its own, not a translation problem.

Three things stay German because they are not this project's to rename: the keys of
`tsbweb.properties`, the properties of `tsbWebConfig` (`Titel`, `Thema`, `LeerlaufMinuten`), and
the `Ja` / `Nein` buttons of `tsbWebOptionPane`. They belong to the libraries in `lib/`.

## Running it

```bash
sh tools/build.sh                 # generate the forms, check them, compile
java -jar build/RapidXERP.jar
```

Then open <http://localhost:8099/>.

| User | Password | Role |
|---|---|---|
| `admin` | `secret` | admin |
| `anna` | `secret` | sales |

In the IDE: right-click `src/Main.rfx` → **Run**, or the arrow beside `Sub Main`.

> `RapidXERP.iml` and `.idea/modules.xml` belong to the project and have to be kept. They are
> what the plugin reads to find the jars: it asks the IDE for the class path of the module the
> `.rfx` file sits in. With no module there is no class path, and every `Imports com.tsbweb.server`
> is underlined as unknown. If that happens, the module was lost — **File ▸ Invalidate Caches**,
> or check that `.idea/modules.xml` is still there.

The database lives in `data/rapidxerp.db` and is created on the first start and filled with
eight customers and twelve articles. Somewhere else: set the environment variable
`RAPIDXERP_DB`. Delete the file and the next start begins again from scratch.

The folder is found by looking upwards for the `.rfxproj`, not by taking the working directory.
The two ways of starting do not agree on that one: the IDE plugin runs the program in the folder
of the `.rfx` file, so the database would otherwise land in `src/` — a second one, beside the one
that grew next to the jar. The line the program prints on startup says which file it opened.

## What it does

* **Customers** and **articles**: search, create, change, delete. The search covers every field
  at once — a customer number, a town or a piece of an e-mail address all lead to the same place.
* **Writing a quotation**: choose a customer, an article and a quantity, add the line. Net, VAT
  and gross stand underneath immediately.
* **Carrying over**: a quotation becomes a delivery note or an invoice straight away, a delivery
  note becomes an invoice. Head and lines are copied, a new number is issued, and the origin
  stays on the new document.
* **Document numbers** are issued by the program: `QU-2026-0001`, `DN-2026-0001`, `IN-2026-0001`.

Two rules refuse something and say why: a customer who appears on a document is not deleted, and
neither is an article that appears on a document line.

## The files

```
RapidXERP.rfxproj        the project file — Type = WEB, sources, libraries
RapidXERP.iml            the IntelliJ module: src as source folder, lib/*.jar attached
.idea/modules.xml        registers that module with the project
tsbweb.properties        what an operator may change without recompiling
data/rapidxerp.db        the database (created on first start)

src/
  Main.rfx               Sub Main, every SessionStatic, the navigation, Class Application
  Database.rfx           the connection, the tables, the sample data
  Util.rfx               numbers, money, dates — the conversions that would otherwise be
                         written out everywhere

  Customer.rfx           the data
  Article.rfx
  Document.rfx           Document and DocumentLine; a document does its own arithmetic

  CustomerFile.rfx       all the SQL, and only here
  ArticleFile.rfx
  DocumentFile.rfx

  SignInForm.rfx         ── eight screens, drawn in the designer ──
  MainForm.rfx           the shell: header, navigation, content area
  CustomerListForm.rfx   CustomerSheetForm.rfx
  ArticleListForm.rfx    ArticleSheetForm.rfx
  DocumentListForm.rfx   DocumentSheetForm.rfx

tools/
  CheckForms.java        reads every screen with the designer's parser and compares
  build.sh               round trip, then compile
  driver.py probe.py     drives the whole application, without a browser
  Shot.java shot.sh      paints the screens into a PNG — no browser, no server
```

## The database

One table per thing, in the singular, all lower case:

```
users(login, displayname, password)      userrole(login, role)
customer(id, number, name, contact, street, postcode, city, country,
         email, phone, vatid, note)
article(id, number, name, description, unit, price, vat, stock)
document(id, kind, number, customer_id, docdate, status, subject, source_id)
documentline(id, document_id, seq, article_id, number, name,
             quantity, unit, unitprice, vat)
```

`docdate` rather than `date`, because a column called after a SQL function is a trap somebody
falls into later. `kind` holds the whole word — `Quotation`, `Delivery note`, `Invoice` — which is
what the screen shows as well.

## Changing the screens in the designer

Right-click a form → **Open in Designer**. All eight are written with the designer's own
generator, so they carry exactly the `' @tsbswx` lines its parser expects.

Everything **above** the `DO NOT EDIT` banner is yours and is written back unchanged; everything
below it belongs to the designer and is produced afresh on every save.

`tools/CheckForms.java` checks precisely that: every form is read in, written out again and
compared. If the same file comes out, a save in the designer cannot break anything.

> **`src/` is the source, and nothing generates it.** It used to: `tools/GenerateForms.java` wrote
> every screen from scratch and took the half above the banner out of `tools/handwritten/*.txt`,
> so a screen edited in the designer was quietly replaced by the old copy the next time the build
> ran. Both are gone. A screen is an ordinary form now — open it, change it, save it.

## The three things that differ from a desktop

**No static field for user data.** Harmless on a desktop, a data leak in a browser: the second
person to sign in would see the first one's data. What belongs to one user goes into a
`SessionStatic` — reachable from anywhere, with no parameter, and still a value of its own per
session. All of them are in `Main.rfx` and nowhere else; the compiler insists on it (RFX0208).

The instructive case is `Draft As Document`: a whole object with its lines, standing for the whole
of the editing. Every added line rebuilds the screen — a field in the form would be gone each
time. `tools/probe.py` checks it with two simultaneous sessions: two drafts, two quantities, no
contact.

**No router.** Write into the session, ask for a rebuild:

```basic
Main.Screen = "customers"
tsbWebSession.rebuild()
```

`MainForm.Content()` then decides afresh. That is the whole of the navigation — a `Select Case`
statement.

**No window.** The browser is the window. A form is attached through `GetRootPane()`, not through
`ShowIn(frame)` — which is why the designer generates no `ShowIn` here.

## Checking it

```bash
java -jar build/RapidXERP.jar &
python3 tools/probe.py
```

`probe.py` speaks the same protocol as `app.js` — an SSE stream for the tree, one POST per event
— and works the application the way a person would: sign in, search, create a customer, write a
quotation with two lines, carry it over into an invoice, tidy up. Every step checks what is on
the screen afterwards.

It creates data and deletes it again. Two runs in a row against the same database still fail on
the counts, because documents stay behind; for a clean run, delete `data/rapidxerp.db` first.

## Appearance

The look and feel is named in `Sub Main` as `config.Thema`. The server **paints** with it —
every component, into SVG — so it is not a matter of taste but what the browser shows. Pick a
theme in the designer and the line is updated when the form is saved. `flatlaf` and all forty-six
ported IntelliJ themes are in `lib/`.

## Learning it

`docs/RapidXERP-for-beginners.md` walks through the whole program for somebody who has not
written one before: what a module is, what a class is, how a click gets from the browser to a
row in the database and back. It follows the files in the order above.

## What is still missing

* Printing a document, or writing a PDF.
* Editing quantities directly in the line table. The table can do it (`EditableColumns` in the
  designer); the application does not read the edits back yet. Until then: remove the line and add
  it again with the right quantity.
* User administration in the interface. Users live in the table `users`, their passwords hashed.
* A header and footer text per document, discounts, part deliveries.
