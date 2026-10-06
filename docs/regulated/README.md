# The regulated build

**A document of its own, because somebody is going to plan around it.** A development procedure that
names *Build ▸ Build RapidFX Jar (regulated)* is a procedure that has to be followed on a given day,
on a given machine, by somebody who did not write any of this — and from then on "the feature is in
the product" is not a good enough statement. This document says what the build form guarantees, how
to establish that it is really present on an installation, what it does **not** claim, and what
would invalidate all of that.

| | |
|---|---|
| **Applies to** | `rapidfx-idea-2026.1.90` and `rfxc 0.1.0` as shipped inside it |
| **Written** | 06.10.2026 |
| **Status of the feature** | implemented and in use; three of five planned measures (§3) |
| **Scope** | desktop and web programs. A mobile project is refused by name — see §3 |

Read §1 first if you have an installation in front of you, §9 if you are deciding whether to rely on
this at all.

> ### It is called *regulated*, not *certified*
>
> Nothing in this document is a claim that this compiler is approved, qualified or certified for any
> purpose. A development tool is validated **by the manufacturer who uses it** — IEC 62304 §5.1.4
> puts that duty on them, and ISO 26262, IEC 61508, EN 50716 and DO-178C each have their equivalent.
> What is described here is the evidence this tool can produce so that such a validation is possible
> at all, and the places where it produces none.

---

## 1 Establishing that the build form is there

Three things, in this order. None of them takes longer than a minute, and the second one exists
because of a day that was lost to it.

**1. The artefact is the one that was published.**

```bash
shasum -a 256 rapidfx-idea-2026.1.90.zip
6c4d69a5a3459c268482991007f3aef9c4a65979dad8b9e23974ead3b21dd7dd
```

That value is in the release notes of the version it belongs to. A plugin zip is an ordinary file
on somebody's download directory; this is the only statement that it is the published one.

**2. It is installed — which is not the same as having installed it.**

*Settings ▸ Plugins* names the version. **Uninstall the previous version first, restart, then install
the zip and restart again.** On 06.10.2026 an install from disk over an existing version did not
replace it: the IDE went on running the old build, and a fault that had been fixed was reported as
still present. Half a day went into reading the shipped class files to establish that the fix was in
the artefact and not in the running IDE.

So: if something behaves as it did before the update, the first question is about the install and
not about the product.

**3. The two entries are in the menu.**

*Build* and *Tools* each carry **Build RapidFX Jar** and **Build RapidFX Jar (regulated)**. Two
entries rather than one with a setting, so that which of the two produced the jar in `build/` is
never a question of what a checkbox was on. The regulated entry is deliberately **not** in the
right-click menus: the ordinary build belongs there because it is done constantly, this one is done
once, at the end.

A test holds that — `RegulatedBuildAvailableTest` fails if the entry loses a menu, if it stops saying
"regulated", if the profile stops enforcing one of its three measures, or if its description stops
naming what it does not enforce. Each of those would otherwise be silent: a build that no longer
refuses still succeeds.

## 2 What it enforces

| | |
|---|---|
| **A warning ends the build** | and **nothing is written** — no jar, no partial jar, and no older jar left in place for the next step to pick up |
| **Every emitted class is verified** | held against the class file format before anything is written. The JVM does this at load; doing it here means the artefact was checked on the machine that built it rather than first on somebody's device |
| **A build record is written** | `build-record.json`, beside the jar, with one fingerprint over the sources, the libraries and the output (§5) |

On the command line the same build is one switch:

```bash
rfxc --profile regulated --jar build/program.jar src
```

and it prints what it is doing before it compiles a line:

```
profile regulated — enforced:
  warnings end the build, and nothing is written
  every emitted class is held against the class file format before writing
  a build record is written, with a fingerprint over sources, libraries and output
profile regulated — NOT yet enforced:
  the documented language subset (constructs whose semantics are unmeasured, and
  constructs that behave differently per target). Not built; do not rely on it.
```

**The menu entry and the switch are one definition.** `rfxc` parses a command line; the IDE compiles
in process, because it already holds the sources in the editor — the two share no code path. What
"regulated" means therefore lives in one place, `com.rapidfx.compiler.build.Profile`, and both ask
it. The sentence the IDE's notification shows and the sentence the console prints are the same
sentence from the same source. A profile implemented on each side would be two profiles, and two
implementations of one rule drift.

## 3 What it does not enforce

Said here rather than in an appendix, because a reader who stops after §2 would otherwise rely on
things that are not there.

**The language subset is not built.** Two of the five measures this profile is meant to have are a
documented subset of the language: refusing constructs whose semantics are unmeasured, and
constructs that behave differently from one target to the next. Which constructs belong in it is a
product decision and not derivable from the compiler. Until it exists, **every regulated build
prints that it is missing** — a profile that quietly enforced three fifths of what its name suggests
would be worse than no profile, because somebody would rely on the other two.

**Mobile is out of scope, and refused rather than silently different.** The regulated entry on a
mobile project answers *"This is a mobile project. It is packaged as an APK or IPA (Build a signed
APK / IPA), not as a desktop installer."* A phone program is compiled ahead of time into a native
image through a different toolchain; nothing in this document describes it.

