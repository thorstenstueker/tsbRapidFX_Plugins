# tsbRapidFX

## The Complete Manual

A Basic for the JVM — the language, the compiler, the Java class library, databases, user interfaces, web applications, the form designers for the desktop, the web and mobile, and packaging into native installers.

It is all one product now: **one IntelliJ IDEA plugin** carries the language, the three designers and tsbDeploy. The separate mobile plugin for Android Studio is gone since 19.09.2026; where older notes still say "Android Studio", read "the IDE".

This manual describes tsbRapidFX as it stands. Every program in it was compiled by the real `rfxc` before it was printed, and the ones that produce output were run. Where a thing does not work yet, the manual says so rather than describing what it ought to do.

---

## What this manual covers

| Part | Chapters | Subject |
|---|---|---|
| I | 1–8 | The compiler and the language |
| II | 9–11 | Using every Java class |
| III | 12–13 | Databases |
| IV | 14–17 | User interface: Swing, windows, dialogs, tables |
| V | 18–21 | Web applications, variables, SessionStatic, the server |
| VI | 22–23 | The designers: desktop and web |
| VII | 24–25 | Mobile: the designer, building and running on a device |
| VIII | 26 | Packaging into native installers with tsbDeploy |

---

## Contents

**Part I — The compiler and the language**

1. What tsbRapidFX is
2. The compiler: rfxc
3. Basics: casing, types, variables, operators
4. Control flow
5. Procedures, lambdas and AddressOf
6. Your own types
7. Error handling
8. The standard library

**Part II — Using every Java class**

9. Java is used, not rebuilt
10. Worked example: java.nio.file, Files.readString and Files.writeString
11. Reading Javadoc and writing tsbRapidFX

**Part III — Databases**

12. JDBC in tsbRapidFX
13. The databases, one by one

**Part IV — User interface**

14. Swing in tsbRapidFX
15. Windows and dialogs — desktop and web
16. JTable — the full way
17. tsbSmartTable — the short way

**Part V — Web applications**

18. Writing a web application
19. Variables: where a value lives and how long
20. SessionStatic: the mechanism, the measurements, the rule
21. The server: what it carries, and how

**Part VI — The designers**

22. The desktop designer
23. The web designer

**Part VII — Mobile**

24. The mobile designer
25. Building and running on a device

**Part VIII — Packaging**

26. Packaging with tsbDeploy

---

## Conventions

Code is set in a monospaced face and is real, compilable tsbRapidFX unless the surrounding text says otherwise. Shell commands are shown the same way; a leading `$` is never part of what you type.

Diagnostic codes look like `RFX0208`. The code is the contract: the wording of a message may change between versions, the code does not. Every code has an entry in the compiler's own message reference.

Names of files, keywords, types and methods appear as `Files.readString`. Keyword casing in running text follows the manual's own habit — `SessionStatic`, `Dim` — but the compiler does not care: keywords are case-insensitive, names are not. That distinction is Chapter 3.
# Part I — The compiler and the language

## 1 What tsbRapidFX is

tsbRapidFX is a programming language with a Basic grammar that compiles **directly to JVM class files**. It is not a transpiler: no Java source is written on the way, and there is no runtime layer between your code and the platform.

```
Hello.rfx
  -> Lexer      line oriented; '_' and an open bracket continue a line
  -> Parser     recursive descent, Pratt for expressions   -> syntax tree
  -> Binder     names -> symbols, expressions -> types     -> bound tree
       :        resolved against the real class path
  -> Lowerer    If / For / While / Do  ->  labels and jumps
  -> Emitter    ASM  ->  .class
  + the tsbRapidFX runtime jar
```

### The three ideas behind it

**Java is used, not rebuilt.** A tsbRapidFX `Integer` *is* an `int`. A tsbRapidFX `String` *is* a `java.lang.String`. A tsbRapidFX `Class` *is* a Java class, and a `Module` is a Java class with static methods. There is no marshalling at the boundary because there is no boundary. That is why the whole JDK is open to you, why a Java call costs nothing, and why Java can call back into tsbRapidFX without an adapter.

What *is* rebuilt is only what Java does not have: the Basic intrinsics — `Left$`, `Format$`, `MsgBox`, `Now` — as a runtime library of about eighty functions. That is a finite set. Copying out the JDK would not be.

**Statically typed.** Every value has a type that is fixed at compile time. Widening conversions happen by themselves (`Integer` to `Double`); narrowing ones you write (`CInt(x)`). This is the difference from VB6 and the reason tsbRapidFX needs neither boxing nor a runtime type check to do ordinary arithmetic.

**Straight to bytecode means the debugger works.** The generated class carries `SourceFile: "Hello.rfx"` and a real `LineNumberTable`, so any Java debugger steps through the Basic source and a stack trace names the line you wrote:

```
at Test.Check(Errors.rfx:48)
at Test.Main(Errors.rfx:11)
```

Local variable names are in the class file too, so a debugger shows them spelled the way you spelled them.

### Where it sits

| Module | What it holds |
|---|---|
| `compiler` | Lexer, parser, binder, lowerer, emitter, `Compilation` — and `ide/`, the answers an editor needs |
| `runtime` | The Basic intrinsics. Dependency-free, because every program carries it |
| `cli` | `rfxc`, the command-line compiler |
| `lsp` | `rfxlsp`, a Language Server Protocol server |
| `intellij-plugin` | The IntelliJ plugin: language, compiler, run, build jar, project templates, Open in Designer |

**One symbol for both worlds.** `Symbol.Method` describes a tsbRapidFX `Sub` and a Java method with the same fields, so there is no separate call machinery for interop. The binder makes the same symbols out of a class file as out of a declaration, and from there `Show(1)` and `list.add("x")` run through identical code — overload resolution, argument conversion, emission.

**The class path is an input, not a setting.** `Compilation.create(source, path)`: `Dim b As Button` means a different type — or none — depending on what is on the path, and every message about a type is an answer relative to it. `rfxc -cp lib/flatlaf.jar` hands the same path to compiling *and* to running, so the two cannot drift apart.

### Why the parser never gives up

Completion always happens on broken code. `btn.` is syntactically incomplete, and that is exactly where the list should open. A compiler that stops at the first error can never serve an IDE, and changing that afterwards is not an extension but a rebuild of the front end. So:

- **The parser always returns a complete tree.** A missing token is inserted zero-width; an unusable line becomes an error node and the next line parses normally. `btn.` therefore yields a member-access node whose target the binder can still type — and that node *is* the completion request.
- **Every node carries its exact source span.** `findAt(offset)` and `pathTo(offset)` answer "what is under the cursor". The containment test includes the end of the span, because the cursor after the dot sits exactly there.
- **An unknown name reports itself once.** The error type is compatible with everything, so the operators it flows through stay quiet. A typo costs one message, not one per use.
- **Diagnostics are objects** with span, code and severity. There is no `System.err.println` anywhere in the compiler.
- **No global mutable state.** A `Compilation` is immutable and cheap; an editor builds a new one on every keystroke and throws the old one away. Nothing to invalidate, nothing that can go stale.

These promises have a test file of their own, kept apart from the language tests. A batch compiler would need none of them — it could stop at the first error and still be good. If one of them breaks, tsbRapidFX goes on compiling and only the IDE quietly stops working.

---

## 2 The compiler: rfxc

### Building it

```
./gradlew :cli:jar
```

The result is `cli/build/libs/rfxc-0.1.0.jar` — one runnable jar holding the compiler and the runtime library.

### The first program

`hello.rfx`:

```basic
Sub Main()
    Print "Hello, world!"
End Sub
```

```
java -jar rfxc-0.1.0.jar --run hello.rfx
```

Compiling only, which produces `hello.class`:

```
java -jar rfxc-0.1.0.jar -o build hello.rfx
java -cp build:rfxc-0.1.0.jar hello
```

The second line shows two things at once. The result is an ordinary class file that `java` starts — and the runtime library has to be present, because `Print` is a call into it.

### The invocation in detail

```
rfxc - the tsbRapidFX compiler

Usage:
  rfxc [options] <file.rfx> [more.rfx ...]
  rfxc [options] <directory>
  rfxc --project <x.rfxproj>

Options:
  -o, --out <directory>     where the .class files go (default: .)
  -cp, --classpath <paths>  jars and directories to compile against,
                            separated by ':' - FlatLaf, say
      --project <x.rfxproj> build a whole project (its sources, libraries
                            and jar name are named in the project file)
      --jar <file.jar>      build a runnable jar instead of .class files
                            (the runtime library goes inside it; -cp entries
                            are named as Class-Path in the manifest)
      --run                 start 'Sub Main' after compiling
  -v, --verbose             list the files written
      --no-color            no colour in error messages
  -h, --help                this help
```

`--classpath` applies to both compiling *and* `--run`. That is deliberate: compiling against one version of a library and running against another is the mistake that shows up later as a `NoSuchMethodError` inside a window.

### Where Sub Main may live

A file needs no `Module`. These two programs are equivalent:

```basic
Sub Main()
    Print "Hello"
End Sub
```

```basic
Module Hello
    Sub Main()
        Print "Hello"
    End Sub
End Module
```

Without a `Module` the class takes the name of the file. `hello.rfx` becomes `hello.class`, and `Sub Main` is findable in it both as `Main` and as `main(String[])` — Java is case-sensitive and tsbRapidFX is not, so both exist.

### Reading an error message

```
hello.rfx:3:21: error RFX0300: Type 'Doesntexist' not found. Is an 'Imports' missing?
  |
3 |         Dim x As Doesntexist
  |                  ^^^^^^^^^^^
```

File, line, column, a code, one sentence, and the place underneath. The code is stable: the wording may change, the code will not.

The compiler does not stop at the first error. It reports everything it finds, and a typo produces exactly one message — not one per use.

### Passing a program on

`--jar` builds a file that starts on its own:

```
rfxc --jar greeting.jar Greeting.rfx
java -jar greeting.jar
```

**The tsbRapidFX runtime library is inside it.** Every program needs it — `Print` is a call into it, and so is `+` — so a jar without it would end at the first statement with a `NoClassDefFoundError`. It costs about thirty kilobytes and it is the entire point of the exercise: one file you can hand over.

**Third-party libraries are not copied in.** Whatever arrived through `-cp` is named in the manifest as `Class-Path` instead:

```
rfxc -cp lib/flatlaf-3.7.2.jar --jar program.jar Program.rfx
```

The `lib` directory then has to sit beside the jar. Copying them in would mean redistributing somebody else's code under somebody else's licence, and thirty kilobytes would become a hundred megabytes.

**Without a `Sub Main` you get a library** — a jar with no `Main-Class`. That is not a mistake; it is how tsbRapidFX code is used from Java:

```java
System.out.println(Tools.Twice(21));   // 42
```

**Compiling twice gives the same file.** Every entry carries the same fixed timestamp, so two builds can be compared by checksum.

### Projects

A program may consist of any number of files. A type declared in one file is visible in all the others — there is nothing to import and nothing to register.

```
MyProgram/
  MyProgram.rfxproj
  src/
    Main.rfx
    MainWindow.rfx
  lib/
    flatlaf-3.7.2.jar
```

The project file:

```
' This project can be built without an IDE:
'     rfxc --project MyProgram.rfxproj

Name    = MyProgram
Sources = src
Library = lib/flatlaf-3.7.2.jar
```

| Key | Meaning |
|---|---|
| `Name` | the name of the built jar |
| `Sources` | a source folder; repeat the key for more than one |
| `Library` | a jar to compile and run against; repeat the key for more than one |
| `Type` | `DESKTOP` or `WEB`; written by the wizard, missing means desktop |

Keys may repeat, order does not matter, and `'` starts a comment. The format cannot express more than that — and a format that cannot express more will not grow into a second language you have to learn.

> **The keys are case-sensitive, and an unknown key is silently ignored** — later versions may add keys, so a typo is not treated as an error. The practical consequence is worth knowing: a misspelled key does not fail. It produces a project with no declared source folder, which then falls back to the whole project directory, and no libraries at all. If a project suddenly compiles the wrong files or cannot find a library class, check the spelling in the `.rfxproj` first.
>
> **The older German spellings still work.** These keys were `Quellen`, `Bibliothek` and `Typ` until 13.09.2026. Both are read, so a project file written before that goes on building; new files are written with the English ones.

Without a `Sources` line, the project directory itself is the source folder.

**Why the file exists at all.** tsbRapidFX needs no build tool; the plugin compiles and builds by itself. But "no build tool" must not come to mean "buildable only in the IDE". Because the project is a *file* and not an IDE setting, `rfxc --project` builds exactly the same thing outside — on a server, reproducibly, under version control.

```
$ rfxc --project MyProgram.rfxproj
written: /tmp/MyProgram/build/MyProgram.jar
start it with: java -jar /tmp/MyProgram/build/MyProgram.jar
```

What it leaves behind:

```
build/
  MyProgram.jar          Main-Class and Class-Path in the manifest,
                         the tsbRapidFX runtime packed inside
  lib/
    flatlaf-3.7.2.jar    copied beside it, named in Class-Path
```

**Move the two together.** The `Class-Path` entries are relative and resolve against the directory the jar sits in.

Because the jar is started by `java -jar`, everything that depends on the application class loader works normally — `ServiceLoader`, and therefore JDBC driver discovery, among them. That is not true of `--run`, and Chapter 12 says why.

Points to watch in a multi-file program:

- **Exactly one `Sub Main`.** More is an error (`RFX0205`) and not a silent choice — otherwise the directory order would decide which program starts.
- **The files are compiled in name order.** No message and no generated byte depends on the order the file system happens to list them in.
- **`Imports` is per file.** What one file imports, another does not see.
- **There are no packages.** `Module Hello` becomes `Hello.class` at the root; two types of the same name are an error (`RFX0201`), even from different files.

### The language server and the IDE

```
./gradlew :lsp:jar
java -jar lsp/build/libs/rfxlsp-0.1.0.jar
```

`rfxlsp` speaks LSP over standard in and out, so any editor that knows the protocol — VS Code, Neovim, Emacs, IntelliJ through LSP4IJ — gets errors as you type, completion, hover, go-to-declaration and an outline.

**The server is thin, and that is the point.** Everything it answers comes out of `com.rapidfx.compiler.ide`, which knows no protocol; it answers in plain records. The server only converts character offsets into line and column. That is also why the same service can serve the designer with no protocol in between.

The IntelliJ plugin goes further: **the compiler runs inside the IDE.** No second process, no protocol, nothing to set up. The plugin compiles `compiler` and `runtime` into itself and calls `LanguageService` as an ordinary Java method. There is no second grammar either — the platform gets a flat token list, and every question about meaning goes to the compiler, so the editor cannot hold a different opinion from `rfxc`.

**Compiled here, executed over there.** The compiler runs in the IDE because it is loaded anyway and has usually compiled the file already, so starting after typing costs nothing. The program itself gets a **JVM of its own**: it may open a window, call `System.exit` or run forever, and none of that may touch the IDE.

Auto-import works the way it does for Java: type `Files`, and the import appears. There are three routes — a suggestion while typing, Alt+Enter on a name already typed and shown in red, and unprompted insertion when there is exactly one candidate and *Add unambiguous imports on the fly* is on.
## 3 Basics: casing, types, variables, operators

### Casing

**Names are case-sensitive. Keywords are not.**

```basic
Dim Total As Integer = 1
Dim total As Integer = 2   ' two different variables

dim x As Integer           ' allowed - dim, Dim and DIM are one word
```

The line falls there because the two cases have nothing to do with each other.

**Names** distinguish because Java lies underneath and every name there has exactly one spelling. A language that let `toUpperCase` through as `TOUPPERCASE` would write your code differently from the way it is called. And in daily use: a typo is then an unknown name rather than a silently different variable.

**Keywords** do not distinguish, because nobody coming from VB6, Xojo or B4J ever wrote `If` with a capital I because they meant something by it — the editor did it. Insisting on the spelling would be a concession with nothing in return.

A word that means something in exactly one place stays an ordinary name everywhere else: `get`, `set`, `in`, `to`, `step`, `each`, `then`, `until`, `when`, `preserve` and `handles`. That has to be so, because `get`, `set` and `in` are also the names of the most-used members of the JDK. After a dot there is never a keyword anyway, only a member name — `names.get(0)` and `it.next()` are calls, not statements.

> A consequence worth knowing early: `Class` is a keyword, so `Class.forName(...)` does not parse. Write `java.lang.Class.forName(...)`. This comes up when you load a JDBC driver by hand — see Chapter 12.

### Statements and lines

**A statement is a line.** A colon separates two statements on one line:

```basic
Dim a As Integer = 1 : Dim b As Integer = 2
```

**A line continues** when it ends with an underscore, or when it ends with a comma, an operator or an open bracket:

```basic
Print "The sum is " + _
      (a + b)

Print Total(1,
            2,
            3)
```

**Comments** come in four spellings. `'`, `REM` and `//` run to the end of the line; `/* ... */` runs across lines.

```basic
' This way
REM And this way
// And this way, if your fingers come from Java
Dim x As Integer = 1   ' also after code
Dim y As Integer = 2   // and so

/*
 * Across several lines, for what needs more than one sentence.
 */
Dim z As Integer = 4 /* even mid-expression */ * 3
```

> **A block comment is whitespace, even across lines.** What was one statement stays one — the newline *inside* the comment does not count. The newline **after** the `*/` ends the statement as always, which is why a comment block between two lines changes nothing. It does not nest: a `/*` inside a comment is text, as in C and Java. A missing `*/` is `RFX0006` — otherwise the rest of the file would silently stop being code.

### The built-in types

| tsbRapidFX | JVM | Range |
|---|---|---|
| `Boolean` | `boolean` | `True` / `False` |
| `Byte` | `byte` | -128 ... 127 |
| `Short` | `short` | -32,768 ... 32,767 |
| `Integer` | `int` | about +/- 2.1 billion |
| `Long` | `long` | about +/- 9.2 quintillion |
| `Single` | `float` | ~7 digits |
| `Double` | `double` | ~15 digits |
| `Char` | `char` | one character |
| `String` | `java.lang.String` | text |
| `Date` | `java.time.LocalDateTime` | date and time |
| `Object` | `java.lang.Object` | anything |
| `Variant` | `java.lang.Object` | another name for `Object` |

> **`Integer` is 32 bits**, not 16 as in VB6. If you need the old width, use `Short`. The reason is the JVM: it has no 16-bit arithmetic, so every operation would have to be masked afterwards — for a type nobody wants in new code.

Beyond these, **every Java class is a type**. See Chapter 9.

### Variables

```basic
Dim count As Integer          ' 0
Dim name As String            ' "" - not Nothing
Dim value As Double = 3.5
Dim sum = 0                   ' type from the initial value: Integer
Var counter As Integer = 1    ' Var means the same as Dim
Const Vat As Double = 0.19    ' immutable
```

**`Var` and `Dim` are the same word.** Two spellings for one idea, so that nobody has to unlearn a habit — coming from Basic you write `Dim`, coming from elsewhere you write `Var`.

Without an initial value a variable gets its type's default: numbers 0, `Boolean` `False`, `String` **`""`** (not `Nothing` — `s + "x"` must never fail on an empty variable), references `Nothing`, a `Structure` a fresh instance.

The type is fixed at the declaration. `Dim sum = 0` makes `sum` an `Integer`, and no later assignment changes that.

### Writing numbers and text

```basic
42          ' Integer
3000000000  ' Long - does not fit in Integer
3.14        ' Double
1.5E7       ' Double, 15000000
1L          ' Long, even though it would fit in an Integer
&HFF        ' hexadecimal, 255
&O17        ' octal, 15
&B1010      ' binary, 10
"Text"      ' String
"He said ""Hello"""    ' two quotes make one
True / False
Nothing     ' the null reference - Null and Nil spell the same thing
#2024-01-15#          ' Date
#2024-01-15 14:30#    ' Date with time
```

### Operators

From tightest to loosest binding:

| Operator | Meaning |
|---|---|
| `^` | power |
| `+` `-` (unary) | sign |
| `*` `/` | times, divide |
| `\` | integer division |
| `Mod` | remainder |
| `+` `-` | plus, minus — `+` also concatenates once one side is text |
| `&` | concatenation (the older spelling) |
| `<<` `>>` `>>>` | bit shifts |
| `=` `<>` `<` `<=` `>` `>=` `Is` `IsNot` `Like` | comparisons |
| `Not` | negation |
| `And` `AndAlso` | and |
| `Or` `OrElse` `Xor` | or |

#### Shifting bits

`And`, `Or` and `Xor` are bitwise between integers and logical between booleans. Plus the three shifts:

```basic
Dim colour As Integer = &HFF00CC
Print (colour >> 16) And 255      ' 255 - the red byte
Print -16 >> 2                    ' -4  - arithmetic, the sign stays
Print -16 >>> 28                  ' 15  - unsigned, zeros from the top
```

The result takes the type of the **left** side: an `Integer` stays an `Integer` even when shifted by a `Long`. The count is masked to the width — five bits for `Integer`, six for `Long` — so `1 << 33` is `2` and not an error. Both are Java's rule and VB.NET's at once.

> **An `L` after a number makes it a `Long`**, even when it would fit in an `Integer`. Without the suffix the magnitude decides. The difference shows up exactly here: `1 << 40` is `256` (masked to five bits), `1L << 40` is `1099511627776`.

#### Three places where tsbRapidFX does not compute like Java

**`/` is always floating point.** `3 / 2` is `1.5`, even between two `Integer`. For truncating division there is `\`: `3 \ 2` is `1`. That is VB's rule and it removes the most common arithmetic surprise a Basic programmer meets in a C-like language.

**`^` binds tighter than the sign.** `-2 ^ 2` is `-4`, not `4`.

**`Not` binds looser than a comparison.** `Not a = b` means `Not (a = b)`. The same line would not even compile in Java.

#### Concatenating with +

`+` concatenates as soon as one side is text, converting the other — the same rule as Java, Xojo and B4J:

```basic
Print "Value: " + 42 + " / " + True + " / " + 1.5
' Value: 42 / true / 1.5

Print 1 + 2          ' 3 - both sides numbers, so it adds
```

`&` does the same and stays for the VB habit. It is the older spelling of one operation, not a second behaviour.

**Both tolerate `Nothing`.** A missing string concatenates to nothing instead of raising an error — and that matters more here than in VB, because every Java method is allowed to return `null`:

```basic
Dim missing As String = System.getProperty("does.not.exist")   ' Nothing
Print "[" + missing + "]"      ' []
```

#### = against Is

`=` asks about the value, `Is` about identity:

```basic
Dim a As String = "x"
Dim b As String = "x"
Print a = b          ' True - same content
Print a Is b         ' depends on whether it is the same object
Print a Is Nothing   ' the usual "not there" test
```

#### AndAlso and OrElse

`And` and `Or` always evaluate both sides. `AndAlso` and `OrElse` stop as soon as the answer is settled — which you need when the right side is not even valid without the left:

```basic
If f IsNot Nothing AndAlso f.Exists() Then
```

#### Like

Compares with VB's wildcards — **not** a regular expression:

| Pattern | Matches |
|---|---|
| `?` | exactly one character |
| `*` | any number, including none |
| `#` | one digit |
| `[abc]` | one of these characters |
| `[!abc]` | none of these characters |

