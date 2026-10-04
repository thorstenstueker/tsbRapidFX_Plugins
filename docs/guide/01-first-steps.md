[Contents](README.md) · [The designer →](02-designer.md)

# 1 First steps

Fifteen minutes from nothing to a running program. No Gradle in your project, no Maven, no network
after the install.

## What you need

| | |
|---|---|
| **IntelliJ IDEA** | Community or Ultimate, 2024.2 or newer |
| **A JDK 21 or newer** | IDEA ships one; `File ▸ Project Structure ▸ SDKs` says which |
| **The plugin** | `rapidfx-idea-<version>.zip` |

Nothing else. **The compiler is inside the plugin** — you do not install RapidFX separately, and a
project you create has no build system in it.

For phones there are two more, and only when you build for one: the **Android SDK** for Android, and
**Xcode** for iPhone. See [6 Mobile](06-mobile.md).

## Installing

**Settings ▸ Plugins ▸ gear ▸ Install Plugin from Disk…**, choose the zip, restart.

That is all. Nothing to put on a path, nothing to configure.

## Your first project

**File ▸ New ▸ RapidFX Project…** — also in the **Tools** menu and on the welcome screen.

Choose a folder, a name, and one of the templates:

| | what you get |
|---|---|
| **RapidFX — Command line** | Two files that print something. The smallest thing that runs |
| **RapidFX — Desktop** | A window drawn in the designer, with FlatLaf |
| **RapidFX — Web** | Two forms and a server. Sign in at `localhost:8099` |

The project is written and opened. **Press the green arrow.**

That is the whole first step: the template compiles and runs as it stands, and there is no
configuration between you and it.

### What the wizard made

```
MyProgram/
  MyProgram.rfxproj         ' the project file — a few lines
  src/
    Main.rfx                ' Sub Main, the entry point
    MainWindow.rfx          ' the desktop template: a form
  lib/                      ' desktop and web: the jars that came along
```

Two files and not one, deliberately: a program may be more than one file, and the template is the
best place to show it. A type declared in one file is visible in all of them — nothing to import,
nothing to register.

## The first program, by hand

If you would rather see it without a wizard, this is a complete program:

```basic
Module Hello
    Sub Main()
        Print "Hello, world!"
    End Sub
End Module
```

Save as `Hello.rfx`, press the arrow. `Print` goes to the Run window.

### Something with a window

```basic
Imports javax.swing
Imports com.formdev.flatlaf

Module Main
    Sub Main()
        FlatLightLaf.setup()

        Dim window As JFrame = New JFrame("Hello")
        window.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE)
        window.setSize(300, 200)

        Dim label As JLabel = New JLabel("Hello, world!", SwingConstants.CENTER)
        window.setContentPane(label)
        window.setVisible(True)
    End Sub
End Module
```

**`FlatLightLaf.setup()` before the first window**, and that order matters: the look and feel only
decides what is created after it, so a component that already exists keeps the look it was born
with. It is the first line of `Sub Main` in every template for that reason.

## What the editor gives you

Highlighting, completion, go to declaration (`⌘B` / `Ctrl+B`), rename (`⇧F6`), find usages, and a
formatter (`⌥⌘L` / `Ctrl+Alt+L`). Errors appear as you type — the same messages the compiler gives,
because it is the compiler giving them.

**Breakpoints work.** A RapidFX class is a Java class with line numbers in it, so IDEA's debugger
steps through `.rfx` files like any other source: set a breakpoint in the gutter, press the bug.

## Where to go next

* **A window you draw instead of typing** → [2 The designer](02-designer.md)
* **More than one screen** → [3 Several pages](03-pages.md) — the chapter most people need second
* **The language itself** → the [complete manual](../tsbRapidFX-Manual.md), which is a different book
* **Something that does not work** → [8 Troubleshooting](08-troubleshooting.md)

---

[Contents](README.md) · [The designer →](02-designer.md)