**Nothing here is a statement about the program.** The build is made accountable: what went in, what
came out, that nothing was waved through. Whether the program is correct is tested by whoever wrote
it.

**Coverage is not part of it.** Which statements of a program were never executed can be measured
today — an ordinary Java coverage tool reports on `.rfx` lines directly, because the emitter writes
`SourceFile` and the `LineNumberTable` correctly per type — but it is a recipe of several commands
rather than a switch, and on a phone it is not possible at all. Making it one command means deciding
whether the coverage tooling ships, which brings a further third-party licence into the delivery.

## 4 Identifying the tool that produced an artefact

For an artefact already in existence, this is the question an audit starts with.

| Where it was built | What identifies the tool | Where to read it |
|---|---|---|
| The IDE | the plugin's version | the record's `tool.version`, and *Settings ▸ Plugins* |
| `rfxc` | the jar's file name **and its SHA-256** | the record's `tool.jar` and `tool.jarSha256` |

**The record admits what it cannot know.** Built from the IDE, `tool.jarSha256` reads
`unavailable — compiled in process by the IDE plugin`: the compiler runs inside the IDE's own class
loader, so there is no single file to hash. A hash of the wrong thing would be worse than an
admission — a record is evidence, and evidence that overstates what it knows is not evidence.

Where a hash is wanted for an IDE build, `rfxc` is inside the installed plugin and is the same
compiler:

| | |
|---|---|
| macOS | `~/Library/Application Support/JetBrains/<IDE>/plugins/rapidfx-idea/lib/rfxmobile/rfxc-0.1.0.jar` |
| Linux | `~/.local/share/JetBrains/<IDE>/plugins/rapidfx-idea/lib/rfxmobile/rfxc-0.1.0.jar` |
| Windows | `%APPDATA%\JetBrains\<IDE>\plugins\rapidfx-idea\lib\rfxmobile\rfxc-0.1.0.jar` |

In `2026.1.90` that jar is
`555654d8047618d47d00c065f03e8739eac84758035cfdb6c71c47bc4e3e9e4d`, byte for byte the one in the
published zip — checked by hashing both.

Both numbers move with every release. The one that belongs to the version you are holding is in that
version's release notes, and §1 is how you establish it before anything else.

## 5 The build record

JSON, so that it can be read by a person and by whatever the reviewer already uses.

| | |
|---|---|
| `fingerprint` | one SHA-256 over the reproducible part of the run |
| `fingerprintCovers` | what that is, in a sentence, so the document explains itself |
| `tool` | name, version, and the compiler jar's SHA-256 where there is one (§4) |
| `environment` | Java version and vendor, operating system and architecture, locale, time zone, encoding |
| `command` | the arguments as given, so the call can be repeated |
| `sources` | every source file, by name and SHA-256 |
| `libraries` | every class path entry by SHA-256 — **by content, without its path** |
| `outputs` | every class file produced, by name and SHA-256 |
| `diagnostics` | everything the compiler said, **including warnings that were suppressed** on the console |

**The fingerprint deliberately excludes the environment, the paths and the time.** A reviewer on
another machine, another JDK and another directory has to be able to recompile and arrive at the
same value; a number that only ever matches where it was written proves nothing. A timestamp would
make two records of one compilation differ, which costs the only property that makes them
comparable. The environment is in the document for the reader, one field away from the hash.

Measured: the same sources built in two directories, once through `--jar` and once through `-o`,
produce the one fingerprint
`a3b223ca4e3076e6a84f0e14ef2acbecc35879d81cb384464258c011bb22707b`.

**A suppressed warning is still in the record.** Silencing a message on the console does not silence
it in the account of the build, which is what makes `--suppress` acceptable in a regulated build at
all: the decision stays visible to whoever reads the record afterwards. There is no way to suppress
a warning from the IDE — either it is fixed or the build is run on the command line, where the
suppression stands in the log.

## 6 Independent verification

One command, and the person running it needs neither the IDE nor any knowledge of RapidFX:

```bash
rfxc --verify build/build-record.json -o /tmp/check src
```

```
verified: this machine reproduces build/build-record.json
  fingerprint a3b223ca4e3076e6a84f0e14ef2acbecc35879d81cb384464258c011bb22707b
```

| Exit | Meaning |
|---|---|
| `0` | this machine reproduces the recorded run |
| `1` | it does not — the sources, the libraries or the emitted classes differ |
| `2` | the record is missing, or carries no fingerprint |

It **writes nothing**: the question is whether the same result comes out, so there is no second
artefact to confuse with the one being examined. A build server can act on the exit code without
reading a word.

Two older properties are what make an answer possible, and both are held by tests rather than by
intention: the compiler **emits deterministically** — every jar entry carries the same fixed
timestamp, so two builds of the same sources are the same bytes — and the class path is an input to
the compilation rather than a setting, so the record can name it.

**What a reproduced fingerprint does not prove.** That the sources were the right sources, that the
libraries were the intended versions, or that the program is correct. It proves that these inputs
produce these outputs, with this compiler, anywhere. Everything else is the manufacturer's (§9).

