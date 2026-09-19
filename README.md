# tsbRapidFX — the IDE plugins

Two plugins for IntelliJ IDEA. Each carries the tsbRapidFX compiler, and each carries a **form
designer** — one for the desktop and the web, one for Android.

> **Downloads are under [Releases](../../releases).** This repository holds the documentation and
> the issue tracker; the plugins themselves are published as release files.

---

## The designer

![The mobile designer](images/designer-mobile.png)

A palette on the left, the form in the middle, everything about the selected component on the
right. What it writes is a plain `.rfx` source file — readable text, not a binary layout format —
and what it reads back is the same file, even after you have edited it by hand.

That round trip is checked on every build: each form is parsed, written out again, and compared
byte for byte. A designer that quietly reformats your source is a designer you stop trusting.

## One form, three places

The same form file, opened by the designer once, running on the desktop, on Android and on iOS:

| Desktop | Android | iOS |
|---|---|---|
| <img src="images/one-form-desktop.png" width="200"> | <img src="images/one-form-android.png" width="320"> | <img src="images/one-form-ios.png" width="150"> |

Nothing is regenerated between them. The components are the same objects; only the backend that
draws them changes.

---

## The two plugins

### tsbRapidFX — Desktop & Web

The language in the IDE — syntax, completion, errors while you type, go to definition, structure
view — and the **Swing designer** for desktop applications and for web applications, which are
the same Swing forms mirrored into a browser.

![tsbErp on the desktop](images/erp-desktop.png)

The same application in a browser, drawn as SVG from the same forms:

![tsbErp in a browser](images/erp-web.png)

### tsbRapidFX Mobile

The **mobile designer** and the toolchain that goes with it: build a project, put it on a device,
run it. Android through `android.widget` and ConstraintLayout; iOS through UIKit.

---

## Installing

1. Download the `.zip` for the plugin you want from [Releases](../../releases).
2. In IntelliJ IDEA: **Settings ▸ Plugins ▸ ⚙ ▸ Install Plugin from Disk…**
3. Choose the zip, then restart the IDE.

Needs **IntelliJ IDEA 2025.2** or newer, and a **JDK 21** or newer to build with. Community
Edition is enough.

The two can be installed side by side. The desktop plugin ignores mobile forms and the mobile one
ignores desktop forms, so they do not fight over a `.rfx` file.

---

## Ninety days, and then

The designer runs for **ninety days** from the first form you open. After that the designer asks
to be bought — and only the designer.

**Everything else carries on for ever:** the editor, the completion, the error marks, the
structure view, and compiling from the IDE. The compiler and the runtime are
[Apache 2.0](https://github.com/thorstenstueker/tsbRapidFX) and always will be. Your forms are
untouched either way — they are ordinary `.rfx` source files and can be opened, read and edited
as text by anything.

You are reminded at fourteen days, seven, three, two and one. Nobody should meet this without
warning.

### Unlocking after purchase

No licence server, no account, no phoning home.

1. The designer shows a **station code** — twenty characters identifying that installation.
2. Send it with the address you bought under.
3. You get a **licence block** back by e-mail.
4. Paste it into **I have a licence…** in the designer.

That is once, for that machine, for the whole major version the licence names ("Valid: 2.x") —
through reinstalling the plugin, updating the IDE and updating the plugin within that version. A
new major version needs a new licence. A second machine has a second station code and needs its
own licence.

The check is a signature the plugin verifies against a public key compiled into it, offline. It
never contacts anything.

---

## Something not working?

[Open an issue](../../issues). Useful to include:

* which plugin, and which version — **Settings ▸ Plugins** shows it
* IntelliJ IDEA's version, from **Help ▸ About**
* what you did and what happened instead

If the compiler reported something, paste the message: it names the file, the line and the column,
and that is usually enough to find it straight away.

Questions about the **language itself** — the compiler, the runtime, `rfxc` — belong in
[thorstenstueker/tsbRapidFX](https://github.com/thorstenstueker/tsbRapidFX/issues), which is where
that code lives.

---

## Licence

The plugins are **commercial software** under the **TSB Commercial License (TSB-CL) 1.5**.
Copyright © 2026 tsb Thorsten Stueker Buero for Technology development. See `LICENSE` — including
what applications built with them may contain and ship (the tsb.mobile backends, tsbWEB,
tsbswing). Questions: licensequestions@stueker.org

The compiler, the runtime and the tsb.mobile API they carry are Apache 2.0 and are published
separately at [thorstenstueker/tsbRapidFX](https://github.com/thorstenstueker/tsbRapidFX). That split is
deliberate: whatever happens to this company, an application written with tsbRapidFX can still be
rebuilt — without the designer, without automation, but without asking anybody's permission.
