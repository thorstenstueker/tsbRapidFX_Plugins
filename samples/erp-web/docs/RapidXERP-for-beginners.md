# RapidX ERP — the code explained

A guide for people who are new to programming

This document walks through a complete, working business application line by line. It assumes you have never written a program before. Nothing is skipped as "obvious", and every word that is jargon is explained the first time it appears.

The application is called **RapidX ERP**. It keeps customers and articles, and it writes quotations, delivery notes and invoices. It runs in a browser. It is about two and a half thousand lines of code, which is small enough to read all of in an afternoon and large enough to be a real program rather than an example.

Read it with the source files open beside you. Every section says which file it is talking about.

### Contents

- **Part 1** — Getting it running
- **Part 2** — The language in twenty minutes
- **Part 3** — The shape of the program
- **Part 4** — The files, one at a time
- **Part 5** — One click, all the way through
- **Part 6** — What is different about a browser
- **Part 7** — The tools folder
- **Part 8** — Words you will meet
- **Part 9** — Things to try

## Part 1 — Getting it running

### What you need

Two things, and you probably have one of them already.

- **A Java runtime**, version 17 or newer. Type `java -version` in a terminal. If you get a version number back, you have it.
- **The RapidFX compiler**, `rfxc-0.1.0.jar`. The build script looks for it where it lies in this repository, under `compiler/cli/build/libs/`. If yours lives elsewhere, set the environment variable `RFXC` to its path.

For the last part of this guide you also want **Python 3**, which macOS and Linux already have. It is used only for the test script.

### Building it

Open a terminal in the project folder and type:

```bash
sh tools/build.sh
```

Three things happen, and the script prints a heading before each one.

- **generating the forms** — the eight screens of the application are written out afresh.
- **designer round trip** — each screen is read back in and compared with what was written.
- **compiling** — the compiler turns all the `.rfx` files into one file, `build/RapidXERP.jar`.

Part 7 explains why the first two steps exist. For now it is enough that they end with `0 with differences` and a line saying the jar was written.

### Running it

```bash
java -jar build/RapidXERP.jar
```

The program prints where its database is and what address it is listening on. Open `http://localhost:8099/` in a browser and sign in as `admin` with the password `secret`.

`localhost` means "this computer". Nobody else on the network is being served — the program is talking only to you.

### Where the data goes

Everything the program remembers lives in a single file: `data/rapidxerp.db`. It is a **SQLite** database, which is a whole database inside one ordinary file, with no separate database program to install.

The first time the program starts, that file does not exist. The program creates it, creates the tables inside it, and fills them with eight customers and twelve articles so that there is something to look at. If you ever want to start over, delete the file and start the program again.

## Part 2 — The language in twenty minutes

The program is written in **RapidFX**. If you have seen Visual Basic, it will look familiar; if you have not, this section is all the language you need to read the rest of this guide.

### Comments

A single quotation mark begins a comment. Everything after it on that line is for humans and is ignored by the computer.

```basic
' This whole line is a comment.
Dim x As Integer = 3    ' and so is this part
```

This program has a great many comments. That is deliberate: they explain **why** something was done, which the code itself can never say.

### Values and variables

A **variable** is a named box that holds a value. You make one with `Dim`, and you say what kind of value it may hold with `As`.

```basic
Dim count As Integer = 8
Dim price As Double = 54.90
Dim name As String = "Miller Electrical Systems GmbH"
Dim finished As Boolean = False
```

The four kinds above cover almost everything in this program:

- `Integer` — a whole number: 0, 8, -3.
- `Double` — a number that may have a fractional part: 54.9, 0.005.
- `String` — a piece of text, written between double quotation marks.
- `Boolean` — either `True` or `False`, and nothing else.

Saying `As Integer` is not bureaucracy. It lets the compiler catch a whole class of mistake before the program ever runs: if you try to put a piece of text into a box declared `As Integer`, you are told about it at build time rather than by a crash in front of a customer.

`Nothing` is the value of a box that holds no object at all. Asking `If x Is Nothing Then` is how you check for it — note `Is`, not `=`.

### Doing things once: Sub and Function

A **Sub** is a named block of instructions. You call it by name, it does its work, and that is that.

```basic
Public Sub Show(name As String)
    Screen = name
    tsbWebSession.rebuild()
End Sub
```

That is a real one, from `Main.rfx`. The part in brackets, `name As String`, is a **parameter** — a value the caller hands in. Somewhere else in the program a line reads `Main.Show("customers")`, and inside the Sub, `name` holds `"customers"`.

A **Function** is the same thing except that it hands a value back, with `Return`. It says what kind of value with `As` at the end of its first line.

```basic
Public Function Gross() As Double
    Return Net() + Tax()
End Function
```

`Public` means anything in the program may call it. `Private` means only the file it sits in may. Marking things `Private` where you can is a kindness to whoever reads the code next: it tells them "you do not have to understand this to use the rest".

### Making decisions

```basic
If cust.Number = "" Then
    lblHint.Text = "A customer number is required."
    Exit Sub
End If
```

`Exit Sub` means "stop this Sub here and go back to whoever called it". Used like this, it is a **guard**: check the thing that must be true, and leave at once if it is not. It saves the rest of the Sub from being written inside an ever deeper stack of `If`s.

When one value is being compared against several possibilities, `Select Case` reads better:

```
Select Case kind
    Case Document.QUOTATION
        Return "QU"
    Case Document.DELIVERYNOTE
        Return "DN"
    Case Document.INVOICE
        Return "IN"
End Select
Return "DO"
```

Two words for combining conditions: `AndAlso` (both must be true) and `OrElse` (either will do).

### Doing things again

Three shapes of loop appear in this program.

```basic
Dim i As Integer
For i = 1 To 5
    b.setString(i, pattern)
Next i
```

