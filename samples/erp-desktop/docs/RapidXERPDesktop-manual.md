# RapidX ERP Desktop — the code explained

A manual for people who are new to programming

This document walks through a complete, working business application line by line. It assumes you have never written a program before. Nothing is skipped as "obvious", and every word that is jargon is explained the first time it appears.

The application is called **RapidX ERP Desktop**. It keeps customers and articles, and it writes quotations, delivery notes and invoices. It runs in a window on your own computer. It is about three thousand lines of code, which is small enough to read all of in an afternoon and large enough to be a real program rather than an example.

There is a second version of the same program that runs in a browser instead. Part 7 of this manual sets the two side by side, because the differences between them are the clearest explanation of what a web framework actually does for you — and of what it costs.

Read this with the source files open beside you. Every section says which file it is about.

### Contents

- **Part 1** — Getting it running
- **Part 2** — The language in twenty minutes
- **Part 3** — The shape of the program
- **Part 4** — The files, one at a time
- **Part 5** — The screens
- **Part 6** — One click, all the way through
- **Part 7** — From the browser to the desktop
- **Part 8** — The tools folder
- **Part 9** — Words you will meet
- **Part 10** — Things to try

## Part 1 — Getting it running

### What you need

- **A Java runtime**, version 17 or newer. Type `java -version` in a terminal. If a version number comes back, you have it.
- **The RapidFX compiler**, `rfxc-0.1.0.jar`. The build script looks for it where it lies in this repository, under `compiler/cli/build/libs/`. If yours lives elsewhere, set the environment variable `RFXC` to its path.

Nothing else. No server, no browser, no database program: the whole database is one file, and the library that reads it travels in `lib/`.

### Building it

```bash
sh tools/build.sh
```

Three things happen, and the script prints a heading before each.

- **generating the forms** — the eight screens are written out afresh.
- **designer round trip** — each screen is read back in and compared with what was written.
- **compiling** — the compiler turns every `.rfx` file into one `build/RapidXERPDesktop.jar`.

Part 8 explains why the first two steps exist. For now it is enough that the second ends with `0 with differences`.

### Running it

```bash
java -jar build/RapidXERPDesktop.jar
```

A window opens asking you to sign in. Use `admin` with the password `secret`.

### Where the data goes

Everything the program remembers lives in one file: `data/rapidxerp.db`. It is a **SQLite** database — a whole database inside an ordinary file, with nothing to install and nothing running in the background.

The first time the program starts, that file does not exist. The program creates it, creates the tables inside it, and puts eight customers and twelve articles in so that there is something to look at. To start over, delete the file.

Two other things are remembered, and they are not in that file: the theme you chose and the size of the window. Those go where your operating system keeps such settings, through the JDK's `Preferences` — a plist on macOS, the registry on Windows, a file under your home folder on Linux. Part 4 comes back to this.

## Part 2 — The language in twenty minutes

The program is written in **RapidFX**. If you have seen Visual Basic it will look familiar; if not, this section is all the language you need for the rest of the manual.

### Comments

A single quotation mark begins a comment. Everything after it on that line is for humans.

```basic
' This whole line is a comment.
Dim count As Integer = 3    ' and so is this part
```

This program has a great many comments. That is deliberate: they say **why** something was done, which the code itself can never say.

### Values and variables

A **variable** is a named box holding a value. `Dim` makes one; `As` says what kind of value it may hold.

```basic
Dim count As Integer = 8
Dim price As Double = 54.90
Dim name As String = "Miller Electrical Systems GmbH"
Dim finished As Boolean = False
```

Those four cover almost everything here:

- `Integer` — a whole number: 0, 8, -3.
- `Double` — a number that may have a fractional part: 54.9, 0.005.
- `String` — a piece of text, in double quotation marks.
- `Boolean` — `True` or `False`, and nothing else.

Saying `As Integer` is not bureaucracy. It lets the compiler catch a whole class of mistake before the program runs: put text into a box declared `As Integer` and you are told at build time rather than by a crash in front of a customer.

`Nothing` is the value of a box holding no object at all. `If x Is Nothing Then` is how you ask — note `Is`, not `=`.

An **array** is a row of boxes with one name. This program has one, in `Passwords.rfx`:

```basic
Dim salt(15) As Byte
```

Sixteen boxes, numbered 0 to 15. `UBound(salt)` gives the last number, 15, and `salt(3)` is the fourth one.

### Doing things once: Sub and Function

A **Sub** is a named block of instructions. Call it by name; it does its work; that is that.

```basic
Public Sub Say(text As String)
    lblMessage.Text = text
End Sub
```

That one is real, from `MainWindow.rfx`. What is in brackets is a **parameter** — a value the caller hands in. Somewhere a line reads `shell.Say("Customer K-1009 saved.")`, and inside the Sub, `text` holds that sentence.

A **Function** is the same but hands a value back, with `Return`, and says what kind with `As` at the end of its first line.

```basic
Public Function Gross() As Double
    Return Net() + Tax()
End Function
```

`Public` means anything may call it; `Private` means only the file it sits in. Marking things `Private` where you can is a kindness to the next reader: it says "you do not have to understand this to use the rest".

### Making decisions

```basic
If cust.Number = "" Then
    lblHint.Text = "A customer number is required."
    Exit Sub
End If
```

`Exit Sub` means "stop here and go back to whoever called". Used like this it is a **guard**: check what must be true, leave at once if it is not. It saves the rest of the Sub from being written inside an ever deeper stack of `If`s.

When one value is compared against several possibilities, `Select Case` reads better:

```
Select Case name
    Case DARK
        FlatDarkLaf.setup()
    Case INTELLIJ
        FlatIntelliJLaf.setup()
    Case Else
        FlatLightLaf.setup()
End Select
```

`Case Else` catches everything not named. Two words combine conditions: `AndAlso` (both) and `OrElse` (either).

### Doing things again

```basic
Dim i As Integer
For i = 0 To 5
    ...
Next i
```

Count from one number to another.

```basic
Dim entry As Object
For Each entry In hits
    Dim k As Customer = CType(entry, Customer)
    ...
Next entry
```

Walk through everything in a list. This is the loop you will meet most often.

```
Do While r.next()
    hits.add(ReadRow(r))
Loop
```

Keep going as long as something is true — here, as long as the database has another row.

### Long lines

A line ends where the line ends. An underscore at the very end says "this carries on below":