```basic
Print "File.txt" Like "*.txt"    ' true
Print "x9" Like "x#"             ' true
```

### Converting between types

**Widening happens by itself.** An `Integer` flows to `Long`, `Single`, `Double`; a reference to its supertype; anything to `Object`.

**Narrowing must be written.** `Dim n As Integer = 1.5` is an error (`RFX0607`), and the message names the remedy:

| Function | Converts to |
|---|---|
| `CBool` `CByte` `CShort` `CInt` `CLng` `CSng` `CDbl` `CChar` | a primitive type |
| `CStr(x)` | text, in VB's spelling |
| `CObj(x)` | `Object` |
| `CType(x, T)` | any type |
| `DirectCast(x, T)` | the same |
| `GetType(T)` | the `java.lang.Class` for a type |

```basic
Dim n As Integer = CInt(1.5)                    ' 1
Dim d As Double = 7                             ' by itself
Dim s As String = CStr(42)                      ' "42"
Dim t As String = CType(list.get(0), String)    ' out of a collection
```

The conversions work on text too: `CInt("42")` is `42` and reports nonsense as an error. For a forgiving reading, use `Val`.

### Arrays

```basic
Dim values(9) As Integer       ' ten elements, indices 0 to 9
Dim empty() As String          ' no array yet, only the variable

values(0) = 1
Print values(0)
Print values.Length            ' 10
Print UBound(values)           ' 9  - the highest index
Print LBound(values)           ' 0  - always

Dim parts As String() = "a,b,c".split(",")
Dim t As String
For Each t In parts
    Print t
Next t
```

> **The number in `Dim values(9)` is the upper bound, not the count.** Nine means ten elements. That is VB's rule, and it is why `For i = 0 To 9` fits a VB array exactly.

There are **no array literals**: `New Object() {...}` is not parsed. Arrays are filled by index, as in VB:

```basic
Dim columns(2) As Object
columns(0) = "Number"
columns(1) = "Name"
columns(2) = "City"
```

Multi-dimensional arrays do not exist yet (`RFX0900`).

### Print

```basic
Print "a line"
Print "no break";              ' semicolon at the end
Print "a"; "b"                 ' a semicolon joins: ab
Print "a", "b"                 ' a comma sets a tab
Print                          ' blank line
```

`Print` writes numbers in VB's spelling — `1` rather than `1.0`, `10000000` rather than `1.0E7`, and a date in the local form. Booleans, though, come out as Java's: **`true` and `false`**, lower case. The difference is deliberate. Number formatting is legibility for a human; a boolean gets compared, logged and written to files, and then it has to be exactly what `String.valueOf` produces on the same JVM.

---

## 4 Control flow

### If

```basic
If number > 100 Then
    Print "large"
ElseIf number > 10 Then
    Print "medium"
Else
    Print "small"
End If
```

On one line, without `End If`:

```basic
If number < 0 Then Print "negative"
If number < 0 Then Print "negative" Else Print "not negative"
```

The condition must be `Boolean`. `If 1 Then` is an error (`RFX0303`) — a number is not a truth.

`If(condition, then, else)` returns a value and evaluates only the branch it chooses:

```basic
Print If(age >= 18, "adult", "minor")
```

`IIf(...)` is the same expression under VB's name. Unlike VB6, where `IIf` was an ordinary function and evaluated *both* branches, it short-circuits here — which is exactly what made `IIf(x Is Nothing, "", x.Name)` crash there.

### Select Case

```basic
Select Case grade
    Case 1
        Print "very good"
    Case 2, 3
        Print "good to fair"
    Case 4 To 5
        Print "sufficient to poor"
    Case Is > 5
        Print "fail"
    Case Else
        Print "no grade"
End Select
```

Four shapes of `Case`, mixable within one branch:

| Written | Means |
|---|---|
| `Case 5` | equal to this value |
| `Case 1, 3, 5` | equal to one of them |
| `Case 1 To 9` | in the range, **both bounds included** |
| `Case Is > 100` | the comparison holds |

It works with anything comparable — text and `Enum` values included. **The expression after `Select Case` is evaluated exactly once**, which for `Select Case NextNumber()` is the difference between one call and one per branch.

### Loops

```basic
Dim i As Integer
For i = 1 To 10
    Print i
Next i

For i = 10 To 1 Step -1
    Print i
Next i

For i As Integer = 1 To 3     ' declared here, valid only in the loop
    Print i
Next i
```

Start, end and step are evaluated **once**, before the first pass. If the step is only known at run time, its sign decides the direction — `Step direction` works. The name after `Next` is optional; if present it must match.

`For Each` runs over arrays and over anything `Iterable` — that is, every Java collection:

```basic
Dim fruit As String
For Each fruit In list
    Print fruit
Next fruit

For Each s As String In list     ' or say it right here
```

If the variable was declared beforehand, its type applies. Otherwise it comes from the sequence: for an array the element type, for a collection `Object`, because generics are erased.

```basic
While supply > 0
    Take()
Wend
```

`End While` works as well; `Wend` is the VB6 spelling, and both close the same loop.

`Do ... Loop` has five shapes, and the difference is *when* the test happens:

```basic
Do While condition      Do Until condition      Do
    ...                     ...                     ...
Loop                    Loop                    Loop While condition

Do                      Do
    ...                     ...
Loop Until condition    Loop            ' endless, left with Exit Do
```

`Until c` means `While Not c`. The two shapes that test at the end run at least once.

```basic
For i = 1 To 100
    If i = 3 Then Continue For     ' next round
    If i > 10 Then Exit For        ' leave the loop
    Print i
Next i
```

`Exit For`, `Exit Do` and `Exit While` leave the loop; `Exit Sub` and `Exit Function` leave the procedure.

### With

Saves the receiver and evaluates it **once**:

```basic
With window
    .setSize(400, 300)
    .Title = "tsbRapidFX"
    .Visible = True
End With
```

With `With list(i)` the indexing happens once, however many members the block touches. A leading dot outside a `With` is a clear error (`RFX0711`).

### GoTo and labels

```basic
Dim i As Integer = 0
Again:
    i += 1
    If i < 3 Then GoTo Again

    If done Then GoTo Finished
    Print "work"
Finished:
    Print "done"
```

A label is a name with a colon at the start of a statement. Jumps go forwards and backwards but only within the same procedure; an unknown label reports `RFX1002`. `GoTo` exists mainly because `On Error GoTo` needs it. A jump out of a `Try ... Finally` runs the `Finally` first.

---

## 5 Procedures, lambdas and AddressOf

### Sub and Function

```basic
Sub Greet(name As String)
    Print "Hello, " + name
End Sub

Function Square(x As Integer) As Integer
    Return x * x
End Function
```

Both are called the same way; writing `Call` in front is allowed and unnecessary. **The order in the source is irrelevant** — a procedure may call one declared further down. If a `Function` runs off the end without a `Return`, it returns its type's default.

### Parameters

**`ByVal` is the default.** The procedure gets a copy; assignments to it stay inside. For reference types the *reference* is copied, not the object — so `list.add(...)` inside is visible outside, `list = New ArrayList()` is not.

**`ByRef` lets the caller see the change:**

```basic
Sub Swap(ByRef a As Integer, ByRef b As Integer)
    Dim h As Integer = a
    a = b
    b = h
End Sub

Dim x As Integer = 1
Dim y As Integer = 2
Swap(x, y)
Print x + " " + y      ' 2 1
```

> **`ByRef` only works when the call is a statement of its own.** The JVM passes everything by value, so a `ByRef` travels in a one-element array and the call expands into copy-in, call, copy-out. Copy-out is a statement, and there is no room for one in the middle of an expression. `Dim s = "" + Increase(x)` therefore reports `RFX0712` — better a message than a silently lost change.

**`Optional` with a default:**

```basic
Function Greeting(name As String, Optional word As String = "Hello") As String
    Return word + ", " + name
End Function

Print Greeting("World")              ' Hello, World
Print Greeting("World", "Hi")        ' Hi, World
```

The default must be a constant and is inserted **at the call site**. After one `Optional`, all further parameters must be optional too (`RFX0710`).

**`ParamArray` takes any number:**

```basic
Function Total(ParamArray values As Integer()) As Integer
    Dim sum As Integer = 0
    Dim w As Integer
    For Each w In values
        sum += w
    Next w
    Return sum
End Function

Print Total()              ' 0
Print Total(1, 2, 3)       ' 6
```

Only on the last parameter, and its type must be an array.

### Overloading

Several procedures may share a name as long as their parameter types differ. Which one is meant is decided by the same rules as for Java methods: first without boxing, then with, then with `ParamArray`, and within a stage the most specific wins. Two declarations that would be the same method after compiling report `RFX0706`.

### Lambdas

A lambda is a procedure without a name, written where a value is expected:

```basic
Dim r As Runnable = Sub() Print "running"
r.run()

Dim f As java.util.function.IntUnaryOperator = Function(x) x * 2
Print f.applyAsInt(4)          ' 8
```

Multi-line, even inside an argument list:

```basic
list.forEach(Sub(x)
    Print "  - " + x
End Sub)
```

**Parameter types need not be written.** They come from the interface the lambda is being placed into — which is why `btn.addActionListener(Sub(e) ...)` never names `ActionEvent`. You *may* write them.

A lambda fits anywhere a **single-method interface** is expected: `Runnable`, `ActionListener`, `Comparator`, `Consumer`, and equally one you declared yourself. It has no type of its own; standing where nothing says what it should become — `Dim x = Sub() ...` — reports `RFX0800`.

A lambda may read what surrounds it: locals, fields, `Me`.

```basic
Public Class Counter
    Private state As Integer

    Public Sub Register(button As JButton)
        button.addActionListener(Sub(e)
            state += 1                    ' a field of this instance
            Print "State: " + state
        End Sub)
    End Sub
End Class
```

Under the hood the body becomes an ordinary static method of the enclosing class, captured values become its first parameters, and the call site keeps an `invokedynamic` through the `LambdaMetafactory` — the same shape javac produces, so the JVM optimises both alike. **No extra class file is created per lambda.**

### AddressOf

Places an existing procedure where an interface is expected — VB's spelling of Java's `this::method`:

```basic
list.forEach(AddressOf Show)
button.addActionListener(AddressOf Handle)

Sub Show(value As Object)
    Print value
End Sub
```

It works for module procedures, for methods of your own class (and then `Me` travels with it), and for Java methods. **`AddressOf` on a private instance method is what makes the form designers possible**: it carries `Me`, so a generated `button.addActionListener(AddressOf OnClick)` can reach every field of the form while the body of `OnClick` stays entirely yours.

---

## 6 Your own types

Five kinds, and each becomes a real class file — an ordinary type as seen from Java.

| Keyword | What it is |
|---|---|
| `Module` | all members shared, not instantiable |
| `Class` | objects with state and behaviour |
| `Interface` | a promise without a body |
| `Enum` | a fixed set of named values |
| `Structure` | a *value*, not a reference |

### Module

A container for procedures and shared data. Everything in it is automatically shared, and nothing can be instantiated from it.

```basic
Module Calculator
    Public Shared Calls As Integer = 0

    Function Double(x As Integer) As Integer
        Calls += 1
        Return x * 2
    End Function
End Module
```

From Java this is a final class with static methods — `Calculator.Double(21)` works there unchanged.

### Class

```basic
Public Class Account
    Private owner As String
    Private balance As Double

    Public Sub New(name As String)
        owner = name
        balance = 0
    End Sub

    Public Sub Deposit(amount As Double)
        If amount <= 0 Then Throw New java.lang.IllegalArgumentException("amount <= 0")
        balance += amount
    End Sub

    Public Function Balance() As Double
        Return balance
    End Function
End Class
```

A field is found without `Me.`; you write `Me` when a parameter hides the name. **Constructors are called `Sub New`.** Without one, the class gets a parameterless constructor. A field with an initial value is set in *every* constructor before its body runs.

**`Shared`** — or `Static`, the same word differently spelled — makes a member belong to the type rather than the instance. In a `Module` neither is needed: everything there is shared anyway.

> VB6's `Static` — a *local* variable that keeps its value between calls — is a different thing and is not built. It would stand in a body rather than in front of a declaration, so the two cannot collide.

### Inheritance

```basic
Public MustInherit Class Animal
    Public Property Name As String

    Public Sub New(name As String)
        Me.Name = name
    End Sub

    Public Overridable Function Describe() As String
        Return "An animal called " + Name
    End Function

    Public MustOverride Function Sound() As String
End Class

Public Class Dog
    Inherits Animal

    Public Sub New(name As String)
        MyBase.New(name)
    End Sub

    Public Overrides Function Describe() As String
        Return "Dog: " + MyBase.Describe()
    End Function

    Public Function Sound() As String
        Return "Woof"
    End Function
End Class
```

| Written | Means |
|---|---|
| `Inherits X` | inherits from exactly one class |
| `MyBase.M()` | calls the base version, **not** the override |
| `MyBase.New(...)` | calls a base constructor; must be the first statement |
| `MustInherit` | the class itself cannot be instantiated |
| `MustOverride` | the method has no body; every inheriting class must supply one |
| `NotInheritable` | nobody inherits from this class |

Calls dispatch dynamically. If the base class has no parameterless constructor, `MyBase.New(...)` must be written with arguments (`RFX0704`).

### Interface and Implements

```basic
Public Interface IShape
    Function Area() As Double
    Function Name() As String
End Interface

Public Class Circle
    Implements IShape

    Private r As Double

    Public Sub New(radius As Double)
        r = radius
    End Sub

    Public Function Area() As Double
        Return 3.14159 * r * r
    End Function

    Public Function Name() As String
        Return "Circle"
    End Function
End Class
```

Several interfaces are listed with commas or on several `Implements` lines. A missing method reports **`RFX0713` at the class** — not later as a crash when somebody calls it.

**Java interfaces too.** That is where tsbRapidFX code becomes usable *by* the JDK: implement `java.lang.Comparable` and `Collections.sort(list)` will sort a list of your own objects, calling back into tsbRapidFX code as it goes.

### Properties

```basic
Public Class Account
    Private amount As Double

    Public Property Balance As Double
        Get
            Return amount
        End Get
        Set(value As Double)
            If value < 0 Then Throw New java.lang.IllegalArgumentException("negative")
            amount = value
        End Set
    End Property
End Class
```

Used like a variable: `account.Balance = 100`. **Without a body you get an auto-property** — field and both accessors are generated:

```basic
Public Property Name As String
```

Only a `Get` makes it read-only, only a `Set` write-only.

> **A property becomes `getX` and `setX`.** That is not cosmetic: the same spelling that reads `btn.Text` on a Swing button reads a tsbRapidFX property, and Java sees a property where you wrote one.

### Enum

```basic
Public Enum Colour
    Red = 1
    Green = 2
    Blue = 4
End Enum

Public Enum Day
    Monday        ' 0
    Tuesday       ' 1
    Wednesday     ' 2
End Enum
```

Without numbers it counts from zero; with numbers, what is written applies and the next one without a number continues from there.

A tsbRapidFX `Enum` is a **real `java.lang.Enum`**:

```basic
Dim c As Colour = Colour.Green
Print c                     ' Green - the name, not the number
Print c.Value               ' 2     - the number after the =
Print c.name()              ' Green
Print Colour.valueOf("Red")

Dim x As Colour
For Each x In Colour.values()
    Print x + " = " + x.Value
Next x

Dim m As java.util.EnumMap = New java.util.EnumMap(GetType(Colour))
```

### Structure

A `Structure` is a **value**, not a reference:

```basic
Public Structure Point
    Public X As Integer
    Public Y As Integer

    Public Function Length() As Double
        Return Math.sqrt(X * X + Y * Y)
    End Function
End Structure
```

```basic
Dim a As Point          ' already there, X = 0, Y = 0 - never Nothing
a.X = 3
a.Y = 4

Dim b As Point = a      ' copied
b.X = 99
Print a                 ' Point(X=3, Y=4)
Print b                 ' Point(X=99, Y=4)
```

What follows from that:

- **Assigning copies**, and so does passing to a procedure. A change inside stays inside.
- **`=` compares the fields**, not identity. Two points with the same content are equal.
- **Usable as a `HashMap` key**, because `hashCode` matches `equals`.
- **`Print` shows the fields**: `Point(X=3, Y=4)`.
- A `Structure` neither inherits nor is inherited from.

The JVM has no value types; all of this is generated. The price is a copy on every hand-over — right for something small like a point, and a reason to use a `Class` for something large.

### Visibility

`Public`, `Private`, `Protected` and `Friend` are all read. `Private` is enforced on fields and methods; the rest is emitted public. Private means the class, not the family. Interface members are necessarily public — a private one could not be implemented and the JVM rejects it — and `AddressOf` on a private method of the same class keeps working, which is what every generated form relies on.

---

## 7 Error handling

There are two routes. `Try/Catch` is the one to use; `On Error` is there for code coming from VB6. Both fill the same `Err`, so that the language does not have two error objects.

### Try ... Catch

```basic
Try
    Dim n As Integer = CInt(input)
    Print "Number: " + n
Catch e As java.lang.IllegalArgumentException
    Print "That was not a number."
End Try
```

Several branches are tested **in the order written**; the first match wins, so the more specific type belongs on top. A `Catch` without a type catches `Exception`.

An `Error` — `OutOfMemoryError`, `StackOverflowError` — is **not** caught. That is deliberate: a program cannot sensibly handle one, and swallowing it turns a crash into a hang.

### When

A branch may add a condition on top of the type:

```basic
Try
    Process(file)
Catch e As java.io.IOException When file.Name.endsWith(".tmp")
    Print "Temporary file - skipped"
Catch e As java.io.IOException
    Throw
End Try
```

If the condition says no, **the next branch is tried**, not rethrown.

### Finally

Runs on every way out of the block — after the normal end, after any `Catch`, on a `Return`, on an `Exit For`, and when the error carries on outwards.

A `Try` needs at least one `Catch` or one `Finally` (`RFX1000`) — otherwise it would do nothing.

### Throw

```basic
Throw New java.lang.IllegalArgumentException("amount must not be negative")
```

A `Throw` with no argument inside a `Catch` passes the caught error on unchanged. Only something below `java.lang.Throwable` can be thrown (`RFX1001`).

### Using

Closes a resource however the block is left:

```basic
Using reader As java.io.BufferedReader = _
        New java.io.BufferedReader(New java.io.FileReader(path))
    Print reader.readLine()
End Using
```

That is a `Try ... Finally` you do not have to write and therefore cannot forget. The type needs a `close()` (`RFX1006`); every `java.lang.AutoCloseable` qualifies, and so does a class of your own as soon as it has such a method. `Using` is the natural shape for a JDBC connection — see Chapter 12.

### Err

```basic
Try
    Err.Raise(513, "Import", "line unreadable")
Catch e As Exception
    Print Err.Number          ' 513
    Print Err.Description     ' line unreadable
    Print Err.Source          ' Import
End Try
```

| Member | Meaning |
|---|---|
| `Err.Number` | the number from `Err.Raise`, otherwise 5 |
| `Err.Description` | the text, otherwise the error's message |
| `Err.Source` | the source, otherwise the error's class name |
| `Err.Exception` | the error itself, for anything beyond |
| `Err.Raise(n [, source [, text]])` | raise an error with a VB number |
| `Err.Clear()` | forget what last went wrong |

`Err` is per thread. Two threads failing at once do not read each other's errors.

### On Error — the VB6 shape

```basic
Sub Load()
    On Error GoTo Failure

    Dim content As String = ReadAllText("/tmp/data.txt")
    Process(content)
    Exit Sub

Failure:
    Print "Error " + Err.Number + ": " + Err.Description
End Sub
```

From the `On Error GoTo` line onwards every error jumps to the named label. Two restrictions, both reported: the label must sit **directly in the method body**, not inside a block (`RFX1004`), and there is **one target per method** (`RFX1003`). `On Error GoTo 0` switches handling off again.

`On Error Resume Next` protects each following statement individually; a failure is recorded and the next statement runs. `Resume` inside a handler does not exist (`RFX0900`).

---

## 8 The standard library

All of the following are available **without a prefix** — no `Imports` needed.

They are not language constructs but static methods in the runtime jar that the compiler keeps in scope. Two things follow: overloads like `Mid(s, 2)` and `Mid(s, 2, 3)` work like any Java method's, and **a function of your own with the same name hides the library's** — the same rule a local has against a field.

> **The dollar sign is decoration.** `Left$` and `Left` are the same function. In VB the `$` form returned a `String` and the other a `Variant`; in a statically typed language that is one and the same thing, but ported code writes the dollar.

### Strings

| Function | Meaning |
|---|---|
| `Len(s)` | number of characters |
| `Left(s, n)` / `Right(s, n)` | the first / last `n` characters |
| `Mid(s, start)` / `Mid(s, start, length)` | from `start`, optionally `length` characters |
| `InStr(s, wanted)` | position from 0, or **-1** if absent |
| `InStr(from, s, wanted)` | the same, starting at `from` |
| `InStrRev(s, wanted)` | searched from the right |
| `Trim(s)` `LTrim(s)` `RTrim(s)` | remove whitespace |
| `UCase(s)` `LCase(s)` | case |
| `Replace(s, old, new)` | replace all occurrences |
| `Space(n)` | `n` spaces |
| `StrDup(n, ch)` | one character `n` times — VB's `String$(n, c)` |
| `StrReverse(s)` | reversed |
| `Split(s)` / `Split(s, sep)` | into a `String()` |
| `Join(parts)` / `Join(parts, sep)` | back together |
| `Chr(code)` / `Asc(s)` | character from code / code of the first character |
| `StrComp(a, b)` | -1, 0 or 1 |

> **Positions count from zero, and "not found" is -1.** That is Java's rule, not VB's, and it is deliberate: `InStr` and `indexOf` answer with the same number on the same string, `Mid` and `substring` likewise. So our functions and the JDK's mix without any +/-1 anywhere — and in a language that sits entirely on Java, that mixing happens constantly.
>
> **Going out of range is not an error.** `Left("Hello", 99)` is `"Hello"`, `Mid` past the end is `""`. VB never threw here.

### Numbers

| Function | Meaning |
|---|---|
| `Abs(x)` `Sgn(x)` `Sqr(x)` | magnitude, sign, square root |
| `Int(x)` | **round down** — towards minus infinity |
| `Fix(x)` | **truncate** — towards zero |
| `Round(x)` / `Round(x, digits)` | banker's rounding |
| `Min(a, b)` `Max(a, b)` | smaller, larger |
| `Exp(x)` `Log(x)` | e-function, natural logarithm |
| `Sin(x)` `Cos(x)` `Tan(x)` `Atn(x)` | trigonometry, in radians |
| `Rnd()` | random number in [0, 1) |
| `Randomize()` / `Randomize(seed)` | reseed / make repeatable |

> **`Int` and `Fix` differ on negatives**: `Int(-8.4)` is -9, `Fix(-8.4)` is -8.
>
> **`Round` rounds halves to even** — `Round(2.5)` is 2 and `Round(3.5)` is 4. Always rounding up skews a column of figures upwards, which is why every commercial standard asks for this variant.

### Conversion and testing

