# Two pages, on a phone

The same two pages as the desktop and web samples beside this one, and the same
question — but the answer is a third one, and the reason is the back gesture.

## Running it

```bash
rfxmobile swing .      # this machine, in a phone-sized window — start here
rfxmobile android .    # build, install, start
rfxmobile ios .        # needs a Mac with Xcode
```

`swing` draws exactly what the phone draws, because it is the same renderer. The
platforms differ in their pixel count, not in their layout.

## What to look at

`PageOne.rfx` and `PageTwo.rfx` are **designer forms**: open either one and the
*Design* tab is there.

**On a desk there is no back gesture**, so `rfxmobile swing` puts it on **Escape** —
and on the first page, where there is nothing to go back to, Escape ends the program.
That is Android's own rule rather than an invention.

The page change is one line:

```basic
App.show(second)
```

`App.show` **is** the stack. `App.depth()` says how deep it is and `App.back()` takes
one off, and the back gesture — which belongs to the system, and which a phone user
will use whether your program expects it or not — is wired to that. So one call gives
you the page change and a working back at the same time.

That is why **page two has no Back button.** It would be a second way to do what the
gesture already does, and the two would have to agree for ever. `App.back()` is there
for the cases where something other than a gesture has to go back — Cancel on a sheet,
say — and this is not one of them.

Note that `second` is a **field, created once**, not `New PageTwo()` at the click.
`App.show` remembers the screen it built for an instance, so going back finds the page
as the user left it; a fresh instance would be a fresh screen every time.

## How this differs from the other two

| | the page change |
|---|---|
| desktop | `ShowIn(window)` — the window is yours, so there is nothing more to it |
| web | write into the session, then `rebuild()` |
| **phone** | `App.show` — which is also what makes the back gesture work |

Open all three side by side and the three models are clear in about a minute.