```
tblCustomers.AddRow(k.Number, k.Name, k.Contact, k.CityLine(), _
                    k.Phone, k.EMail)
```

The `_` is not part of the instruction; it only tells the compiler to keep reading.

### Modules, classes and objects

These are the two ways this program groups code, and the difference matters.

A **Module** is a box of functions with no data of its own. There is exactly one of each, always. `Util`, `Database`, `CustomerFile`, `Look`, `Settings` are modules, called by name: `Util.Money(1234.5)`.

A **Class** is a blueprint for making objects. From the class `Customer` you can make as many customers as you like, each with its own name and street and telephone number.

```basic
Dim k As Customer = New Customer()
k.Name = "Miller Electrical Systems GmbH"
```

`New Customer()` makes one. What a fresh one starts with is set in its **constructor** — a special Sub called `New`, which the language runs for you whenever an object is made.

Inside a class, `Public Property Name As String` declares a piece of data every object of that class carries. `Me` means "this object" — you will see `New CustomerListForm(Me)` in `MainWindow.rfx`, which is a window handing itself to a screen so that the screen can call back.

### Promising to have certain methods: Implements

```basic
Public Class WindowSaver
    Implements WindowListener
```

An **interface** is a list of method names. `Implements` promises to have all of them, and in return the object may be handed to anything that expects that interface — here, to a window that will call `windowClosing` when it is closed. `WindowListener` names seven methods and this program cares about one, so six of them are empty. That is normal.

### Tidying up after yourself: Using

Some things must be given back when you have finished — a connection to a database is the example here. `Using` guarantees it:

```
Using c As Connection = Database.Connect()
    ...
End Using
```

Whatever happens inside, including something going wrong, the connection is closed at `End Using`. Forgetting to close things by hand is a classic way for a program that worked all morning to stop working in the afternoon.

### When something goes wrong: Try

```
Try
    Return Double.parseDouble(clean)
Catch e As java.lang.NumberFormatException
    Return 0
End Try
```

Run the first part. If it fails in the way named after `Catch`, run the second instead of stopping the program. Here: what the user typed was not a number, so treat it as zero.

### Lambdas: a block of code as a value

```basic
signOut.addActionListener(Sub(e)
    SignOut()
End Sub)
```

That `Sub(e) ... End Sub` is a **lambda** — a small nameless Sub handed over as a value. The menu entry keeps it and runs it when somebody chooses that entry. It is how nearly everything in the menu bar is wired up.

### Reaching into Java

RapidFX runs on the Java platform and can use anything Java can. `Imports javax.swing` at the top of a file lets you write `JButton` instead of `javax.swing.JButton`. When you see a call in lower case with brackets — `frame.setVisible(True)`, `r.getString(1)` — you are looking at Java underneath.

## Part 3 — The shape of the program

Four layers, with information flowing up and down but never sideways.

```
   the window          Main  ->  one JFrame

   the shell           MainWindow      menu bar, header, navigation, content area
                       SignInForm
              |
              |  ShowCustomerSheet(17)   -  a method call with an argument
              v
   the screens         CustomerListForm  CustomerSheetForm
                       ArticleListForm   ArticleSheetForm
                       DocumentListForm  DocumentSheetForm
              |
              |  ask for objects, hand back objects
              v
   the files           CustomerFile  ArticleFile  DocumentFile  UserFile
                       (everything that is SQL, and nothing else)
              |
              |  read and write rows
              v
   the database        Database  ->  data/rapidxerp.db

   carried between them:  Customer  Article  Document / DocumentLine  User
   used by everybody:     Util (numbers, money, dates)
   about the program:     Look (themes)  Settings (what is remembered)  Passwords
```

The rule that gives the program its shape: **the word SELECT appears only in the four file modules.** A screen never writes SQL. It says `CustomerFile.Search("meyer")` and gets customers back. It has no idea whether that search is a `LIKE` in SQLite, a full-text index or a call to a server in another country — and because it has no idea, that could be changed one day without touching a single screen.

The rule the other way round: `CustomerFile` never touches a button, a label or a table. It deals in `Customer` objects and nothing else.

The objects in the middle — `Customer`, `Article`, `Document` — are what the two halves have in common. They know about neither SQL nor Swing, which is what makes them safe to carry anywhere.

> Seven of the files in `src/` are the same, line for line, as in the web version of this program: `Util`, `Customer`, `Article`, `Document`, `CustomerFile`, `ArticleFile` and `DocumentFile`. Nothing in them knew about screens, so the move from a browser to a window had nothing in them to change. That is the layering paying for itself.

## Part 4 — The files, one at a time

### Main.rfx — where everything begins

Every program has one place where it starts, and here it is `Sub Main`. It is fifteen lines and does four things, in an order that matters.

```basic
Sub Main()
    Look.Setup(Settings.Theme())
    Database.Start()
    Print "RapidX ERP Desktop - database: " + Database.FilePath()

    SwingUtilities.invokeLater(Sub()
        Start()
    End Sub)
End Sub
```

**The look and feel first.** `UIManager` — Swing's register of what things look like — only decides what is built from now on. A component that already exists keeps the look it was born with. So the theme has to be chosen before the first window, or the first window is the one thing in the program wearing the wrong clothes.

**Then the database.** The other way round would mean a window on screen while the file it needs is still being created.

**Then over to the event thread.** Swing does all of its work on one thread, called the **event dispatch thread**. Everything that touches a window has to happen there — building one, changing a label, anything. `Sub Main` is not that thread, so it hands the job over with `invokeLater` and ends. The program lives on inside the window.

> A program that builds its windows on the wrong thread usually works. That is what makes it dangerous: it fails once a month, on a bigger screen, in front of somebody else.

`Start()` then makes the one thing this program has:

```basic
Private Sub Start()
    Dim frame As JFrame = New JFrame()
    frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE)
    New SignInForm(frame).Open()
End Sub
```

**One JFrame for the whole program.** A `JFrame` is a window. Signing in is not a separate window that appears and vanishes — it is simply the first thing this window shows. When somebody signs in, `MainWindow` takes the same frame over. That is why the frame is handed to the form's constructor.

`EXIT_ON_CLOSE` says what the close button means: end the program. Without it, closing the window would hide it and leave the program running invisibly.

### Look.rfx — the four themes, and the two-step change