| Function | Meaning |
|---|---|
| `Val(text)` | reads the leading number, otherwise 0 — never throws |
| `Str(number)` | to text; a leading space for non-negative numbers |
| `Hex(n)` `Oct(n)` | hexadecimal, octal |
| `IsNumeric(text)` `IsDate(text)` | could it be read as a number / a date? |
| `IsNothing(x)` | is the reference empty? |
| `TypeName(x)` | type name as text |

### Date and time

A `Date` is a `java.time.LocalDateTime`, so it combines freely with `java.time`.

| Function | Meaning |
|---|---|
| `Now()` `Today()` `TimeOfDay()` | now, midnight today, the time of day |
| `Year(d)` `Month(d)` `Day(d)` | date parts |
| `Hour(d)` `Minute(d)` `Second(d)` | time parts |
| `Weekday(d)` | **1 = Sunday** ... 7 = Saturday, as in VB |
| `MonthName(n)` `WeekdayName(n)` | names in the local language |
| `DateSerial(y, m, d)` `TimeSerial(h, m, s)` | build a date / a time |
| `DateAdd(unit, count, d)` | arithmetic |
| `DateDiff(unit, from, to)` | whole units between |
| `CDate(text)` | read from text |
| `Timer()` | seconds since midnight |

The units for `DateAdd` and `DateDiff` are VB's abbreviations: `"yyyy"` years, `"m"` months, `"d"` days, `"ww"` weeks, `"h"` hours, `"n"` **minutes**, `"s"` seconds.

### Formatting

| Function | Meaning |
|---|---|
| `Format(value, pattern)` | number or date by pattern |
| `FormatNumber(x, digits)` | with thousands separators |
| `FormatCurrency(x)` | as currency |
| `FormatPercent(x, digits)` | as a percentage |
| `FormatDateTime(d [, name])` | date in a named form |

Number patterns are those of `java.text.DecimalFormat`. **Date patterns are VB's**, not Java's, and the difference matters:

| VB | Means | Java would write |
|---|---|---|
| `mm` | month | `MM` |
| `nn` | minute | `mm` |
| `hh` | hour, **24-hour** | `HH` |
| `yyyy` `dd` `ss` | as in Java | |

```basic
Print Format(d, "dd.mm.yyyy hh:nn")      ' 15.01.2024 14:30
```

Untranslated, `hh:nn` would turn 14:30 into 02:30 — the right digits for the wrong half of the day, with no error message. That is why `Format` translates the patterns.

### Files

| Function | Meaning |
|---|---|
| `ReadAllText(path)` `ReadAllLines(path)` | whole file as text / as `String()` |
| `WriteAllText(path, text)` `WriteAllLines(path, lines)` | write; directories are created |
| `AppendAllText(path, text)` | append |
| `FileExists(path)` `DirExists(path)` | does it exist? |
| `FileLen(path)` `FileDateTime(path)` | size, timestamp |
| `Kill(path)` `FileCopy(from, to)` `Rename(from, to)` | delete, copy, move |
| `MkDir(path)` `RmDir(path)` `CurDir()` | directories |
| `Dir(pattern)` / `Dir()` | first match / next match |

> The VB6 statements `Open ... For Input As #1`, `Print #1`, `Input #1` and `EOF` do not exist. They would need their own syntax and a channel registry; in their place stand these whole-file calls, which VB6 did not have and which every program had to write for itself.

For anything beyond them, use `java.nio.file` directly — that is Chapter 10.

### Dialogs and system

| Function | Meaning |
|---|---|
| `MsgBox(text [, buttons [, title]])` | message; returns the button pressed |
| `InputBox(text [, title [, default]])` | input; Cancel gives `""` |
| `Beep()` | a tone |
| `Command()` | the command line as one string, without the program name |
| `CommandArgs()` | the same arguments separately, as `String()` |
| `Environ(name)` | environment variable, otherwise `""` |
| `Shell(command)` | run a command; **waits**, returns the exit value |

```basic
If MsgBox("Really delete?", rfxYesNo + rfxQuestion, "Question") = rfxYes Then
    Kill(path)
End If
```

Which window appears decides itself: with no display — a build server, `--headless` — `MsgBox` writes to the console and returns the default instead of crashing. Everywhere else it is Swing's `JOptionPane`, wearing whatever look and feel is installed.

**Constants.** Buttons `rfxOKOnly` `rfxOKCancel` `rfxAbortRetryIgnore` `rfxYesNoCancel` `rfxYesNo` `rfxRetryCancel`; icons (added on) `rfxCritical` `rfxQuestion` `rfxExclamation` `rfxInformation`; answers `rfxOK` `rfxCancel` `rfxAbort` `rfxRetry` `rfxIgnore` `rfxYes` `rfxNo`; text `rfxCrLf` `rfxNewLine` `rfxCr` `rfxLf` `rfxTab` `rfxNullString`.

### Miscellaneous

| Function | Meaning |
|---|---|
| `IIf(cond, then, else)` / `If(cond, then, else)` | value by condition, **short-circuiting** |
| `UBound(array)` `LBound(array)` | highest, lowest index |
| `Print` | output, see Chapter 3 |
| `Err` | the error object, see Chapter 7 |
# Part II — Using every Java class

## 9 Java is used, not rebuilt

The whole Java ecosystem is open: the JDK, Swing, any jar from Maven Central. There is nothing to register, nothing to generate and no layer in between.

### Finding types

```basic
Imports java.util
Imports javax.swing

Dim list As ArrayList = New ArrayList()              ' via Imports
Dim m As java.util.HashMap = New java.util.HashMap() ' fully qualified
Dim sb As StringBuilder = New StringBuilder()        ' java.lang is always there
```

`Imports` names **a Java package or a single class** — both, and the class spelled exactly as Java spells it:

```basic
Imports java.nio.file            ' the whole package
Imports java.nio.file.Files      ' only this one class
```

There is no tsbRapidFX namespace system of its own. A language whose purpose is the JDK uses the JDK's naming, or every example from the internet would have to be translated first. That is precisely why the second form exists: every Java example starts with `import java.nio.file.Files;`, and you should be able to carry that across line for line.

If both are named, **the explicitly named class wins**. `java.util.List` and `java.awt.List` are both called `List`; whoever writes one of them down means it.

An `Imports` with neither a package nor a class behind it is reported as a **warning**, `RFX0610` — not an error, because a package can be real and merely missing right now, for instance when a library has not been built yet.

**Nested classes** are written with a dot, not with the dollar sign that stands in the class file:

```basic
Dim e As java.util.Map.Entry = java.util.Map.entry("a", "b")
```

### Creating and using objects

```basic
Dim file As java.io.File = New java.io.File("/tmp/x.txt")
Print file.exists()
Print file.getAbsolutePath()
```

Static members through the type name:

```basic
Print Integer.MAX_VALUE
Print Math.max(3, 7)
Print Integer.parseInt("42")
Print System.getProperty("java.version")
```

> **`Integer` is two things, and both are right.** As a *type* it is `int`, so `Dim n As Integer` allocates nothing. As the *receiver* of a static member it is `java.lang.Integer`, so `Integer.MAX_VALUE` stays reachable. The same holds for `Double`, `Boolean` and the rest.

### Overloads

Which version of a method is meant is decided by Java's rules — fixed arity without boxing, then with boxing, then varargs, and within a stage the most specific wins:

```basic
Dim sb As StringBuilder = New StringBuilder()
sb.append("x=")      ' append(String)
sb.append(42)        ' append(int)   - not append(Object)
sb.append(1.5)       ' append(double)
sb.append(True)      ' append(boolean)
```

That the `int` version wins is not merely tidy: the `Object` version would allocate an object for every number. If nothing fits, the message lists the signatures that exist (`RFX0602`).

### Varargs

```basic
Print String.format("%s is %d year old", "tsbRapidFX", 1)
Print String.format("no arguments")
```

### Properties in the VB style

If a Java class follows the JavaBean convention, its state reads and writes like a variable:

```basic
window.Title = "tsbRapidFX"        ' calls setTitle
Print window.Width                 ' calls getWidth
Print button.Enabled               ' calls isEnabled
```

The shortcut applies only when there is **no real field** of that name and the matching `get`/`is`/`set` method exists. So it never invents a member; it only writes an existing one the way VB writes it. `file.exists()` stays a method call — `exists` is a question, not a property.

**Java method names are case-exact.** It is `signIn`, not `SignIn`. Only JavaBean *properties* get the VB spelling: `.Text`, `.Password`, `.Font`, `.ToolTipText`.

### Generics are erased

tsbRapidFX has no type arguments. An `ArrayList` is a raw `ArrayList`, and what comes out is `Object`:

```basic
Dim list As ArrayList = New ArrayList()
list.add("Hello")

Dim s As String = CType(list.get(0), String)
Print s.length()
```

That costs one `CType` at the point of extraction and nothing else. Putting things *in* boxes automatically: `list.add(5)` stores an `Integer`.

### Arrays

Arrays coming from Java behave like your own:

```basic
Dim parts As String() = "a,b,c".split(",")
Print parts.Length
Print parts(0)
```

### Interfaces and events

Where a single-method interface is expected, write a lambda or an `AddressOf`:

```basic
button.addActionListener(Sub(e)
    Print "clicked"
End Sub)

button.addActionListener(AddressOf Handle)

list.sort(Function(a, b) CType(a, String).compareTo(CType(b, String)))
```

And the other way round: a tsbRapidFX class may implement a Java interface, so that the JDK calls back into your code.

### What the compiler does with the class path

It **loads** the classes rather than reading the class files itself, so the JDK does the resolution across module path, jar and directory. A class therefore has to be loadable, which is the same condition the finished program is under anyway.

**Nothing is ever initialised.** Naming a class starts nothing: `Imports com.formdev.flatlaf` installs no look and feel. `Class.forName` always runs with `initialize=false`.

### A jar from Maven Central

Whatever is not in the JDK has to be on the class path:

```
java -jar rfxc-0.1.0.jar \
     -cp lib/flatlaf-3.7.2.jar \
     --run window.rfx
```

After that it is like everything else:

```basic
Imports com.formdev.flatlaf

FlatDarkLaf.setup()          ' from now on things are built dark
FlatLaf.updateUI()           ' and what is already built follows
```

---

## 10 Worked example: java.nio.file

This chapter is one package, followed all the way through, because `java.nio.file` shows every rule of Chapter 9 at once: a static-only class, a factory method, checked exceptions, varargs, an interface with erased generics, and a whole family of overloads.

The tsbRapidFX standard library already has `ReadAllText` and `WriteAllText` (Chapter 8). Everything below is the JDK's own, reached directly — which is the point: **there is no tsbRapidFX wrapper to wait for.**

### Files.readString and Files.writeString

The Javadoc says:

```
public static String readString(Path path) throws IOException
public static Path writeString(Path path, CharSequence csq, OpenOption... options)
       throws IOException
```

In tsbRapidFX:

```basic
Imports java.nio.file

Module Files1
    Sub Main()
        Var target As Path = Path.of("/tmp/rfxman/notes.txt")
        Files.writeString(target, "First line" + rfxNewLine + "Second line")
        Var text As String = Files.readString(target)
        Print text
        Print "Characters: " + Len(text)
        Files.deleteIfExists(target)
    End Sub
End Module
```

Output:

```
First line
Second line
Characters: 22
```

Four things happened there, and none of them needed anything special:

- **`Path.of(...)`** is a static factory on an interface. Interfaces with static methods are ordinary receivers; write the name and call it.
- **`throws IOException` was ignored.** `throws` is a Java *compiler* rule, not a JVM one, and tsbRapidFX does not have it. The exception still flies if the file is missing — you catch it when you want to.
- **`Files.writeString(target, text)`** picked the two-argument overload, with the `OpenOption...` varargs empty.
- **`rfxNewLine`** is the standard library's line separator, and `+` concatenated a `String` with it.

### The whole package, in one program

```basic
Imports java.nio.file
Imports java.nio.charset

Module Files2
    Sub Main()
        Var dir As Path = Path.of("/tmp/rfxman/work")
        Files.createDirectories(dir)

        Var file As Path = dir.resolve("report.txt")
        Files.writeString(file, "alpha" + rfxNewLine)
        Files.writeString(file, "beta" + rfxNewLine, StandardOpenOption.APPEND)
        Files.writeString(file, "gamma" + rfxNewLine, StandardCharsets.UTF_8, _
                StandardOpenOption.APPEND)

        Var line As Object
        For Each line In Files.readAllLines(file)
            Print "line: " + CStr(line)
        Next line

        Print "Exists: " + Files.exists(file)
        Print "Size: " + Files.size(file)

        Try
            Print Files.readString(dir.resolve("missing.txt"))
        Catch e As java.io.IOException
            Print "not readable: " + e.getMessage()
        End Try

        Files.deleteIfExists(file)
        Files.deleteIfExists(dir)
    End Sub
End Module
```

Output:

```
line: alpha
line: beta
line: gamma
Exists: true
Size: 17
not readable: /tmp/rfxman/work/missing.txt
```

Point by point:

- **`Files.writeString(file, text, StandardOpenOption.APPEND)`** — varargs need nothing; you simply pass the argument. `StandardOpenOption` is an enum in `java.nio.file`, so the `Imports` covers it.
- **The three-and-more-argument overload** takes a `Charset` before the options. `StandardCharsets.UTF_8` needs `Imports java.nio.charset`, which is a second package and a second `Imports` line — that is all.
- **`Files.readAllLines` returns a `List<String>`.** Generics are erased, so the loop variable is declared `As Object` and `CStr` turns it into text. Declaring it `As String` works too, and then the conversion happens for you:

```basic
Var line As String
For Each line In Files.readAllLines(file)
    Print "line: " + line
Next line
```

- **`Files.exists` returns a `boolean`** and `Print` writes it as `true`, Java-style.
- **`Files.size` returns a `long`** and prints as `17`, VB-style — no `.0`, no exponent.
- **The `Try/Catch`** catches `java.io.IOException`. `NoSuchFileException` is a subclass of it, so the broader branch is enough; a narrower one first would work too.

### Streams and lambdas over a directory

`Files.list`, `Files.walk` and `Files.lines` return a `java.util.stream.Stream`. It is `AutoCloseable`, so it belongs in a `Using`:

```basic
Imports java.nio.file

Using entries As java.util.stream.Stream = Files.list(Path.of("/tmp"))
    entries.forEach(Sub(p) Print CType(p, Path).getFileName())
End Using
```

Two rules from Chapter 9 meet here: the lambda fills the single-method interface `Consumer`, and because generics are erased its parameter arrives as `Object`, so `CType` says what you know it to be.

### Byte content

```basic
Var raw As Byte() = Files.readAllBytes(path)
Files.write(path, raw)
Print "bytes: " + raw.Length
```

`Byte()` is a tsbRapidFX array of `Byte`, which *is* a Java `byte[]`. `.Length` and `UBound(raw)` both read it.

### When to use which

| You want | Use |
|---|---|
| A whole small file, no fuss | `ReadAllText` / `WriteAllText` (Chapter 8) |
| Encoding, append, atomic move, permissions | `java.nio.file.Files` |
| A directory listing you can filter | `Files.list` / `Files.walk` in a `Using` |
| Line-by-line over something large | `Files.lines` in a `Using` |
| The VB idiom `Dir(pattern)` | `Dir` (Chapter 8) |

The standard library is the short road for the common case. `java.nio.file` is the whole road, and it is never further away than an `Imports`.

---

## 11 Reading Javadoc and writing tsbRapidFX

Everything in the JDK is available to you, and so is everything on Maven Central. What is *not* available is a tsbRapidFX version of their documentation — the Java 21 runtime image alone holds **28,094 classes**, 14,853 of them not counting nested ones. Nothing anyone could write would be more than a slice of that, and a slice that goes stale.

So the skill worth having is reading Javadoc as it is. It is shorter than it feels: most of a Javadoc page already reads the way you would write it, and the places where it does not come to about fifteen rules.

### Most of it reads straight across

```
Javadoc:   public void setTitle(String title)
tsbRapidFX: window.setTitle("Hello")
```

```
Javadoc:   public int indexOf(String str)
tsbRapidFX: Var at As Integer = text.indexOf("dampf")
```

No semicolons, no type in front of a call, and the argument types mean exactly what they say. If a Javadoc page shows you nothing but signatures, you can use it without knowing any of the rules below.

### The one that really bites: generics

```
Javadoc:   public interface List<E>
           E get(int index)
           boolean add(E e)
```

That `E` looks like a type you chose. In tsbRapidFX there is no type argument, so **`get` gives you back an `Object`** and you say what it is:

```basic
Imports java.util

Var names As ArrayList = New ArrayList()
names.add("World")

Var first As String = CType(names.get(0), String)
Print first.toUpperCase()
```

Putting things *in* needs nothing: `add(E)` accepts anything, and a number is boxed on the way. Taking things *out* is where the cast goes.

> **Why it is like this.** Generics are erased on the JVM: `List<String>` and `List<Integer>` are the same class at run time, and the type argument exists only in the Java compiler. tsbRapidFX does not carry one, so it tells you the truth — `Object` — instead of a promise it cannot keep.

### The rules, in one table

| Javadoc writes | You write | Note |
|---|---|---|
| `new ArrayList<>()` | `New ArrayList()` | no type argument |
| `E get(int)` | returns `Object` | `CType(..., String)` |
| `null` | `Nothing` | `Null` and `Nil` spell the same |
| `true` / `false` | `true` / `false` | either case; prints as `true` |
| `static` | `Shared` or `Static` | both work, any case |
| `final` on a field | read-only | assigning is an error |
| `throws IOException` | nothing | a hint, not an obligation |
| `for (String s : list)` | `For Each s In list` | |
| `String...` (varargs) | just pass the arguments | or pass one array |
| `int[] a` | `Dim a(9) As Integer` | `a.Length`, `UBound(a)` |
| `protected void m()` | `Public Overrides Sub m()` | reachable from a subclass |
| `Outer.Inner` | `Outer.Inner` | a dot, never a `$` |
| `int` / `Integer` | `Integer` | boxing happens for you |
| a one-method interface | a lambda | `Sub(e) ...` |
| `getX()` / `setX(v)` | `.X` also works | JavaBean property |
| `'c'` (char literal) | `"c".charAt(0)` | tsbRapidFX has no character literal |

### Checked exceptions do not exist here

```
Javadoc:   public static String readString(Path path) throws IOException
```

Java forces you to catch that or declare it. tsbRapidFX does neither:

```basic
Imports java.nio.file

Var text As String = Files.readString(Path.of("notes.txt"))
```

If the file is missing, the exception still flies; you catch it if you want to. **Read `throws` as a hint** — it tells you what can go wrong, and you decide whether to handle it.

### Extending a Java class

```
Javadoc:   protected void paintComponent(Graphics g)
```

`protected` means "for subclasses", and that is what it means here too:

```basic
Imports javax.swing
Imports java.awt

Public Class Canvas
    Inherits JPanel

    Public Overrides Sub paintComponent(g As Graphics)
        MyBase.paintComponent(g)
        g.drawString("drawn by tsbRapidFX", 20, 20)
    End Sub
End Class
```

A base class whose constructor is `protected` — `java.util.TimerTask` is the classic — can still be inherited from. What you cannot do is `New` it from outside, which is exactly what Java means by it.

> A base class with **no parameterless constructor at all** is a different matter: you must call `MyBase.New(...)` with arguments as the first statement, or the compiler reports `RFX0704`. That is worth remembering, because it is what stops some library base classes from being subclassed casually.

### What to ignore

- **Type parameters** — `<E>`, `<K,V>`, `<? super T>`. Read them as `Object`.
- **`throws`** — a hint, see above.
- **`final`** on a parameter or a local. It says nothing about how you call the method.
- **`@Override`, `@FunctionalInterface`** and other annotations. tsbRapidFX has no annotation syntax; nothing you write needs them.
- **Module names** (`java.base`, `java.desktop`) unless a library is not on your class path at all — and then it is a `-cp` question, not a code one.

### The editor already does half of this

Hover over a call and tsbRapidFX tells you the signature **in its own terms**, not Java's:

```
Function get(index As Integer) As Object
```

That line answers the generics question before you ask it. In the IDE this is faster than looking anything up — and it is the same information the compiler used, so it cannot disagree with what your program does.

### Where the documentation is

| Library | Address |
|---|---|
| JDK | `https://docs.oracle.com/en/java/javase/21/docs/api/` |
| FlatLaf | `https://javadoc.io/doc/com.formdev/flatlaf/latest/` |
| Anything on Maven Central | `https://javadoc.io/doc/<group>/<artifact>` |

Pick the version you actually compile against. Which JDK that is depends on the JVM running the compiler, not on tsbRapidFX.
# Part III — Databases

## 12 JDBC in tsbRapidFX

There is no database layer in tsbRapidFX, and there is not going to be one. `java.sql` is in the JDK, it is the interface every database in the world ships a driver for, and Chapter 9 already explained why using it is nothing special: `Connection`, `PreparedStatement` and `ResultSet` are ordinary Java types reached by ordinary calls.

What this chapter adds is the shape a database program takes in Basic, the two places where tsbRapidFX surprises a Java programmer, and a worked connection for each of the usual databases.

### The shape, once

```basic
Imports java.sql

Module Db
    Sub Main()
        Var url As String = "jdbc:h2:/tmp/shop"
        Using c As Connection = DriverManager.getConnection(url, "sa", "")
            Using s As Statement = c.createStatement()
                s.executeUpdate("CREATE TABLE IF NOT EXISTS customer (" + _
                        "id INT AUTO_INCREMENT PRIMARY KEY, " + _
                        "name VARCHAR(80) NOT NULL, city VARCHAR(80))")
            End Using

            Using ins As PreparedStatement = c.prepareStatement( _
                    "INSERT INTO customer (name, city) VALUES (?, ?)")
                ins.setString(1, "Anna Berger")
                ins.setString(2, "Hamburg")
                ins.executeUpdate()
            End Using

            Using q As PreparedStatement = c.prepareStatement( _
                    "SELECT id, name, city FROM customer WHERE city = ? ORDER BY name")
                q.setString(1, "Hamburg")
                Using rows As ResultSet = q.executeQuery()
                    Do While rows.next()
                        Print rows.getInt("id") + " | " + rows.getString("name") _
                                + " | " + rows.getString("city")
                    Loop
                End Using
            End Using
        End Using
    End Sub
End Module
```

That program was compiled and run against a real H2 database. Its output:

```
1 | Anna Berger | Hamburg
2 | Bert Cole | Hamburg
```

Everything worth knowing is in it:

- **`Using` is the right shape for every JDBC object.** `Connection`, `Statement`, `PreparedStatement` and `ResultSet` are all `AutoCloseable`, so `Using` closes them however the block is left — including on an exception. That is the `try`-with-resources of Java, written in Basic, and it is the reason a tsbRapidFX database program leaks nothing.
- **Parameters are `?` and are set by 1-based index**, exactly as JDBC defines them. Never build SQL by concatenating user input; a `PreparedStatement` is both faster and the only defence against injection that actually holds.
- **`Do While rows.next()`** is the loop. `rows.next()` returns a `boolean`, so the condition is already the right type.
- **Column access by name** — `rows.getString("name")` — survives a change in the `SELECT` order. By index, `rows.getString(2)`, is faster and more brittle; pick per taste, but pick one.
- **`Print rows.getInt("id") + " | " + ...`** works because `+` concatenates once one side is text (Chapter 3).

### Transactions