Count from one number to another. Here it fills in five identical search values.

```basic
Dim entry As Object
For Each entry In hits
    Dim k As Customer = CType(entry, Customer)
    ...
Next entry
```

Walk through everything in a list, one item at a time. This is the loop you will see most often.

```
Do While r.next()
    hits.add(ReadRow(r))
Loop
```

Keep going as long as something is true. Here: keep reading as long as the database has another row.

### Long lines

A line of RapidFX ends where the line ends. When a line is too long to read, an underscore at the very end says "this carries on below":

```
tblCustomers.AddRow(k.Number, k.Name, k.Contact, k.CityLine(), _
                    k.Phone, k.EMail)
```

The `_` is not part of the instruction. It is only a note to the compiler to keep reading.

### Modules and classes

These are the two ways this program groups code, and the difference between them matters.

A **Module** is a box of functions with no data of its own. There is exactly one of each, always, everywhere. `Util`, `Database`, `CustomerFile` are modules. You call into them by name: `Util.Money(1234.5)`.

A **Class** is a blueprint for making objects. From the class `Customer` you can make as many customers as you like, and each one has its own name, its own street, its own phone number.

```basic
Dim k As Customer = New Customer()
k.Name = "Miller Electrical Systems GmbH"
```

`New Customer()` makes one. The values a fresh one starts with are set in its constructor — a special Sub called `New`, which the language runs for you whenever an object is made.

Inside a class, `Public Property Name As String` declares a piece of data every object of that class carries.

### Tidying up after yourself: Using

Some things must be given back when you have finished with them — a connection to a database is the example in this program. `Using` guarantees it:

```
Using c As Connection = Database.Connect()
    ...
End Using
```

Whatever happens inside — including something going wrong — the connection is closed when `End Using` is reached. Forgetting to close things by hand is one of the classic ways a program that worked all morning stops working in the afternoon.

### When something goes wrong: Try

```
Try
    Return Double.parseDouble(clean)
Catch e As java.lang.NumberFormatException
    Return 0
End Try
```

Run the first part. If it fails in the particular way named after `Catch`, run the second part instead of stopping the program. Here: if what the user typed is not a number, treat it as zero.

### Reaching into Java

RapidFX runs on the Java platform, so it can use anything Java can. `Imports java.sql` at the top of a file says "I am going to use the database classes"; after that you can write `Connection` instead of `java.sql.Connection`. When you see a call in lower case with brackets — `r.getString(1)`, `folder.mkdirs()` — you are looking at Java underneath.

## Part 3 — The shape of the program

Before reading any single file, it is worth seeing how they stand in relation to each other. There are four layers, and information flows up and down between them but never sideways.

```
   the screens          SignInForm  MainForm
                        CustomerListForm  CustomerSheetForm
                        ArticleListForm   ArticleSheetForm
                        DocumentListForm  DocumentSheetForm
              |
              |  ask for objects, hand back objects
              v
   the files            CustomerFile   ArticleFile   DocumentFile
                        (everything that is SQL, and nothing else)
              |
              |  read and write rows
              v
   the database         Database  ->  data/rapidxerp.db

   carried between them:   Customer   Article   Document / DocumentLine
   used by everybody:      Util  (numbers, money, dates)
   held for each user:     Main  (the SessionStatics)
```

The rule that gives the program its shape is this: **the word SELECT appears only in the three file modules.** A screen never writes SQL. It says `CustomerFile.Search("meyer")` and gets customers back. It has no idea whether that search is a `LIKE` in SQLite, a full-text index or a call to a server in another country — and because it has no idea, that can be changed one day without touching a single screen.

The same rule the other way round: `CustomerFile` never touches a button, a label or a table on screen. It deals in `Customer` objects and nothing else.

The objects in the middle — `Customer`, `Article`, `Document` — are what the two halves have in common. They know about neither SQL nor Swing. That is what makes them safe to carry anywhere.

## Part 4 — The files, one at a time

### Main.rfx — where everything begins

Every program has one place where it starts. Here it is `Sub Main`.

```basic
Sub Main()
    Database.Start()

    Dim config As tsbWebConfig = New tsbWebConfig()
    config.Port = 8099
    config.Titel = "RapidX ERP"
    config.Thema = "FlatLightLaf"
    config.LeerlaufMinuten = 30
    ...
    tsbWebStart.Go(config, New Application(), auth)
End Sub
```

Read it downwards. First the database is made ready. Then a settings object is filled in: which port to listen on, what to call the window, which look and feel to paint with, and how long a session may sit idle before it is thrown away. Finally the server is started and never returns — from here on the program only reacts to what people do.

> Some names here are German — `Titel`, `Thema`, `LeerlaufMinuten`. They belong to the tsbWEB library in `lib/`, not to this project, which is why they were not translated with the rest.

The order of the first two statements matters. `Database.Start()` comes before the server, because the moment the server is up somebody can sign in, and signing in reads a table. Start them the other way round and the first user meets a database that does not exist yet.

#### SessionStatic — the idea that makes this a web program

Above `Sub Main` sits a list of declarations that look ordinary and are not:

```
SessionStatic Screen As String
SessionStatic CustomerId As Integer
SessionStatic Draft As Document
SessionStatic Message As String
```

Here is the problem they solve. Many people use this program at once. Each has their own browser, their own screen, their own half-finished quotation. But the program is one program, running once, in one computer's memory.

An ordinary shared variable would be shared by everyone. The second person to sign in would see the first person's customer on their screen. That is not an inconvenience; it is a data leak.

A `SessionStatic` looks like a shared variable and behaves like a private one. You write `Main.Screen = "customers"` from anywhere, with no parameter passing, no plumbing — and yet every signed-in person has their own value. The server keeps one set per **session**, and a session is one person's visit.