Swing's own appearance is called Metal and nobody keeps it. **FlatLaf** replaces it — one library, the same on every platform, and the reason `lib/` has two jars whose names begin with `flatlaf`.

The module is short, and the interesting part is that changing the theme while the program runs takes **two** steps:

```basic
Public Sub Switch(name As String)
    Setup(name)
    FlatLaf.updateUI()
End Sub
```

`Setup` tells `UIManager` what to build from here on. `FlatLaf.updateUI()` goes through the windows that already exist and has them rebuild themselves. Do only the first and the open window does not change. Do only the second and there is nothing new to rebuild it with. Both, in that order.

There is a third thing in this file that looks out of place and is not:

```basic
Private Sub Words()
    UIManager.put("OptionPane.yesButtonText", "Yes")
    UIManager.put("OptionPane.noButtonText", "No")
    ...
End Sub
```

The standard confirmation dialog takes its button texts from the machine's own language. On a German computer it says "Ja" and "Nein" in the middle of an otherwise English program. These four lines settle it — and they sit here, called at the end of every `Setup`, because installing a look and feel replaces the very table they are written into. Set once in `Sub Main`, they would be gone again after the first theme change.

> The web version of this program could not fix that at all. Its dialogs came from a library that had the words built in.

### Settings.rfx — what is remembered between two starts

A web page never has to think about this: the browser remembers its own window. A desktop program is expected to.

```basic
Private Function Node() As java.util.prefs.Preferences
    Return java.util.prefs.Preferences.userRoot().node("com/tsb/rapidxerpdesktop")
End Function
```

`Preferences` is the JDK's small key-and-value store, kept wherever the operating system keeps such things. Nothing here is important enough to fail over, so every read has a default: a machine where this has never run behaves exactly like one where it has.

Two of the decisions in this file are the sort a program only makes after somebody has been annoyed by it.

**A maximised window is not worth measuring.** Its size is the size of the screen; writing that down would make the window unusable on a smaller screen tomorrow. So the maximised state is stored and the size underneath it is left as it was.

**A remembered position may no longer exist.** A laptop unplugged from its second screen is the ordinary case: the window was at x = 2400, and putting it back there opens it where nobody can see it. So `OnScreen` asks the actual screens this computer has right now:

```basic
Dim screens As GraphicsDevice() = _
        GraphicsEnvironment.getLocalGraphicsEnvironment().getScreenDevices()
```

...and if the corner is on none of them, the window is centred instead. The rectangle is shrunk by a hundred pixels, so that a window whose title bar is just off the edge does not count either.

### WindowSaver.rfx — an interface with one method that does anything

```basic
Public Class WindowSaver
    Implements WindowListener

    Public Sub windowClosing(e As WindowEvent)
        Settings.SaveWindow(CType(e.getWindow(), JFrame))
    End Sub

    Public Sub windowOpened(e As WindowEvent)
    End Sub
    ...
```

Seven methods, six of them empty. That is not a fault in the design: an interface says everything a listener may be told, and being told more than you want to know costs six empty Subs.

Which of the two closing methods to use is the whole content of this class. `windowClosing` arrives before the window is disposed of, which is the only moment at which its size can still be read. `windowClosed` would be too late — by then there is nothing left to measure.

`MainWindow` puts one of these on the frame when it opens and takes it off again on signing out, so that the small sign-in window is never mistaken for the size the user wanted.

### Util.rfx — the small conversions

Numbers and dates travel constantly between three places that each want them differently: what a person types, what the database stores, and what the screen shows. Each conversion has one right answer, and writing it out again in every form would be eight chances to get it slightly wrong.

```basic
Public Function ToNumber(text As String) As Double
    If text Is Nothing Then
        Return 0
    End If
    Dim clean As String = Replace(Trim(text), ",", ".")
    clean = Replace(clean, " ", "")
    ...
```

`Trim` removes spaces from the ends; a comma is dealt with either way — `1,234.50` has its thousands separator removed, `12,50` has its comma read as the decimal point; then any remaining spaces go. If what is left is still not a number, the `Try` returns 0.

That leniency is a decision, not laziness. A quantity field with a stray space must not produce an error box. The worst that happens is a quantity of zero, which the user can see and fix; an error dialog is something they cannot.

The date pair is one idea in two functions:

```basic
Public Function DateShown(iso As String) As String
    Return Mid(iso, 8, 2) + "." + Mid(iso, 5, 2) + "." + Left(iso, 4)
End Function
```

`DateShown` hands the date on as it stands; `DateToIso` reads one back. The database always holds the ISO form, because that is what SQLite can sort and compare correctly. The screen shows the same form, and that is a change from how this program started: it used to show `08.09.2026`. A date written that way is the eighth of September to one reader and the ninth of August to the next, and this program is read in more than one country. `DateToIso` still understands the old spelling, because somebody who has typed it for twenty years will type it again.

### Customer.rfx and Article.rfx — the data itself

The simplest files here, and worth reading first if the others feel dense.

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

A list of what a customer is, and a constructor giving a new one sensible starting values. `Id = 0` is the convention this whole program uses for "never saved". You will meet `If cust.Id = 0 Then` in several places and it always asks the same question: is this new, or does it already exist in the database?

```basic
Public Overrides Function toString() As String
    If Number = "" Then
        Return Name
    End If
    Return Number + "  " + Name
End Function
```

`toString` is a name the platform knows: whenever something needs to show an object as text, this is what it calls. That fact is put to work in the document sheet, where the customer drop-down holds actual `Customer` objects rather than strings. The box displays `toString`, so the user sees `K-1003` Southgate Building Centre KG — and what comes back out when they choose is a whole `Customer`, not a line of text somebody has to look up again.

`Article` is the same shape with different fields. One comment in it is worth reading twice: `Price` is always the **net** price per unit and `Vat` is a percentage — 19, not 0.19. Getting that wrong by a factor of a hundred is a mistake every business program makes once.

### Document.rfx — a document that adds up

Two classes: `Document`, the whole thing, and `DocumentLine`, one row on it.

The first decision is at the top:

```basic
Public Const QUOTATION As String = "Quotation"
Public Const DELIVERYNOTE As String = "Delivery note"
Public Const INVOICE As String = "Invoice"
```

A quotation, a delivery note and an invoice are **not** three classes. They are one class with a different `Kind`. `Const` means the value never changes while the program runs, and writing `Document.QUOTATION` rather than the bare text `"Quotation"` turns a typo into a build error instead of a screen that mysteriously shows nothing.

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