## 7 What the toolchain fetches, and what is pinned

A regulated build is a supply-chain question as much as a compiler question, so this is stated
explicitly rather than left to be discovered.

**A regulated build of a desktop or web program downloads nothing.** The compiler, the runtime
library and the designers are inside the plugin; the build runs in process or through `rfxc`. There
is no network step in the route described by §2, which is also why it works on a machine that has
none.

Two things are fetched rather than shipped, and **both belong to the mobile toolchain**, which §3
puts out of scope for this build form:

| | | |
|---|---|---|
| `tsbthemachine-dist` | the iOS AOT toolchain, ~140 MB | only for an iOS build |
| `sqlite-jdbc` | the JDBC driver, 14 MB | only for a mobile project that touches `java.sql` |

Since 06.10.2026 both are pinned by a **SHA-256 this project recorded**, not by a checksum fetched
beside the artefact. The difference is the whole point: a checksum served by the host that serves the
file can establish that the two agree, and if the artefact is replaced both change together. A
release asset can be replaced in place with one command.

* A **cached** copy is checked rather than merely found — `isRegularFile` accepts a half-written
  download and the artefact of a version the project has moved off.
* The published checksum is still read where there is no pin, `.sha256` first and `.sha1` only as a
  fallback, and it is shape-checked so a mirror answering an error page with HTTP 200 counts as *no
  checksum* rather than as a mismatch.
* A recorded checksum says which algorithm it is by its length — 64 hex digits SHA-256, 40 SHA-1 —
  so pins can be upgraded one at a time. SHA-1 is read, and nothing is pinned with it any more: a
  collision is a pair of files with one digest, and SHA-1 has been producing those on demand since
  2017.
* `MachineVersionTest` ties the pin to the version: the checksum's own documentation must name the
  version that is fetched, the pin must be a SHA-256, and where the archive lies in the cache its
  bytes are held against it.

## 8 Known anomalies

**[`KNOWN-ANOMALIES.md`](KNOWN-ANOMALIES.md), published beside this document** and
attached to the release it belongs to. Two halves, and both are evidence rather than prose:

* the **compiler limitations** are *generated* — each entry is produced by compiling a program that
  provokes the limitation, so an entry proves the limitation is still there and that the wording is
  the one a reader will meet. When one is lifted its entry disappears, and a test fails until
  somebody has looked;
* the **platform anomalies** are facts about the runtime's C libraries and data, measured against a
  real JDK, each naming the experiment that holds it.

**The list belongs to one version.** Use the copy attached to the release you are qualifying, not
the one in the repository's main branch, which moves with development. The release asset and the
version named at the top of this document go together.

Its risk column is deliberately empty, and that is the one place this document asks something of
the reader. Under ISO 14971 a risk belongs to a hazard in a particular device, not to a defect in a
tool — the same wrong date is an inconvenience in a stock list and something else in a dosing
interval. The entries state what happens so that the classification can be made by whoever knows the
device; making it from here would be making somebody else's assessment for them.

## 9 Who is responsible for what

| | |
|---|---|
| **This tool provides** | the three measures of §2, a build record that identifies inputs, tool and outputs, a one-command reproduction check, a generated list of known anomalies, and an explicit statement of what is not enforced |
| **The manufacturer does** | the tool validation itself: deciding which of these measures its process needs, running them on its own build, keeping the records, and assessing what a defect in this tool could do to its device |

That division is not modesty, it is where the standards put it. IEC 62304 §5.1.4 asks the
manufacturer to validate the tools it uses in software development; nobody can do that from here,
because it depends on what is being built and on what the consequence of an error would be.

## 10 What invalidates a qualification

A qualification is of one version on one procedure. These are the events that end it, and each has
an obvious re-check:

| Event | What to do |
|---|---|
| a new plugin or `rfxc` version | re-run §1 and the acceptance protocol; record the new version and checksums |
| a new JDK under the IDE | re-run the protocol. The record's `environment` will differ by design; the fingerprint must not |
| the language subset arriving | re-read §3: the profile will then enforce more than it does today, and its printed description will say so |
| a moved toolchain pin (mobile only) | out of scope for this build form; `MachineVersionTest` is what notices |

## 11 The acceptance protocol

[`ACCEPTANCE.md`](ACCEPTANCE.md) is the executable part: eleven steps with the expected result of
each, to be run on the installation that will be used and kept with the result. It is written so that
somebody who has never seen RapidFX can execute it, and so that each step fails visibly rather than
quietly.

---

## Where the detail is

Named rather than linked, because these two documents travel in one repository and the others in
another, and a link that resolves in one place and dangles in the other is worse than a name.

| | |
|---|---|
| The switches, one by one | *A build that can be accounted for* — chapter 15 of the language chapters |
| The same ground as a narrative | Part IX, chapter 28 of the complete manual |
| From the IDE, for whoever builds | *A build you can hand over* — chapter 9 of the user guide |
| What is checked, and by whom | `docs/TESTPLAN.md` §9 in the development repository |
| The protocol | [`ACCEPTANCE.md`](ACCEPTANCE.md), beside this file |