The comment in the file explains why they are all declared here and nowhere else: the compiler insists on it (it will tell you `RFX0208` if you try elsewhere), and it means that everything one user privately owns is on a single screen of text.

`Draft As Document` is the one to understand properly. It holds a whole document — head, lines, totals — while somebody is writing it, before it has ever been saved. Part 5 follows it through a click.

#### The navigation

There is no address bar in this program, no `/customers` and `/invoices`. Changing screen is two lines:

```basic
Public Sub Show(name As String)
    Screen = name
    tsbWebSession.rebuild()
End Sub
```

Write which screen you want into the session, then ask for a rebuild. What "rebuild" does is covered at the end of this file:

```basic
Public Function createRoot(session As tsbWebSession) As Component
    If session.SignedIn Then
        Return New MainForm().GetRootPane()
    End If
    Return New SignInForm().GetRootPane()
End Function
```

The server calls this whenever the display has to be built: once when somebody arrives, and again after every `rebuild()`. It makes one decision — signed in or not — and hands back a screen.

Notice `New MainForm()`, every single time. Never a stored one. A form kept between rebuilds would be state living outside the session, and on a server that is exactly the state that leaks to the next person.

### Util.rfx — the small conversions

Numbers and dates travel constantly between three places that each want them differently: what a person types, what the database stores, and what the screen shows. Every one of those conversions has one right answer, and writing it out again in each form would mean eight chances to get it slightly wrong.

```basic
Public Function ToNumber(text As String) As Double
    If text Is Nothing Then
        Return 0
    End If
    Dim clean As String = Replace(Trim(text), ",", ".")
    clean = Replace(clean, " ", "")
    ...
```

`Trim` removes spaces from the ends. Then a comma is dealt with either way — `1,234.50` has its thousands separator removed, `12,50` has its comma read as the decimal point, and then any remaining spaces go. If what is left still is not a number, the `Try` block returns 0.

That leniency is a decision, not laziness. A quantity field with a stray space in it must not produce an error box. The worst that can happen is a quantity of zero, which the user can see and fix; an error dialog is something they cannot.

The date pair is the clearest example of one idea in two functions:

```basic
Public Function DateShown(iso As String) As String
    Return Mid(iso, 8, 2) + "." + Mid(iso, 5, 2) + "." + Left(iso, 4)
End Function
```

`DateShown` hands the date on as it stands; `DateToIso` reads one back. The database always holds the ISO form, because that is what SQLite can sort and compare correctly. The screen shows the same form, and that is a change from how this program started: it used to show `08.09.2026`. A date written that way is the eighth of September to one reader and the ninth of August to the next, and this program is read in more than one country. `DateToIso` still understands the old spelling, because somebody who has typed it for twenty years will type it again.

### Customer.rfx and Article.rfx — the data itself

These are the simplest files in the program, and worth reading first if the others feel dense.

```basic
Public Class Customer

    Public Property Id As Integer
    Public Property Number As String
    Public Property Name As String
    ...

    Public Sub New()
        Id = 0
        Number = ""
        ...
        Country = "Germany"
    End Sub
```

A list of what a customer is, and a constructor that gives a brand-new one sensible starting values. `Id = 0` is the convention this whole program uses for "this has never been saved". You will see `If cust.Id = 0 Then` in several places, and it always means the same thing: is this new, or does it already exist in the database?

Two functions do a little more:

```basic
Public Overrides Function toString() As String
    If Number = "" Then
        Return Name
    End If
    Return Number + "  " + Name
End Function
```

`toString` is a name the platform knows: whenever something needs to show an object as text, this is what it calls. That fact is put to work in the document sheet, where the customer drop-down holds actual `Customer` objects rather than strings. The drop-down displays `toString`, so the user sees `K-1003` Southgate Building Centre KG — and what comes back out when they choose is a whole `Customer`, not a line of text somebody has to look up again.

`Article` is the same shape with different fields. One comment in it is worth reading twice: `Price` is always the **net** price per unit, and `Vat` is a percentage — 19, not 0.19. Getting that wrong by a factor of a hundred is a mistake every business program makes once.

### Document.rfx — a document that adds up

This file holds two classes: `Document` (the whole thing) and `DocumentLine` (one row on it).

The first decision is at the top:

```basic
Public Const QUOTATION As String = "Quotation"
Public Const DELIVERYNOTE As String = "Delivery note"
Public Const INVOICE As String = "Invoice"
```

A quotation, a delivery note and an invoice are **not** three classes. They are one class with a different `Kind`. `Const` means the value can never change while the program runs, and writing `Document.QUOTATION` rather than the bare text `"Quotation"` means a typo becomes a build error instead of a screen that mysteriously shows nothing.

Why one class? Because the central feature of this program is turning one document into another, and with one class that is copying the head and changing one field. With three classes it would be six conversions written out by hand.

The second decision is the arithmetic:

```basic
Public Function Net() As Double
    Dim total As Double = 0
    Dim entry As Object
    For Each entry In Lines
        total = total + CType(entry, DocumentLine).Amount()
    Next entry
    Return total
End Function
```

The total is **not stored anywhere**. It is added up from the lines every time it is asked for. That looks wasteful and is exactly right: a stored total that no longer matches its own rows is the bug every business program has once, and it is found by a customer rather than by a programmer.

`CType(entry, DocumentLine)` is a **cast**. The list hands back plain `Object`s; this says "treat this one as a DocumentLine", which is what lets `.Amount()` be called on it.

`Tax()` does the same walk but multiplies each line's amount by its own VAT rate, because different lines can carry different rates — the sample data has a book at 7 % among items at 19 %. `Gross()` is simply `Net() + Tax()`.

At the bottom, `DocumentLine` repeats fields that the article already has: number, name, unit price, VAT rate. That is not sloppiness, and the comment says why:

> A quotation written in March has to show March's price in December, however much the article has gone up in the meantime.

