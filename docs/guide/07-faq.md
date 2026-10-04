[← Mobile](06-mobile.md) · [Contents](README.md) · [Troubleshooting →](08-troubleshooting.md)

# 7 FAQ

## The language

**Is this VB6?**

No, and it does not pretend to be. The grammar is the classic Basic grammar — `Dim`, `Sub`,
`Select Case`, `For Each` — and underneath is the JVM rather than the VB runtime. A RapidFX class
*is* a Java class. the *Moving from VB6* chapter of the [complete manual](../tsbRapidFX-Manual.md) lists what stays the
same, what changes and what is missing.

**Can I use Java libraries?**

Yes, any of them, directly:

```basic
Imports com.fasterxml.jackson.databind

Dim mapper As ObjectMapper = New ObjectMapper()
Dim json As String = mapper.writeValueAsString(customer)
```

Put the jar in `lib/`. There is no binding layer and nothing to generate — your class and a Java
class are the same kind of thing to the JVM.

**Can Java call my RapidFX classes?**

Yes. A `Public Class` compiles to a public Java class with the methods you declared. Hand the jar to
a Java project and it is a library.

**Is there `null`?**

`Nothing`, and it is Java's `null`. `IsNot Nothing` is the test.

**Generics?**

You can *use* generic Java types — `ArrayList`, `Map`, `Optional` — and the compiler checks what
goes in and comes out. You cannot *declare* a generic type of your own. In practice that matters
less than it sounds for the kind of program this is for.

**Lambdas?**

Yes, and they become ordinary functional interfaces:

```basic
names.forEach(Sub(item) Print CStr(item))

button.addActionListener( _
    Sub(e)
        lblStatus.setText("clicked")
    End Sub)
```

A one-line `Sub(x) …` takes **the rest of the line** as its body, commas included. In an argument
list with more arguments after it, write it across lines with `End Sub`.

---

## The project

**Do I need Gradle or Maven?**

No. A RapidFX project has a `.rfxproj` of a few lines and no build system. The compiler is inside
the plugin.

**Where do libraries go?**

`lib/`. The wizard marks it; `Build Jar` puts them beside the jar and names them in the manifest's
`Class-Path`.

**Can several people work on one project?**

Yes — it is text files in a folder. Put it in Git. The one thing worth knowing is that a designer
form has a generated region, so two people moving buttons in the same form at the same time produces
a conflict in that region, exactly as it would in any other generated file.

**Can I mix RapidFX and Java in one project?**

Not in one module today. What you can do is build the RapidFX part as a jar and depend on it, in
either direction.

---

## Desktop, web and mobile

**Is a web application really the same as a desktop one?**

Yes, for the forms. The same `.rfx` draws the same screen in a `JFrame` and in a browser, because
the server paints Swing and sends the drawing. What differs is the entry point and how you change
screens — see [3 Several pages](03-pages.md).

**Can one project be desktop and web at once?**

Yes, and the templates are built so that it can: two entry points, shared forms. Keep the decision
*which screen now* out of the forms and in one place.

**Can mobile share the forms too?**

No — its widgets are a different set, so a screen is drawn twice. **Everything below the screen is
shared**: model classes, database code, calculations. Keep those in files that import neither
toolkit.

**Does the web version work on a phone browser?**

It is a drawing in a browser, so it displays. Whether it is pleasant depends on your form: a layout
designed for 1400 pixels is not better in a browser on a phone than anywhere else. For a real phone
application, build the mobile one.

**How many users does the web server take?**

Each session is a thread and about 1.2 MB. Hundreds on a modest machine; it is memory that runs out
first, and `IdleMinutes` is the dial.

---

## Mobile

**Do I need a Mac?**

For iPhone, yes — to build and to publish. Android works from Windows, macOS and Linux. That is a
deliberate decision and not an omission; a build server for other people's platforms is a month of
maintenance a year.

**Do I need an Apple developer account?**

For the simulator, no. For a device or the App Store, yes — that is Apple's rule, not ours.

**How big is an app?**

Tens of megabytes: the program, the runtime, and 10 MB of locale data so that dates and numbers are
right in 28 languages. An empty project is around 30 MB.

**Is it really compiled, not interpreted?**

Really compiled. Machine code, ahead of time, for arm64. There is no JIT on the device and no
bytecode interpreter.

**Can I call platform APIs that have no facade?**

On iOS, yes — the whole Cocoa Touch binding is there and callable. On Android it is harder, because
the Android class library lives in a second VM; the way across is the order channel, which is how
`Location` works. If you need something, that is the pattern to follow.

---

## Running and shipping

**How do I give somebody my program?**

Desktop: an installer, built by tsbDeploy for Windows, macOS and Linux from any of them — with a
Java runtime inside, so the person installing needs no Java. Web: one jar and a port. Mobile: the
stores, or an APK directly.

**Does my customer need a licence?**

No. What you compile is yours. The licensing module in this repository is for *your* product's
licences, if you want one.

**Which Java version does the result need?**

Desktop and web: 21 or newer, and tsbDeploy bundles one so the question does not arise. Mobile: none
— it is machine code.

---

## Practical

**Does the debugger work?**

Yes, IDEA's own. A RapidFX class carries line numbers, so breakpoints, stepping and variable
inspection work in `.rfx` files like in any other source.

**Is there a formatter?**

`⌥⌘L` / `Ctrl+Alt+L`. It indents and leaves everything else alone — it will not rewrite your line
breaks or reorder anything.

**Where do I report something?**

The issue tracker of this repository. A `.rfx` file that shows it is worth ten paragraphs of
description.

---

[← Mobile](06-mobile.md) · [Contents](README.md) · [Troubleshooting →](08-troubleshooting.md)