The total is **not stored anywhere**. It is added up from the lines every time it is asked for. That looks wasteful and is exactly right: a stored total that no longer matches its own rows is the bug every business program has once, and it is found by a customer rather than a programmer.

`CType(entry, DocumentLine)` is a **cast**. The list hands back plain `Object`s; this says "treat this one as a DocumentLine", which is what lets `.Amount()` be called on it.

`Tax()` does the same walk but multiplies each line's amount by its own rate, because lines can carry different ones — the sample data has a book at 7 % among items at 19 %. `Gross()` is `Net() + Tax()`.

At the bottom, `DocumentLine` repeats fields the article already has: number, name, unit price, VAT rate. The comment says why:

> A quotation written in March has to show March's price in December, however much the article has gone up in the meantime.

The line copies those values when it is created and keeps them for ever. `FromArticle` is the one place where an article and a line ever touch. The self-test checks it: it raises an article's price by fifty euros and asks the document for its total again, which does not move.

### Passwords.rfx — hashing, and why it is slow on purpose

The web version borrowed this from its session library. A desktop program has no such library, and writing it out is no loss — it is thirty lines and almost every program eventually needs them.

A password is never stored. What is stored is the result of running it through **PBKDF2**, a deliberately slow one-way function. From the password you can compute the hash; from the hash you cannot get back to the password. Checking a password means hashing what was typed and comparing the two results.

Three things separate this from a naive hash:

- **The salt** — sixteen random bytes, different for every user, stored beside the hash. Two people with the same password therefore have different hashes, and a table of pre-computed hashes is of no use to an attacker.
- **The iterations** — 120000 rounds. Unnoticeable once, at sign-in; ruinous for somebody trying millions of guesses.
- **The comparison** — `MessageDigest.isEqual` always looks at every byte. A plain `=` would stop at the first difference, and how long it took would leak how much of the guess was right.

What goes into the database looks like this:

```
pbkdf2$120000$<salt, base64>$<hash, base64>
```

The iteration count travels with the hash on purpose. Raising it next year must not invalidate the passwords stored this year — old rows keep their old number, and are still checkable.

**Base64** turns arbitrary bytes into letters and digits, so that a hash can live in a text column and come back out byte for byte.

`Matches` returns `False` for anything it cannot read, and never an error. A broken row in the users table must mean "you cannot sign in", not a crash on the sign-in screen.

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

An **environment variable** wins if it is set — that is how you point the program at another database without recompiling, and it is how the self-test gets a database of its own.

`ProjectRoot()` climbs up to five folders looking for a file ending in `.rfxproj`. That looks like overkill until you know the bug it prevents: started from the IDE, the program runs in the folder of the `.rfx` file, so a plain relative path would put a second database inside `src/`, and nobody would understand where their customers had gone.

#### One connection at a time

Every operation opens its own connection and closes it again. On a desktop nobody would be hurt by a single connection held open — but a connection living in a field is one somebody has to remember to close, and the day the program grows a background thread it becomes a bug. Opening a SQLite file costs a file handle and no network at all.

Two settings are applied to each new connection:

- `busy_timeout = 5000` — if the file is briefly locked, wait five seconds rather than give up. One person cannot collide with themselves, but two copies of the program pointed at one file on a shared drive can, and that is a thing people do.
- `foreign_keys = ON` — SQLite does not enforce relationships between tables unless asked. This is what makes deleting a document delete its lines too.

#### The tables

`Tables()` creates all six, and every statement says `CREATE TABLE IF NOT EXISTS`, so running it against an existing database changes nothing. That is what lets `Start()` be called on every start with no special cases.

```
CREATE TABLE IF NOT EXISTS documentline (
  id INTEGER PRIMARY KEY AUTOINCREMENT,
  document_id INTEGER NOT NULL,
  seq INTEGER NOT NULL,
  ...
  FOREIGN KEY (document_id) REFERENCES document(id) ON DELETE CASCADE)
```

Three pieces of vocabulary in one statement:

- `PRIMARY KEY AUTOINCREMENT` — every row gets a number nobody has to invent, and no two rows share it. That number ends up in the `Id` property.
- `FOREIGN KEY` — this column points at a row in another table. A line belongs to a document.
- `ON DELETE CASCADE` — when the document goes, its lines go with it. Without this, deleting a document would leave its lines behind for ever, belonging to nothing.

#### The sample data

Note the `If`:

```basic
If RowCount(c, "users") = 0 Then
    AddUser(c, "admin", "Administration", "secret", "admin")
```

Sample data goes in **only** when the table is empty. A second start must add nothing and overwrite nothing, or every restart would duplicate the catalogue.

### CustomerFile.rfx — all the SQL for customers

This is where to look to learn how a program talks to a database. Six public functions, all the same shape.

#### The search

```basic
Dim pattern As String = "%" + LCase(Trim(Util.Text(text))) + "%"

Using c As Connection = Database.Connect()
    Dim b As PreparedStatement = c.prepareStatement( _
            "SELECT id, number, name, contact, street, postcode, city, country, " _
            + "email, phone, vatid, note FROM customer " _
            + "WHERE LOWER(number) LIKE ? OR LOWER(name) LIKE ? " _
            ...
```

`ArrayList` is a list that grows as you add to it, and it is what a search hands back.

`pattern` wraps what the user typed in percent signs. In SQL, `%` inside a `LIKE` means "any amount of anything", so `%yacht%` matches a name with `yacht` anywhere in it. `LCase` makes it lower case and the SQL puts `LOWER(...)` round the column, so `YACHT` and `yacht` find the same thing.

The question marks are the important part. A `PreparedStatement` is a query with holes in it, and the values are supplied separately:

```basic
Dim i As Integer
For i = 1 To 5
    b.setString(i, pattern)
Next i
```

Why not glue the user's text into the query? Because then a customer named `'; DROP TABLE customer; --` would delete your database. That attack is **SQL injection**, and question-mark placeholders are the cure. Nowhere in this program is a value pasted into SQL as text.

Then the reading:

```basic
Dim r As ResultSet = b.executeQuery()
Do While r.next()
    hits.add(ReadRow(r))