```basic
c.setAutoCommit(False)
Try
    Using ins As PreparedStatement = c.prepareStatement( _
            "INSERT INTO customer (name, city) VALUES (?, ?)")
        ins.setString(1, "Anna Berger")
        ins.setString(2, "Hamburg")
        ins.addBatch()
        ins.setString(1, "Bert Cole")
        ins.setString(2, "Hamburg")
        ins.addBatch()
        ins.executeBatch()
    End Using
    c.commit()
Catch e As SQLException
    c.rollback()
    Throw
Finally
    c.setAutoCommit(True)
End Try
```

`addBatch` / `executeBatch` is the difference between one round trip and a thousand. On a remote database it is usually the single largest speed-up available for an import.

### Reading a result into your own objects

```basic
Imports java.sql
Imports java.util

Public Class Customer
    Public Property Id As Integer
    Public Property Name As String
    Public Property City As String
End Class

Public Class CustomerStore
    Private url As String

    Public Sub New(connectionUrl As String)
        url = connectionUrl
    End Sub

    Public Function InCity(city As String) As ArrayList
        Var found As ArrayList = New ArrayList()
        Using c As Connection = DriverManager.getConnection(url)
            Using q As PreparedStatement = c.prepareStatement( _
                    "SELECT id, name, city FROM customer WHERE city = ? ORDER BY name")
                q.setString(1, city)
                Using rows As ResultSet = q.executeQuery()
                    Do While rows.next()
                        Var one As Customer = New Customer()
                        one.Id = rows.getInt("id")
                        one.Name = rows.getString("name")
                        one.City = rows.getString("city")
                        found.add(one)
                    Loop
                End Using
            End Using
        End Using
        Return found
    End Function
End Class
```

Reading them back out costs a `CType`, because the `ArrayList` is raw:

```basic
Var one As Object
For Each one In store.InCity("Hamburg")
    Print CType(one, Customer).Name
Next one
```

Auto-properties earn their keep here: `Public Property Name As String` generates the field and both accessors, so the class is three lines and Java sees a proper bean.

### Nulls

A SQL `NULL` comes back as `Nothing` for object types and as `0` / `False` for primitives — which is why `wasNull` exists:

```basic
Var amount As Double = rows.getDouble("amount")
If rows.wasNull() Then
    Print "no amount recorded"
Else
    Print FormatCurrency(amount)
End If
```

For a text column, the `Nothing` check is enough, and `+` tolerates it (Chapter 3), so `"[" + rows.getString("city") + "]"` prints `[]` rather than throwing.

Writing a null:

```basic
ins.setNull(2, Types.VARCHAR)
```

### Dates

A tsbRapidFX `Date` is a `java.time.LocalDateTime`, and modern JDBC drivers accept `java.time` directly through `getObject` / `setObject`:

```basic
Var due As Date = rows.getObject("due_at", GetType(java.time.LocalDateTime))
ins.setObject(3, Now())
```

`GetType(T)` is tsbRapidFX's way of writing Java's `T.class`. The older `rows.getTimestamp("due_at").toLocalDateTime()` works as well and is what you will find in most examples on the web.

### Two things that will catch you

These are not database problems. They are two places where tsbRapidFX behaves differently from Java, and both show up for the first time when somebody writes their first JDBC program.

**1. `Class` is a keyword, so `Class.forName` does not parse.**

```basic
Class.forName("org.h2.Driver")            ' RFX0103: expected an expression
java.lang.Class.forName("org.h2.Driver")  ' correct
```

Keywords are case-insensitive in tsbRapidFX (Chapter 3), which means the word `Class` is taken at the start of a statement no matter how it is spelled. Writing the type out fully sidesteps it, because after a dot there is never a keyword.

**2. `rfxc --run` does not auto-discover drivers.**

Since JDBC 4, `DriverManager` finds drivers through `ServiceLoader`. Under `--run` the program is loaded by a class loader of the compiler's making, and the service lookup does not reach the driver jar. You get:

```
java.sql.SQLException: No suitable driver found for jdbc:h2:/tmp/shop
```

Two remedies, and the second is the better one:

```basic
' Works under --run: register the driver by hand
java.lang.Class.forName("org.h2.Driver")
Var c As Connection = DriverManager.getConnection(url)
```

```
# Or compile and run the class normally, where ServiceLoader works as designed
rfxc -cp lib/h2-2.4.240.jar -o out Db.rfx
java -cp "out:rfxc-0.1.0.jar:lib/h2-2.4.240.jar" Db
```

A built jar (`--jar`) or a project (`--project`) is started by `java -jar` and therefore has no such problem. **`--run` is a convenience for quick tries; a database program should be built and started.**

### Connection pooling

`DriverManager.getConnection` opens a real connection every time, and on a network database that is tens of milliseconds. For anything serving more than one user, put a pool in front of it. HikariCP is the usual choice and it is an ordinary jar:

```basic
Imports com.zaxxer.hikari

Public Class Database
    Private Shared pool As HikariDataSource

    Public Shared Sub Open(url As String, user As String, secret As String)
        Var config As HikariConfig = New HikariConfig()
        config.setJdbcUrl(url)
        config.setUsername(user)
        config.setPassword(secret)
        config.setMaximumPoolSize(20)
        pool = New HikariDataSource(config)
    End Sub

    Public Shared Function Take() As java.sql.Connection
        Return pool.getConnection()
    End Function
End Class
```

`Using c As Connection = Database.Take()` then returns the connection to the pool at `End Using` instead of closing a socket.

> **This is one of the rare things that genuinely belongs in a `Shared` field**, even in a web application: a pool is infrastructure that every user shares alike, and it holds nobody's data. Chapter 19 draws that line precisely.

---

## 13 The databases, one by one

Everything below differs in exactly two places: the jar on the class path and the URL. The code from Chapter 12 does not change.

### What to put on the class path

| Database | Maven coordinates | Driver class |
|---|---|---|
| SQLite | `org.xerial:sqlite-jdbc` | `org.sqlite.JDBC` |
| H2 | `com.h2database:h2` | `org.h2.Driver` |
| PostgreSQL | `org.postgresql:postgresql` | `org.postgresql.Driver` |
| MySQL | `com.mysql:mysql-connector-j` | `com.mysql.cj.jdbc.Driver` |
| MariaDB | `org.mariadb.jdbc:mariadb-java-client` | `org.mariadb.jdbc.Driver` |
| SQL Server | `com.microsoft.sqlserver:mssql-jdbc` | `com.microsoft.sqlserver.jdbc.SQLServerDriver` |
| Oracle | `com.oracle.database.jdbc:ojdbc11` | `oracle.jdbc.OracleDriver` |
| Derby (embedded) | `org.apache.derby:derby` | `org.apache.derby.jdbc.EmbeddedDriver` |
| HSQLDB | `org.hsqldb:hsqldb` | `org.hsqldb.jdbc.JDBCDriver` |
| Firebird | `org.firebirdsql.jdbc:jaybird` | `org.firebirdsql.jdbc.FBDriver` |

The driver class is only needed for the `java.lang.Class.forName` case above. Normally the URL is enough.

### SQLite — a file, and nothing to install

```basic
Var url As String = "jdbc:sqlite:/var/lib/myapp/customers.db"
Using c As Connection = DriverManager.getConnection(url)
```

In memory, for a test that must leave nothing behind:

```basic
Var url As String = "jdbc:sqlite::memory:"
```

SQLite is the right answer whenever the database belongs to the program rather than to an organisation: an appliance, a desktop application, a small web service on one machine. There is no server, no user, no password, and the whole thing is one file you can copy.

Two things to know. It takes a **write lock on the whole file**, so concurrent writers queue; turn on WAL mode if that starts to hurt:

```basic
Using s As Statement = c.createStatement()
    s.execute("PRAGMA journal_mode = WAL")
    s.execute("PRAGMA foreign_keys = ON")
End Using
```

The second: foreign keys are **off by default**, which surprises everyone once.

### H2 — a file or a server, and pure Java

```basic
' A file, with the database staying open while the JVM lives
Var url As String = "jdbc:h2:/tmp/shop;DB_CLOSE_DELAY=-1"
Using c As Connection = DriverManager.getConnection(url, "sa", "")
```

```basic
' In memory
Var url As String = "jdbc:h2:mem:test;DB_CLOSE_DELAY=-1"

' A server somewhere
Var url As String = "jdbc:h2:tcp://localhost:9092/~/shop"
```

H2 has no native part at all, so the jar runs on every platform your program does — the same property that made FlatLaf the look and feel of choice. It also speaks a large part of PostgreSQL's dialect, which makes it a good stand-in for tests of a program that will run on PostgreSQL.

### PostgreSQL

```basic
Var url As String = "jdbc:postgresql://db.example.com:5432/shop"
Using c As Connection = DriverManager.getConnection(url, "shopuser", secret)
```

With options in the URL, which is where PostgreSQL likes them:

```basic
Var url As String = "jdbc:postgresql://db.example.com:5432/shop" _
        + "?ssl=true&sslmode=require&ApplicationName=Kundenverwaltung"
```

`RETURNING` is worth knowing, because it replaces a second round trip:

```basic
Using ins As PreparedStatement = c.prepareStatement( _
        "INSERT INTO customer (name, city) VALUES (?, ?) RETURNING id")
    ins.setString(1, "Anna Berger")
    ins.setString(2, "Hamburg")
    Using rows As ResultSet = ins.executeQuery()
        If rows.next() Then Print "new id: " + rows.getInt(1)
    End Using
End Using
```

### MySQL and MariaDB

```basic
' MySQL
Var url As String = "jdbc:mysql://db.example.com:3306/shop" _
        + "?useUnicode=true&characterEncoding=UTF-8&serverTimezone=UTC"

' MariaDB - same shape, different scheme
Var url As String = "jdbc:mariadb://db.example.com:3306/shop"

Using c As Connection = DriverManager.getConnection(url, "shopuser", secret)
```

> On MySQL, ask for `utf8mb4` in the schema rather than `utf8`. The older name is three bytes wide and cannot store an emoji or several perfectly ordinary CJK characters — a defect that surfaces months later as a truncated row.

Generated keys, the portable way:

```basic
Using ins As PreparedStatement = c.prepareStatement( _
        "INSERT INTO customer (name, city) VALUES (?, ?)", _
        Statement.RETURN_GENERATED_KEYS)
    ins.setString(1, "Anna Berger")
    ins.setString(2, "Hamburg")
    ins.executeUpdate()
    Using keys As ResultSet = ins.getGeneratedKeys()
        If keys.next() Then Print "new id: " + keys.getLong(1)
    End Using
End Using
```

That form works on MySQL, MariaDB, H2, SQLite and SQL Server alike, and it is the one to reach for when a program has to be portable.

### Microsoft SQL Server

```basic
Var url As String = "jdbc:sqlserver://db.example.com:1433" _
        + ";databaseName=shop;encrypt=true;trustServerCertificate=false"
Using c As Connection = DriverManager.getConnection(url, "shopuser", secret)
```

With Windows integrated security, and no user or password in the call:

```basic
Var url As String = "jdbc:sqlserver://db.example.com:1433" _
        + ";databaseName=shop;integratedSecurity=true;encrypt=true"
Using c As Connection = DriverManager.getConnection(url)
```

Note the semicolons: SQL Server separates its URL options with `;`, not `&`. Since driver version 10 `encrypt` defaults to `true`, which is why a connection that worked for years may start failing after an upgrade with a certificate complaint rather than a login one.

### Oracle

```basic
' Service name - the modern form
Var url As String = "jdbc:oracle:thin:@//db.example.com:1521/SHOPPDB"

' SID - the older form, still everywhere
Var url As String = "jdbc:oracle:thin:@db.example.com:1521:SHOP"

Using c As Connection = DriverManager.getConnection(url, "shopuser", secret)
```

Oracle's `ojdbc11` jar is not on Maven Central under a permissive licence in the way the others are; it comes from Oracle's own repository and carries their terms. That is a licensing matter, not a technical one — once the jar is on the class path, nothing in the code differs.

### Derby and HSQLDB — embedded, in pure Java

```basic
' Derby, creating the database on first use
Var url As String = "jdbc:derby:/var/lib/myapp/shop;create=true"

' HSQLDB, a file
Var url As String = "jdbc:hsqldb:file:/var/lib/myapp/shop;shutdown=true"
Using c As Connection = DriverManager.getConnection(url, "SA", "")
```

Both are alternatives to H2 with the same virtue: no native library, so one jar serves every platform.

### Firebird

```basic
Var url As String = "jdbc:firebirdsql://db.example.com:3050/shop?encoding=UTF8"
Using c As Connection = DriverManager.getConnection(url, "SYSDBA", secret)
```

### Building it all

Whatever the database, the build is the same two steps:

```
rfxc -cp "lib/*" -o out src/*.rfx
java -cp "out:rfxc-0.1.0.jar:lib/*" Main
```

or, as a project:

```
Name       = Kundenverwaltung
Sources = src
Library = lib/postgresql-42.7.4.jar
```

```
rfxc --project Kundenverwaltung.rfxproj
java -jar build/Kundenverwaltung.jar
```

The driver jar is named in the manifest as `Class-Path` and stays in `lib/` beside the built jar — not copied inside it. Move the two together.

### Using a database for sign-in

For a web application the credentials usually come from a database, and tsbWEB has that already. `tsbWebJdbcAuth` takes a supplier of connections and two queries:

```basic
Var users As tsbWebJdbcAuth = New tsbWebJdbcAuth( _
        Sub() java.sql.DriverManager.getConnection("jdbc:sqlite:/var/lib/tsbweb/users.db"), _
        "SELECT display_name, password_hash FROM users WHERE login = ?", _
        "SELECT role FROM user_roles WHERE login = ?")
```

The first argument is a lambda filling a single-method interface (Chapter 5). The queries are yours so that an existing user table does not have to be rebuilt. Passwords are checked with `PBKDF2WithHmacSHA256`, 210,000 rounds, salted per user — about 83 ms per attempt, out of the JDK, with no extra dependency. **The password is verified in this process and never sent to the database.** An unknown user costs the same time as a wrong password, because otherwise the difference would be a way to read out the user list.
# Part IV — User interface

## 14 Swing in tsbRapidFX

Swing is the interface toolkit for tsbRapidFX. Not one of several — the only one.

The reason is distribution. Swing is in the JDK: one jar, every platform, no `lib/` directory of native parts. JavaFX ships different jars per platform (`mac-aarch64`, `win`, ...), so a JavaFX program only runs on the platform it was built on. For a system whose promise is "build a jar and hand it over", that settled it.

The second reason is that the language fits. tsbRapidFX maps `btn.Text = "x"` onto `getText`/`setText` — JavaBeans. Swing *is* a JavaBean library. JavaFX wants `textProperty().bind(...)`, for which tsbRapidFX has no syntax.

### A window, by hand

```basic
Imports javax.swing
Imports java.awt

Public Class CounterWindow
    Private frame As JFrame
    Private display As JLabel
    Private state As Integer = 0

    Public Sub New()
        frame = New JFrame("tsbRapidFX")
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE)
        frame.setSize(400, 200)

        Var content As JPanel = New JPanel()
        content.setLayout(New GridLayout(2, 1, 8, 8))

        display = New JLabel("State: 0", SwingConstants.CENTER)

        Var button As JButton = New JButton("+1")
        button.addActionListener(Sub(e)
            state += 1
            display.Text = "State: " + state
        End Sub)

        With content
            .add(display)
            .add(button)
        End With

        frame.setContentPane(content)
    End Sub

    Public Sub Show()
        frame.Visible = True
    End Sub
End Class
```

Three tsbRapidFX habits are visible there and worth naming:

- **`display.Text = ...`** is `setText`. The JavaBean shortcut (Chapter 9) is what keeps generated form code from being a wall of setter calls.
- **The lambda captures `state` and `display`**, which are fields of the instance. Under the hood the body becomes a static method and the captured values become parameters; you never see that.
- **`With content`** evaluates the receiver once and saves repeating it.

### Look and feel

Swing's own look is Metal, and everybody replaces it in the first hour. FlatLaf is the one the templates bring:

```basic
Imports com.formdev.flatlaf

Sub Main()
    FlatLightLaf.setup()          ' before the first window
    SwingUtilities.invokeLater(Sub()
        New CounterWindow().Show()
    End Sub)
End Sub
```

Two rules, and forgetting either one is the usual cause of a half-themed window:

- **Set it before the first window is built.** `UIManager` decides only what is built *from now on*.
- **Switching at run time takes two calls.** `FlatDarkLaf.setup()` tells the `UIManager`; `FlatLaf.updateUI()` makes the windows already built follow. One alone is not enough.

FlatLaf is a single jar under Apache 2.0, identical on every platform. The `flatlaf-intellij-themes` jar adds around forty ported IntelliJ themes.

> **A look and feel is process-wide.** All windows share it, and — this matters in Chapter 21 — all *sessions* of a web server share it too. Per-user theming is not possible.

### Threading

Swing is single-threaded: components are touched on the event dispatch thread and nowhere else.

```basic
' Start the interface on the EDT
SwingUtilities.invokeLater(Sub()
    New CounterWindow().Show()
End Sub)

' From a worker thread back onto the EDT
SwingUtilities.invokeLater(Sub() display.Text = "done")
```

Long work does not belong in an event handler on either target. On the desktop it freezes the window; on the web it blocks that user's session thread (Chapter 21). Start a thread:

```basic
Var worker As java.lang.Thread = New java.lang.Thread(Sub()
    Var answer As String = LongQuery()
    SwingUtilities.invokeLater(Sub() display.Text = answer)
End Sub)
worker.start()
```

### What is in the box

Everything `javax.swing` has, because it *is* `javax.swing`. Beyond the JDK, two libraries were settled on for tsbRapidFX and both are in the designer's catalogue:

| Purpose | Library | Licence |
|---|---|---|
| Look and feel | FlatLaf | Apache 2.0 |
| Date and time pickers | swing-datetime-picker (DJ-Raven) | MIT |
| Charts | XChart (Knowm) | Apache 2.0 |

And two components written in tsbRapidFX itself, shipped as `tsbswing-1.0.jar`:

| Component | What it does |
|---|---|
| `ShowPicture` | a picture that actually scales — aspect ratio kept, cached, stepwise halving, built at the screen's own resolution |
| `tsbSmartTable` | a table you fill by throwing strings at it — Chapter 17 |

`JLabel` plus `ImageIcon` does not scale at all, and `Image.getScaledInstance` is the old area-averaging filter: slow, lazy, and it redoes the work on every repaint. Neither keeps the proportions or knows about a high-resolution screen. That is why `ShowPicture` exists, with `Fit`, `Fill`, `Stretch` and `Original`.

---

## 15 Windows and dialogs — desktop and web

This chapter answers two questions that come up on the first day and are answered very differently on the two targets: **how do I open another window and close this one**, and **how do I build a dialog**.

The short version:

| | Desktop | Web |
|---|---|---|
| Another screen | a second `JFrame`, `dispose()` the first | write the session, call `tsbWebSession.rebuild()` |
| Standard dialog | `JOptionPane.show...` | `tsbWebOptionPane.show...` |
| Dialog of your own | `JDialog(owner, title, True)` | `tsbWebDialog(content, title)` |
| Does it block? | yes, `setVisible(True)` returns on close | yes, `showModal()` returns on close |

### Desktop: opening a window and closing another

A window is a `JFrame`. Opening the next one and letting the current one go is two lines:

```basic
Private Sub OnOpenDetail(e As ActionEvent)
    Var detail As DetailWindow = New DetailWindow()
    detail.Show()
    frame.dispose()
End Sub
```

Order matters. Show the new window **first**, then dispose the old one. The other way round there is an instant in which no window is showing, and on some platforms that is enough for the application to lose focus or, if it was the last window, to end.

The whole of it, both directions:

```basic
Imports javax.swing
Imports java.awt
Imports java.awt.event

Module Windows
    Sub Main()
        SwingUtilities.invokeLater(Sub()
            New MainWindow().Show()
        End Sub)
    End Sub
End Module

Public Class MainWindow
    Private frame As JFrame = New JFrame("Main")

    Public Sub New()
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE)
        frame.setSize(420, 240)
        frame.setLocationRelativeTo(Nothing)

        Var toDetail As JButton = New JButton("Open details")
        toDetail.addActionListener(AddressOf OnOpenDetail)

        Var panel As JPanel = New JPanel()
        panel.add(toDetail)
        frame.setContentPane(panel)
    End Sub

    Public Sub Show()
        frame.Visible = True
    End Sub

    Private Sub OnOpenDetail(e As ActionEvent)
        Var detail As DetailWindow = New DetailWindow()
        detail.Show()
        frame.dispose()
    End Sub
End Class

Public Class DetailWindow
    Private frame As JFrame = New JFrame("Details")

    Public Sub New()
        frame.setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE)
        frame.setSize(420, 240)
        frame.setLocationRelativeTo(Nothing)

        Var back As JButton = New JButton("Back")
        back.addActionListener(Sub(e)
            New MainWindow().Show()
            frame.dispose()
        End Sub)

        Var panel As JPanel = New JPanel()
        panel.add(back)
        frame.setContentPane(panel)
    End Sub

    Public Sub Show()
        frame.Visible = True
    End Sub
End Class
```

**The close operation is the setting people get wrong.**

| Constant | What the close button does |
|---|---|
| `JFrame.EXIT_ON_CLOSE` | ends the whole program |
| `JFrame.DISPOSE_ON_CLOSE` | releases this window; the program lives on if another window remains |
| `JFrame.HIDE_ON_CLOSE` | hides it; the window stays in memory and can be shown again |
| `JFrame.DO_NOTHING_ON_CLOSE` | nothing — you handle it in a `WindowListener` |

Exactly one window in a program should carry `EXIT_ON_CLOSE`, and it should be the one whose closing genuinely means "I am finished". Every secondary window gets `DISPOSE_ON_CLOSE`. Putting `EXIT_ON_CLOSE` on all of them is the reason a program sometimes ends when a report window is closed.

`dispose()` releases the window and its native peer. `setVisible(False)` merely hides it, so the window and everything in it stay in memory — which is what you want if the user will come back to it, and a leak if they will not.

**Asking before closing.** `WindowListener` has seven methods, so it is not a single-method interface and a lambda will not fill it. Inherit from `WindowAdapter` instead, which implements the other six as empty bodies, and override the one you want:

```basic
Public Class ClosingGuard
    Inherits java.awt.event.WindowAdapter

    Private frame As JFrame

    Public Sub New(owner As JFrame)
        frame = owner
    End Sub

    Public Overrides Sub windowClosing(e As java.awt.event.WindowEvent)
        Var answer As Integer = JOptionPane.showConfirmDialog(frame, _
                "Discard unsaved changes?", "Close", JOptionPane.YES_NO_OPTION)
        If answer = JOptionPane.YES_OPTION Then frame.dispose()
    End Sub
End Class
```

```basic
frame.setDefaultCloseOperation(JFrame.DO_NOTHING_ON_CLOSE)
frame.addWindowListener(New ClosingGuard(frame))
```

### Desktop: standard dialogs

`JOptionPane` covers most of what a program asks:

