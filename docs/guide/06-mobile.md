[← Web](05-web.md) · [Contents](README.md) · [FAQ →](07-faq.md)

# 6 Mobile

One program, compiled ahead of time, drawing the same screen on an iPhone and on an Android. Not
native widgets that look different on each — one rendering, one layout, one set of pixels.

This chapter is the product view. **Part VII of the [complete manual](../tsbRapidFX-Manual.md)**
covers the same ground from the code side, with every example compiled by a test; read that one when
you are writing and this one when you are deciding.

## What you need

| | |
|---|---|
| **Android** | The Android SDK. `ANDROID_HOME`, an emulator or a device with USB debugging |
| **iPhone** | A Mac with Xcode. The simulator needs no account; a device needs a signing identity |

`rfxmobile` downloads the compiler toolchain on first use — about 130 MB, once per machine, shared
by every project.

## A project

```
MyApp/
  rfxmobile.properties
  SignIn.rfx
  MainMenu.rfx
  assets/
    logo.png
```

```properties
name    = My App
form    = SignIn
package = de.example.myapp

titlebar    = off
theme       = Dark
orientation = portrait
```

The manifest, the `Info.plist` and the entry point are **generated** — including the permissions,
which are read out of your compiled forms. See *Permissions* below.

## Running it

```bash
rfxmobile swing   MyApp          ' on this machine, in a phone-sized window
rfxmobile android MyApp          ' build, install, start
rfxmobile ios     MyApp --device "iPhone 17"
rfxmobile android MyApp --release
```

**`swing` is the one you will use most.** It starts in seconds, needs no device, and draws exactly
what the phone draws — the same renderer. The platforms differ in their pixel count, not in their
layout.

## What is different from the desktop

**The widgets are a different set.** `tsb.mobile.Button`, not `javax.swing.JButton`. A desktop form
does not become a mobile form by changing an import; it is drawn again in the mobile designer.

**What you can share is everything below the screen** — model classes, database code, calculations.
Keep those in files that import neither toolkit and they compile for all three worlds.

**One screen at a time, and a stack.** See
[3 Several pages](03-pages.md#mobile-a-stack-and-a-back-gesture-that-is-not-yours).

**Nothing may block.** Android kills an application that holds the drawing thread for a few seconds,
with a dialogue the user reads as a crash. That is what `Work` is for, below.

## Data

A phone carries its own SQLite database:

```basic
Data.useDriverNamed("SQLite.JDBCDriver")
Dim c As Connection = DriverManager.getConnection("jdbc:sqlite:" + Data.file("erp.db"))
```

`Data.file(name)` gives the full path in the application's private directory — the one place both
platforms let a program write, and the one that is removed when the program is uninstalled.

**`useDriverNamed` and not `useDriver`**: ahead-of-time compilation keeps the classes a program
*reaches*, and a driver loaded by name is reached by nobody. The build is told for you when a form
touches `java.sql`.

## Something that takes a while

```basic
Work.text( _
    Function() As String
        Return fetch("https://erp.example.com/customers")
    End Function, _
    Sub(answer As String)
        lblStatus.setText(answer)
    End Sub, _
    Sub(e As Throwable)
        lblStatus.setText("Server not reachable")
    End Sub)
```

The first lambda runs away from the screen and **must not touch a widget**. The other two run where
widgets are allowed.

| | |
|---|---|
| `Work.text(job, done, failed)` | Work producing a string |
| `Work.run(job, done, failed)` | Anything else, with a cast |
| `Work.chain(first, second, done, failed)` | Two steps back to back, one answer |
| `Work.all(done, failed, job…)` | Several at once, results in the order asked for |
| `Work.whenBusy(watcher)` | For a spinner; told on the edges only |
| `Work.pending()` | How many are outstanding |

An answer belongs to the form that asked: leave a form while its fetch is in flight and the handler
is not called. A form merely covered by another is still answered.

## The network

Ordinary `java.net`. There is no `HttpClient` — the runtime comes from Android's class library,
where `java.net.http` does not exist — so `HttpURLConnection` is the way, and **always with both
timeouts set**:

```basic
c.setConnectTimeout(5000)
c.setReadTimeout(5000)
```

HTTPS needs no configuration. The trust store carries its root certificates and the permission is
written into the manifest for you.

## Where the device is

```basic
Location.whenKnown(AddressOf OnPlaceFound)

Private Sub OnPlaceFound(place As Place)
    If place Is Nothing Then
        lblWhere.setText("No location: " + Location.access().name())
    Else
        lblWhere.setText(place.latitude() & ", " & place.longitude())
    End If
End Sub
```

Nothing here throws, including at your desk where there is no location: `access()` says
`UNAVAILABLE`, `last()` is `Nothing`, and a handler is called once with `Nothing`.

## Permissions

You do not write them. The build reads your compiled forms:

| what your program uses | Android | iOS |
|---|---|---|
| `Location` | `ACCESS_FINE_LOCATION` | `NSLocationWhenInUseUsageDescription` |
| `java.net.URL`, `Socket`, … | `INTERNET` | — |

Apple's reason text is the one thing that cannot be derived, because a reviewer reads it:

```properties
reason.location = Shows the delivery address on a map while you are using the app.
```

**Bring your own manifest and yours wins** — and then nothing can be inserted into it, so the build
warns rather than being silent.

## Signing and the stores

`--release` signs with the project's key and puts the result in `build/release/`. Passwords come
from the environment or the system keychain, never from `rfxmobile.properties`.

Icons are generated from one source image in every size both stores ask for, including the 1024
marketing icon App Store Connect wants at upload.

**Publishing for iOS needs a Mac.** That is a decision, not an omission: a build server for other
people's platforms is two days of maintenance per provider per year. Android releases from any
desktop system.

## What is not there yet

* **Camera, barcode, sensors, Bluetooth.** Location is the only hardware with a facade so far.
* **Some of `java.base`** — about 318 members of classes that do exist, mostly in corners a business
  program does not reach. `rfxc` tells you at *compile* time rather than on the device.

---

[← Web](05-web.md) · [Contents](README.md) · [FAQ →](07-faq.md)