Loop
```

A `ResultSet` is a cursor over the answer, one row at a time. `r.next()` moves to the next row and reports whether there was one, which is why the loop reads as it does.

`ReadRow` is where a database row becomes an object:

```basic
Private Function ReadRow(r As ResultSet) As Customer
    Dim k As Customer = New Customer()
    k.Id = r.getInt(1)
    k.Number = Util.Text(r.getString(2))
    ...
```

The numbers are column positions in the `SELECT`, counting from 1, in the order they were written. `Util.Text` guards against an empty column: the database can hand back `Nothing`, and a text field on screen cannot hold `Nothing`.

> This is why the column list is written out by hand and not as `SELECT *`. With `*`, a column's position depends on the order it happens to sit in the table, and adding one some day would silently shift everything after it.

#### Saving

```basic
If k.Id = 0 Then
    ... INSERT ...
Else
    ... UPDATE ...
```

One function for both, told apart by the rule from earlier. The caller never has to decide. After an `INSERT`, the new row's number is fetched and written back into the object, so saving twice updates rather than inserting a duplicate — which is what you want when somebody presses Save twice.

#### Deleting, and a rule with a reason

```basic
If documents > 0 Then
    Return "The customer appears on " + documents + " documents and cannot be deleted."
End If
```

Look at what this returns: a `String`, not a `Boolean`. Empty means it worked; anything else is a sentence to show the user, written by the code that knows why. An invoice with no recipient is worse than one stale customer on file — so the deletion is refused, and the person is told the reason rather than merely that it did not work.

`ArticleFile.rfx` is deliberately the same file with different columns. Reading it after this one takes two minutes, which is the point.

### DocumentFile.rfx — the two harder ideas

#### Saving a whole document at once

A document is a head plus any number of lines, which may have been added to, removed from and re-ordered since it was loaded. The method here is blunt and correct: delete every line the document has in the database, then write the current ones back.

Working out which individual rows changed would be three separate cases — deleted, inserted, edited — and whoever forgets the third finds out from an invoice that does not add up. A document has ten lines, not ten thousand; the blunt way costs nothing.

But it creates a danger, and that is the second idea:

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

This is a **transaction**. Normally each statement takes effect the moment it runs. Here that is switched off: everything up to `commit()` is held provisionally. If it all succeeds, `commit()` makes it real, all at once. If anything fails, `rollback()` undoes the lot as if it had never happened.

Without it, a failure halfway through would leave the old lines deleted and the new ones half written — a document that has silently lost half its value. `Throw` re-raises the original problem so it is not swallowed in silence.

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

Copying field by field rather than reusing the objects matters. If both documents shared the same line objects, editing the invoice would silently edit the quotation it came from. The self-test checks exactly that.

Note what this function does **not** do: it does not save anything. It hands back an object. Whether it ever reaches the database is the user's decision, made by pressing Save.

#### The numbers

`QU-2026-0001`. The prefix says what it is, the year says when, and the last four digits count up. `NextNumber` looks for the highest existing number with that prefix and adds one. A new year starts again at 0001, which is what an accountant expects.

### UserFile.rfx — the query the web version did not have to write

There, a library was handed two `SELECT`s and did the rest: hashing, checking, and hanging the answer on the session. A desktop program has no session and no such helper, so it is written out — and it comes to eleven lines.

```basic
If r.next() Then
    If Passwords.Matches(password, r.getString(3)) Then
        who = New User()
        ...
```

One answer for both kinds of failure, deliberately. Telling somebody "that user exists, but the password is wrong" tells them something they did not know.

## Part 5 — The screens

### Two halves and a banner

Open any form file — `CustomerSheetForm.rfx`, say — and scroll. Some way down:

```
'******************************************************************************
'
'                                 DO NOT EDIT
'                              TSB RAPIDFX LOGIC
'
'******************************************************************************
```

That line of asterisks divides the file in two, and the division is absolute.

**Above it** is code written by a person: imports, the class, the button handlers. The designer reads it, keeps it, and writes it back unchanged.

**Below it** is generated. Every component on the screen is declared there, and one long Sub called `InitDesignerComponents` puts them all in place:

```basic
' @tsbswx Button btnSave parent=rootPane L=140 T=356 W=160 H=32
Private btnSave As JButton = New JButton()
...
btnSave.setName("btnSave")
btnSave.setText("Save")
btnSave.addActionListener(AddressOf OnSave)
```

The `' @tsbswx` comments look like comments and are not: they are how the designer reads the file back. Each says a type, a name, a parent and a position, and together they are the drawing. Editing that half by hand and getting one slightly wrong means the screen opens in the designer as an empty canvas — and the next save writes the empty canvas back over your work.

`AddressOf OnSave` is the wiring. It does not call `OnSave`; it hands the button the name of what to call later, when somebody clicks. That is the whole event model: a click happens, and a Sub whose name begins with `On` runs.

### Two of the eight own the window

At the bottom of `SignInForm.rfx` and `MainWindow.rfx`, and nowhere else, the designer has generated this:

```basic
Public Sub ShowIn(frame As JFrame)
    frame.setTitle("RapidX ERP")
    frame.setContentPane(rootPane)
    frame.setSize(540, 380)
    frame.setLocationRelativeTo(Nothing)
    frame.setVisible(True)
End Sub
```

Those two take turns owning the one window the program has, and `ShowIn` is how each takes it over. The other six never own it — they are handed to `MainWindow` as panels and dropped into the content area — so the generator switches `ShowIn` off for them.

> In the web version no form has a `ShowIn`, because there is no window on a server to show anything in. Same designer, same forms, one setting.

### SignInForm — the smallest one

```basic
Private Sub OnSignIn(e As ActionEvent)
    Dim typed As String = java.lang.String.valueOf(txtPassword.getPassword())
    Dim who As User = UserFile.SignIn(txtUser.Text, typed)
    If who Is Nothing Then
        lblHint.Text = "Sign-in failed. Please try again."
        txtPassword.setText("")
        txtPassword.requestFocusInWindow()
        Exit Sub
    End If

    New MainWindow(frame, who).Open()
End Sub
```

`getPassword` hands back an array of characters rather than a `String`, and that is on purpose in the JDK: a `String` cannot be wiped once it exists, so a password in one sits in memory until the garbage collector happens to get round to it.

The last line is the whole of "logging in" here: the same frame, a different form inside it.

The `e As ActionEvent` parameter is there because every handler must have that shape. This one never looks at it.

### MainWindow — the shell, and the whole of the navigation