```basic
' A message
JOptionPane.showMessageDialog(frame, "Saved.", "Done", _
        JOptionPane.INFORMATION_MESSAGE)

' A question
Var answer As Integer = JOptionPane.showConfirmDialog(frame, "Really delete?", _
        "Question", JOptionPane.YES_NO_OPTION, JOptionPane.WARNING_MESSAGE)
If answer = JOptionPane.YES_OPTION Then
    Delete()
End If

' A line of text; Cancel gives Nothing
Var name As String = JOptionPane.showInputDialog(frame, "Your name?", "Sign in", _
        JOptionPane.QUESTION_MESSAGE)
If name IsNot Nothing Then
    Print "Hello, " + name
End If
```

The first argument is the parent, and it decides which window the dialog centres over and which window it blocks. Passing `Nothing` centres on the screen.

The standard library's `MsgBox` and `InputBox` (Chapter 8) are the same thing in VB's spelling, with one extra courtesy: **with no display they fall back to the console** instead of throwing, so a program with a message does not die on a build server.

### Desktop: a dialog of your own

For anything with more than one field, build a `JDialog`. Created modal, `setVisible(True)` **blocks** until the dialog closes — which is what lets you write the answer-handling on the next line:

```basic
Private Sub OnEdit(e As ActionEvent)
    Var dialog As JDialog = New JDialog(frame, "Customer", True)   ' True = modal
    Var field As JTextField = New JTextField("Anna Berger", 20)

    Var ok As JButton = New JButton("OK")
    ok.addActionListener(Sub(ev)
        result = field.Text
        dialog.dispose()
    End Sub)

    Var cancel As JButton = New JButton("Cancel")
    cancel.addActionListener(Sub(ev)
        result = ""
        dialog.dispose()
    End Sub)

    Var body As JPanel = New JPanel()
    body.add(field)
    body.add(ok)
    body.add(cancel)

    dialog.setContentPane(body)
    dialog.pack()
    dialog.setLocationRelativeTo(frame)
    dialog.Visible = True          ' blocks here

    Print "answer: " + result      ' runs after the dialog closed
End Sub
```

Four details:

- **`New JDialog(frame, "Customer", True)`** — the third argument is modality. Without it the dialog is modeless, `setVisible` returns immediately, and the line after it runs before the user has answered.
- **The result travels in a field**, because a lambda can write a field of the enclosing instance but cannot return a value to the code after `setVisible`.
- **`pack()`** sizes the dialog to its content's preferred size. Use it instead of `setSize` for a dialog whose contents depend on translated text.
- **`setLocationRelativeTo(frame)`** centres over the parent. `Nothing` centres over the screen.

A **designer-drawn form** goes into a dialog just as well — that is the usual case in a real program:

```basic
Var form As CustomerForm = New CustomerForm()
Var dialog As JDialog = New JDialog(frame, "Customer", True)
dialog.setContentPane(form.GetRootPane())
dialog.pack()
dialog.setLocationRelativeTo(frame)
dialog.Visible = True
```

### Web: there are no windows

On a server there is no `JFrame`. `new JFrame()` throws `HeadlessException` in the `Window` constructor, before any code of yours runs, and no amount of arrangement changes that: a `Window` needs a screen.

So the web target replaces the *idea* of a window rather than the class. **The browser is the window**, and what you swap is what the browser shows.

> **There is no `tsbWebFrame` in the shipped libraries.** An early design sketched one for hand-written windows; the built product does not have it. Everything below is the road that exists.

The mechanism is one function and one call:

```basic
Public Function createRoot(session As tsbWebSession) As Component
```

`createRoot` runs **once per session and again on every screen change**, on the session thread, with the session already in reach. Whatever it returns is an ordinary Swing component. To change screens, write into the session and ask for a rebuild:

```basic
Private Sub OnOpenDetails(e As ActionEvent)
    Screen = "details"                ' a SessionStatic - see Chapter 20
    tsbWebSession.rebuild()
End Sub
```

There is **no router and no navigation**. `createRoot` decides afresh:

```basic
Public Class ScreenApp
    Implements tsbWebApp

    Public Function title() As String
        Return "Screens"
    End Function

    Public Function createRoot(session As tsbWebSession) As Component
        If Not session.SignedIn Then Return New SignInScreen().GetRootPane()
        If "details".equals(WebWindows.Screen) Then
            Return New DetailScreen().GetRootPane()
        End If
        Return New MainScreen().GetRootPane()
    End Function
End Class
```

Three things about that, all of which matter:

- **The rebuild happens *after* the handler returns, never in the middle of it.** A handler that touches the old screen on its way out therefore cannot leave half a page behind.
- **A new instance per session, never a shared one.** Two users editing the same `JTextField` would watch each other type.
- **Signing in needs no code at all.** After a successful `signIn` the server notices the change and rebuilds by itself, so the handler has nothing to do on the success path.

```basic
Private Sub OnSignIn(e As ActionEvent)
    If Not tsbWebSession.signIn(login.Text, secret.Password) Then
        hint.Text = "Sign-in failed"
    End If
    ' On success: nothing. The server rebuilds.
End Sub
```

`tsbWebSession.signOut()` signs out and rebuilds in the same way.

### Web: standard dialogs

`tsbWebOptionPane` has the same method names, the same constants and the same return values as `JOptionPane`. Ported code changes one word:

```basic
Var answer As Integer = tsbWebOptionPane.showConfirmDialog( _
        Nothing, "Really delete?", "Question", False)

If answer = tsbWebOptionPane.YES_OPTION Then
    Delete()
    tsbWebOptionPane.showMessageDialog(Nothing, "Done.", "Finished")
End If
```

| Constant | Value |
|---|---|
| `YES_OPTION`, `OK_OPTION` | 0 |
| `NO_OPTION` | 1 |
| `CANCEL_OPTION` | 2 |
| `CLOSED_OPTION` | -1 |

The fourth argument of `showConfirmDialog` is **`withCancel`**: `False` gives Yes and No, `True` adds Cancel. There are two- and three-argument overloads as well, matching `JOptionPane`'s.

A caller that only checks for `YES_OPTION` therefore treats a dismissal as a no — which is the safe direction, and the one `JOptionPane` has always taken.

> **The buttons are labelled in German** — *Ja*, *Nein*, *Abbrechen* — in the version shipped at the time of writing. For an English-facing application, build the dialog yourself with `tsbWebDialog` (below), where the labels are yours.

**It blocks.** The line after `showConfirmDialog` runs only once the user has answered. Behind that is a nested event loop on the session thread — the same construction Swing's EDT uses to serve a modal `JDialog`. The waiting thread keeps taking work off its own queue, so the click carrying the answer arrives, the button fires, and the loop ends.

**Modality is enforced, not suggested.** While a dialog is open, events aimed at anything underneath it are dropped at the server. Not greyed out and hoped for.

The `parent` argument is accepted and ignored. On a desktop it decides which window to centre over; here there is one page.

`showInputDialog` exists too, with `(parent, message)`, `(parent, message, preset)` and `(parent, message, preset, title)`. Check that order against your ported line: it is *preset then title*, which is not where `JOptionPane` puts them in every one of its overloads.

Why `JOptionPane` needs replacing at all is narrow: its own `show...` methods build a `JDialog`, and a `JDialog` is a `Window`, and a `Window` cannot exist without a screen. Everything below that — the panel, the buttons, the layout — is ordinary Swing and is built the ordinary way.

### Web: a dialog of your own

`tsbWebDialog` takes a component and a title, and `showModal()` returns whatever `close(...)` was given:

```basic
Private Sub OnEdit(e As ActionEvent)
    Var form As CustomerDialog = New CustomerDialog("Anna Berger")
    Var dialog As tsbWebDialog = New tsbWebDialog(form.GetRootPane(), "Customer")
    form.Attach(dialog)

    Var answer As Object = dialog.showModal()      ' blocks here
    If answer Is Nothing Then
        Print "cancelled"
    Else
        Print "answer: " + CType(answer, String)
    End If
End Sub
```

```basic
Public Class CustomerDialog
    Private root As JPanel = New JPanel()
    Private field As JTextField
    Private owner As tsbWebDialog

    Public Sub New(preset As String)
        field = New JTextField(preset, 20)

        Var ok As JButton = New JButton("OK")
        ok.addActionListener(Sub(e) owner.close(field.Text))

        Var cancel As JButton = New JButton("Cancel")
        cancel.addActionListener(Sub(e) owner.close())

        root.add(field)
        root.add(ok)
        root.add(cancel)
    End Sub

    Public Sub Attach(dialog As tsbWebDialog)
        owner = dialog
    End Sub

    Public Function GetRootPane() As JPanel
        Return root
    End Function
End Class
```

Compare that with the desktop version above and the difference is exactly two things: the class name, and that the answer comes back from `showModal()` instead of out of a field. The second is an improvement — `close(value)` hands the result up directly, so there is no shared field to keep in step.

- **`showModal()` must run on the session thread.** From anywhere else it throws `IllegalStateException` with the thread's name in the message, because a dialog needs a queue to pump and there is none elsewhere. In practice this means: call it from an event handler, not from a worker thread you started.
- **`close()` with no argument** returns `Nothing`, which is the natural reading of Cancel.
- **The content is an ordinary component tree**, so a designer-drawn form does just as well as a hand-built panel.

### Web: files in and out

The web target adds one thing the desktop does not need, because a browser cannot reach the server's disk:

```basic
' Out
tsbWebFiles.send(reportBytes, "Report.pdf", "application/pdf")
tsbWebFiles.send("Number;Name" + rfxCrLf + "1;Anna", "Customers.csv")

' In - blocks, like JFileChooser.showOpenDialog
Var content As Byte() = tsbWebFiles.ask(".csv")
If content IsNot Nothing Then
    Load(content, tsbWebFiles.lastName())
End If
```

`ask` returns `Nothing` when the user cancelled. The download link is 256 bits, belongs to one session, is valid **once**, and expires after five minutes.

### Menus, on both targets

Menus are ordinary Swing and work unchanged in the browser, accelerators included:

```basic
Var bar As JMenuBar = New JMenuBar()
Var file As JMenu = New JMenu("File")

Var save As JMenuItem = New JMenuItem("Save")
save.setAccelerator(KeyStroke.getKeyStroke( _
        java.awt.event.KeyEvent.VK_S, java.awt.event.InputEvent.CTRL_DOWN_MASK))
save.addActionListener(AddressOf OnSave)
file.add(save)

file.addSeparator()
file.add(New JCheckBoxMenuItem("Show grid", True))
bar.add(file)

screen.add(bar, BorderLayout.NORTH)
```

Submenus, separators, check items and radio items all travel. A context menu attaches as usual: `field.setComponentPopupMenu(popup)`.

> On the web, **the modifiers travel raw**. The server might say Command because it is a Mac; a user on Windows has to read Ctrl. The browser labels and recognises them for *its* platform.

### What the web cannot do

Named plainly, because finding out later is worse:

- **The focus lives in the browser.** `requestFocus()` from the program has no effect.
- **`javax.swing.Timer` fires on the real EDT** and is not currently redirected to the session thread. For anything repeating, start a thread of your own — it inherits the session automatically.
- **The look and feel is process-wide.** All sessions share it; per-user theming is not possible.
- **A long handler blocks its session** — exactly as on the desktop, and with the same remedy.

---

## 16 JTable — the full way

`JTable` is the table when you need sorting, renderers, mixed column types or a model of your own. It is the JDK's, so everything written about it anywhere applies here unchanged.

### The shortest complete table

```basic
Imports javax.swing
Imports javax.swing.table
Imports java.awt

Public Class TableDemo

    Private grid As JTable
    Private model As DefaultTableModel

    Public Function Build() As JComponent
        Var columns(2) As Object
        columns(0) = "Id"
        columns(1) = "Name"
        columns(2) = "City"

        model = New DefaultTableModel(columns, 0)
        grid = New JTable(model)
        grid.setAutoCreateRowSorter(True)
        grid.setSelectionMode(ListSelectionModel.SINGLE_SELECTION)
        grid.setRowHeight(22)
        grid.getSelectionModel().addListSelectionListener(AddressOf OnRowPicked)

        Var row(2) As Object
        row(0) = Integer.valueOf(1)
        row(1) = "Anna Berger"
        row(2) = "Hamburg"
        model.addRow(row)

        Return New JScrollPane(grid)
    End Function
End Class
```

Points that are tsbRapidFX rather than Swing:

- **`Var columns(2) As Object`** gives *three* slots, indices 0 to 2. The number in the brackets is the upper bound (Chapter 3). There are no array literals, so the columns are filled by index.
- **`Integer.valueOf(1)`** rather than plain `1`, because `DefaultTableModel` wants an `Object[]` and the boxed form says so plainly. Passing `1` boxes it for you as well; writing it out is clearer about what the array holds.
- **`New JScrollPane(grid)`** is not optional. A bare `JTable` draws no column headers — the header is a separate component that only a scroll pane puts in place.

### Selection

```basic
Private Sub OnRowPicked(e As javax.swing.event.ListSelectionEvent)
    If e.getValueIsAdjusting() Then Return
    Var picked As Integer = grid.getSelectedRow()
    If picked < 0 Then Return
    Var actual As Integer = grid.convertRowIndexToModel(picked)
    Print "picked: " + CStr(model.getValueAt(actual, 1))
End Sub
```

Three guards, and each of them is a real bug if you leave it out:

- **`getValueIsAdjusting()`** is true while the mouse is still down, so a drag across five rows fires five times. Return early on it and you get one event per selection.
- **`picked < 0`** means nothing is selected. Clearing a selection also fires the listener.
- **`convertRowIndexToModel`** is the one people forget. With `setAutoCreateRowSorter(True)` the visible order is not the model order, so row 0 on screen may be row 47 in the model. Reading the model with the view index gives the wrong record — silently, and only after somebody clicks a column header.

### Your own model

`DefaultTableModel` stores `Object`s and knows nothing about your data. For real data, extend `AbstractTableModel` and let it read the list you already have:

```basic
Imports javax.swing.table
Imports java.util

Public Class CustomerModel
    Inherits AbstractTableModel

    Private rows As ArrayList = New ArrayList()

    Public Sub SetRows(source As ArrayList)
        rows = source
        fireTableDataChanged()
    End Sub

    Public Overrides Function getRowCount() As Integer
        Return rows.size()
    End Function

    Public Overrides Function getColumnCount() As Integer
        Return 3
    End Function

    Public Overrides Function getColumnName(column As Integer) As String
        Select Case column
            Case 0
                Return "Id"
            Case 1
                Return "Name"
            Case 2
                Return "City"
        End Select
        Return ""
    End Function

    Public Overrides Function getColumnClass(column As Integer) As java.lang.Class
        If column = 0 Then Return GetType(java.lang.Integer)
        Return GetType(String)
    End Function

    Public Overrides Function getValueAt(row As Integer, column As Integer) As Object
        Var one As Customer = CType(rows.get(row), Customer)
        Select Case column
            Case 0
                Return java.lang.Integer.valueOf(one.Id)
            Case 1
                Return one.Name
            Case 2
                Return one.City
        End Select
        Return ""
    End Function

    Public Overrides Function isCellEditable(row As Integer, column As Integer) As Boolean
        Return column > 0
    End Function

    Public Overrides Sub setValueAt(value As Object, row As Integer, column As Integer)
        Var one As Customer = CType(rows.get(row), Customer)
        If column = 1 Then one.Name = CStr(value)
        If column = 2 Then one.City = CStr(value)
        fireTableCellUpdated(row, column)
    End Sub
End Class
```

**`getColumnClass` is what earns the effort.** It is how the table knows to right-align numbers, draw a `Boolean` as a checkbox and a `LocalDate` in the locale's format — none of which `DefaultTableModel` can do, because it stores everything as `Object`.

`GetType(T)` is tsbRapidFX's `T.class` (Chapter 3). Note `java.lang.Integer` written out: as a *type* the bare word `Integer` means `int`, which is not a class object.

### Renderers

A renderer decides how one cell looks. Because it is a single-method interface, a lambda fills it:

```basic
Imports javax.swing.table
Imports java.awt

Var money As TableCellRenderer = Function(table, value, selected, focused, row, column)
    Var label As JLabel = New JLabel(FormatCurrency(CDbl(value)))
    label.setHorizontalAlignment(SwingConstants.RIGHT)
    label.setOpaque(True)
    If selected Then
        label.setBackground(table.getSelectionBackground())
        label.setForeground(table.getSelectionForeground())
    End If
    Return label
End Function

grid.getColumnModel().getColumn(3).setCellRenderer(money)
```

For anything used on thousands of rows, prefer extending `DefaultTableCellRenderer` and reconfiguring the one component it already returns — building a fresh `JLabel` per cell is the usual reason a large table scrolls badly.

### Sorting and filtering

```basic
Var sorter As TableRowSorter = New TableRowSorter(model)
grid.setRowSorter(sorter)

' Sort by name, ascending
Var keys As ArrayList = New ArrayList()
keys.add(New javax.swing.RowSorter.SortKey(1, javax.swing.SortOrder.ASCENDING))
sorter.setSortKeys(keys)

' Filter as the user types
search.getDocument().addDocumentListener(New SearchWatcher(sorter, search))
```

A `Comparator` for a column is a lambda:

```basic
sorter.setComparator(2, Function(a, b) _
        CType(a, String).compareToIgnoreCase(CType(b, String)))
```

### Column widths

```basic
grid.setAutoResizeMode(JTable.AUTO_RESIZE_OFF)
grid.getColumnModel().getColumn(0).setPreferredWidth(60)
grid.getColumnModel().getColumn(1).setPreferredWidth(240)
```

`JTable`'s own resizing spreads spare width **evenly** rather than in proportion to the preferred widths, which is why setting preferred widths and leaving auto-resize on does not do what it looks like it should. Switching it off and computing the pixels is the only way a proportion means a proportion — and that is exactly what `tsbSmartTable` does for you in the next chapter.

### In the browser

A `JTable` works in a tsbWEB application unchanged, and it travels as **structure, not as a picture**: the browser builds a real table you can select in, sort and walk with the arrow keys.

It is also **virtualised**. Ten thousand rows are no problem — the server sends a window of about 200 rows around whatever the user is looking at and adds more as they scroll. Un-windowed it would be some 695 kB of cells and a field 200,000 pixels tall.

The cell text comes from **the renderer**, not the raw value, so a `LocalDate` appears as `2026-09-05` and your own `TableCellRenderer` still decides how an amount looks. Sorting is done by Swing's own `RowSorter`, your `Comparator` included; the browser only reports which header was hit.

---

## 17 tsbSmartTable — the short way

`tsbSmartTable` is a table you fill by throwing strings at it. It sits **beside** `JTable`, not instead of it: when you need sorting, renderers, mixed column types or a model of your own, take the `JTable`. This one is for the other nine times out of ten.

> **On the name.** The class is `tsbSmartTable`; in the designer's palette it appears as **SmartTable**. It lives in `tsbswing-1.0.jar`, is written in tsbRapidFX itself, and is compiled by `rfxc` — the designer generates the language its own library is written in.

### The whole idea

```basic
table.Columns = "Name, Age, City"
table.Add("Ada")
table.Add("36")
table.Add("London")
table.Reload()
```

The headers decide how many columns there are, so three headers make every three values a row. **Nothing else has to be declared, and there is no model to build.** A `JTable`'s model is in there, but it is not yours to touch.

That one flat list is the whole trick. Throwing values in one after another is how a table is actually filled — out of a query, out of a file, out of a loop — and having to build a row object first is the step that makes `JTable` feel heavy.

### A worked example

```basic
Imports javax.swing
Imports java.awt

Public Class QuickDemo

    Private quick As tsbSmartTable

    Public Function Build() As JComponent
        quick = New tsbSmartTable()
        quick.Columns = "Id, Name, City"
        quick.ColumnWidths = "60, 50%, 50%"
        quick.EditableColumns = "1, 2"

        quick.AddRow(1, "Anna Berger", "Hamburg")
        quick.AddRow(2, "Bert Cole", "Bremen")

        quick.Add(3)
        quick.Add("Cem Demir")
        quick.Add("Kiel")

        quick.Reload()

        Print "rows: " + quick.RowCount()
        Print "cell: " + quick.Value(0, 1)
        Return quick
    End Function
End Class
```

Output:

```
rows: 3
cell: Anna Berger
```

`Add` takes an `Object`, not a `String`, on purpose: a table is mostly filled out of a query or a loop, and half of what goes in is a number. Having to write `CStr` around every one of them would be exactly the ceremony this class exists to remove. `AddRow` is a `ParamArray` for when you have the values together anyway.

**Fill first, show once.** `Reload` empties the display and puts out everything currently in the list — a thousand rows are one event rather than a thousand. Ten thousand rows take about 2 ms.

### Straight out of a query

This is what the class is for:

```basic
Imports java.sql

Public Sub Load(c As Connection, city As String)
    quick.Clear()
    Using q As PreparedStatement = c.prepareStatement( _
            "SELECT id, name, city FROM customer WHERE city = ? ORDER BY name")
        q.setString(1, city)
        Using rows As ResultSet = q.executeQuery()
            Do While rows.next()
                quick.Add(rows.getInt("id"))
                quick.Add(rows.getString("name"))
                quick.Add(rows.getString("city"))
            Loop
        End Using
    End Using
    quick.Reload()
End Sub
```

Compare that with the `AbstractTableModel` of Chapter 16 — a class, eight overrides and a list of objects — and the trade is plain. You give up column classes, renderers and sorting; you get eleven lines.

### The properties

| Property | Meaning |
|---|---|
| `Columns` | headers, comma separated: `"Name, Age, City"`. Setting them decides the column count |
| `ColumnWidths` | widths, comma separated, in header order — see below |
| `EditableColumns` | which columns can be typed into, by index: `"1, 2"`. Empty means none |
| `RowHeight` | height of one row, in pixels |
| `ShowGrid` | draw the grid lines |

| Method | Meaning |
|---|---|
| `Add(value)` | throw one value in |
| `AddRow(v1, v2, ...)` | a whole row at once |
| `Clear()` | empty the list |
| `Reload()` | empty the display and put everything out — one event |
| `RowCount()` / `ColumnCount()` | the current shape |
| `Value(row, column)` | one cell, edits included |
| `SetValue(row, column, text)` | write one cell as if it had been typed |
| `Values()` | every value in order, edits included |
| `Table()` | the `JTable` inside, for anything not wrapped |

### Column widths that mean something

Two kinds, and they mix:

```basic
quick.ColumnWidths = "50%, 25%, 25%"     ' shares - each keeps its fraction as the window changes
quick.ColumnWidths = "160, 60, 200"      ' pixels - fixed, and they stay fixed
quick.ColumnWidths = "200, 50%, 50%"     ' the first is fixed, the other two share what is left
```

Shares are the useful kind: a form that can be resized has no business pinning a name column to 160 pixels. **The numbers need not add up to a hundred** — what counts is their ratio, so `"1, 2, 1"` is a quarter, a half and a quarter. Empty means an equal share for every column; a zero or an unreadable entry leaves that column alone.

This is the one thing `tsbSmartTable` does that a plain `JTable` will not do for you. `JTable`'s own resizing spreads spare width evenly rather than in proportion, so `"1%, 2%, 1%"` on a raw table comes out as three equal columns — measured, not assumed. `tsbSmartTable` switches auto-resize off and computes the pixels at layout time, giving the rounding remainder to the last share so the columns add up to the table exactly instead of leaving a one-pixel gap at the right.