The line copies the values at the moment it is created and keeps them for ever. `FromArticle` is the single place where an article and a line ever touch.

### Database.rfx — the file, the schema, the first start

#### Finding the file

```basic
Public Function FilePath() As String
    Dim configured As String = Environ("RAPIDXERP_DB")
    If configured <> "" Then
        Return configured
    End If
    Return ProjectRoot() + java.io.File.separator + "data" _
            + java.io.File.separator + "rapidxerp.db"
End Function
```

An **environment variable** wins if it is set — that is how you point the program at a different database without recompiling. Otherwise the file sits in `data/` at the project root.

`ProjectRoot()` climbs up to five folders looking for a file ending in `.rfxproj`. That looks like overkill until you know the bug it prevents: started from the IDE, the program runs in the folder of the `.rfx` file, so a plain relative path would put a second database inside `src/`, and nobody would understand where their customers had gone.

#### One connection at a time

```basic
Public Function Connect() As Connection
    Dim c As Connection = DriverManager.getConnection("jdbc:sqlite:" + FilePath())
```

Every operation in this program opens its own connection and closes it again. Keeping one open for the whole program would be the obvious saving and the wrong one: with many sessions, a shared connection is precisely where two users pull results out from under each other.

Two settings are applied to each new connection:

- `busy_timeout = 5000` — if the file is momentarily locked, wait five seconds rather than give up at once. Two people saving in the same second is normal, not an error.
- `foreign_keys = ON` — SQLite does not enforce relationships between tables unless you ask it to. This is what makes deleting a document delete its lines as well.

#### The tables

`Tables()` creates all six, and every statement says `CREATE TABLE IF NOT EXISTS`, so running it on an existing database changes nothing. That is what lets `Start()` be called every time the program starts without special cases.

```
CREATE TABLE IF NOT EXISTS documentline (
  id INTEGER PRIMARY KEY AUTOINCREMENT,
  document_id INTEGER NOT NULL,
  seq INTEGER NOT NULL,
  ...
  FOREIGN KEY (document_id) REFERENCES document(id) ON DELETE CASCADE)
```

Three pieces of vocabulary in that one statement:

- `PRIMARY KEY AUTOINCREMENT` — every row gets a number nobody has to invent, and no two rows share it. That number is what ends up in the `Id` property.
- `FOREIGN KEY` — this column points at a row in another table. A line belongs to a document.
- `ON DELETE CASCADE` — when the document goes, its lines go with it. Without this, deleting a document would leave its lines behind for ever, belonging to nothing.

#### The sample data

```basic
Private Sub SampleData(c As Connection)
    If RowCount(c, "users") = 0 Then
        AddUser(c, "admin", "Administration", "secret", "admin")
```

Note the `If`. Sample data goes in **only** when the table is empty. A second start must add nothing and overwrite nothing, or every restart would duplicate the catalogue.

The passwords are hashed rather than stored:

```basic
Dim hasher As tsbWebPasswords = New tsbWebPasswords()
b.setString(3, hasher.hash(password))
```

A **hash** is a one-way scramble: it can be checked against a password, but it cannot be turned back into one. Even a demo password is worth hashing, because a database file travels with a project and people reuse passwords.

### CustomerFile.rfx — all the SQL for customers

This is where to look if you want to learn how a program talks to a database. Six public functions, and every one of them the same shape.

#### The search

```basic
Public Function Search(text As String) As ArrayList
    Dim hits As ArrayList = New ArrayList()
    Dim pattern As String = "%" + LCase(Trim(Util.Text(text))) + "%"

    Using c As Connection = Database.Connect()
        Dim b As PreparedStatement = c.prepareStatement( _
                "SELECT id, number, name, contact, street, postcode, city, country, " _
                + "email, phone, vatid, note FROM customer " _
                + "WHERE LOWER(number) LIKE ? OR LOWER(name) LIKE ? " _
                ...
```

Read it in pieces.

`ArrayList` is a list that grows as you add to it. It is what a search hands back.

`pattern` wraps what the user typed in percent signs. In SQL, `%` inside a `LIKE` means "any amount of anything", so `%yacht%` matches a name with `yacht` anywhere in it. `LCase` makes it lower case and the SQL uses `LOWER(...)` on the column, so that searching for `YACHT` and `yacht` find the same thing.

The question marks are the important part. A `PreparedStatement` is a query with holes in it, and the values are supplied separately:

```basic
Dim i As Integer
For i = 1 To 5
    b.setString(i, pattern)
Next i
```

Why not simply glue the user's text into the query text? Because then a customer named `'; DROP TABLE customer; --` would delete your database. That attack is called **SQL injection**, and question-mark placeholders are the cure. There is no place in this program where a value is pasted into SQL as text.

Then the reading:

```basic
Dim r As ResultSet = b.executeQuery()
Do While r.next()
    hits.add(ReadRow(r))
Loop
r.close()
b.close()
```

A `ResultSet` is a cursor over the answer, one row at a time. `r.next()` moves to the next row and reports whether there was one — which is why the loop reads exactly as it does.

`ReadRow` at the bottom of the file does the last step, and it is worth seeing in full because it is where a database row becomes an object:

```basic
Private Function ReadRow(r As ResultSet) As Customer
    Dim k As Customer = New Customer()
    k.Id = r.getInt(1)
    k.Number = Util.Text(r.getString(2))
    k.Name = Util.Text(r.getString(3))
    ...
```

The numbers are column positions in the `SELECT`, counting from 1, in the order they were written. `Util.Text` guards against a column that is empty: the database can hand back `Nothing`, and a text field on screen cannot hold `Nothing`.

> This is why the column list in `SELECT` is written out by hand and not as `SELECT *`. With `*`, the position of a column depends on the order they happen to sit in the table, and adding a column one day would silently shift everything after it.

#### Saving

```basic
Public Function Save(k As Customer) As Integer
    Using c As Connection = Database.Connect()
        If k.Id = 0 Then
            ... INSERT ...
        Else
            ... UPDATE ...