The frame everything else sits inside: a menu bar, a title, a "signed in as" line, five navigation buttons, a message line, and an empty panel below.

#### Changing screen

```basic
Public Sub ShowCustomerSheet(id As Integer)
    Place(New CustomerSheetForm(Me, id).GetRootPane(), "customers")
End Sub
```

That is the navigation. A method, with an argument, doing the thing. The customer list calls `shell.ShowCustomerSheet(k.Id)` and the right customer appears.

`Place` swaps the one child of the content area:

```basic
Private Sub Place(panel As JPanel, area As String)
    lblMessage.Text = ""
    pnlContent.removeAll()
    pnlContent.add(panel, BorderLayout.CENTER)
    pnlContent.revalidate()
    pnlContent.repaint()
    Highlight(area)
End Sub
```

All three Swing lines are needed. `removeAll` takes the old screen out, `revalidate` tells the layout to work the new one out, and without `repaint` the old pixels can be left behind on screen.

`BorderLayout.CENTER` means "fill me completely". In the designer, `pnlContent` is an empty panel anchored to all four edges, so you can see where it sits and how it grows; at run time it is given a `BorderLayout` and one child.

#### The screens that stay alive

```basic
Private customerList As CustomerListForm
Private articleList As ArticleListForm
Private documentList As DocumentListForm
```

Three fields, and they are the plainest advantage a desktop program has.

```basic
Public Sub ShowCustomers()
    If customerList Is Nothing Then
        customerList = New CustomerListForm(Me)
    Else
        customerList.Refresh()
    End If
    Place(customerList.GetRootPane(), "customers")
End Sub
```

The first time, a list is built. Every time after that, the same one is refreshed and put back on screen — so the search text is still in the box, the scroll position is where it was, and the selected row is still selected. Nobody had to save any of that anywhere.

The sheets are not kept. A sheet is about one particular record; the next one is a different record, so a new form is right.

#### The message line

```basic
Public Sub Say(text As String)
    lblMessage.Text = text
End Sub
```

`Place` clears it whenever a screen changes. So whoever wants to leave a message says it **after** asking for the new screen, and the order in `CustomerSheetForm.OnSave` is deliberate:

```
CustomerFile.Save(cust)
shell.ShowCustomers()
shell.Say("Customer " + cust.Number + " saved.")
```

#### The menu bar

The one thing a window has that a web page does not, and where the commands that are not screens belong. Four menus, built in four small functions so that none of them is long.

```basic
Dim out As JMenuItem = New JMenuItem("Sign out")
out.addActionListener(Sub(e)
    SignOut()
End Sub)
menu.add(out)
```

Make the entry, give it something to do, add it. The keyboard shortcuts go through one helper:

```basic
Private Function Item(text As String, key As Integer) As JMenuItem
    Dim entry As JMenuItem = New JMenuItem(text)
    entry.setAccelerator(KeyStroke.getKeyStroke(key, _
            Toolkit.getDefaultToolkit().getMenuShortcutKeyMaskEx()))
    Return entry
End Function
```

`getMenuShortcutKeyMaskEx` asks the system which key that is — `Ctrl` on Windows and Linux, `Cmd` on a Mac — so people get the shortcut they already know rather than the one the programmer happens to use.

The View menu uses `JRadioButtonMenuItem` in a `ButtonGroup`, so that the menu also says which theme is on. Choosing one does two things, in this order:

```basic
Private Sub UseTheme(name As String)
    Settings.SetTheme(name)
    Look.Switch(name)
End Sub
```

Remember it, then switch to it. The choice has to survive the next start.

#### Signing out

```basic
Public Sub SignOut()
    Settings.SaveWindow(frame)
    frame.removeWindowListener(saver)
    frame.setJMenuBar(Nothing)
    New SignInForm(frame).Open()
End Sub
```

Write the size down while the window still has the size worth writing down. Take the listener off, so the small sign-in window is not measured later. Take the menu bar away, because the sign-in screen has no menus. Then hand the frame to a new sign-in form — which is exactly what `Sub Main` did at the very beginning.

### The three list screens

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
    lblHint.Text = ""
    Return CType(hits.get(row), Customer)
End Function
```

...row 4 is turned straight back into a customer. Nothing has to read a number out of a cell and look it up again.

`Chosen` also handles "nothing is selected" once, for everybody, so every button that needs a selection begins the same way:

```basic
Dim k As Customer = Chosen()
If k Is Nothing Then
    Exit Sub
End If
```

Filling the table:

```basic
tblCustomers.Clear()
Dim entry As Object
For Each entry In hits
    Dim k As Customer = CType(entry, Customer)
    tblCustomers.AddRow(k.Number, k.Name, k.Contact, k.CityLine(), _
                        k.Phone, k.EMail)
Next entry
tblCustomers.Reload()
```

Add every row, and **then** call `Reload()` once.

And `Refresh`, which is what makes a kept screen work:

```basic
Public Sub Refresh()
    Dim row As Integer = tblCustomers.Table().getSelectedRow()
    Fill()
    SelectRow(row)
End Sub
```

The data may have changed underneath — somebody has just saved a customer — but what was typed and what was selected has not, and both are put back.

`DocumentListForm` has two extras. `Display(kind)` says what the screen currently is, and the carry-over buttons obey the rules of business:

```
btnToDeliveryNote.setVisible((kind = Document.QUOTATION))
btnToInvoice.setVisible((kind <> Document.INVOICE))
```

An invoice becomes nothing further, and a delivery note becomes only an invoice. The rule is expressed once, as visibility, rather than as an error message after the fact.

### The two sheet screens

A **sheet** shows one thing, all its fields, with Save and Cancel. `CustomerSheetForm` and `ArticleSheetForm` are the same file twice.

Three Subs do all the work, and their names say the direction of travel.

`Fetch` gets the object — a brand-new one with the next free number, or an existing one from the file:

```basic
Private Function Fetch(id As Integer) As Customer
    If id = 0 Then
        Dim fresh As Customer = New Customer()
        fresh.Number = CustomerFile.NextNumber()
        Return fresh
    End If
    ...
```

`Display` moves the object into the fields. `Collect` moves the fields back into the object. Note `Trim` on the way in and not on the way out: trailing spaces are a typing accident, removed once, at the point where typed text becomes data.

Then saving is four lines of checking and three of doing.

### DocumentSheetForm — the most interesting screen

A head, a table of lines, a way to add and remove them, running totals, and three buttons.

**The document is an ordinary field.**

```basic
Private doc As Document