It is also the only table whose column widths the designer can set.

### Editing, and reading the edits back

An edit writes straight back into the same flat list you filled, so reading the changes out afterwards is the same call as putting them in:

```basic
quick.EditableColumns = "1, 2"
' ... the user types ...
Print quick.Value(0, 1)          ' one cell
Var everything As ArrayList = quick.Values()   ' all of it, in order
```

There is no second copy to keep in step. That is a deliberate difference from the `JTable` route, where the model and your object list are two things and keeping them together is your problem.

### What is underneath

Worth knowing, because it explains the two rough edges:

- **It inherits from `JScrollPane`** with a `JTable` inside, and it sets the column header explicitly in its constructor. A bare `JTable` picks its header up only once it has been added to a window — `JTable` does it from `addNotify` — so a table painted without ever being shown, which is exactly what a designer canvas does, would otherwise have no heading at all.
- **There is a `TableModel` in there.** "No model" is a promise about the *API*, not about the construction: a `JTable` without a model does not exist, and hand-drawing a grid would mean rebuilding selection, keyboard handling, scrolling and editing.

Because it inherits from `JScrollPane`, you do **not** wrap it in one:

```basic
panel.add(quick)                    ' right
panel.add(New JScrollPane(quick))   ' wrong - a scroll pane in a scroll pane
```

### Reaching the JTable inside

For anything the wrapper does not cover — selection listeners, renderers, a sorter — `Table()` hands out the real `JTable`, and it behaves like one:

```basic
quick.Table().setSelectionMode(ListSelectionModel.SINGLE_SELECTION)
quick.Table().getSelectionModel().addListSelectionListener(AddressOf OnPicked)
quick.Table().setAutoCreateRowSorter(True)
```

Once you find yourself using `Table()` for three different things, that is the signal to move to the `JTable` route of Chapter 16.

### Which one to use

| Take | When |
|---|---|
| `tsbSmartTable` | the data is text or numbers, it comes out of a query or a loop, and you want it on screen |
| `tsbSmartTable` | you want proportional column widths without writing a layout listener |
| `JTable` | columns have types — a checkbox, a date, a right-aligned amount |
| `JTable` | you need sorting, filtering or your own renderer |
| `JTable` | the data lives in objects of yours and the table should read them, not copy them |
# Part V — Web applications

## 18 Writing a web application

**You write Swing.** There is no web-only component, no annotation, no markup and no template language. The same forms show the same screens on a desktop if you hand them to a `JFrame` instead of to a server.

The server is `tsbWEB`. What it does with your Swing:

- **Swing computes the geometry.** Layout managers and font metrics run without a screen, and the browser gets finished coordinates. **No layout is rebuilt in CSS.**
- **The look and feel paints every component itself, as SVG.** FlatLaf's tick, its dropdown arrow, its slider knob — not approximated, drawn. Vector, not a bitmap: sharp when zoomed, and small.
- **Real HTML elements sit on top** wherever something is typed, chosen or scrolled. A `JTextField` gets its border painted and a real `<input>` inside it, with caret, selection and input method.
- **Tables, lists and trees travel as data**, not as a picture. The browser builds a real table you can select in, sort and walk with the arrow keys.

### The three building blocks

**1. `tsbWebApp` — one decision.**

```basic
Public Class Application
    Implements tsbWebApp

    Public Function title() As String
        Return "My web project"
    End Function

    Public Function createRoot(session As tsbWebSession) As Component
        If Not session.SignedIn Then Return New SignInForm().GetRootPane()
        If "customers".equals(Screen) Then Return New CustomerForm().GetRootPane()
        Return New MainForm().GetRootPane()
    End Function
End Class
```

`createRoot` runs **once per session and again on every screen change**, on the session thread, with the session already in reach. What it returns is an ordinary Swing component. For a form from the designer that is exactly `GetRootPane()`; `ShowIn` is never used, because it expects a `JFrame` and here the browser is the window.

> **A new instance per session, never a shared one.** Two users editing the same `JTextField` would watch each other type.

**2. `tsbWebSession` — what belongs to one user.** Reachable from anywhere, with no parameter and no static variable. Chapter 19 covers it in full.

**3. `rebuild()` — the screen change.** There is no router. Write into the session, ask for a rebuild, and `createRoot` decides afresh. Chapter 15 covers it in full.

### The entry point

```basic
Imports com.tsbweb.server
Imports com.tsbweb.session

Module Main
    Sub Main()
        Var config As tsbWebConfig = New tsbWebConfig()
        config.Port = 8099
        config.Titel = "Customer management"
        config.Thema = "FlatLightLaf"
        config.LeerlaufMinuten = 30

        Var users As tsbWebFixedAuth = tsbWebFixedAuth.builder() _
                .user("anna", "Anna Berger", "secret", java.util.Set.of("admin")) _
                .build()

        tsbWebStart.Go(config, New Application(), users)
    End Sub
End Module
```

One line, and the program is a server you can double-click. `tsbWebStart.Go` does four things in a fixed order, and **three of them fail silently in the wrong one**:

1. **`java.awt.headless` first.** `GraphicsEnvironment` answers that question once and remembers. A manifest cannot set a system property, so a double-clicked jar has nowhere else to put it, and on a machine with no display AWT would not start at all.
2. **The configuration file second**, because the theme may be in it. Reading a properties file touches no AWT.
3. **The look and feel third.** `UIManager` is process-wide, and a component keeps the appearance it was built with.
4. **The server last**, and then the main thread stays put — otherwise the program ends and takes the server with it.

`tsbWebStart.Serve(...)` does the same and returns instead of waiting. Without sign-in, `tsbWebStart.Go(config, New Application())` is enough.

> `config.Thema` is **not decoration**: the server paints every component with this look and feel and sends the drawing, so this is what the browser shows. Pick a theme in the designer and this line is updated for you when the form is saved — preview and server cannot drift apart.

### What an operator may change

A file **`tsbweb.properties` beside the jar** overrides what the program set:

```
port                = 8443
titel               = Customer management North
thema               = dunkel
zertifikat          = /etc/tsbweb/key.p12
zertifikat.passwort = secret
leerlauf.minuten    = 20
sitzungen.max       = 750     # without this line: no ceiling
```

Only keys that are present take effect, so a file with one line changes one thing. At start-up the output says **what was taken**:

```
Customer management North running on http://localhost:8443/
  Idle 20 min
  from tsbweb.properties: {port=8443, thema=dunkel, titel=Customer management North}
```

Otherwise somebody who edited the file and sees nothing happen could not tell a typo from a file in the wrong directory. **A typo is named, not swallowed:** `port = eightthousand` reports itself and the built-in value stays.

The certificate password belongs in the environment variable `TSBWEB_ZERT_PASSWORT` rather than in the file — it is read first. A password in a file survives a backup, a copy and a glance over the shoulder; if it has to be there, then with `chmod 600`. The start-up output never shows the value, only `(set)`.

### HTTP or HTTPS — never both

There are **two methods and no switch**:

```basic
tsbWebServer.http(8099, New Application(), users)

tsbWebServer.https(8443, New Application(), users, _
        java.nio.file.Path.of("/etc/tsbweb/key.p12"), password)
```

Which server is running decides whether the session cookie is marked `Secure`. There is no second opinion that can be wrong. The old arrangement — a flag beside the port — allowed two failures that only show up in production: an unencrypted server with a `Secure` cookie (the browser never sends it back, nothing works, nobody can see why), and an encrypted server without it (the session id travels in clear the moment a user meets an `http://` link). The same reasoning governs the `Strict-Transport-Security` header.

Whoever needs both starts **two servers** — and then knows they have two.

```
keytool -genkeypair -alias tsbweb \
        -keyalg RSA -keysize 2048 -validity 365 \
        -dname "CN=localhost, O=Your house, C=DE" \
        -storetype PKCS12 \
        -keystore key.p12 \
        -storepass YOURPASSWORD -keypass YOURPASSWORD
```

PKCS#12 or JKS — the type is detected from the file, not configured, so there is nothing to set wrongly. `https(...)` **overwrites the `char[]` it was given before it returns**, which is why it takes a `char[]` and not a `String`: a string cannot be overwritten and lies around until the garbage collector takes pity on it. TLS versions and cipher suites stay with the JDK, whose defaults are reviewed with every Java release; a list written down here would be a list nobody maintains.

### What the server serves

| Route | Purpose |
|---|---|
| `GET /` | the HTML shell, once |
| `GET /tsb/app.js`, `/tsb/app.css` | the browser half |
| `GET /tsb/stream` | the event stream (SSE): tree, patches, dialogs, files |
| `POST /tsb/event` | input, batched |
| `GET /tsb/file?t=...` | a download the program offered |
| `POST /tsb/upload` | a file the program asked for |

That is all of it. There are no per-page routes and no router: which screen a user sees is decided by `createRoot`.

**Why SSE and not WebSocket.** `com.sun.net.httpserver` offers **no protocol upgrade** — `HttpExchange` does not hand out the socket. A WebSocket would mean a second server on a second port with a hand-written frame codec. Server-sent events need none of that: an ordinary response that is never closed. Measured overhead is about 14 ms per event, and 200 simultaneous streams hold without trouble.

### Creating a project

In IntelliJ: **New Project → RapidFX — Web**. The wizard writes three `.rfx` files, copies four jars into `lib/` and adds the project file, `tsbweb.properties`, a README and a `.gitignore`. No Maven, no Gradle, no network — whoever creates a project has everything needed to compile and start it.

Two of the three files are **drawn in the designer**. Right-click → **Open in Designer** (Chapter 23).

By hand:

```
java -jar rfxc.jar --project MyProject.rfxproj
java -jar build/MyProject.jar
```

Or without a project file:

```
java -jar rfxc.jar -cp "lib/*" -o out MyProgram.rfx
java -Djava.awt.headless=true -cp "out:lib/*" Main
```

### Pitfalls, named

- **No static field for user data.** Harmless on the desktop, a data breach on the web: the second person to sign in reads the first one's. Chapter 20 is entirely about this.
- **A long handler blocks its session** — exactly as on the desktop. For long work, start a thread; it inherits the session automatically.
- **The look and feel is process-wide.** All sessions share it.
- **`javax.swing.Timer` fires on the real EDT** and is not redirected to the session thread. Use a thread for anything repeating.
- **The focus lives in the browser.** `requestFocus()` from the program has no effect.

```basic
Var worker As java.lang.Thread = New java.lang.Thread(Sub()
    ' tsbWebSession is reachable here - the scope is inheritable
    Var answer As String = LongQuery()
    display.Text = answer
End Sub)
worker.start()
```

---

## 19 Variables: where a value lives and how long

Every variable in tsbRapidFX answers two questions: **who can see it**, and **whose value is it**. On the desktop the second question has one answer and nobody notices it. On a server it has as many answers as there are users, and getting it wrong is the single most expensive mistake in the whole system.

This chapter lays out all five kinds side by side. The next chapter takes the fifth one apart.

### The five kinds

| Kind | Written | Seen by | Value belongs to |
|---|---|---|---|
| Local | `Dim x As Integer` in a body | the block it is in | this call |
| Parameter | `Sub S(x As Integer)` | the procedure | this call |
| Instance field | `Private x As Integer` in a class | the class (and `Private` means the class) | this object |
| Shared field | `Public Shared x As Integer` | everyone who can name the type | **the whole process** |
| Session static | `SessionStatic x As Integer` in the main module | everyone who can name the module | **one session** |

### Locals and parameters

```basic
Sub Count()
    Dim i As Integer          ' 0
    Dim name As String        ' "" - not Nothing
    Var total = 0             ' type from the initial value: Integer
    Const Vat As Double = 0.19
End Sub
```

A local lives from its declaration to the end of the procedure and dies with the call. Two calls, two values, even on the same thread — this is the only kind of variable that needs no thought at all, which is why it should be your first choice for everything.

Its type is fixed at the declaration. `Dim sum = 0` makes `sum` an `Integer` and no later assignment changes that.

A `For` variable declared in the header exists only inside the loop:

```basic
For i As Integer = 1 To 3
    Print i
Next i
' i is gone here
```

> The generated `LocalVariableTable` covers the whole method rather than the exact scope, so a *debugger* shows names outside their block. That is debug information only; the compiler still enforces the scope.

**A local hides a field of the same name**, and a parameter is a local. That rule is what makes the constructor idiom work:

```basic
Public Sub New(owner As String)
    Me.owner = owner          ' Me. reaches the field the parameter hides
End Sub
```

### Instance fields

```basic
Public Class Account
    Private owner As String
    Private balance As Double = 0
End Class
```

One value per object. A field with an initial value is set **in every constructor before its body runs**, so a second constructor cannot forget it.

`Private` is enforced: it means *the class*, not the family. A subclass cannot see it, and neither can another class in the same file. What still works — and every generated form depends on it — is `AddressOf` on a private method of the same class:

```basic
button.addActionListener(AddressOf OnClick)   ' OnClick may be Private
```

**A form lives entirely in its instance.** Both designers hold to that rule, and it is why two sessions showing the same form are independent without anybody arranging it.

### Shared fields

```basic
Public Class Tools
    Public Shared Counter As Integer = 0

    Public Static Sub Report(text As String)     ' the same word, spelled differently
        Counter += 1
        Print "[" + Counter + "] " + text
    End Sub
End Class
```

`Shared` and `Static` are two spellings of one idea — `Shared` from VB.NET, `Static` from Java, Xojo and B4J. In a `Module` neither is needed: everything there is shared anyway.

One value **for the whole process**. On a desktop that means one value for the one user sitting there. On a server it means one value for everybody at once, and that is where the trouble starts.

Initialisers of shared fields run in the class initialiser, the first time the class is touched.

> VB6's `Static` — a *local* that keeps its value between calls — is a different thing and is not built. It would stand inside a body, not in front of a declaration, so the two cannot collide.

### Session statics

```basic
Module Main
    SessionStatic UserName As String
    SessionStatic Basket As java.util.ArrayList

    Sub Main()
        ...
    End Sub
End Module
```

Reachable exactly like a `Shared` — by bare name inside the module, qualified as `Main.UserName` from anywhere else — but **one value per session**. `SessionShared` is a synonym, for the same reason `Static` is a synonym for `Shared`.

On a desktop a program has exactly one session, so a session static there behaves like an ordinary static. On a web server each user gets their own. **The same program, unchanged, isolates per user the moment a server is underneath it.** That is the whole point of the keyword, and Chapter 20 is about how it is built and when to use it.

Two rules the compiler enforces:

```basic
Public Class Holder
    Public SessionStatic Counter As Integer     ' RFX0208
End Class
```

```
error RFX0208: 'Counter' is SessionStatic, but 'Holder' does not hold 'Sub Main'.
Session statics are declared in the main module only, so that every one of them is
in one place.
```

```basic
Public Shared SessionStatic Counter As Integer  ' RFX0209
```

```
error RFX0209: 'Counter' cannot be both SessionStatic and Shared. A SessionStatic
has one value per session; a Shared has one for all of them.
```

### The session's own named values

Beside session statics there is a second per-session store, reached through the session object itself:

```basic
tsbWebSession.setValue("screen", "customers")
Var screen As Object = tsbWebSession.getValue("screen")

Print tsbWebSession.hasValue("screen")
tsbWebSession.removeValue("screen")
Var names As java.util.Set = tsbWebSession.valueNames()
```

**This is the escape hatch, not the main road.** A declared `SessionStatic` is typed, is found by completion, and fails at compile time when it is misspelled. A name in this map is none of those things — `getValue("scren")` returns `Nothing` and says nothing. Use it when the set of keys is genuinely dynamic; use a `SessionStatic` otherwise.

The two are separate maps on purpose, even though merging them would save a field. The keys a session static uses are written by the compiler — `Main.Counter` — and a program that listed `valueNames()` would suddenly see them. Two maps, two audiences: one the program's own, one the language's.

### The bare name `tsbWebSession`

The compiler adds exactly one name for web programs, and it adds it **at the very end of name resolution**:

```basic
If tsbWebSession.SignedIn Then
    Print "Hello, " + CType(tsbWebSession.User.get(), tsbWebUser).DisplayName
End If
```

The name binds to a lookup of the current thread's session, and everything after that is an ordinary instance access. Static entry point, per-thread state — the same pattern `Err` already uses.

**A program's own member of that name always wins.** This was checked for every kind of thing a name can already be, because the lookup tries them in order and one of them being wrong would be enough:

| If your program has | Then `tsbWebSession` means |
|---|---|
| a local of that name | the local |
| a parameter of that name | the parameter |
| a field of that name | the field |
| a property of that name | the property |
| a function of that name | the function |
| a type of that name | that type |
| none of these | the current session |

So the addition cannot change the meaning of an existing program. It is also deliberately unlike `Err`, whose alias sits *before* the program's own types and therefore hides a `Class Err` of your own; that pattern was not copied.

The lookup itself is an **`InheritableThreadLocal`**, so a background thread that your code starts inherits the session of the thread that started it. Without that, `tsbWebSession` would read `Nothing` inside every worker.

> **A caveat, stated plainly.** The documentation for tsbWEB describes declaring your own `tsbWebSession` subclass with typed fields, and the compiler does support binding to it. The server, however, always creates the base `tsbWebSession`: `tsbWebSessionRegistry` has a `Factory` for a custom type, but `tsbWebServer` hard-codes the base one and `tsbWebConfig` has no setting for it. There is therefore **no way to get your own session class instantiated through `tsbWebStart.Go`** as things stand. Until that hook exists, the typed, checked, completion-friendly route is `SessionStatic`, and the untyped one is `getValue` / `setValue`.

### Scope on the desktop, scope on the web

The point of the design is that these are the same list. A desktop program has one session; a web program has one per user. Nothing in between changes:

| | Desktop | Web |
|---|---|---|
| Local, parameter | one per call | one per call |
| Instance field | one per object | one per object, and forms are per session |
| `Shared` | one, for the user sitting there | **one, for every user at once** |
| `SessionStatic` | one, for the user sitting there | one per user |

Only one row differs. That row is Chapter 20.

---

## 20 SessionStatic: the mechanism, the measurements, the rule

### The problem it removes

A `Shared` field in a multi-user server is the classic data leak: one user's number sits where every other user's code can read it, and **nothing about writing it looks wrong**. The usual advice — "do not use statics on a server" — asks people to remember a rule at every declaration, which is a rule they will forget once and never find again.

`SessionStatic` removes the choice rather than restating the rule.

### How it is built

A field declared `SessionStatic` **is not a JVM field at all.**

That sentence is the whole design. The emitter walks the fields of a type and skips it:

```
// A SessionStatic gets no slot at all. That absence is the feature: with no field
// there is nothing for one user's value to sit in where another user's code can
// reach it, so the isolation cannot be undone by forgetting.
if (field.isSessionStatic()) continue;
```

Instead, every read becomes a call and every write becomes a call:

| Source | Bytecode |
|---|---|
| `Counter` (read) | `LDC "Main.Counter"` · `INVOKESTATIC RfxSessionStatics.getInt` |
| `Counter = 5` (write) | `LDC "Main.Counter"` · the value, boxed · `INVOKESTATIC RfxSessionStatics.put` |

There is no `GETSTATIC` and no `PUTSTATIC`, because there is no slot to address. **The isolation is the construction, not the discipline.**

The key is `Owner.Field` — `Main.Counter` — qualified by the owning type so that two modules may each have a `Counter` without one reading the other's. It never reaches a user's eyes; it only has to be stable and unique.

The read side has **one runtime method per shape** — `getInt`, `getLong`, `getDouble`, `getFloat`, `getShort`, `getByte`, `getChar`, `getBoolean`, `getObject` — rather than one returning `Object`. The alternative writes a checkcast plus an unboxing sequence into the generated code at every read, in eight variants, and a compiler backend is the worst place to keep eight nearly identical things correct.

For an object type a `CHECKCAST` follows, because `getObject` hands back an `Object` and every reader downstream was bound against the field's declared type. For an **array** the cast uses the descriptor form (`[Ljava/lang/String;`) rather than the internal name — otherwise a session-static array would come back as an `Object` and fail verification at the first `ARRAYLENGTH`.

### Where the value lives

`com.rapidfx.runtime.RfxSessionStatics` holds the mechanism and knows nothing about sessions. It asks its host exactly one question:

```java
@FunctionalInterface
public interface Scope {
    Map<String, Object> current();
}
```

*Which map belongs to whoever is being served on this thread.*

- **With nothing installed** there is **one `ConcurrentHashMap` for the process**. That is not a placeholder; it is the correct answer when there is one user, and it is what makes a desktop program behave sensibly.
- **tsbWEB installs a different scope at start-up**, which answers with the current session's own map.

**Why the mechanism lives in the runtime and not in tsbWEB:** because it has to work on a desktop too, and that is the whole point of it. Had it lived in tsbWEB, every desktop project would have needed the server jars just to declare a variable — and the migration story would have been a lie.

The server installs it **reflectively**, and for the same reason the look and feel is installed reflectively next door: tsbWEB must not depend on the tsbRapidFX runtime. It is a Gradle build resolving from Maven Central; the runtime is a jar from a sibling checkout, packed inside the finished program's own jar.

```java
static boolean install() {
    Class<?> statics = Class.forName("com.rapidfx.runtime.RfxSessionStatics");
    Class<?> scope   = Class.forName("com.rapidfx.runtime.RfxSessionStatics$Scope");
    Object handler = Proxy.newProxyInstance(..., (proxy, method, args) ->
            "current".equals(method.getName()) ? mapForThisThread() : ...);
    statics.getMethod("install", scope).invoke(null, handler);
    return true;
}
```

Three outcomes, all deliberate:

- **The class is absent** — the program declares no session static and carries no tsbRapidFX runtime. Nothing to install, which is not an error, so it reports nothing and moves on. This is the normal case for a server written in Java.
- **The class is there and takes the hook** — the usual case.
- **The class is there and refuses the hook** — reported **loudly** on standard error, because session statics would then silently be shared between users.

`mapForThisThread()` asks the session scope. For a thread that serves no session — the server's own plumbing, or a test — it answers with a private map nobody else can see. Better than throwing: this is reached from generated code that has no idea it is being asked about sessions, and an exception out of a field read would surface a long way from the mistake.

This is where it lands in the session:

```java
/** Where this session's SessionStatic variables keep their values. */
private final Map<String, Object> sessionStatics = new ConcurrentHashMap<>();
```

**Concurrent**, because a background thread the program started inherits this session and may touch a session static from there.

### Untouched means empty

Reading a value nobody has written gives **0, False or Nothing** — the same as an uninitialised static in VB, and the same in every session.

There is deliberately **no way to pre-fill one for everybody**. That would be a shared value again, wearing this keyword as a disguise. If you need a starting value per session, set it where the session begins:

```basic
Public Function createRoot(session As tsbWebSession) As Component
    If Main.Basket Is Nothing Then Main.Basket = New java.util.ArrayList()
    ...
End Function
```

Writing `Nothing` **removes** the entry rather than storing it, because a `ConcurrentHashMap` will not hold a null — and "set it back to Nothing" has to work, or the keyword is a trap.

### The measurements

Measured on the machine this manual was written on: JDK 25, 20,000,000 read-and-write pairs after a two-million-round warm-up, `Long` field, arithmetic identical in both loops.

