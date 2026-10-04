# RapidX ERP Desktop

A small business application in a window of its own: customers, articles, quotations, delivery
notes and invoices in a SQLite database. Written in **RapidFX**, every screen drawn in
**tsbDesignerSWX**, shown in one `JFrame`.

This is the desktop twin of **RapidXERP**, which does the same work in a browser, and of **ERP
Mobile**, which does it on a phone. How much of it is really shared is worth measuring rather than
claiming, so:

**Desktop and web share their data layer byte for byte** — `Article`, `ArticleFile`,
`CustomerFile`, `DocumentFile`, `Document` and `Util` are the same files, and only `Customer` and
`Database` differ at all. What differs between the two is everything the server has to do
instead of a window.

**The phone is close but not identical**, and every place it parts company has a reason worth
knowing. The records differ by two lines. The file classes differ by ten or twelve, all for one
cause: JDBC's generated keys are an *optional* feature, and iOS declines them — MobiVM's built-in
javasqlite answers `SQLFeatureNotSupportedException: generated keys not supported`, with the flag
and without it. So the phone asks `last_insert_rowid()` through `Database.LastId`, which every
SQLite driver has. `Database` and `Util` are half rewritten, one because the file lives in a
directory the system chose and the other because the desktop's locale machinery is not there.

**That is the honest shape of "write once"**: the screens are drawn three times, the data layer
travels nearly whole, and the handful of lines that do not travel are each a decision somebody
can read.

The program is English throughout — identifiers, screen texts, comments, the database schema and
every word of the sample data. It is the project people are shown first, so there is nothing on
screen that has to be translated for a reader.

Amounts read `1,405.39 EUR` and dates `2026-09-08`, both written with an explicit English locale
rather than the machine's own: a demonstration that says `1.405,39` on one laptop and `1,405.39` on
the next is a demonstration of the wrong thing. The company is in Germany — the euro and 19 % VAT
are its own, not a translation problem.

## Running it

```bash
sh tools/build.sh                     # generate the forms, check them, compile
java -jar build/RapidXERPDesktop.jar
```

| User | Password | Role |
|---|---|---|
| `admin` | `secret` | admin |
| `anna` | `secret` | sales |

In the IDE: right-click `src/Main.rfx` → **Run**, or the arrow beside `Sub Main`.

> `RapidXERPDesktop.iml` and `.idea/modules.xml` belong to the project and have to be kept. They
> are what the plugin reads to find the jars: it asks the IDE for the class path of the module the
> `.rfx` file sits in. With no module there is no class path, and every `Imports com.formdev.flatlaf`
> is underlined as unknown. If that happens, the module was lost — **File ▸ Invalidate Caches**.

The database lives in `data/rapidxerp.db` and is created on the first start and filled with
eight customers and twelve articles. Somewhere else: set the environment variable
`RAPIDXERP_DB`. Delete the file and the next start begins again from scratch.

## What it does

Everything the web version does:

* **Customers** and **articles**: search, create, change, delete. The search covers every field
  at once — a customer number, a town or a piece of an e-mail address all lead to the same place.
* **Writing a quotation**: choose a customer, an article and a quantity, add the line. Net, VAT
  and gross stand underneath immediately.
* **Carrying over**: a quotation becomes a delivery note or an invoice straight away, a delivery
  note becomes an invoice. Head and lines are copied, a new number is issued, and the origin
  stays on the new document.
* **Document numbers** are issued by the program: `QU-2026-0001`, `DN-2026-0001`, `IN-2026-0001`.
* Two rules refuse something and say why: a customer who appears on a document is not deleted,
  and neither is an article that appears on a document line.

And the things that only a window can:

* A **menu bar** — File ▸ Sign out / Exit, Go ▸ the five screens with `Ctrl`/`Cmd 1…5`,
  View ▸ Light / Dark / IntelliJ / Darcula, Help ▸ About.
* The **theme changes while the program runs**, and is remembered for next time.
* The **window remembers its size and place**, unless that place is on a screen that is no longer
  attached.
* The screens **stay alive**. Leave the customer list, come back, and your search is still there —
  because it is the same form, not a new one.

## What the server used to do, and what took its place

| | web | desktop |
|---|---|---|
| where a user's state lives | `SessionStatic` in `Main.rfx` | fields of `MainWindow` |
| changing screen | a string in the session, then `rebuild()` | `shell.ShowCustomerSheet(17)` |
| the display after a click | built again from scratch | one panel swapped |
| the document being written | had to live in the session | a field of `DocumentSheetForm` |
| signing in | `tsbWebJdbcAuth` | `UserFile.SignIn` and `Passwords` |
| confirmations | `tsbWebOptionPane`, buttons `Ja` / `Nein` | `JOptionPane`, buttons set to `Yes` / `No` |
| the look and feel | fixed in `Sub Main`, painted by the server | the View menu, changed while it runs |