```

One function for both cases, told apart by the rule from earlier: `Id = 0` means new. The caller never has to decide.

After an `INSERT`, the new row's number is fetched and written back into the object:

```basic
Dim s As ResultSet = b.getGeneratedKeys()
If s.next() Then
    k.Id = s.getInt(1)
End If
```

So the object the caller handed in comes back knowing its own id. Saving twice therefore updates rather than inserting a duplicate — which is what you want when somebody presses Save twice.

#### Deleting, and a rule with a reason

```basic
Public Function Delete(id As Integer) As String
    ...
    If documents > 0 Then
        Return "The customer appears on " + documents + " documents and cannot be deleted."
    End If
```

Look at what this function returns: a `String`, not a `Boolean`. Empty means it worked; anything else is a sentence to show the user, written by the code that knows why.

The check itself counts documents belonging to the customer first. An invoice with no recipient is worse than one stale customer on file, so the deletion is refused — and the person is told why, not merely that it did not work.

`ArticleFile.rfx` is deliberately the same file with different columns: `All`, `Search`, `Load`, `Save`, `Delete`, `NextNumber`. Reading it after this one takes two minutes, which is the point.

### DocumentFile.rfx — the two harder ideas

Most of this file is the same shape again. Two things in it are new.

#### Saving a whole document at once

A document is a head plus any number of lines. Saving it means writing both, and the lines might have been added to, removed from and re-ordered since it was loaded.

The method used here is blunt and correct: delete every line the document has in the database, then write the current ones back.

```basic
Dim wipe As PreparedStatement = c.prepareStatement( _
        "DELETE FROM documentline WHERE document_id = ?")
...
Dim ins As PreparedStatement = c.prepareStatement( _
        "INSERT INTO documentline (document_id, seq, ...")
Dim seq As Integer = 0
For Each entry In doc.Lines
    ...
    seq = seq + 1
Next entry
```

Working out which individual rows changed would be three separate cases — deleted, inserted, edited — and whoever forgets the third finds out from an invoice that does not add up. A document has ten lines, not ten thousand; the blunt way costs nothing.

But it does create a danger, and that is the second idea:

```
c.setAutoCommit(False)
Try
    ...
    c.commit()
Catch e As java.lang.Exception
    c.rollback()
    Throw
End Try
```

This is a **transaction**. Normally each statement takes effect the moment it runs. Here that is switched off: everything between here and `commit()` is held provisionally. If it all succeeds, `commit()` makes it real, all at once. If anything at all fails, `rollback()` undoes the lot as if it had never happened.

Without it, a failure halfway through would leave the old lines deleted and the new ones only half written — a document that has silently lost half its value. `Throw` at the end re-raises the original problem so that it is not swallowed in silence.

#### Carrying one document into another

```basic
Public Function CarryOver(source As Document, kind As String) As Document
    Dim fresh As Document = New Document()
    fresh.Kind = kind
    fresh.Number = NextNumber(kind)
    fresh.CustomerId = source.CustomerId
    ...
    fresh.SourceId = source.Id
```

A new document, the same customer, the same subject, a new number of the right kind — and `SourceId` remembering where it came from. Then every line is copied field by field into a brand new `DocumentLine`.

Copying field by field rather than reusing the objects matters. If both documents shared the same line objects, editing the invoice would silently edit the quotation it came from.

Note also what this function does **not** do: it does not save anything. It hands back an object. Whether it ever reaches the database is the user's decision, made by pressing Save.

#### The numbers

```basic
Public Function NextNumber(kind As String) As String
    Dim year As String = Left(Util.TodayIso(), 4)
    Dim prefix As String = ShortCode(kind) + "-" + year + "-"
```

`QU-2026-0001`. The prefix says what it is, the year says when, and the last four digits count up. The function looks for the highest existing number with that prefix and adds one. A new year starts again at 0001, which is what an accountant expects.

### The eight screens

#### Two halves and a banner

Open any of the form files — `CustomerSheetForm.rfx`, say — and scroll. Some way down you meet this:

```
'******************************************************************************
'
'                                 DO NOT EDIT
'                            TSB RAPIDFX WEB LOGIC
'
'******************************************************************************
```

That line of asterisks divides the file in two, and the division is absolute.

**Above it** is code written by a person. Your imports, your class, your button handlers. The designer reads it, keeps it, and writes it back unchanged.

**Below it** is generated. Every component that appears on the screen is declared there, and one long Sub called `InitDesignerComponents` puts them all in place:

```basic
' @tsbswx Button btnSave parent=rootPane L=140 T=356 W=160 H=32
Private btnSave As JButton = New JButton()
...
btnSave.setName("btnSave")
btnSave.setText("Save")
btnSave.addActionListener(AddressOf OnSave)
```

The `' @tsbswx` comments look like comments and are not: they are how the designer reads the file back. Every one says a type, a name, a parent and a position, and together they are the drawing. Editing that half by hand and getting one of them slightly wrong means the screen opens in the designer as an empty canvas — and the next save writes the empty canvas back over your work.

`AddressOf OnSave` is the wiring. It does not call `OnSave`; it hands the button the name of what to call later, when somebody clicks. That is the whole event model of this program: a click happens, and a Sub whose name begins with `On` runs.

#### SignInForm — the smallest one

```basic
Private Sub OnSignIn(e As ActionEvent)
    If tsbWebSession.signIn(txtUser.Text, txtPassword.Password) Then
        Main.Screen = "customers"
        Main.Message = "Welcome to RapidX ERP."
    Else
        lblHint.Text = "Sign-in failed. Please try again."
    End If
End Sub
```