Public Sub New(owner As MainWindow, document As Document)
    InitDesignerComponents()
    shell = owner
    doc = document
    ...
```

That is worth pausing on, because in the web version it was impossible. There, every added line rebuilt the whole display, which threw this form away and made a new one — so a field would have been gone each time, and the document had to be put in the session instead. Here the form stays on screen for as long as the editing lasts. A field is enough, and the paragraph in the web version explaining why it could not be one is simply gone.

**The combo boxes hold objects.**

```basic
Dim none As Customer = New Customer()
none.Number = ""
none.Name = "— please choose a customer —"

customers = New ArrayList()
customers.add(none)
customers.addAll(CustomerFile.All())
```

A fake customer at position zero. Without it a drop-down selects its first entry by itself, and the first real customer in the file would sit there pre-selected — so a quotation would go to them by accident. An empty line would not do, because an empty line does not read as an instruction.

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

The comment above `building` in the source is the best bug story in this project. In short: adding the first item to a drop-down selects it, which fires the "customer chosen" handler, which ran while the box was still being filled and wrote customer number 0 into the document. Carrying a quotation over into an invoice therefore lost its customer. One `Boolean`, set to `False` at the end of the constructor, fixes it.

**Adding a line.**

```
CollectHead()
doc.AddLine(DocumentLine.FromArticle(CType(chosen, Article), quantity))
txtQuantity.Text = "1"
ShowLines()
```

`CollectHead()` first, because the user may have typed a date or a subject since the last time and those must not be lost. Then the article and quantity become a line, the quantity box resets to 1 ready for the next one, and the table is redrawn.

Nothing has touched the database. The document is still only a draft.

## Part 6 — One click, all the way through

A user adds a line to a quotation. Everything below has been described already; this is only the order it happens in.

- **The mouse.** The user has chosen an article and typed `10`, and clicks **Add line**. The operating system tells Java, which puts an event on the queue of the event dispatch thread.
- **The button.** Swing takes the event off the queue and runs what was registered with `AddressOf` — `OnLineAdd`, on that same thread. Every line that follows runs there too, which is why none of it needs to worry about anything happening at the same time.
- **In OnLineAdd.** `cmbArticle.getSelectedItem()` returns an `Article` object, because that is what was put into the box. `Util.ToNumber("10")` gives `10`. `CollectHead()` copies the date and subject from the screen into the document.
- **In DocumentLine.FromArticle.** A new line is made, and the article's number, name, unit, price and VAT rate are copied onto it. From this moment the line no longer depends on the article: the price is frozen.
- **In Document.AddLine.** The line is appended to `doc.Lines` — a field of the form that is on screen, which will still be there in a moment.
- **In ShowLines.** The table is cleared and refilled from `doc`. `Util.QuantityText` and `Util.Money` turn the numbers into the text a German user expects. Then `Totals()` asks the document for `Net()`, `Tax()` and `Gross()` — each of which walks the lines and adds up, right now, from what is actually there.
- **On the screen.** Swing repaints the table and three labels. Nothing else on the window is touched.
- **In the database.** Nothing. Not one byte. The document reaches SQLite when, and only when, somebody presses Save.

That last point is the one to keep. A draft that lives in a form costs nothing, can be abandoned by clicking Back, and never leaves a half-finished document lying in the database for somebody to find next year.

## Part 7 — From the browser to the desktop

The same program exists in a version that runs in a browser. Comparing them is the shortest way to see what a web framework is actually for.

### The table

```
                          web                        desktop
  ---------------------------------------------------------------------------
  a user's state          SessionStatic in Main      fields of MainWindow
  changing screen         a string, then rebuild()   ShowCustomerSheet(17)
  after a click           whole display rebuilt      one panel swapped
  the document in hand    had to be in the session   a field of the form
  signing in              a library did it           UserFile + Passwords
  confirmations           Ja / Nein, unchangeable    JOptionPane, set to Yes / No
  the look and feel       fixed, painted by server   the View menu, live
  the window              the browser was it         one JFrame, ours
  testing it              probe.py, over the wire    SelfTest, through the front door
