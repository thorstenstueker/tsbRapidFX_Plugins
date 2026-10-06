[← FAQ](07-faq.md) · [Contents](README.md)

# 8 Troubleshooting

In the order worth checking, with what each symptom actually means.

## The IDE

**The *Design* tab is missing, or *Open in Designer* does nothing.**

That file is not a designer form. A form carries a banner the designer wrote; a hand-typed file does
not, and the designer refuses rather than opening an empty canvas over your code and emptying the
file on the first *Apply*. Start from a template, or draw a new form and move your code into it.

**Everything is red, nothing resolves.**

The `src` folder is not marked as a source folder. Right-click it ▸ *Mark Directory as* ▸ *Sources
Root*. The wizard does this; a project copied by hand or checked out without its `.idea` does not
have it.

**A library in `lib/` is not found.**

It has to be attached to the module as well as present on disk: *File ▸ Project Structure ▸
Modules ▸ Dependencies ▸ + ▸ JARs or directories*.

**Completion does not offer a Java class.**

It is not on the class path of that module. The same place as above.

---

## Compiling

**`Type 'X' not found. Is an 'Imports' missing?`**

Either the import, or the jar is not on the class path. Check the import first — the compiler will
tell you the package it looked in.

**`'X' is not declared.`**

A name the compiler does not know in this scope. On a designer form the usual cause is that the
widget is named differently in the designer than in your code: the property table shows the *label*,
the code uses the *id*.

**`unbound lambda in the emitter`, with a stack trace.**

A compiler defect, not yours. Please report it with the file. One known cause was fixed on
03.10.2026 — a lambda at a fixed parameter of a varargs method.

**A one-line lambda swallows the arguments after it.**

`Sub(x) Print x` takes the **rest of the line** as its body, commas included. Write it across lines
with `End Sub` when something follows it:

```basic
doSomething( _
    Sub(x)
        Print x
    End Sub, _
    "and this")
```

**Since 05.10.2026 the compiler says so itself.** The error used to be
`No overload of 'doSomething' accepts (<Lambda>)` and nothing more, which is true and tells you
nothing: the second argument is not missing, it is inside the first. Now the same message carries
a line naming the cause — but only when there really is a comma in the lambda's body, so a call
that is simply short of an argument still says only that.

---

## Running — desktop

**The window is grey, or half of it looks like Windows 95.**

The look and feel was installed after a component was created. `FlatLightLaf.setup()` belongs in the
first line of `Sub Main`, before any window.

**The window freezes when I press a button.**

The handler is doing the work on the event thread. Move it to a thread of its own and come back with
`SwingUtilities.invokeLater`.

**Closing a second window ends the program.**

`EXIT_ON_CLOSE` on a window that is not the main one. A second window gets `DISPOSE_ON_CLOSE`.

**`Build Jar` works, `java -jar` does not.**

The libraries are beside the jar and named in its manifest, so the jar needs them in the same folder.
Copy `lib/` along, or build an installer, which packs everything.

---

## Running — web

**The browser shows nothing, and the console shows nothing.**

Check that the port is free and that you went to `http://` and not `https://` — there is no TLS in
the program itself; that belongs on a reverse proxy in front of it.

**Everybody sees the same data, or user A sees user B's.**

A `Shared` field where a `SessionStatic` belonged. `Shared` is one value for every user at once.
Both are declared at the top of `Main.rfx` so that this is visible on one screen.

**A click does nothing.**

A screen change needs both halves: write into the session **and** `rebuild()`. Without the rebuild,
`createRoot` is never asked again.

**One user's click hangs the whole session.**

A handler is blocking. Each session has its own thread, so other users are fine — but that one waits.

---

## Running — mobile

**`rfxmobile android` says no device.**

`adb devices` has to list one. An emulator has to be running and finished booting; a device needs
USB debugging and the dialogue on its screen answered.

**`INSTALL_FAILED_INSUFFICIENT_STORAGE`.**

The emulator is full, not your machine. Uninstall old builds:
`adb uninstall de.example.myapp`.

**The app installs and dies immediately.**

Look at the log — `adb logcat` on Android, the Xcode console or `xcrun simctl launch --console-pty`
on iOS. The usual cause is a class the ahead-of-time compiler did not keep because nothing *reached*
it: a JDBC driver loaded by name, a provider looked up by string. That is what
`Data.useDriverNamed` exists for.

**A screen stays on "loading" for ever.**

A result that arrived with no frame to arrive into, or a handler on the wrong thread. Both are
handled by `Work` — if you started a thread yourself, that is the cause.

**Dates and numbers are in the wrong format.**

Fixed in tsbTheMachine 27.5.0, and `rfxmobile` pins the version it needs. If you see `1,234.5` where
`1.234,5` belongs, the toolchain is older than that: delete `~/.rfxmobile/tsbthemachine-*` and let
it fetch again.

**The build warns about an unsupported `invokedynamic`.**

A library compiled for Java 21 or newer using something this compiler cannot resolve. It will build
and then fail **on the device** at that line. Compile that library for Java 17, or build with
`-indy:strict=true` so that it fails here instead.

---

## Still stuck

The build log says more than the IDE does. For mobile, `rfxmobile` prints every step and the
toolchain's own output; for desktop, the Run window has the whole stack trace.

A `.rfx` file that shows the problem is worth ten paragraphs of description. The issue tracker of
this repository is the place.

---

[← FAQ](07-faq.md) · [Contents](README.md) · [A build you can hand over →](09-regulated.md)