Nine lines, and they contain the whole of signing in. Notice what is missing: no rebuild is asked for on success. The server sees for itself that the session changed identity, issues a new session id and rebuilds — which is a security measure as much as a convenience.

The `e As ActionEvent` parameter is there because that is the shape every handler must have. This one never looks at it.

#### MainForm — the shell, and the whole router

This is the frame everything else sits inside: a title, a "signed in as" line, five navigation buttons, a message line, and an empty panel below.

The entire navigation of the application is this:

```basic
Private Function Content() As Component
    Select Case Main.CurrentScreen()
        Case "customers"
            Return New CustomerListForm().GetRootPane()
        Case "customersheet"
            Return New CustomerSheetForm().GetRootPane()
        ...
    End Select
    Return New CustomerListForm().GetRootPane()
End Function
```

Look at what a person clicking "Articles" actually does. `OnArticles` runs `Main.Show("articles")`, which writes `"articles"` into the session and asks for a rebuild. `createRoot` builds a new `MainForm`. Its constructor calls `Content()`. The `Select Case` matches, and an `ArticleListForm` is built and dropped into the empty panel.

That is all. No routing table, no URLs, no state machine.

The last line — the one after `End Select` — is a **default**. If the session somehow holds a screen name nobody recognises, the customer list is shown rather than a blank panel. A program should always have an answer.

The constructor also does one thing the designer cannot express:

```
pnlContent.setLayout(New BorderLayout())
pnlContent.add(Content(), BorderLayout.CENTER)
```

In the designer, `pnlContent` is an empty panel anchored to all four edges, so you can see where it sits and how it grows. At run time it is given a `BorderLayout`, which means "the one thing inside me fills me completely".

#### The three list screens

`CustomerListForm`, `ArticleListForm` and `DocumentListForm` are the same screen three times over. Read one and you have read all three.

```basic
Private hits As ArrayList = New ArrayList()
```

This one field is the trick that makes the rest simple. The table on screen holds text; this list holds the actual objects, in the same order, row for row. So when the user selects row 4:

```basic
Private Function Chosen() As Customer
    Dim row As Integer = tblCustomers.Table().getSelectedRow()
    If row < 0 OrElse row >= hits.size() Then
        lblHint.Text = "Please select a row first."
        Return Nothing
    End If
    Main.CustomerRow = row
    lblHint.Text = ""
    Return CType(hits.get(row), Customer)
End Function
```

...row 4 is turned straight back into a customer. Nothing has to read the number out of a cell and look it up again.

`Chosen` also handles "nothing is selected" once, for everybody, by showing a hint and returning `Nothing`. Every button that needs a selection then begins the same way:

```basic
Dim k As Customer = Chosen()
If k Is Nothing Then
    Exit Sub
End If
```

Filling the table:

```basic
Private Sub Fill()
    hits = CustomerFile.Search(txtSearch.Text)

    tblCustomers.Clear()
    Dim entry As Object
    For Each entry In hits
        Dim k As Customer = CType(entry, Customer)
        tblCustomers.AddRow(k.Number, k.Name, k.Contact, k.CityLine(), _
                            k.Phone, k.EMail)
    Next entry
    tblCustomers.Reload()
    ...
```

Add every row, and **then** call `Reload()` once. Doing it the other way — reloading after each row — would send a thousand updates to the browser instead of one. This is the only place in the program where performance is thought about at all, and that is about right for a program this size.

`DocumentListForm` has two extras. `SetLabels` decides what the screen currently is, and the carry-over buttons obey the rules of business:

```
btnToDeliveryNote.setVisible((kind = Document.QUOTATION))
btnToInvoice.setVisible((kind <> Document.INVOICE))
```

An invoice becomes nothing further, and a delivery note becomes only an invoice. The rule is expressed once, as visibility, rather than as an error message after the fact.

#### The two sheet screens

A **sheet** shows one thing, all of its fields, with Save and Cancel. `CustomerSheetForm` and `ArticleSheetForm` are the same file twice.

Three Subs do all the work, and their names say the direction of travel.

```basic
Private Function Fetch() As Customer
    If Main.CustomerId = 0 Then
        Dim fresh As Customer = New Customer()
        fresh.Number = CustomerFile.NextNumber()
        Return fresh
    End If
    ...
```

`Fetch` gets the object: a brand-new one with the next free number, or the existing one from the file. Which is wanted is decided by `Main.CustomerId` — a SessionStatic. That is why this form takes no constructor parameter, and why the list screen needs to know nothing about how the sheet is built. It sets a number in the session and asks for a screen.

```basic
Private Sub Display()
    txtNumber.Text = cust.Number
    txtName.Text = cust.Name
    ...
```

`Display` moves the object into the fields. `Collect` moves the fields back into the object:

```basic
Private Sub Collect()
    cust.Number = Trim(txtNumber.Text)
    cust.Name = Trim(txtName.Text)
    ...
```

Note `Trim` on the way in and not on the way out. Trailing spaces are a typing accident; they are removed once, at the point where typed text becomes data.

And then saving is four lines of checking and three of doing:

```basic
Private Sub OnSave(e As ActionEvent)
    Collect()
    If cust.Number = "" Then
        lblHint.Text = "A customer number is required."
        Exit Sub
    End If
    ...
    CustomerFile.Save(cust)
    Main.CustomerId = cust.Id
    Main.ShowWith("customers", "Customer " + cust.Number + " saved.")
End Sub
```

`ShowWith` changes screen and leaves a sentence behind. `MainForm` picks it up with `Main.TakeMessage()`, which returns it and clears it in the same breath — a message is meant to appear once, not to follow you around.

#### DocumentSheetForm — the most interesting screen

This is the one worth reading slowly. It has a head, a table of lines, a way to add and remove lines, running totals, and three buttons.

