# pages-desktop

Three screens in one window — plus a second window and a modal dialog, because the desktop can do
both and a phone cannot.

Open the folder in IntelliJ and press the green arrow, or:

```bash
rfxc -cp lib/flatlaf-3.7.2.jar -o build src/*.rfx
java -cp build:lib/flatlaf-3.7.2.jar Main
```

## What it shows

**Swapping one window.** A form is not a window — it is a panel with a `ShowIn(frame)` that fills
one. So changing screens is handing the **same frame** to another form, and that is the whole of
desktop navigation.

**A second window**, for a detail beside the list rather than instead of it. With
`DISPOSE_ON_CLOSE` and not `EXIT_ON_CLOSE`, which is the mistake everybody makes once: the main
window ends the program when it closes, a second window must only close itself.

**A modal dialog**, in `CustomerPicker`. `setVisible(True)` does not return until the dialog closes,
which is how the answer is read afterwards — the one thing a phone cannot do.

## Note

The forms are written by hand, so that the whole of each is readable. A real project draws them in
the designer; `ShowIn` and `GetRootPane` are what the designer would have generated.