The one place this shows most plainly is `DocumentSheetForm`. In the browser, adding a line
rebuilt the whole display, so the document being written could not be a field of the form — it
had to be put in the session. Here the form stays on screen while it is being worked on, and the
document is an ordinary field. The paragraph in the web version explaining why it could not be
one is simply gone.

## The files

```
RapidXERPDesktop.rfxproj  the project file — sources and libraries
RapidXERPDesktop.iml      the IntelliJ module: src as source folder, lib/*.jar attached
.idea/modules.xml         registers that module with the project
data/rapidxerp.db         the database (created on first start)

src/
  Main.rfx                Sub Main: the look, the database, one JFrame
  Look.rfx                the four themes, and the two steps a change takes
  Settings.rfx            what is remembered between two starts
  WindowSaver.rfx         a WindowListener with one method that does anything

  Database.rfx            the connection, the tables, the sample data
  Passwords.rfx           PBKDF2: hashing a password and checking one
  Util.rfx                numbers, money, dates

  Customer.rfx            the data
  Article.rfx
  Document.rfx            Document and DocumentLine; a document does its own arithmetic
  User.rfx

  CustomerFile.rfx        all the SQL, and only here
  ArticleFile.rfx
  DocumentFile.rfx
  UserFile.rfx

  SignInForm.rfx          ── eight screens, drawn in the designer ──
  MainWindow.rfx          the shell: menu bar, header, navigation, content area
  CustomerListForm.rfx    CustomerSheetForm.rfx
  ArticleListForm.rfx     ArticleSheetForm.rfx
  DocumentListForm.rfx    DocumentSheetForm.rfx

tools/
  CheckForms.java         reads every screen with the designer's parser and compares
  build.sh                round trip, then compile
  SelfTest.java           one round through the whole program
  selftest.sh             runs it, on a database of its own
  Shot.java shot.sh       paints the window into a PNG, without showing it
```

`src/Util.rfx`, `Customer.rfx`, `Article.rfx`, `Document.rfx`, `CustomerFile.rfx`,
`ArticleFile.rfx` and `DocumentFile.rfx` are the same files as in the web project, apart from two
comments. That is not thrift: those seven know nothing about screens, so there was nothing in
them for the move to change.

## The database

```
users(login, displayname, password)      userrole(login, role)
customer(id, number, name, contact, street, postcode, city, country,
         email, phone, vatid, note)
article(id, number, name, description, unit, price, vat, stock)
document(id, kind, number, customer_id, docdate, status, subject, source_id)
documentline(id, document_id, seq, article_id, number, name,
             quantity, unit, unitprice, vat)
```

The same schema as the web version, so the two programs can be pointed at one and the same file.
`password` holds a PBKDF2 hash written by `Passwords.Hash`, not a password.

## Changing the screens in the designer

Right-click a form → **Open in Designer**. All eight are written with the designer's own
generator, so they carry exactly the `' @tsbswx` lines its parser expects.

Everything **above** the `DO NOT EDIT` banner is yours and is written back unchanged; everything
below it belongs to the designer and is produced afresh on every save.

Two of the eight — `SignInForm` and `MainWindow` — carry a generated `ShowIn(JFrame)`, because
they take turns owning the one window this program has. The other six are handed over as panels
and have none; that is `showIn=false` on the `' @tsbswx form` line of each of them.

> **`src/` is the source, and nothing generates it.** It used to: `tools/GenerateForms.java` wrote
> every screen from scratch and took the half above the banner out of `tools/handwritten/*.txt`,
> so a screen edited in the designer was quietly replaced by the old copy the next time the build
> ran. Both are gone. A screen is an ordinary form now — open it, change it, save it.

## Checking it

```bash
sh tools/build.sh
sh tools/selftest.sh
```

Ninety-eight checks, on a temporary database of its own, so it never touches `data/`.

The first half works the data layer directly: the sample data, password hashing, signing in,
searching, the arithmetic of a document, that a price on a line is frozen, carrying over, and the
two deletion rules.

The second half builds the real forms and clicks their real buttons with `doClick()` — a search,
a new customer, a refused save, a price with a comma, a quotation with two lines, carrying it
over, and the navigation. It reads the results back out of the components themselves.

Not covered: the two confirmation dialogs. `JOptionPane` stops and waits for an answer, and a
test that has to answer its own dialogs is testing the JDK. The rule underneath — a customer with
documents is not deleted — is checked where it is written.

## Learning it

`docs/RapidXERPDesktop-manual.md` explains the whole program for somebody who has not written
one before, and has a chapter on exactly what changes when a web application becomes a desktop
one.

## What is still missing

* Printing a document, or writing a PDF. On the desktop this is the obvious next thing:
  `JTable` can print itself in four lines.
* Editing quantities directly in the line table. The table can do it (`EditableColumns` in the
  designer); the application does not read the edits back yet.
* User administration in the interface. Users live in the table `users`, their passwords hashed.
* A header and footer text per document, discounts, part deliveries.