**The document lives in the session, not in the form.**

```basic
doc = Main.Draft
If doc Is Nothing Then
    doc = New Document()
    doc.Kind = Main.CurrentDocumentKind()
    doc.Number = DocumentFile.NextNumber(doc.Kind)
    Main.Draft = doc
End If
```

Here is why that matters. Every time a line is added, the screen is rebuilt — and a rebuild makes a **new** `DocumentSheetForm`. Anything held in a field of the old one is gone. The document has to survive that, and it does, because it lives in `Main.Draft`.

Notice that `doc` and `Main.Draft` are the same object, not a copy. Changing one changes the other, which is exactly what is wanted here.

**The combo boxes hold objects.**

```basic
Dim none As Customer = New Customer()
none.Number = ""
none.Name = "— please choose a customer —"

customers = New ArrayList()
customers.add(none)
customers.addAll(CustomerFile.All())
```

A fake customer at position zero. Without it, a drop-down selects its first entry by itself, and the first real customer in the file would sit there pre-selected — so a quotation would go to them by accident. An empty line would not do, because an empty line does not read as an instruction.

**The guard against events that are not a person's doing.**

```basic
Private building As Boolean = True
...
Private Sub OnCustomerChosen(e As ActionEvent)
    If building Then
        Exit Sub
    End If
    CollectHead()
End Sub
```

The comment above `building` in the source is the best bug story in this project, and worth reading in full. In short: adding the first item to a drop-down selects it, which fires the "customer chosen" handler, which ran while the box was still being filled and wrote customer number 0 into the document. Carrying a quotation over into an invoice therefore lost its customer. One `Boolean`, set to `False` at the end of the constructor, fixes it.

**Adding a line.**

```basic
Private Sub OnLineAdd(e As ActionEvent)
    Dim chosen As Object = cmbArticle.getSelectedItem()
    If chosen Is Nothing Then
        lblHint.Text = "Please choose an article first."
        Exit Sub
    End If

    Dim quantity As Double = Util.ToNumber(txtQuantity.Text)
    If quantity = 0 Then
        quantity = 1
    End If

    CollectHead()
    doc.AddLine(DocumentLine.FromArticle(CType(chosen, Article), quantity))
    txtQuantity.Text = "1"
    ShowLines()
    lblHint.Text = ""
End Sub
```

`CollectHead()` before adding, because the user may have typed a date or a subject since the last time; those must not be lost. Then the article and quantity become a line, the quantity box resets to 1 ready for the next one, and the table is redrawn.

Nothing has touched the database. The document is still only a draft.

**Saving, and converting.**

```
DocumentFile.Save(doc)
Main.DocumentId = doc.Id
Main.DocumentKind = doc.Kind
Main.Draft = Nothing
```

`Main.Draft = Nothing` is the tidy-up. The draft has become a real saved document; leaving it in the session would mean the next New showed the old one.

`OnConvert` saves first and then carries over, and the comment explains the ordering: an invoice whose quotation never existed would be hard to explain to anybody.

## Part 5 — One click, all the way through

Take one action — a user adds a line to a quotation — and follow it end to end. Everything below has already been described; this is only the order it happens in.

- **In the browser.** The user has chosen an article and typed `10`, and clicks **Add line**. The browser sends one small message: the id of that button, and the word `click`.
- **In the server.** The session is found from the cookie. The button with that id is located, and whatever was registered with `AddressOf` is run — `OnLineAdd`, on this session's own thread.
- **In OnLineAdd.** `cmbArticle.getSelectedItem()` returns an `Article` object, because that is what was put into the box. `Util.ToNumber("10")` gives `10`. `CollectHead()` copies the date and subject from the screen into the document.
- **In DocumentLine.FromArticle.** A new line is made, and the article's number, name, unit, price and VAT rate are copied onto it. From this moment the line no longer depends on the article: the price is frozen.
- **In Document.AddLine.** The line is appended to `doc.Lines`. Because `doc` **is** `Main.Draft`, the session's document has grown by one line.
- **In ShowLines.** The table is cleared and refilled from `doc`. For each line, `Util.QuantityText` and `Util.Money` turn numbers into the text a German user expects. Then `Totals()` asks the document for `Net()`, `Tax()` and `Gross()` — each of which walks the lines and adds up, right now, from what is actually there.
- **Back to the browser.** The server compares the screen with what the browser last saw and sends only the differences: some new table rows and three changed labels.
- **In the database.** Nothing. Not one byte. The document reaches SQLite when, and only when, somebody presses Save.

That last point is the one to keep. A draft that lives in the session costs nothing, can be abandoned by clicking Back, and never leaves a half-finished document lying in the database for somebody to find next year.

## Part 6 — What is different about a browser

This program is a Swing program. The same forms would run on a desktop in a window. Three things had to be done differently, and they are the three things worth taking away from the whole project.

### There is no such thing as a shared variable

On a desktop there is one user, so a variable shared by the whole program is harmless. On a server it is a leak: the second person to sign in sees the first one's data.

Everything one user privately owns is a `SessionStatic`, all of them declared in `Main.rfx`. The compiler enforces it. `tools/probe.py` proves it, at the end of its run, by driving two sessions at once: two people each start a quotation, each adds a line, and each still sees only their own.

### There is no router

Web frameworks usually have a table of addresses mapped to code. This program has a string in the session and a `Select Case`:

```
Main.Screen = "customers"
tsbWebSession.rebuild()
```

That is the entire navigation layer. It is worth noticing what it costs: there are no bookmarkable addresses, and the back button does not move between screens. For an application used all day by people who are signed in, that trade is usually the right one.

### There is no window

On a desktop a form is shown in a `JFrame`. Here the browser is the window, so a form is handed over as a panel:

```
Return New MainForm().GetRootPane()
```

