[← Several pages](03-pages.md) · [Contents](README.md) · [Web →](05-web.md)

# 4 Desktop

Swing, drawn in the designer, running as an ordinary Java program — and packaged as an installer
for Windows, macOS and Linux when you are done.

## The entry point

```basic
Imports javax.swing
Imports com.formdev.flatlaf

Module Main
    Sub Main()
        FlatLightLaf.setup()

        Dim window As JFrame = New JFrame()
        window.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE)

        New MainWindow().ShowIn(window)
    End Sub
End Module
```

Three things in that, each worth a sentence:

**The look and feel comes before the first window.** `UIManager` only decides what is created from
here on, so a component that exists already keeps the look it was born with. Put it anywhere else
and half your program is Metal.

**The frame belongs in `Main` and not in the form.** That is what lets the same form sit in a frame
now and in a dialog later without a line of it changing — see [3 Several pages](03-pages.md).

**`ShowIn` sets the title, the size and the position** out of what the designer knows, so those live
in the designer and not in two places.

## What runs where

A Swing program is an ordinary Java program. `Print` goes to the console, `System.out` works,
threads work, and everything in the JDK is reachable. Two rules of Swing's own hold here as
everywhere:

**Build and change widgets on the event thread.** Everything the designer generates and every
handler already runs there. Work started yourself does not:

```basic
Dim worker As Thread = New Thread(New LoadTask())
worker.start()

' …and from the worker, back onto the event thread:
SwingUtilities.invokeLater(New UpdateTask())
```

**Do not block the event thread.** A handler that reads a file or asks a server freezes the window
until it finishes — the same rule that mobile makes unavoidable and the desktop merely makes
unpleasant.

## The look and feel

Four themes ship with the plugin: **Light**, **Dark**, **IntelliJ**, **Darcula**, all FlatLaf.

```basic
FlatLightLaf.setup()        ' or FlatDarkLaf, FlatIntelliJLaf, FlatDarculaLaf
```

Follow the system setting instead:

```basic
If isSystemDark() Then
    FlatDarkLaf.setup()
Else
    FlatLightLaf.setup()
End If
```

To change it while the program runs, set it and tell Swing to rebuild every window:

```basic
FlatDarkLaf.setup()
Dim window As java.awt.Window
For Each window In java.awt.Window.getWindows()
    SwingUtilities.updateComponentTreeUI(window)
Next window
```

## Building a jar

In the IDE: **Build ▸ Build RapidFX Jar**. On the command line:

```bash
rfxc --project MyProgram.rfxproj
```

The result is one runnable jar. Libraries in `lib/` end up **beside** it and in the manifest's
`Class-Path`, not inside it — which keeps the jar small and lets you replace a library without
recompiling.

```bash
java -jar MyProgram.jar
```

For the jar that gets signed and handed to somebody else there is a second entry,
**Build RapidFX Jar (regulated)** — it refuses on a warning and writes down what it built from. See
[9 A build you can hand over](09-regulated.md).

## An installer

Double-clickable installers for all three desktop systems, **from any of them** — no CI, no
jpackage, no cloud:

| | formats |
|---|---|
| **Windows** | Setup EXE, MSI · signed with Authenticode |
| **macOS** | DMG, and PKG when built on a Mac · signed, optionally notarised |
| **Linux** | AppImage |

Each carries a Java runtime, cut to the modules your program actually uses, so the person installing
needs no Java at all. The runtimes for all six platform-and-architecture combinations are fetched
once into a local repository and reused.

That is **tsbDeploy**, and it has three faces: a Swing application of its own, a command line, and
an IntelliJ plugin that can take the application straight out of an *Application* run
configuration — module, classes, libraries and main class — and bring its own entry to the run
toolbar.

Licence notices for everything it bundles — the starter, the setup program, the AppImage runtime,
the Java runtime — are generated into every installer, with the source locations.

## Where the designer's forms come from

The desktop designer is **tsbDesignerSWX**, and it is the same tool the web uses, because a web
application *is* a Swing application here. See [5 Web](05-web.md) — even if you never write one, the
first two paragraphs explain why the forms are shared.

---

[← Several pages](03-pages.md) · [Contents](README.md) · [Web →](05-web.md)
