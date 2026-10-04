# pages-mobile

Three screens on a phone: a list, a detail, and a picker that hands a choice back.

```bash
rfxmobile swing   pages-mobile        # at your desk, in a phone-sized window
rfxmobile ios     pages-mobile
rfxmobile android pages-mobile
```

## What it shows

**The stack.** `App.show` pushes, `App.back` pops, and the user's own back gesture pops it too —
the Android button and the iOS edge swipe, with no wiring. That is what a phone has and the other
two worlds do not.

**Passing something in:** a constructor. `New CustomerDetail(index)` — a form is an ordinary object
and there is no other mechanism, because none is needed.

**Getting something back:** a handler, in `CustomerPicker`. There is no `ShowDialog` returning a
value on a phone, so the caller hands in a handler and the picker calls it **before** it pops
itself — while it is still in front and the calling form is still underneath, which is exactly why
that handler may set a label there.

## Note

The forms are written by hand, so that the whole of each is readable in one file. A real project
draws them in the designer; the region below the line here is what the designer would have written.