This is why the generator sets the target to `WEB` before anything else — a web form must not carry a `ShowIn(JFrame)`, because creating a window on a server with no display fails.

### And one thing that is not what it looks like

```
config.Thema = "FlatLightLaf"
```

That looks like a preference. It is not. The server **paints** every component with that look and feel, into pictures, and sends the pictures. There is no CSS underneath deciding what a button looks like. That line is what the browser shows.

## Part 7 — The tools folder

Four files, none of them part of the running program, all of them worth knowing about.

### CheckForms.java — the round trip

Every screen in `src/` is a designer form and nothing generates it. `CheckForms` is what makes that
claim checkable: it reads each one with the designer's own parser, writes it out again in memory
and compares the two. If the same text comes back, the designer can open the file and a save
changes no line.

It writes nothing to disk. That matters more than it sounds: there used to be a `GenerateForms`
here that wrote every screen from scratch, taking the half above the banner out of
`tools/handwritten/*.txt` — so a screen you had just edited in the designer was replaced by the
old copy the next time somebody built, without a word. The round trip could not notice, because it
was comparing the freshly written file with itself. Both the generator and the copies are gone.

### CheckForms.java — the round trip

The one promise this application makes to the designer, tested in three words: **read, write, compare**.

Every form is read with the designer's parser, written out again with the designer's writer, and compared with the original. If the same file comes out, then opening the screen in the designer and saving it changes nothing. If it does not, the build stops and the first differing line is printed.

### driver.py — working the application without a browser

The application talks to its browser over two channels: a stream of updates going out, and one small POST per event coming back. `driver.py` speaks exactly the same protocol.

Because the messages carry a type, a position and a text but no field names, it finds things the way a person points at a screen:

```
s.click("Save")
s.type_in(s.at("JTextField", 0, 40), "yacht")
```

By what a button says, or by where a field sits.

### probe.py — one round through everything

The test. It signs in, searches, creates a customer, edits it, creates an article, writes a quotation with two lines, checks the arithmetic, converts it to an invoice and to a delivery note, tries to delete a customer who has documents, tidies up, and finishes by running two sessions side by side.

Forty-seven checks, and each one looks at what is on the screen afterwards:

```
check(has(s, "Total: 1,405.39 EUR"), "gross total 1,405.39 EUR")
```

That is the difference between "it did not crash" and "it is right".

```bash
java -jar build/RapidXERP.jar &
python3 tools/probe.py
```

It leaves documents behind, so two runs against the same database will fail on the counts. Delete `data/rapidxerp.db` for a clean run.

## Part 8 — Words you will meet

- **Cast** — telling the compiler to treat a value as a more specific type. `CType(entry, Customer)`.
- **Class** — a blueprint for objects. `Customer` is a class; a particular customer is an object.
- **Commit / rollback** — make a group of database changes real, or undo all of them.
- **Constructor** — the `Sub New()` that runs when an object is made.
- **Cursor** — a position in a set of database rows. `ResultSet` is one; `r.next()` moves it.
- **Environment variable** — a setting outside the program, read with `Environ(...)`.
- **Foreign key** — a column pointing at a row in another table.
- **Handler** — a Sub that runs when something happens. Here they all begin with `On`.
- **Hash** — a one-way scramble of a password. Checkable, not reversible.
- **Instance** — one particular object made from a class.
- **Module** — a box of functions with no data of its own, of which there is exactly one.
- **Parameter** — a value handed to a Sub or Function when it is called.
- **PreparedStatement** — a SQL query with `?` holes, filled in separately. The cure for injection.
- **Primary key** — the column that identifies a row uniquely. Always `id` here.
- **Property** — a named piece of data belonging to an object.
- **ResultSet** — the answer to a query, read one row at a time.
- **Session** — one person's visit. Each has its own `SessionStatic` values.
- **SessionStatic** — a variable reachable from anywhere that still has a value of its own per session.
- **SQL** — the language databases are asked questions in.
- **SQL injection** — an attack where typed text is treated as part of a query. Prevented by `?`.
- **SQLite** — a whole database inside one ordinary file.
- **Swing** — the Java toolkit these screens are built from.
- **Transaction** — a group of database changes that all happen or none do.

## Part 9 — Things to try

In roughly increasing order of difficulty. Each one is genuinely doable, and each one teaches something specific.

- **Change a label.** Open `ArticleSheetForm.rfx` in the designer, click the label that says `Net price`, change it and save. Then look at the diff: one line below the banner moved, and nothing above it did.
- **Change the theme.** In `Sub Main`, replace `"FlatLightLaf"` with `"FlatDarkLaf"`, rebuild and reload the browser. All forty-six ported IntelliJ themes are in `lib/`.
- **Add a field to the article.** A `weight` column. You will need to touch, in this order: `Database.rfx` (the `CREATE TABLE` and the `INSERT`), `Article.rfx` (the property and the constructor), `ArticleFile.rfx` (four SQL statements plus `ReadRow` and `BindRow`), and `ArticleSheetForm.rfx` (a field in the designer, then `Display` and `Collect` above the banner). Following that chain once teaches the layering better than any diagram.
- **Show the totals on the document list.** Look at how `DocumentListForm.Fill` already calls `d.Net()` and `d.Gross()`, and add a footer line with the sum of all of them.
- **Add a status.** Documents have a `Status` column that is always `"open"`. Give the document sheet a drop-down with `open`, `sent` and `paid`, and show it in the list.
- **Make the delete rule kinder.** Instead of refusing to delete a customer with documents, offer to show the documents. `CustomerFile.Delete` already knows how many there are.
- **Write a test for something.** Copy a block out of `tools/probe.py` and add a check of your own. If it fails, you have found a bug; if it passes, you have kept one out.

The best way to understand a program is to change it and see what breaks. Everything here is covered by `probe.py`, so you will find out quickly.
