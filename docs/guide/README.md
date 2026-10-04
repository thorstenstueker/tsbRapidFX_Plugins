# RapidFX — the user guide

You have the plugin installed and want to build something. This is the short way in.

The **[complete manual](../tsbRapidFX-Manual.md)** is a different book: it describes the language
itself, chapter by chapter, and you reach for it when you want to know how `Select Case` works.
This one describes the *product* — the wizard, the designer, running things, and the one question
that has a different answer in each of the three worlds: **how do you get from one screen to the
next.**

---

## Contents

| | |
|---|---|
| **[1 First steps](01-first-steps.md)** | Install, create a project, run it. Fifteen minutes |
| **[2 The designer](02-designer.md)** | Drawing a form, anchors, what the generated region is |
| **[3 Several pages](03-pages.md)** | **Switching screens on desktop, mobile and web** — three worlds, three answers |
| **[4 Desktop](04-desktop.md)** | Windows, dialogs, the look and feel, packaging an installer |
| **[5 Web](05-web.md)** | A Swing program as a server, sessions, who may see what |
| **[6 Mobile](06-mobile.md)** | iPhone and Android, the database, background work, location |
| **[7 FAQ](07-faq.md)** | The questions that actually get asked |
| **[8 Troubleshooting](08-troubleshooting.md)** | When something does not work, in the order to check it |

**The examples** are real projects, not fragments: [`samples/`](../../samples/README.md) — one
directory each, each one runnable as it stands, and the three of them show the *same program* so
that the difference between the worlds is visible side by side.

---

## What RapidFX is, in one page

A Basic for the JVM. The grammar is the classic Basic grammar; underneath it lie the JVM and the
whole Java ecosystem, with no layer in between — a RapidFX class *is* a Java class, and anything
with a jar can be called from it.

```basic
Imports java.util

Module Hello
    Sub Main()
        Dim names As ArrayList = New ArrayList()
        names.add("World")

        Dim name As String
        For Each name In names
            Print "Hello, " + name + "!"
        Next name
    End Sub
End Module
```

That compiles to `Hello.class` and runs with `java`. No build system in the project, no XML, no
annotations.

### The four things you can build

| | what it is | how it runs |
|---|---|---|
| **Command line** | A program that prints | `java -jar` |
| **Desktop** | Swing, drawn in the designer | A window, or an installer for Windows, macOS and Linux |
| **Web** | **The same Swing forms**, painted on a server and sent to a browser | `java -jar`, then `http://localhost:8099/` |
| **Mobile** | iPhone and Android, compiled ahead of time | `rfxmobile ios` / `rfxmobile android` |

**A web application is a desktop application.** There is no web-only component, no annotation, no
markup and no template language: the same form shows the same screen in a browser as it does in a
`JFrame`. That is worth knowing before you start, because it decides how you structure a project —
see [3 Several pages](03-pages.md).

Mobile is the one that is genuinely different, and it is different because a phone is: one screen
at a time, a back gesture, and no window to put anything in.