| Path | ns per read + write |
|---|---|
| `Shared` — a plain `PUTSTATIC` / `GETSTATIC` pair | **1.06** |
| `SessionStatic`, desktop scope (one map for the process) | **6.36** |
| `SessionStatic`, server-shaped scope (thread-local, then map) | **7.70** |

So a session static costs roughly **5 to 7 nanoseconds more than a static**, and about six to seven times as much in relative terms. The relative figure looks alarming and means nothing: the absolute figure is the one to reason with.

Put it in scale. On the same system:

| Operation | Cost |
|---|---|
| `SessionStatic` read + write | ~7 ns |
| Mirroring one event to the browser, p50 | 100,000 ns (0.1 ms) |
| One HTTP round trip through SSE | ~14,000,000 ns (14 ms) |
| One `DriverManager.getConnection` to a network database | tens of milliseconds |
| One `PBKDF2` password check | 83,000,000 ns (83 ms) |

A session static would have to be touched **about fourteen thousand times** to equal the cost of a single event reaching the browser. In an event handler — which is where a session static is read — the number is going to be single digits.

Where the extra nanoseconds go is no mystery: a `ThreadLocal.get`, a hash of a short constant string, and one `ConcurrentHashMap` lookup. The string is a compile-time constant in the class file's pool, so its hash is computed once and cached by `String` itself.

**Memory.** One entry per declared session static per session, in a `ConcurrentHashMap`: on the order of 50 bytes plus the value. Measured against 209 kB of heap per session at 3,800 sessions (Chapter 21), a dozen session statics per user are not detectable.

### When Static, when SessionStatic

This is the rule, and it is short.

> **`Shared` is for what every user may see alike. `SessionStatic` is for what belongs to one user.**

`Shared` — one value for the whole process — is right for:

| Use | Why |
|---|---|
| A catalogue, a price list, a lookup table | Every user sees the same thing, by definition |
| Configuration read at start-up | Same for everyone, written once, read many times |
| A connection pool, a thread pool, a cache of static assets | Infrastructure; it holds nobody's data |
| A counter of something process-wide — requests served, jobs run | It is a property of the server, not of a user |
| A constant, which is `Const` and therefore `Shared` anyway | Nothing to leak |

`SessionStatic` — one value per session — is right for:

| Use | Why |
|---|---|
| The signed-in user's name, id, roles | The definition of user data |
| The current screen, tab, filter, sort order | The user's view of the world |
| A shopping basket, a form in progress, a wizard's step | State between clicks that is nobody else's |
| A per-user preference: page size, chosen theme, last folder | Belongs to one person |
| A cached query result *for this user* | Correct per session; wrong shared |

The test to apply at the declaration, in one sentence: **would it be a bug if the next person to sign in saw this value?** If yes, `SessionStatic`. If no, `Shared`.

And the corollary that catches the remaining cases: **if you are unsure, take `SessionStatic`.** Being wrong that way costs seven nanoseconds. Being wrong the other way costs a data breach.

### Two more rules the compiler enforces, and why

**All session statics are declared in the main module** (`RFX0208`). They are reachable from everywhere, so they belong where everybody can find them. Scattered across twenty classes they would be exactly what statics always become — and the point of the keyword is that the reader of a program can see, on one screen, everything that is per-user in it.

The web project template makes that visible by writing the two headings out:

```basic
Module Main

'******************************************************************************
'                              SESSION STATICS
'
'   Sessionstatic variables are variables reachable from everywhere inside of
'   a Session. Every user gets their own value; nobody can read anybody else's.
'
'   Declared here and nowhere else, so that all of them are on one screen.
'******************************************************************************

    ' SessionStatic UserName As String


'******************************************************************************
'                              STATIC VARIABLES
'
'   This static variables are reachable from everywhere in every session.
'
'   Only for what every user may see alike - a catalogue, a price list, a
'   configuration. A user's own data in one of these is a data leak: the second
'   person to sign in would read the first one's.
'******************************************************************************

    ' Shared PriceListVersion As String
```

**A field cannot be both** (`RFX0209`). One holds per session, the other for everyone. Guessing which was meant would be the wrong kindness.

### Migrating a desktop program to the web

This is what the whole construction buys, so it is worth stating as a procedure:

1. Find every `Shared` field in the program.
2. For each, ask the question above: would it be a bug if the next user saw this?
3. Change the ones that answer yes to `SessionStatic`, and move them into the main module.
4. Change nothing else.

The program still runs identically on the desktop, because there a session static *is* a static — one session, one map. And it is now correct on a server, because the isolation is in the emitted code rather than in anybody's memory.

That is the whole migration for state. What remains is windows becoming screens (Chapter 15) and the entry point becoming a server (Chapter 18).

---

## 21 The server: what it carries, and how

Everything in this chapter is measured. Where a number has no measurement behind it, it is not here.

### The architecture, in one picture

```
Browser                          |  Server (one JVM)
                                 |
per node: painted SVG            |  tsbWebSessionRegistry
  and on top a real <input>      |    +- tsbWebSession   (per user, no static state)
  wherever typing or choosing    |    +- session thread  (platform, serial)
  ^ patch (SSE)                  |    +- component registry (int <-> Component)
  v event (POST)                 |
                                 |  tsbWebMirror         - tree to JSON, patch computation
                                 |  tsbWebGraphics2D     - the look and feel paints to SVG
                                 |  tsbWebRepaintManager - what needs repainting
                                 |  real javax.swing underneath
```

### The insight it all rests on

**Swing computes the geometry for us.** Layout managers, font metrics and look-and-feel sizes all run without a screen. The browser gets finished pixel coordinates and positions absolutely; **no layout is rebuilt in CSS or JavaScript**. That is why "replace Swing components with JS components" works here at all: the hard half stays in Java.

Measured headless on JDK 25:

| Works headless | Does not |
|---|---|
| `JPanel` tree, `doLayout()`, exact bounds | `JFrame` — `HeadlessException` |
| `getPreferredSize()` including font metrics | `JWindow`, `JDialog` |
| Colours and borders from the look and feel | |
| `JRootPane`, `JTable`, `JTree`, `JComboBox`, `JScrollPane` | |
| `ActionListener` firing through `doClick()` **without an EDT** | |
| `RepaintManager` reporting `setText` as exactly one dirty region | |
| `paint(Graphics)` into an image | |

From which the cut follows: **only top-level windows are replaced. Everything below is real Swing.**

### The session thread

**One platform thread per session, not a virtual one.** AWT takes the `synchronized` monitor `Component$AWTTreeLock` on its way through layout, and `synchronized` pins a virtual thread to its carrier on Java 21. For the SSE writers, virtual threads *are* right — both variants hold 200 streams.

Incoming events go through a `BlockingQueue` into exactly that thread. So there is **no concurrency within a session and no shared tree between sessions**. Measured: 200 sessions × 100 events, 0 errors, identical geometry in every thread.

That single decision is what makes the rest of the model honest — and it is also the thing that sets the ceiling, as the numbers below show.

### Mirroring

`tsbWebMirror` walks the component tree and produces one node each:

```
"42": { "t": "JButton", "x": 16, "y": 124, "w": 388, "h": 46,
        "text": "+1", "fn": "SansSerif,bold,18",
        "fg": "#000000", "bg": "#eeeeee", "kids": [ 43, 44 ] }
```

The id is the key, not a field: the whole tree travels as `{ "kind": "tree", "root": 1, "theme":
{ ... }, "nodes": { "42": { ... } } }`. `kids` holds ids, not nested nodes — the tree is flat on
the wire and put together again in the browser.

`vis` and `on` appear **only when false**. Everything visible and enabled says so by saying
nothing, which is the common case and therefore the one that should cost no bytes.

Measured: 30 nodes is about 1,788 bytes as a full tree. After the first build, only **patches** go over the wire — lists of changed fields per id, plus structural changes on add and remove.

**Ids are per-session consecutive integers**, held in an `IdentityHashMap<Component,Integer>` and a reverse map. Class names, hash codes and field names **never** cross the wire. An incoming event names only a number, resolved against the registry of *that* session; a foreign or invented id finds nothing.

### Painting, and why it is fast

`tsbWebRepaintManager` is the reason the numbers below are what they are. Painting is the expensive half of a patch, and usually exactly one component has changed. Swing already knows this — every `repaint()`, `setText` and `setEnabled` lands at the `RepaintManager` — so here it collects those reports for the mirror instead of scheduling a paint.

The mirror repaints a component when **either** a property it reads anyway has changed (bounds, text, selection, enabled) **or** Swing reported it dirty. The second catches everything we do not know about — a custom `paintComponent` reading a model of its own. A component that is neither changed nor reported keeps its picture.

| | Before | After |
|---|---|---|
| Events per second | 1,820 | **70,000** |
| p50 per event | 6.4 ms | **0.1 ms** |
| p99 per event | 49 ms | **2.7 ms** |
| Patch, average | 456 bytes | 420 bytes |

A factor of 64, and almost all of it was one image being re-encoded as PNG on every patch.

> `RepaintManager.setCurrentManager` is **the only process-wide thing in the whole design**, because that is how the class is built. What it holds is component references and nothing else — no user data. A component belongs to exactly one session for its whole life, so an entry can only be collected by the session it belongs to.

### The look and feel paints itself, as SVG

`tsbWebGraphics2D` is a `Graphics2D` that does not rasterise the drawing commands but records them as SVG. Every component is therefore drawn by **its own** look and feel — FlatLaf's tick, its dropdown arrow, its slider knob, the title of a `TitledBorder`, and anything a program paints itself. No CSS re-implementation.

**Why that stays small.** Measured against FlatLaf, drawing ten components calls `fill(Shape)`, `drawString`, `setColor`, `setFont`, `clipRect`, `translate` and `create` — and almost nothing else. `draw(Shape)` does not appear at all: FlatLaf paints a border as a filled `Area` (outside minus inside). Two things fold the rest together:

- A `PathIterator` reduces *every* shape — `Area`, `RoundRectangle2D`, `Ellipse2D`, `Path2D` — to the same four segment kinds. One loop, no special cases.
- `Stroke.createStrokedShape` turns a stroke into a fillable shape, so `draw` is a `fill` of a different shape. Strokes, caps and joins come out exactly as Java2D would have computed them — because Java2D did compute them.

**Text stays text.** As `<text>`, not as outlines: outlines would not be selectable, would be unreadable to a screen reader, and would be several times the size. Instead Java measures the string itself and writes the width as `textLength`, so the browser stretches its own glyphs to exactly the width Swing measured.

Who is painted how:

| | Painted | On top |
|---|---|---|
| Button, checkbox, combo, slider | **completely**, in all three mouse states | a transparent `<select>` / `<range>` or a click area |
| Text field, text area | **the border only** | a real `<input>` with the border's insets |
| Panel, border | **background and border only** | children as nodes of their own |

Text fields get only the border because caret, selection and input method cannot be painted. Containers do not paint their children — those are separate nodes, or the screen would be on the page twice and a changed label would mean re-sending the whole form.

**The three mouse states travel in advance.** A button arrives as `svg`, `svgH` (hover) and `svgP` (pressed), so the browser can follow the pointer without a round trip. Two sharp traps live there, both handled: `setPressed(false)` fires the action on an *armed* button — hence `setArmed(false)` first, or the click handler would run on every patch — and `setRollover` fires `ChangeListener`s, which are detached beforehand and reattached in a `finally`.

### The colour rule

Five things the look and feel does **not** paint: the text *inside* an `input`, the rows of a list, table or tree, the menu titles, the opened menu panel, and the frame of a dialog. Whatever is not stated for those is not "already known" but unknown, and the browser takes its own black — the page declares no `color-scheme`, so the light form of `CanvasText` applies even on a dark desktop.

In a light theme this is invisible. In a dark one it is black text in a dark field.

The carrier is `tsbWebTheme`: it reads the `UIDefaults`, travels as CSS variables in the tree message, and is applied through the CSSOM (a `<style>` block would have failed the `style-src 'self'` content security policy). Guarded by a test of **50 themes × 20 probes**, comparing desktop colour against browser colour. Before the rule was found, **878 of 1,000 comparisons disagreed.**

> The practical form of the rule: a literal colour written into `app.css` is very probably a bug. The only exceptions are the two things that belong to the page itself — the veil over a dialog, and the connection-lost line.

### Security

- **Session id:** 256 bits from `new SecureRandom()`, cookie `HttpOnly; SameSite=Strict; Path=/`, plus `Secure` on an HTTPS server. **Never in the URL** — it would land in server logs and in the `Referer`. Explicitly **not** `SecureRandom.getInstanceStrong()`: that is `NativePRNGBlocking` here and can hang on low entropy, and a server that blocks while creating a session is an outage. For session ids the non-blocking provider is cryptographically sufficient.
- **Session fixation:** after a successful sign-in a new id is issued and the old one discarded, together with the CSRF token. The state of the unauthenticated visit is deliberately discarded, not carried over.
- **How the id rotates without an HTTP response.** Sign-in happens inside a Swing handler, where there is no response left for a `Set-Cookie`. So the new id goes out over the event stream, and the browser sends it thereafter in the header `X-tsbWeb-Session`, which the server checks **before** the cookie. That is not merely a way round the problem, it is the better arrangement: an id that lives only in the browser's memory cannot be planted from outside, and session fixation has nothing left to plant.
- **CSRF:** every `POST` must carry `X-tsbWeb-Csrf`, compared in constant time. The cookie alone is no proof, because the browser sends it on foreign-triggered requests too.
- **The client can name no class and no method** — only component ids from its own session.
- **Limits:** message at most 64 kB, file at most 20 MB; event rate limited per session; idle timeout 30 minutes by default.
- **No default ceiling on the number of sessions.** A session is created for every visitor, signed in or not, so a crawler would fill any number small enough to protect memory — and then no real user gets in. The limit would be the attack, not the defence. What protects is the idle timeout. Whoever wants a ceiling anyway sets `sitzungen.max`.

Passwords are checked with `PBKDF2WithHmacSHA256`, 210,000 rounds, salted per user — 83 ms per attempt, contained in the JDK, and it saves the dependency bcrypt or Argon2 would cost. Nothing is stored in clear and nothing is logged.

### Capacity: the numbers

Two campaigns, both with server and load generator in **separate JVMs** — otherwise the test client's own sockets count against the server.

**Throughput, 1,000 sessions, 10 cores:**

| Measure | Value |
|---|---|
| 1,000 simultaneous sessions | 0 errors |
| Memory per session (RSS) | 1.2 MB |
| Clicks per second | 10,400 |
| Response time | p50 76 ms · p95 181 ms · p99 250 ms |
| Mirroring per event (200 sessions) | p50 0.1 ms · p99 2.7 ms |

Above 1,000 the load generator was the bottleneck, not the server. For higher throughput figures there is **no evidence**, so none is given.

**Concurrency, on a Mac, a screen with six components:**

| Sessions | Page build p50 | p99 | Errors |
|---|---|---|---|
| 500 | 32 ms | 229 ms | 0 |
| 1,000 | 19 ms | 73 ms | 0 |
| 2,000 | 14 ms | 62 ms | 0 |
| **3,000** | **19 ms** | **58 ms** | **0** |
| 3,500 | 19 ms | 61 ms | 0 |
| 3,800 | 69 ms | 358 ms | 0 |

Read that table twice, because it is not the shape people expect.

**At 3,000 concurrent sessions the server is not merely surviving; it is at its best.** A page builds in 19 ms at the median and 58 ms at the 99th percentile — *better* than at 500 sessions, where p99 was 229 ms. The improvement between 500 and 2,000 is warm-up: by a thousand sessions the JIT has compiled every path that matters and the heap has settled, and the per-session work is genuinely independent, so more sessions mostly means better-used cores.

The curve stays flat to 3,500 and only lifts at 3,800, close under the ceiling. At 3,800 simultaneous sessions: **3,833 threads, 3,801 open connections, 794 MB of heap — about 209 kB per session — and a *new* visitor is served the page in under 1.5 ms.**

### Where the hard wall is

**Not memory. The operating system's thread limit.**

tsbWEB holds one platform thread per session, and macOS allows a process `kern.num_taskthreads` = **4096**. Above that the process dies, and unambiguously:

```
pthread_create failed (EAGAIN)
java.lang.OutOfMemoryError: unable to create native thread
    at com.tsbweb.server.tsbWebLiveSession.start
```

That is not a heap problem — with `-Xmx6g` less than a sixth was in use. **On Linux `/proc/sys/kernel/threads-max` is orders of magnitude higher**, so the same application goes considerably further there. It is worth checking anyway: inside a container the cgroup's `pids.max` limits it as well.

Practically: **on a Mac do not plan above about 3,500**; on Linux look up the machine's own limits; beyond that, a load balancer and several processes.

The second wall, met before the threads, is the process-wide `Component$AWTTreeLock`. Measured: **15,700 layout operations per second** against a realistic 200–400. The p99 of 13.5 ms already includes that contention under forty times realistic load. Both walls lie far beyond the recommended figure.

### The recommended figure, and why it is lower

**Up to 750 concurrent users per server**, depending on the size and load of the application.

The number is deliberately far below what was measured, and the reason is not caution about tsbWEB:

> **The measuring screen does nothing.** It paints and computes layout. A real application queries a database, and a handler that waits 200 ms on SQL blocks its session thread — exactly as on the desktop. What a server really carries depends on the handlers, not on tsbWEB.

750 leaves room for that, and for the morning when everybody arrives at once. Memory is nowhere the limit: at 1.2 MB per session a 64 GB machine would carry tens of thousands on paper.

Above that, put a load balancer in front. **Sessions are not shared between JVMs**: each user must stay with their server (sticky sessions).

### Operating it

```
java -Xmx4g -jar CustomerManagement.jar
```

**No `-Djava.awt.headless=true` needed** — `tsbWebStart` sets it first of all, before anything touches AWT. Whoever calls the server without `tsbWebStart` must pass it on the command line: the server never draws to a screen, and without the flag AWT looks for a display and fails on a machine without one.

**No X server, no Xvfb, no display** is required. That is the difference from tools that render into an image server-side.

### What is missing, named honestly

There is **no operations console, no metrics endpoint, no cluster and no session failover**. A restart ends every session. For the intended scale that is acceptable; whoever needs more should know it beforehand.
# Part VI — The designers

## 22 The desktop designer

**tsbDesignerSWX** is a visual form designer for Java Swing that generates tsbRapidFX. No XML, no build tool, no intermediate file format.

### One form, one file, one banner

Every form is exactly **one `.rfx` file**, divided by a banner:

```basic
Imports javax.swing
Imports java.awt
Imports java.awt.event

Public Class LoginForm

    Public Sub New()
        InitDesignerComponents()
    End Sub

    ' Handles OnBtnLoginClick - the body is yours.
    Private Sub OnBtnLoginClick(e As ActionEvent)
        Print "that was me"
    End Sub



'*****************************************************************************
'*****************************************************************************
'*****************************************************************************
'
'                                 DO NOT EDIT
'                             TSB RAPIDFX LOGIC
'
'*****************************************************************************
'*****************************************************************************
'*****************************************************************************



    ' @tsbswx form class=LoginForm root=AnchorPanel platform=DESKTOP w=1280 h=800 showIn=true
    Private rootPane As JPanel = New JPanel()
    Private rootPaneLayout As SpringLayout = New SpringLayout()

    ' @tsbswx Button btnLogin parent=rootPane L=24 T=100 W=120 H=30
    Private btnLogin As JButton = New JButton()

    Private Sub InitDesignerComponents()
        rootPane.setName("rootPane")
        rootPane.setLayout(rootPaneLayout)
        rootPane.setPreferredSize(New Dimension(1280, 800))

        btnLogin.setName("btnLogin")
        btnLogin.setText("Sign in")
        btnLogin.addActionListener(AddressOf OnBtnLoginClick)
        btnLogin.setPreferredSize(New Dimension(120, 30))
        rootPaneLayout.putConstraint(SpringLayout.WEST, btnLogin, 24, SpringLayout.WEST, rootPane)
        rootPaneLayout.putConstraint(SpringLayout.NORTH, btnLogin, 100, SpringLayout.NORTH, rootPane)
        rootPane.add(btnLogin)
    End Sub

    Public Function GetRootPane() As JPanel
        Return rootPane
    End Function

    Public Sub ShowIn(frame As JFrame)
        frame.setTitle("LoginForm")
        frame.setContentPane(rootPane)
        frame.setSize(1280, 800)
        frame.setLocationRelativeTo(Nothing)
        frame.setVisible(True)
    End Sub

End Class
```

(The rules are drawn seventy-eight characters wide; they are shortened here to fit the page.)

**Above the banner is yours**: the constructor, event handlers, helper methods, more fields. The designer writes that half back character for character and only ever *adds* two things — a missing `Imports` and an empty body for an event you wired up. Both are purely additive; nothing is ever removed.

**Below the banner** are the component fields and the whole build method. That half is rewritten in full on every save.

> **Why the banner is large.** It replaced a single comment line. One line reads like every other comment in the file, and the one thing a reader must not do here is edit below it — so the warning was given the weight of the consequence: three rules of asterisks, four blank lines of air on each side, and the words spelled out. It costs twenty lines once per file and buys a boundary nobody scrolls past. It also says **which** designer wrote the file, desktop or web.
>
> The older one-line marker is still recognised and never written again. A file carrying it opens, and the first save replaces it with the banner — so old forms upgrade themselves rather than becoming unopenable.

### AddressOf is what makes event wiring possible

```basic
btnLogin.addActionListener(AddressOf OnBtnLoginClick)
```

`AddressOf` on a **private instance method** carries `Me` with it, so the handler can touch every field of the form while its body stays entirely yours. That one language feature is the reason the designer can generate event wiring at all without owning the behaviour.

### What the meta line is for

The `' @tsbswx` lines are tsbRapidFX comments — the compiler ignores them entirely. They exist so the designer can *reopen* the file.

Most of a form can be read back out of the code: `btnLogin.setText("Sign in")` says exactly what the caption is. Some things cannot be:

- **Whether 24 pixels from the left is an anchor or a frozen position.** The code has `putConstraint(WEST, btnLogin, 24, WEST, rootPane)` either way.
- **Settings a layout manager swallows.** A `FlowPanel` writes `New FlowLayout(FlowLayout.LEFT, 12, 12)` and you could read that back — but a `BoxLayout` has no spacing at all, and a `GridLayout` forgets its column count, so the number would simply be gone next time.

So: what the code states unambiguously is read from the code; what it cannot state travels in the meta line. `L=24 T=100` are the anchors, `W=120 H=30` the size, `p.hgap=12` a design-time setting. **Delete the line and you lose the layout on the next open — not the program.** It still compiles and runs.

### No statics

A form lives entirely in its instance:

```basic
New LoginForm().ShowIn(frame)
```

Two instances of the same form are independent. There is not one `Shared` field in the generated code, and none in the designer either: catalogue, undo stack, renderer and open forms all hang off a context the window owns. That is also what makes a form usable on a web server without a single change (Chapter 18).

### Only deviations are saved

A property still at its declared default produces **no line**. A value that is set back to the default is forgotten again, which keeps the generated half small and readable — it says exactly what you changed.

### The component catalogue

Around thirty types in seven categories, declarative rather than a class per component:

