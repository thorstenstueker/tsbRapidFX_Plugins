# Acceptance protocol — the regulated build

**Eleven steps on the installation that is going to be used.** Each one says what to do, what the
right answer looks like, and what it means if the answer is different. It is written to be executed
by somebody who has not used RapidFX before, and kept with its result.

Read [`README.md`](README.md) first if you have not: §3 there lists what this build form does **not**
enforce, and a protocol that passes does not change that list.

| | |
|---|---|
| Plugin version under test | |
| `rfxc` version | |
| IDE and build | |
| JDK (`java -version`) | |
| Operating system | |
| Executed by / date | |

**What you need:** the plugin zip, an IDE, a JDK 21 or newer, and a RapidFX project that builds. A
project with at least one jar in `lib/` exercises more of the protocol than one without — step 9
depends on it.

**Fetch two files from the release as well** and keep them with the result: that version's
`KNOWN-ANOMALIES.md`, which is an input to the assessment this protocol does not make (README §8),
and the release notes, which carry the checksum step 1 compares against.

Throughout, `RFXC` stands for the compiler inside the installed plugin:

| | |
|---|---|
| macOS | `~/Library/Application Support/JetBrains/<IDE>/plugins/rapidfx-idea/lib/rfxmobile/rfxc-0.1.0.jar` |
| Linux | `~/.local/share/JetBrains/<IDE>/plugins/rapidfx-idea/lib/rfxmobile/rfxc-0.1.0.jar` |
| Windows | `%APPDATA%\JetBrains\<IDE>\plugins\rapidfx-idea\lib\rfxmobile\rfxc-0.1.0.jar` |

---

## 1 The artefact is the published one

```bash
shasum -a 256 rapidfx-idea-<version>.zip
```

**Expected:** the value in that version's release notes, digit for digit.

**If not:** stop. Everything below would be a statement about an unknown file. Download it again
from the release page; if it differs twice, say so before installing it anywhere.

## 2 It is installed, and it is the one running

1. *Settings ▸ Plugins* — if an earlier RapidFX is there, **uninstall it and restart**.
2. Install the zip from disk, restart again.
3. *Settings ▸ Plugins* names the version under test.

**Expected:** the version under test, after a restart that happened *after* the install.

**If not:** an install over an existing version does not reliably replace it. This is not
theoretical — on 06.10.2026 an IDE went on running the previous build while reporting the new
version, and a fault that had been fixed was reported as still present. Uninstall first.

> Record the result of this step even when it is boring. It is the step that makes every other
> result attributable to a version.

## 3 Both entries are in the menu

Open a RapidFX project and look at **Build**, then at **Tools**.

**Expected:** both menus carry **Build RapidFX Jar** and **Build RapidFX Jar (regulated)**. The
regulated entry is *not* in the right-click menus — that is deliberate, not a defect.

**If not:** the build form is unreachable from the IDE on this installation. Do not continue; the
procedure you are qualifying cannot be followed.

## 4 A clean project builds, and leaves a record

**Build ▸ Build RapidFX Jar (regulated)** on a project with no warnings.

**Expected:** `build/<Name>.jar` and **`build/build-record.json`** beside it, and a notification
naming the jar.

**If the build refuses:** read the message — it lists warnings with file and line. That is step 7's
behaviour arriving early; fix them, or use a clean project for steps 4 to 6 and keep the warning
project for step 7.

## 5 The notification says what is enforced — and what is not

Read the notification from step 4 to the end.

**Expected:** three measures named — a warning ends the build and nothing is written, every emitted
class is held against the class file format, a build record is written — and then a second half,
`NOT yet enforced`, naming the documented language subset.

**If the second half is missing:** report it. The product is then overstating itself in the one
sentence somebody will quote, and that is a defect of the same seriousness as a wrong build.

## 6 The record identifies the build

Open `build/build-record.json`.

**Expected:** every `.rfx` of the project under `sources`, each with a SHA-256; every class under
`outputs`; a 64-digit `fingerprint`; `tool.version` equal to the version from step 2; and
`environment` matching the machine.

`tool.jarSha256` reads `unavailable — compiled in process by the IDE plugin`. **That is correct**:
the compiler runs inside the IDE, so there is no single jar to hash, and the record says so rather
than hashing something else. Step 8 is where a hash of the compiler comes from.

**If a source file is missing from the list:** stop and report it. A record that does not name an
input cannot be used as evidence about that input.

## 7 A warning ends the build, and nothing is written

Add a line that warns but does not break the program — an import of something that is not on the
class path does it:

```basic
Imports com.nosuch.library
```