```

### What went away

**The session.** In a browser, one program serves many people at once. An ordinary shared variable would be shared by all of them — the second person to sign in would see the first one's data. That is not an inconvenience; it is a data leak. The web version answers it with `SessionStatic`: a variable that looks shared and behaves private, one value per visitor.

On the desktop there is one person and one window, and their state is fields of `MainWindow`. The whole idea was standing in for something a window gives you for nothing.

**The router.** The web version changed screen by writing a string into the session and asking for a rebuild:

```
Main.Screen = "customers"
tsbWebSession.rebuild()
```

...and a `Select Case` somewhere else turned the string back into a screen. Here that is:

```
shell.ShowCustomers()
```

The detour existed because the display was rebuilt from one place that knew nothing about customers. Notice what the string cost: a name that no compiler checks. Misspell `"custmers"` and the web version silently shows the default screen; misspell `ShowCustmers` and this one does not build.

**Handing values along.** The same reason gives the desktop version its arguments back. `ShowCustomerSheet(17)` says which customer. The web version had to leave 17 in the session and have the sheet pick it up, because nothing could hand anything to a form that a central builder constructed.

**The rebuild.** This is the one users feel. In the browser, adding a line to a quotation rebuilt the entire display — which is why the document being edited could not be a field of the form, and why a list could not remember its own search box. Here nothing is rebuilt; one panel is exchanged. Screens keep their state because they are still the same objects.

### What arrived

**A menu bar**, and keyboard shortcuts that are the ones the system already uses.

**A theme that can be changed while the program runs.** In the web version the server painted every component with the look and feel and sent pictures, so the theme was fixed at startup.

**A window with a size and a place** — which somebody has to remember, which is `Settings.rfx`.

**Password hashing of our own**, because the library that did it was part of the web stack.

**A different kind of test.** `probe.py` could work the web version from outside, because there was a wire between the browser and the program to speak over. A desktop program has no such seam, which is rather the point of one; so `SelfTest.java` goes in through the front door instead, builds the real forms and calls `doClick()` on their real buttons.

### What did not change at all

Seven files: `Util`, `Customer`, `Article`, `Document`, `CustomerFile`, `ArticleFile`, `DocumentFile`. Line for line, apart from two comments.

That is the layering paying for itself, and it is the most useful thing in this whole comparison. None of those seven knew whether they were in a browser or a window, so the move had nothing in them to change. If they had contained one `tsbWebSession` between them, all seven would have had to be rewritten.

## Part 8 — The tools folder

Five files, none of them part of the running program.

### CheckForms.java — the round trip

Every screen in `src/` is a designer form and nothing generates it. `CheckForms` is what makes that
claim checkable: it reads each one with the designer's own parser, writes it out again in memory
and compares the two. If the same text comes back, the designer can open the file and a save
changes no line.

It writes nothing to disk. That matters more than it sounds: there used to be a `GenerateForms`
here that wrote every screen from scratch, taking the half above the banner out of
`tools/handwritten/`. A screen you had just edited in the designer was replaced by the old copy
the next time somebody built, without a word — and the round trip could not notice, because it was
comparing the freshly written file with itself. Both the generator and the copies are gone.

### CheckForms.java — the round trip

The one promise this application makes to the designer, tested in three words: **read, write, compare**. Every form is read with the designer's parser, written out again with the designer's writer, and compared with the original. If the same file comes out, then opening the screen in the designer and saving it changes nothing. If not, the build stops and prints the first differing line.

### SelfTest.java — one round through everything

Ninety-eight checks, on a temporary database of its own, so a failed run cannot leave rubbish in `data/` and a passing run proves the first start works.

The first half works the data layer directly — the sample data, hashing, signing in, searching, the arithmetic, the frozen price, carrying over, the two deletion rules.

The second half builds the real forms and clicks their real buttons:

```
onEdt(() -> {
    text(list, "txtSearch").setText("yacht");
    button(list, "btnSearch").doClick();
});
check(rows(list, "tblCustomers") == 1, "searching narrows it to one");
```

`doClick()` runs exactly the listener a mouse would. The fields it reaches for are private, and that is not an oversight: they are private because nothing in the program should touch them, and a test is not part of the program.

`onEdt` is `SwingUtilities.invokeAndWait` — everything Swing has to happen on the event dispatch thread, in a test as much as in the program, and the test waits so that it sees a finished screen rather than half of one.

What is deliberately left out: the two confirmation dialogs. `JOptionPane` stops and waits for an answer, and a test that has to answer its own dialogs is testing the JDK rather than this program. The rule underneath is checked where it is written.

## Part 9 — Words you will meet

- **Array** — a numbered row of boxes with one name. `Dim salt(15) As Byte`.
- **Base64** — a way of writing arbitrary bytes as letters and digits.
- **Cast** — telling the compiler to treat a value as a more specific type. `CType(entry, Customer)`.
- **Class** — a blueprint for objects. `Customer` is a class; a particular customer is an object.
- **Commit / rollback** — make a group of database changes real, or undo all of them.
- **Constructor** — the `Sub New()` that runs when an object is made.
- **Cursor** — a position in a set of database rows. `ResultSet` is one; `r.next()` moves it.
- **Event dispatch thread** — the one thread Swing does all its work on.
- **Environment variable** — a setting outside the program, read with `Environ(...)`.
- **Foreign key** — a column pointing at a row in another table.
- **Handler** — a Sub that runs when something happens. Here they all begin with `On`.
- **Hash** — a one-way scramble of a password. Checkable, not reversible.
- **Interface** — a list of method names a class can promise to have, with `Implements`.
- **JFrame** — a window.
- **Lambda** — a small nameless Sub handed over as a value.
- **Look and feel** — the set of rules that decide what every component looks like.
- **Module** — a box of functions with no data of its own, of which there is exactly one.
- **Parameter** — a value handed to a Sub or Function when it is called.
- **PBKDF2** — a deliberately slow hash, made slow so that guessing is slow.
- **PreparedStatement** — a SQL query with `?` holes, filled in separately. The cure for injection.
- **Primary key** — the column identifying a row uniquely. Always `id` here.
- **Property** — a named piece of data belonging to an object.
- **ResultSet** — the answer to a query, read one row at a time.
- **Salt** — random bytes mixed into a password before hashing, different for every user.
- **SQL** — the language databases are asked questions in.
- **SQL injection** — an attack where typed text is treated as part of a query. Prevented by `?`.
- **SQLite** — a whole database inside one ordinary file.
- **Swing** — the Java toolkit these screens are built from.
- **Transaction** — a group of database changes that all happen or none do.

## Part 10 — Things to try

In roughly increasing order of difficulty. Each is genuinely doable, and each teaches something specific. Run `sh tools/selftest.sh` after every one.

- **Change a label.** Open `ArticleSheetForm.rfx` in the designer, click the label that says `Net price`, change it and save. Then look at the diff: one line below the banner moved, and nothing above it did.
- **Add a theme.** FlatLaf ships more than four, and all forty-six ported IntelliJ themes are in `lib/`. Add one to `Look.rfx` and to the View menu.
- **Give the nav buttons mnemonics.** `btnCustomers.setMnemonic(KeyEvent.VK_C)` in `MainWindow.New`, so that Alt+C works.
- **Add a field to the article.** A `weight` column. You will touch, in this order: `Database.rfx` (the `CREATE TABLE` and the `INSERT`), `Article.rfx` (the property and the constructor), `ArticleFile.rfx` (four SQL statements plus `ReadRow` and `BindRow`), and `ArticleSheetForm.rfx` (a field in the designer, then `Display` and `Collect` above the banner). Following that chain once teaches the layering better than any diagram.
- **Print a document.** This is the thing a desktop program can do that the web version cannot, and `JTable` already knows how: `tblLines.Table().print()` is very nearly all of it. Put it in the File menu, and only enable it while a document sheet is on screen.
- **Remember the last screen.** `Settings` already remembers a theme. Remember which screen the user was on and go back to it after signing in.
- **Add a status.** Documents have a `Status` column that is always `"open"`. Give the document sheet a drop-down with `open`, `sent` and `paid`, and show it in the list.
- **Ask before throwing work away.** Clicking Back on a document sheet with unsaved lines loses them without a word. A confirmation there is four lines — and deciding when to ask is the interesting part.
- **Write a check of your own.** Copy a block out of `tools/SelfTest.java` and add one. If it fails, you have found a bug; if it passes, you have kept one out.

The best way to understand a program is to change it and see what breaks. Everything here is covered by the self-test, so you will find out quickly.
