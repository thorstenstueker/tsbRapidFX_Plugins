[← Troubleshooting](08-troubleshooting.md) · [Contents](README.md)

# 9 A build you can hand over

**Build ▸ Build RapidFX Jar (regulated)** is the build for the jar that leaves your machine — the one
that gets signed, installed, and used by somebody who did not write it.

It is for the case where you will be asked afterwards: *what is in this jar, what was it built from,
with which compiler, and was anything ignored on the way?* In a regulated field that question comes
on paper, years later, about a version nobody has open any more. The ordinary build cannot answer
it; this one writes the answer down.

> **"Regulated" is the situation, not a certificate.** Nothing here says this compiler is approved
> for anything — a development tool is validated by whoever uses it, and this build is what makes
> that possible. Where a measure is not implemented, the notification says so every single time.

---

## What it does

In **Build** and in **Tools**, right under the ordinary *Build RapidFX Jar*. It does everything that
one does, and three things more:

| | |
|---|---|
| **A warning ends the build** | and **nothing is written** — no jar, not a partial one, and no older one left behind |
| **Every class is verified** | held against the class file format before anything is written, rather than first on somebody else's machine |
| **A build record is written** | `build-record.json`, beside the jar |

What you get in `build/`:

```
build/
  MyProgram.jar        the program, as the ordinary build writes it
  build-record.json    what it was built from, and one number that identifies the whole build
  lib/
    flatlaf-3.7.2.jar  as usual, beside the jar
```

The notification names the three measures — and then names what this build does **not** enforce. Two
of the five measures planned for it are a documented subset of the language, and that subset is not
built yet. It is printed every time rather than left out, because somebody who read only the word
*regulated* would assume it was there.

**Two menu entries rather than a checkbox.** Which of the two produced the jar in `build/` must never
be in doubt, and a setting is invisible at the moment you are building. The ordinary build is also in
the editor's context menu because it is done fifty times a day; this one is not, because it is done
once, at the end.

## When it refuses

A warning you have lived with all week will stop this build:

```
1 warning(s), and profile regulated was asked for. Nothing was written.
RFX0610 Main.rfx:1 'com.nosuch.library' is neither a package nor a class on the class path.
```

The notification lists every warning with its file and line, and nothing has been written.

There is one warning the compiler itself raises, and you will meet it here: **`RFX0610`, an
`Imports` that finds nothing.** Usually a library that is on disk but not attached to the module
(*File ▸ Project Structure ▸ Modules ▸ Dependencies*), or a typo in the import. It matters more in
this build than in the ordinary one, because an import that resolves to nothing means the program
was compiled against less than it names — and the jar would be signed in that state.

On the command line there is a second one, `RFX9001`, for a library that could not be read at all.
The IDE knows its own module dependencies and does not need it.

**There is no "ignore this warning" in the dialog, on purpose.** Either you fix it, or you build on
the command line with `--suppress RFX0610` — where the suppression stands in the build log and in the
record, and somebody can see that a decision was taken. A button that made a warning disappear from
the IDE would leave no trace of it anywhere.

A mobile project is refused too, with a different sentence: a phone application is packaged as an
APK or an IPA (*Build a signed APK / IPA*), and that route has its own signing and its own checks.

## The build record

`build-record.json` is plain JSON, meant to be read by a person and by whatever your QA already uses:

| | |
|---|---|
| `fingerprint` | one SHA-256 that stands for *these sources produced these classes* |
| `tool` | which RapidFX this was, by plugin version |
| `environment` | Java, operating system, locale, time zone — recorded, but **not** part of the fingerprint |
| `sources` | every `.rfx` file, by name and SHA-256 |
| `libraries` | every library, by content — not by file name or path |
| `outputs` | every class file produced, by name and SHA-256 |
| `diagnostics` | everything the compiler said, including warnings that were suppressed elsewhere |

The fingerprint leaves out the environment, the paths and the time deliberately. It has to be
reproducible on a different machine in a different directory, and a timestamp would make two records
of the same build disagree.

One thing the record admits rather than fakes: built from the IDE, it cannot hash the compiler's own
jar, because the compiler runs inside the IDE. It says so, and names the plugin version instead.

## Checking a build later

The record is checkable with one command, and whoever runs it needs neither this IDE nor any
knowledge of RapidFX. `rfxc` travels inside the plugin:

| | |
|---|---|
| macOS | `~/Library/Application Support/JetBrains/<IDE>/plugins/rapidfx-idea/lib/rfxmobile/rfxc-0.1.0.jar` |
| Linux | `~/.local/share/JetBrains/<IDE>/plugins/rapidfx-idea/lib/rfxmobile/rfxc-0.1.0.jar` |
| Windows | `%APPDATA%\JetBrains\<IDE>\plugins\rapidfx-idea\lib\rfxmobile\rfxc-0.1.0.jar` |

```bash
java -jar rfxc-0.1.0.jar --verify build/build-record.json -o /tmp/check src
```

```
verified: this machine reproduces build/build-record.json
  fingerprint a3b223ca4e3076e6a84f0e14ef2acbecc35879d81cb384464258c011bb22707b
```

It writes nothing, and the exit code carries the answer as well: `0` reproduced, `1` not reproduced,
`2` the record is missing or is not one. A build server can act on that without reading a word of
output.

The whole build is available on the command line too, which is what a build server wants:

```bash
java -jar rfxc-0.1.0.jar --profile regulated --project MyProgram.rfxproj
```

**The menu entry and the switch are one definition**, so they cannot come to mean different things —
the notification and the console print the same sentences.

## What it does not answer

**Which lines of your program were never executed.** Coverage is measurable on a RapidFX program
today, but it is still a handful of commands rather than a button, and on a phone it is not possible
at all — a mobile program is compiled ahead of time, so there is nothing to instrument.

**Whether your program is correct.** This build certifies nothing about what the program does. It
makes the *build* accountable: what went in, what came out, and that nothing was waved through.

**If you have to qualify this build form rather than just use it**, there is a document for that:
[`docs/regulated/`](../regulated/README.md) — what it guarantees, how to prove it is installed, what identifies the tool that
produced a given jar, and an eleven-step acceptance protocol with the expected result of each step.

**Part IX of the [complete manual](../tsbRapidFX-Manual.md)** goes through the same ground in more detail, including the
individual switches this build is made of — `--record`, `--verify`, `--strict`, `--suppress` and
`--verify-output`, all of them also in `rfxc --help`.

---

[← Troubleshooting](08-troubleshooting.md) · [Contents](README.md)