Delete `build/` first, so that what is there afterwards is only what this build wrote. Then run
**Build RapidFX Jar (regulated)**.

**Expected:** it refuses. The notification lists `RFX0610` with the file and the line, and **`build/`
holds no jar** — not an old one, not a partial one.

**If a jar appears:** this is the most serious possible failure of the protocol. The whole value of
a refusal is that the next step in a pipeline has nothing to pick up. Report it, and do not use the
build form until it is resolved.

Afterwards: remove the line again, so that steps 8 and 9 run on the real program.

## 8 The command line agrees with the IDE

Give it the same class path the project has — every jar from `lib/`, separated by `:` (`;` on
Windows). **Steps 8 and 9 have to use the same `-cp`;** the libraries count towards the fingerprint,
which is the point of recording them.

```bash
java -jar $RFXC --profile regulated -cp lib/helper.jar --jar build/cli.jar src
```

**Expected:** before anything is compiled, the same two halves as step 5 — the three enforced
measures and the `NOT yet enforced` subset, word for word as the notification had them. Then:

```
written: build/cli.jar  (16 entries)
start it with: java -jar build/cli.jar
record: /tmp/acc/build/build-record.json  fingerprint 269eca675e3721e7…ca6677ed
```

`libraries` in that record is one SHA-256 per class path entry — by content, without the path.

**If the wording differs:** the two are supposed to read one definition. A difference means they
have drifted, and one of the two is now lying about what it did.

```bash
shasum -a 256 $RFXC
```

Record that value: it is the identification of the compiler that step 6 could not give.

## 9 Somebody else can reproduce it

This is the step the whole build form exists for. Use the record from step 8, and give the same
class path the build had — every jar from `lib/`:

```bash
java -jar $RFXC --verify build/build-record.json -cp lib/helper.jar -o /tmp/check src
echo "exit: $?"
```

**Expected:**

```
verified: this machine reproduces build/build-record.json
  fingerprint 269eca675e3721e70a57f52d9c32e7d388d478dc1938255ab9a68c1bca6677ed
exit: 0
```

and **`/tmp/check` does not even exist afterwards** — the check does not produce an artefact.

**If it says NOT reproduced:** the message names the three candidates — the sources, the libraries
or the emitted classes.

The common and harmless cause is a class path that differs from the build's. Leaving `-cp` off
entirely looks exactly like a real failure:

```
rfxc: NOT reproduced.
  recorded:  269eca675e3721e70a57f52d9c32e7d388d478dc1938255ab9a68c1bca6677ed
  here:      c53fb45803e2afccc4fa9c154f620cf4f62d4a4b4ba591751368f47a4719cb2d
```

So check the class path first, and compare the record's `libraries` against a fresh `--record` of
the same call. A difference that survives that is a real finding — and one byte added to a library
does produce it, which is what the field is there for.

**Worth doing on a second machine with another operating system and another JDK.** That is the
strong form of this step: a fingerprint that agrees across two unlike machines is the statement
worth having, and one that only agrees where it was made says almost nothing.

## 10 A changed source is noticed

Change one character of one `.rfx` — a letter inside a string literal is enough — and repeat step 9
without rebuilding.

**Expected:** `NOT reproduced`, exit `1`, two different fingerprints printed.

**If it still says verified:** the check is not checking. Stop; every result above loses its
meaning.

Put the character back.

## 11 A missing record fails loudly

```bash
java -jar $RFXC --verify build/nothing-here.json -o /tmp/check src
echo "exit: $?"
```

**Expected:** `rfxc: --verify build/nothing-here.json — no such file`, exit `2`.

**If it exits 0:** a build script would read that as "verified" and nothing would ever say
otherwise. Report it.

---

## The result

| Step | | Result | Note |
|---|---|---|---|
| 1 | The artefact is the published one | | |
| 2 | Installed, and the one running | | |
| 3 | Both entries in Build and Tools | | |
| 4 | Clean build, jar and record | | |
| 5 | The notification names what is not enforced | | |
| 6 | The record identifies the build | | |
| 7 | A warning refuses, and writes nothing | | |
| 8 | The command line says the same thing | | |
| 9 | Reproduced — exit 0 | | |
| 10 | A changed source is noticed — exit 1 | | |
| 11 | A missing record is exit 2 | | |

**What a complete pass establishes:** that on this machine, with this version, the build form behaves
as documented. It establishes nothing about the program being built (§3 of the README), and it is
not a validation of the tool — that is the manufacturer's, and §9 of the README says what each side
carries.

**Keep with the result:** the version from step 2, the two checksums from steps 1 and 8, and the
`build-record.json` of step 4. Those four are what makes this protocol re-executable later against
the same thing.
