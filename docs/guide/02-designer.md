[← First steps](01-first-steps.md) · [Contents](README.md) · [Several pages →](03-pages.md)

# 2 The designer

Draw a window instead of typing it. The designer writes RapidFX, you write RapidFX, and the two
halves live in one file with a line between them.

## Opening it

Right-click a `.rfx` file ▸ **Open in Designer**, or use the **Design** tab beside **Code**.

A file only offers that if it *is* a designer form — if it carries the banner the designer writes.
A hand-typed file does not, which is deliberate: the designer would otherwise open an empty canvas
over your code and empty the file on the first *Apply*.

**To make an existing file a form**, let the designer create it: the templates in the wizard are
designer forms, so the quickest way is to start from one.

## What a form file looks like

```basic
Imports tsb.mobile          ' or javax.swing for desktop and web

Public Class CustomerList

    ' ─────────────────────────────────────────────────────────
    '  YOUR HALF. Everything above the banner is yours.
    ' ─────────────────────────────────────────────────────────

    Private Sub OnFormBuilt()
        lblTitle.setText("Customers")
        loadCustomers()
    End Sub

    Private Sub OnNewClick(source As JButton)
        ' …
    End Sub

'******************************************************************************
'                                 DO NOT EDIT
'                              TSB RAPIDFX DESIGNER
'******************************************************************************

    ' @tsbfx form class=CustomerList platform=SWING w=800 h=600 showIn=true
    Private lblTitle As JLabel = New JLabel()
    …
End Class
```

**Above the banner is yours and is never touched.** Below it is regenerated on every *Apply* —
anything you type there is gone the next time you move a button.

The handler stubs are written for you: name a button `btnNew` and the designer puts
`Private Sub OnNewClick(source As JButton)` *above* the banner, empty, once. From then on it is
yours, and renaming the button does not come back and change it.

## Anchors, which are the part worth understanding

An anchor is **not a position**. It is a rule about what happens when the window changes size, and
it is why a form drawn at 800×600 still looks right at 1400×900 and on a phone in landscape.

| | what it means |
|---|---|
| `LEFT 16` | sixteen from the left edge of the container |
| `TOP 8 @lblTitle.BOTTOM` | eight below the bottom of `lblTitle` |
| `RIGHT 16` **and** `LEFT 16` together | stretches: both distances are kept, so the width follows the window |

A widget with only `LEFT` and `TOP` keeps its size and moves. One with `LEFT` and `RIGHT` keeps its
distances and grows. That is the whole model, and it covers most of what a business form needs.

On the desktop this becomes a `SpringLayout`, which is the only Swing layout manager that can state
such a rule. On a phone it becomes the same rule in the mobile toolkit's own layout. The form does
not know which.

## Properties

The table on the right shows everything the selected widget has. Two things worth knowing:

**Only what you change is written.** A property still at its default produces no line at all, so the
generated region says what you actually did and nothing else. That is what keeps the files readable
and the diffs small.

**The id is what the code uses, not the label.** The property table shows *Tool tip text*; the
generated call is `setToolTipText`. The complete list of ids per widget type is generated from the
catalogue:

* desktop and web — the widget reference that ships with the plugin
* mobile — the same, for the mobile catalogue

Both are regenerated from the code and therefore cannot drift.

## Themes

The theme picker decides how the preview is drawn, and for the desktop it also writes the
`FlatLightLaf.setup()` line in `Main.rfx` when you save. The four shipped themes are **Light**,
**Dark**, **IntelliJ** and **Darcula**.

A theme is design time only in one sense and not in another: the designer uses it to draw, and the
program uses it to run — but a form carries no theme itself. Which is what lets the same form look
native on a light desktop and dark on a phone that is set that way.

## Two designers, one idea

| | |
|---|---|
| **tsbDesignerSWX** | desktop and web — Swing components, drawn as themselves |
| **the mobile designer** | phones — the `tsb.mobile` widgets |

They are two programs because the widget sets are two sets. What they share is the file format, the
banner, the anchor model and the rule that your half is never touched.

**The preview is not a picture of the API — it is the API.** The desktop designer's canvas paints
real Swing components through exactly the calls the generated form makes, so the preview cannot
drift from the running program. The mobile designer paints through one of the three mobile backends,
the same way. What it cannot promise is the phone's own pixel count.

## When the designer and the code disagree

They cannot, by construction: the parser that reads a form is the exact inverse of the generator
that writes one. If you edit the generated region by hand and the designer reads something else
back, that is a defect and worth reporting — but the usual cause is simpler, namely that the edit
was below the banner and got regenerated.

---

[← First steps](01-first-steps.md) · [Contents](README.md) · [Several pages →](03-pages.md)