| Category | Types |
|---|---|
| Controls | Label, Button, ToggleButton, RadioButton, CheckBox, Hyperlink, Separator, Slider, ProgressBar, ScrollBar, Spinner, **ShowPicture** |
| Text | TextField, PasswordField, TextArea, EditorPane, FormattedTextField |
| Choice | ComboBox, DatePicker, TimePicker |
| Data | List, Table, Tree, **SmartTable** |
| Containers | **AnchorPanel, FlowPanel, GridPanel** |
| Menu and toolbar | MenuBar, PopupMenu |
| Charts | LineChart, AreaChart, ScatterChart, BarChart, PieChart, BubbleChart |

A component is a specification — a class name, a default size, a render key, an attachment strategy and a list of property descriptors. Properties come in bundles along the Swing class hierarchy, so adding a control is **one entry** in the catalogue, not a new file.

**SmartTable** in that list is `tsbSmartTable` from Chapter 17, and it is the only table whose column widths the designer can set.

### Three containers, deliberately

The JavaFX designer this one descends from had sixteen. A stack pane, a split pane, a tabbed pane, two kinds of box and two kinds of grid mostly offer ways to be undecided. What a form needs is a page you can place things on freely, and a couple of panels to drop onto it:

| Container | Layout manager | What it is for |
|---|---|---|
| **AnchorPanel** | `SpringLayout` | the page — an anchor is a rule about resizing |
| **FlowPanel** | `FlowLayout` | a row that wraps; alignment and both gaps are yours to set |
| **GridPanel** | `GridBagLayout` | cells, when a row is not enough; a child can span columns and a cell can carry weight |

Six of the containers are the same `JPanel` with a different layout manager. That is not a shortcut; it is what Swing is. Inventing a `VBoxPanel` class would put a type into the generated code that the JDK does not have.

### Why SpringLayout

**An anchor is not a position — it is a rule about what happens when the window resizes.** The obvious translation of an anchor pane, a null layout with `setBounds`, throws that away and freezes the design into pixels.

`SpringLayout` is the one Swing manager that can state it: *this component's west edge is 24 from the parent's west edge*. Set both anchors on an axis and the component stretches, exactly as the canvas shows.

Each anchor container therefore gets a `SpringLayout` field of its own, because the constraints live on the layout rather than on the child. And **a right or bottom anchor is a negative distance** — a constraint always reads "this edge is N from that edge", so sitting 24 in from the right is -24 from `EAST`.

Each `GridBagConstraints` is built inline rather than as a shared, mutated local. A reused `GridBagConstraints` is the classic Swing bug — every later child silently inherits the previous one's span — and a form the designer wrote must not contain it.

The layout engine is **solver-free**: every rule is a direct formula, so the preview and the generated code cannot drift.

### The preview is not a drawing

The canvas paints **real Swing components**. A configured `JButton` is built and painted through a `CellRendererPane` — the mechanism `JTable` uses for every cell it draws — without the component ever belonging to a container.

This is the single biggest difference from the JavaFX designer, which had to hand-draw an approximation of all seventy controls, because a Swing application cannot instantiate a JavaFX button. A Swing designer has no such problem. What follows:

- the preview is **pixel-identical** to the running form, and cannot drift from it;
- a new component type needs no painter — 1,158 lines of hand-drawn painters are simply gone;
- every FlatLaf theme is honoured exactly, because FlatLaf is what draws it.

One prototype per type is reconfigured on each paint, restoring the *captured* original foreground, background, font and border between paints — rather than setting null, which is the obvious thing and is wrong: null on a Swing component means null, and the first component to paint with a null font takes the whole canvas down.

### Themes

**View ▸ Themes** lists the four core FlatLaf themes plus the roughly forty ported IntelliJ themes, discovered from the jar rather than typed out.

**Selecting a theme is the preview.** Moving through the list repaints the whole window, canvas included; Cancel puts back the one that was there.

That is simpler than its JavaFX ancestor, which needed two steps — apply, then copy the stylesheet into your project — and the reason is worth saying. A JavaFX theme is a CSS file to be found, parsed and planted. A FlatLaf theme is a class in a jar the project already declares. Nothing to copy, and nothing to approximate.

The trade is that **a Swing look and feel is global**. The theme is remembered per form and travels in the meta line, but switching tabs changes the whole window rather than one canvas.

Installing the look and feel in the built program is one line in your `Sub Main`, not in the form:

```basic
Imports com.formdev.flatlaf
FlatDarkLaf.setup()
```

### What stays design time

Four things the designer deliberately does not generate:

- **Chart data.** Series come from your application at run time. A generated `addSeries` you would have to delete first is worse than an empty chart.
- **List and table contents.** A `JList` has no `addItem`; its content is a model the application owns.
- **Spinner ranges.** A `SpinnerNumberModel` is a line for your half of the file.
- **Container spacing, gaps and column counts.** A `BoxLayout` has no spacing and a `GridLayout` forgets its column count — so these travel in the meta line, which is exactly what it is for.

### Operating it

| Action | How |
|---|---|
| Insert a component | Drag from the palette, or double-click |
| Multiple selection | Shift-click or rubber band |
| Move / resize | Drag, eight handles, arrow keys (Shift = 10 px) |
| Undo / redo | Cmd+Z / Cmd+Shift+Z |
| Duplicate | Cmd+D |
| Zoom | Cmd+wheel, the zoom field, "Fit to window" |
| Pan | Middle mouse button |
| Rename | The first field in the properties panel |
| Theme | View ▸ Themes |

Dragging over a container marks it with a green dashed outline — that is where the component lands.

### The check that matters

```
./gradlew verifyGeneratedCode
```

It generates one form per component type — with **every** property moved off its default — and runs the real `rfxc` over them. It needs the compiler built next door.

A round-trip test only proves the designer can read back what it wrote. **Only a compiler proves that what it wrote is valid tsbRapidFX.** On its first run it found four calls that looked entirely reasonable and do not exist:

- `setMinWidth` / `setMinHeight` / `setMaxWidth` / `setMaxHeight` — carried over from JavaFX, where a region really has them. A Swing component has `setMinimumSize(Dimension)`.
- `XChartPanel.setTitle` — the title belongs to the chart inside the panel, not to the panel.
- `getChart().setXAxisTitle` — that one is on a subclass of the type the getter returns, and tsbRapidFX erases type arguments, so the call is made against the base type and does not compile.
- `setMnemonic("S"c)` — tsbRapidFX has no character literal. It is `"S".charAt(0)`.

**Every one of those passed the whole unit suite first.** The task is not optional; run it after any change to the catalogue.

### Known limits

- **Renaming a component** changes the generated field name. If your code above the banner uses it you get a compile error — deliberately visible, but with no automatic refactoring.
- **Unused imports stay** when a component is deleted. An import you added by hand matters more than a tidy list, so the designer only ever adds.
- **There is no package field.** tsbRapidFX has no packages, so there was nothing for it to say.

---

## 23 The web designer

There is **one designer and two targets**. The web designer is not a second program: it is the same designer, told that this form is drawn for a browser.

That is the point. A form is ordinary Swing either way — the same `JPanel` with the same layout managers — so the difference comes down to three things.

### The three differences, and nothing else

**1. No `ShowIn(JFrame)` is generated.**

On a desktop, `ShowIn` is what a form is normally used through. On a server there is no frame: the browser is the window, and `new JFrame()` throws `HeadlessException` in the `Window` constructor. A generated `ShowIn` would be a method nobody can call, in every file.

So a web form ends at:

```basic
    Public Function GetRootPane() As JPanel
        Return rootPane
    End Function

End Class
```

and that is exactly the handle `createRoot` needs:

```basic
Public Function createRoot(session As tsbWebSession) As Component
    Return New MainForm().GetRootPane()
End Function
```

It stays **switchable rather than removed**, because a form drawn for a browser is ordinary Swing and may well be wanted on a desktop too. The meta line records the choice as `showIn=true` or `showIn=false`.

**2. The design surface means something different.**

On the desktop it is a window. On the web it is a **browser viewport** — which is why the phone and tablet sizes matter there and not here.

| Preset | Size |
|---|---|
| Dialog | 560 × 400 |
| Desktop 1024×768 | 1024 × 768 |
| Desktop 1280×800 | 1280 × 800 |
| Laptop 1440×900 | 1440 × 900 |
| Desktop Full HD | 1920 × 1080 |
| Tablet landscape | 1280 × 800 |
| Tablet portrait | 800 × 1280 |
| Phone landscape | 844 × 390 |
| Phone portrait | 390 × 844 |
| Custom | anything from 120 to 8000 |

The orientation is **derived from the dimensions**, not stored beside them — the other way round was a bug in the designer this one takes its metric from, where a hard-coded portrait orientation rotated every landscape platform. The toolbar's rotate button simply swaps width and height.

The size is written into the code as well as into the meta line:

```basic
rootPane.setPreferredSize(New Dimension(1280, 800))
```

That line matters more on the web than on the desktop. A meta line is read by the designer and by nobody else; the code is what runs, and the server lays the tree out headless from its preferred size. **A form that lost that line came out too short in the browser** — which is exactly what happened once, and is the reason it is generated.

**3. The banner says which one it is.**

```
'                                 DO NOT EDIT
'                             TSB RAPIDFX WEB LOGIC
```

against the desktop's `TSB RAPIDFX LOGIC`. So the difference that matters most about a form is legible before reading a line of its code.

### Where the target is recorded

The target travels in the `' @tsbswx form` line, so **a file carries its own answer** and nothing has to be guessed from the folder it sits in:

```basic
' @tsbswx form class=MainForm root=AnchorPanel platform=DESKTOP w=1280 h=800 showIn=false target=WEB
```

The project says the same thing in its `.rfxproj`. When the IDE starts the designer it passes that on, so a **new** form is born with the right target already set.

**A project is desktop *or* web**, fixed when it is created. There is no run-time switch; that would be an application having to serve two programming models at once.

### Opening it from the IDE

Right-click a `.rfx` file → **Open in Designer**. The IDE hands over three things it already knows, so the designer does not have to ask:

1. the file,
2. the source folder to save into,
3. whether this is a web project or a desktop one — from the `.rfxproj`.

Everything unsaved goes to disk first. The designer reads the file, not the editor's buffer, and opening it on a version the user cannot see is the kind of confusion that ends with somebody losing work. After the designer saves, the file is reloaded on focus.

### Why a separate process and not an editor tab

`UIManager` is JVM-wide. A designer embedded in the IDE therefore cannot install FlatLaf without repainting the IDE, so an embedded canvas draws with the **IDE's** look and feel.

On the desktop that would be ugly. **In a web project it would be wrong**, and that is the sharper reason: the server paints every component into SVG *with FlatLaf's own delegates* (Chapter 21), so a canvas drawn by anything else is showing something the browser will never show. The preview would be lying.

In its own JVM the designer installs FlatLaf properly. The canvas is then the same drawing the browser gets, the theme can be switched, and it runs at the speed of the standalone application — because it *is* the standalone application.

The designer is carried inside the plugin as a resource jar and unpacked into the IDE's system directory on first use.

> **An operational note worth knowing.** That unpacking was once keyed only by file name, so the first unpack held forever: installing a freshly built plugin did not help and *could* not help, because the running designer came from a stale copy in the IDE cache. It is now keyed by the content's own SHA-256, written through a neighbouring file with `ATOMIC_MOVE`, with older unpackings deleted best-effort. If a designer ever behaves like an older version, look in the IDE cache directory before looking at the source.

### The theme, and why it cannot drift

The look and feel of a web application lives in `Sub Main` as `config.Thema` (Chapter 18). **Choose a theme in the designer, and saving the form updates that line** — so the preview and the server cannot disagree.

Verified by measurement rather than by eye: background `#ffffff` / `#000000` light against `#333333` / `#eeeeee` dark, compared between the designer's canvas and the browser's page.

### What a web form may contain

Everything the desktop catalogue has. The components are the same Swing classes, and Chapter 21 lists how each is delivered — buttons painted in all three mouse states, text fields as a painted border with a real `<input>` inside, tables and trees as structure rather than as a picture.

Two consequences worth planning for while drawing:

- **A dialog is not a window.** Draw the dialog's contents as an ordinary form and hand its `GetRootPane()` to a `tsbWebDialog` (Chapter 15). There is nothing to draw differently.
- **A screen change is not a window change.** Draw each screen as its own form; `createRoot` picks between them.

### Creating a web project

**New Project → RapidFX — Web** writes three `.rfx` files, copies four jars into `lib/`, and adds the project file, `tsbweb.properties`, a README and a `.gitignore`. No Maven, no Gradle, no network.

Two of the three files are **built by the designer's own generator**, not pasted in as text resources. That is deliberate: a `.rfx` pasted in as a resource would have to carry a hand-written generated region — field declarations, a build method, and the meta lines saying which component sits where — and every one of those has to match what the parser expects, or the form opens in the Design tab as an empty canvas and the first save quietly deletes it. The wizard builds the form model and lets the writer write it, exactly as pressing **Apply to code** does. The file cannot disagree with the parser, because the generator produced it.

What you get is a project that **runs** on the first press: sign in, count, sign out — with the counter living in the session rather than in a `Shared` field, because on a desktop that difference is invisible and in a browser it is the whole game.

---

# Part VII — Mobile

The same language, the same compiler and a designer of the same shape now reach the phone and the
tablet as well. A mobile form is a `.rfx` file like any other; what differs is the banner at its
head (`' @tsbmob`), the API it binds against (`tsb.mobile`) and the three backends that draw it —
`android.widget` with ConstraintLayout, UIKit through MobiVM for iOS, and, for the preview and for
a desktop run, Swing through the ported `java.desktop`.

Until 19.09.2026 this lived in a plugin of its own for Android Studio. It does not any more:
**there is one plugin, in IntelliJ IDEA, and mobile sits in it beside desktop and web.** One
`New RapidFX Project…`, one *Run* button, one editor that knows `.rfx`.

## 24 The mobile designer

**tsbDesignerMobile** is the mobile form designer. It is the same program as the desktop designer
in all but its catalogue and its backends: a palette on the left, the form in the middle,
everything about the selected component on the right, and what it writes is a plain `.rfx` file
that reads back byte for byte.

### A window of its own

The designer opens in a window of its own, not as an editor tab — and that is not a matter of
taste. `UIManager` applies to the whole JVM, so a canvas embedded in the IDE cannot install a
look-and-feel without repainting the IDE around itself; it would inevitably paint in whatever the
IDE wears. With `renderer = swing` it is FlatLaf that draws on the device, through the same
`java.desktop` port — so a canvas in the IDE's appearance would show not a different picture but a
wrong one. In its own JVM the designer installs FlatLaf properly and the canvas is the same drawing
the device gets.

The designer is carried inside the plugin as a resource jar and unpacked into the IDE's system
directory on first use, keyed by the content's own SHA-256. If a designer ever behaves like an
older version, look in the IDE cache directory before looking at the source.

### The device question comes first

A new form asks two things — name and device — and the device is asked first, because afterwards
it can only be answered by redrawing every form. Everything else — the name, the package — is one
line in `rfxmobile.properties` and changeable at any time.

Phone, tablet and large-phone geometries differ in size and in their safe areas; the designer's
canvas matches the project's device and orientation, so the preview is the same shape the device
is. Choose **portrait** or **landscape** at creation, and, if you want, that the app is locked to
the one orientation.

### One catalogue, three canvases

On the form are a label and a button to begin with — two widgets rather than none, so that both
halves of the file show at once: a property set below the banner and an empty handler body above
it. What the catalogue offers is drawn identically on all three backends; where a backend cannot do
a thing, the designer does not offer it. Colours are one place this was earned rather than assumed:
a component's `background` and its foreground `color` are honoured on Android, on iOS and in the
Swing preview alike, verified on the device rather than by eye.

## 25 Building and running on a device

### The Run button, and the target beside it

**Run** sits in the toolbar beside the button the IDE starts its own projects with. One click
builds the project and runs it; the little arrow next to it switches between **desktop**, **Android**
and **iOS** and remembers the choice per project — because whoever works on a layout all afternoon
runs on the desktop, where a round takes a second instead of a simulator start.

What is built is the **directory**, not the file: a mobile project is a folder of `.rfx` files, and
which of them starts is the project's answer (`rfxmobile.properties`), not the editor's. The build
runs line by line into a console with a progress indicator and a cancel button. The work is done by
`rfxmobile`, the mobile toolchain, which the plugin carries beside its own jar.

The starter project runs on the first press — type a name, forward, back — in about **2 s on the
desktop, 3 s on Android and 19 s on iOS**, all three out of the same folder.

### Android, without Android Studio

Android needs the Android SDK, but not Android Studio. **Tools ▸ Set Up Android SDK…** installs it,
with the licences shown one by one and an emulator device; the SDK is found through `ANDROID_HOME`,
`ANDROID_SDK_ROOT` or the default folder on each OS. **Run on Android** offers to start the emulator
when nothing is connected.

Two rules for the emulator, each of which costs an afternoon if unknown:

- **Exactly one Android device may be running** — not two emulators, and not an emulator beside a
  phone on a cable. The install step does not name a device to `adb`, so with two it cannot know
  which is meant and the build waits for an unambiguity that will not come.
- **Whoever switches the emulator off with the power button must close its window too.** The power
  button switches the device off, not the emulator; the window keeps the AVD occupied, and the next
  build waits for a device that will not come up.

The desktop simulation has neither problem: it is an ordinary window with a close button, and
several side by side do not disturb each other.

### iOS is built on a Mac

The iOS build calls `xcrun`, `codesign` and the simulator, and Apple's SDK licence permits Apple
hardware only — so iOS builds on a Mac and nowhere else, and says so in one sentence at the start
rather than after a long download. MobiVM's ahead-of-time compiler is 127 MB and is fetched on the
first iOS build into `~/.rfxmobile/`, once per machine, against a published checksum; it does not
ship in the plugin. Android, the desktop and the web build on any machine as they are.

### A signed APK or IPA

Below the run actions, kept apart because it is a different intent, sit **Build a signed APK** and
**Build a signed IPA**: a release build takes a signing key and produces a file somebody uploads.
The password is asked for in a dialog, not on a console — under an IDE there is no console to ask
through. Where the password comes from, and where it must never be kept, is its own note: a signing
key belongs in an environment variable or a keychain, never in the project.

---

# Part VIII — Packaging

A program that compiles and runs is not yet a program someone can install. The last step turns a
desktop or web application into a native installer — an `.exe`, an `.msi`, a `.dmg`, a `.pkg` or an
AppImage — with a Java runtime inside it, so that whoever receives it needs nothing installed
first. That step is **tsbDeploy**, and since it is carried in the same plugin, it is one menu away
from the program you just built.

## 26 Packaging with tsbDeploy

### What it produces

tsbDeploy builds native installers for **Windows, macOS and Linux**, each for **x86_64 and
aarch64**, and it builds them **from any one of those hosts** — no CI, no `jpackage`, no npm.

| Target | Formats | Signing |
|---|---|---|
| Windows | Setup `.exe` (a small Rust program), `.msi` (a pure-Java MSI writer) | Authenticode, through jsign |
| macOS | `.dmg` (pure Java; on a Mac through `hdiutil`), `.pkg` (Mac build host only, `pkgbuild`) | rcodesign, notarisation optional |
| Linux | AppImage (a SquashFS writer plus the type2 runtime) | — |

The `.pkg` is the one exception to *from any host*: it is produced on a macOS build host only,
because it uses Apple's `pkgbuild`.

### The runtime comes from the target, not the host

Every installer carries its own Java runtime, built with `jlink` for the platform it targets rather
than the one you are on. tsbDeploy keeps a local JDK repository (`~/.tsbdeploy/jdks`) of Liberica
Standard JDK 25 for all six target platforms; `jlink` and `jdeps` run on your machine, the `jmods`
come from the target, and only the modules your program actually uses go in. So a Windows aarch64
installer built on an Intel Mac carries a correct, minimal aarch64 Windows runtime.

### Two kinds of project

tsbDeploy takes the program in one of two shapes, and the choice is a switch:

- **A JAR list** — the output of a Maven or Gradle build; or
- **A finished application folder** with no build system at all: the main jar, a `lib/` folder and a
  resources folder. That layout lands unchanged in the installation root — which is the application's
  working directory — so a manifest `Class-Path` and relative paths like `resources/logo.png` keep
  working. `jdeps` considers every jar in `lib/`, and the main class may come from the manifest.

A RapidFX build is the second shape exactly: **Build ▸ Build RapidFX Jar** writes `build/<Name>.jar`
with `lib/` beside it, the `LICENSE`/`NOTICE` of the project, and — for a web program —
`tsbweb.properties`. tsbDeploy takes that folder and turns it into installers.

### From the IDE

The plugin can take the application straight from a run configuration — build the module, pack the
classes and libraries, read the main class — and it brings its own **tsbDeploy** run configuration
for the toolbar. A RapidFX project offers **Package with tsbDeploy…** directly. Behind the menu is
the same tool the standalone Swing application and the command line use; the IDE only contributes
the two things it is good at, a project to read and a console to report into.

> tsbDeploy also ships as a **standalone plugin** and as a **Swing desktop application**, for
> packaging programs that were not written in RapidFX. The standalone plugin and the combined
> RapidFX plugin are marked incompatible with each other, so only one is ever installed: whoever has
> RapidFX has tsbDeploy already.

### A native launcher, and honest installers

The program is started by a small native launcher written in Rust — no console window on Windows,
and a menu entry created on an AppImage's first run. An optional splash screen (PNG, GIF or JPEG)
covers the moment before the first window.

Every installer carries a `LICENSES.md` with the notices for the launcher, the setup program, the
AppImage runtime and the Java runtime, each naming where its source can be had; the source archives
of the copyleft components can be placed in the output folder as well. The launcher and the setup
program are themselves under **MIT OR Apache-2.0**, so the installers they make carry only the usual
attribution, not a copyleft obligation. The corresponding sources of OpenJDK and libfuse live
permanently at
[thorstenstueker/tsbdeploy-sources](https://github.com/thorstenstueker/tsbdeploy-sources).

### Signing credentials live in the environment, never in a file

Signing reads its credentials only from environment variables — `CERTIFICATE_CHAIN`, `PRIVATE_KEY`,
`PRIVATE_KEY_PASSWORD` — and never from a file committed beside the project. That is the same rule
the mobile signing follows, and for the same reason: a key in the repository is a key everyone who
clones it has.

---

## Closing note

Four things in this manual are worth carrying away more than the rest.

**The JDK is not a foreign country.** `Files.readString`, a `JTable`, a JDBC `PreparedStatement` — none of them needs a wrapper, an adapter or a translation table. Read the Javadoc, apply the fifteen rules of Chapter 11, and write the line.

**One rule decides whether a program survives its second user.** `Shared` is for what everybody may see alike; `SessionStatic` is for what belongs to one person. The cost of choosing the safer one is seven nanoseconds.

**One form runs in four places.** Desktop, web, Android and iOS out of the same `.rfx` file — not a port and not a subset, but the same components drawn by a different backend. Swing computes the geometry without a screen, and a look and feel can be asked to paint into SVG or onto a phone instead of into desktop pixels.

**The last step is not an afterthought.** A program nobody can install is not finished. tsbDeploy makes the installer — for six platforms, from the one you are on, with the runtime inside — so that "it runs here" becomes "it runs for them".
