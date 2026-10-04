[← The designer](02-designer.md) · [Contents](README.md) · [Desktop →](04-desktop.md)

# 3 Several pages

Almost every real program has more than one screen: a sign-in, then a menu, then a list, then a
detail. How you get from one to the next is **the one question with a different answer in each of
the three worlds** — and the answers are different because the worlds are, not because nobody
agreed on one.

| | the model | the call |
|---|---|---|
| **Desktop** | One window, you swap what is in it | `New OtherForm().ShowIn(frame)` |
| **Web** | No navigation at all — the session says what to show | `session.setValue(…)` then `rebuild()` |
| **Mobile** | A stack, with a back gesture that pops it | `App.show(form)` / `App.back()` |

Read the one you need. They do not build on each other.

---

## Desktop: one window, swapped contents

A designer form is **not** a window. It is a panel with a `ShowIn(frame)` that puts itself into one:

```basic
Public Sub ShowIn(frame As JFrame)        ' written by the designer
    frame.setTitle("Sign in")
    frame.setContentPane(pnlRoot)
    frame.setSize(420, 300)
    frame.setLocationRelativeTo(Nothing)
    frame.setVisible(True)
End Sub
```

That is what makes a swap trivial: hand the **same frame** to another form.

```basic
' In SignInForm, when the password was right:
Private Sub OnSignInClick(source As JButton)
    If Not checkPassword(txtPassword.getText()) Then
        lblHint.setText("Wrong password")
        Return
    End If
    New MainMenu().ShowIn(windowOf(Me))
End Sub

' The frame a form currently sits in — Swing knows, you only have to ask.
Private Function windowOf(form As Object) As JFrame
    Return CType(SwingUtilities.getWindowAncestor(pnlRoot), JFrame)
End Function
```

The old form is dropped on the floor and garbage-collected with everything in it. If you want to go
*back* to it with its text fields still filled, keep the instance:

```basic
Module Screens
    Shared SignIn As SignInForm = New SignInForm()
    Shared Menu As MainMenu = New MainMenu()
End Module

' …then
Screens.Menu.ShowIn(frame)        ' the same instance, so the same state
```

### A second window instead of a swap

Sometimes you want both at once — a list and a detail beside it. Then make a second frame:

```basic
Dim detail As JFrame = New JFrame()
detail.setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE)   ' not EXIT_ON_CLOSE
New CustomerDetail().ShowIn(detail)
```

**`DISPOSE_ON_CLOSE` and not `EXIT_ON_CLOSE`**, which is the mistake everybody makes once: the main
window ends the program when it closes, a second window must only close itself.

### A dialog

A dialog is a window that blocks until it is answered, and a designer form goes into one exactly as
it goes into a frame — because `ShowIn` only ever touched the content pane:

```basic
Dim dialog As JDialog = New JDialog(windowOf(Me), "Choose a customer", True)
dialog.setContentPane(New CustomerPicker().GetRootPane())
dialog.setSize(500, 400)
dialog.setLocationRelativeTo(windowOf(Me))
dialog.setVisible(True)        ' returns when the dialog closes
```

`True` is the modal flag, and `setVisible` does not return until the dialog is gone — which is how
you read a result out of the form afterwards.

---

## Web: there is no navigation

This is the one that surprises people, and it is worth reading even if you do not write web
applications yet, because it explains what a session is.

**A web program has no router, no URL for each screen and no navigation call.** It has one method
that decides what this user is looking at, and it is asked again whenever something changes:

```basic
Public Class Application
    Implements tsbWebApp

    Public Function createRoot(session As tsbWebSession) As Component
        If Not session.SignedIn Then
            Return New SignInForm().GetRootPane()
        End If

        Dim screen As String = CStr(session.getValue("screen"))
        If screen = "customers" Then
            Return New CustomerList().GetRootPane()
        ElseIf screen = "invoice" Then
            Return New InvoiceForm().GetRootPane()
        Else
            Return New MainMenu().GetRootPane()
        End If
    End Function
End Class
```

To change the screen, write what you want into the session and ask for a rebuild:

```basic
Private Sub OnCustomersClick(source As JButton)
    tsbWebSession.setValue("screen", "customers")
    tsbWebSession.rebuild()
End Sub
```

`createRoot` runs again and decides afresh. The old component tree is let go; a new one is built.

### Why it works this way

A browser can reload, go back, open a second tab, or sit untouched for an hour and then click. A
navigation *stack* would have to answer all of those, and every one of its answers would be a guess
about what the user meant.

A single question — *what should this user see now?* — has none of those problems. The session is
the only state, the answer is recomputed from it, and a reload produces the same screen because it
produces the same answer.

### What the session is

One per signed-in user, holding whatever you put in it, and **unreachable from any other session**:

```basic
tsbWebSession.setValue("screen", "customers")
tsbWebSession.setValue("customerId", 4711)
Dim id As Integer = CInt(tsbWebSession.getValue("customerId"))
```

Or, typed, at the top of `Main.rfx`:

```basic
SessionStatic UserName As String
```

A `SessionStatic` reads like a normal variable and is one per user. A plain `Shared` is **one for
everybody** — right for a price list, and a data leak for anything belonging to a person. Both forms
are declared together at the top of `Main.rfx` for exactly that reason: so that the distinction is
on one screen.

### Passing something to the next screen

There is no parameter, because there is no call. Put it in the session:

```basic
Private Sub OnRowClick(row As Integer)
    tsbWebSession.setValue("customerId", idOfRow(row))
    tsbWebSession.setValue("screen", "customerDetail")
    tsbWebSession.rebuild()
End Sub
```

and read it in the form that is built next, in its constructor or its first method.

---

## Mobile: a stack, and a back gesture that is not yours

A phone shows one screen at a time and has a back gesture the user expects to work. So mobile has
the thing the other two do not: **a stack**.

```basic
App.show(New CustomerDetail())   ' push
App.back()                       ' pop; False when there is nothing to go back to
App.current()                    ' the form in front
App.depth()                      ' how deep the stack is
```

`App.back()` is also what the Android back button and the iOS edge swipe do, without you wiring
anything. That is the whole reason the stack exists.

### A form is built once and remembered

```basic
App.show(New CustomerList())     ' built
App.show(New CustomerDetail())   ' built
App.back()                       ' the list again — with its scroll position and its text
```

Showing an instance that is already on the stack brings it back as it was. If you want it rebuilt:

```basic
App.forget(theList)
App.show(theList)                ' built afresh
```

### Passing something

A form is an ordinary object, so a constructor or a property does it:

```basic
Public Class CustomerDetail
    Implements Form

    Private kundennummer As Integer

    Public Sub New(nummer As Integer)
        kundennummer = nummer
    End Sub

    Private Sub OnFormBuilt()
        lblNumber.setText(CStr(kundennummer))
    End Sub
End Class

' and from the list:
App.show(New CustomerDetail(4711))
```

### Coming back with a result

There is no `ShowDialog` that returns a value, because a phone has no modal dialogs in that sense.
The way back is a handler the detail calls before it pops:

```basic
Public Class CustomerPicker
    Implements Form

    Private wenn As SelectHandler

    Public Sub New(handler As SelectHandler)
        wenn = handler
    End Sub

    Private Sub OnOkClick(source As Button)
        wenn.onSelect(lstCustomers.getSelectedIndex())
        App.back()
    End Sub
End Class

' and from the caller:
App.show(New CustomerPicker(AddressOf OnCustomerChosen))

Private Sub OnCustomerChosen(index As Integer)
    lblCustomer.setText("Customer " & index)
End Sub
```

**The handler runs before `App.back()`, so it runs while the picker is still on screen** — and it
sets a label on the *calling* form, which is still there underneath. That is allowed and is what you
want.

### Loading while a screen is open

A phone is usually waiting for a server, and the screen must not stop. That is `Work`, and it has
its own section in [6 Mobile](06-mobile.md) — but the one thing that belongs here is what happens
when somebody leaves:

```basic
Work.text( _
    Function() As String
        Return holeKunden()
    End Function, _
    Sub(antwort As String)
        tblKunden.setRows(parse(antwort))
    End Sub)
```

If the user presses back while that is in flight, **the handler is not called**. The form is off the
stack, its screen is gone, and writing to a label on it would be writing where nobody will look. A
form that is merely *covered* by another is still answered, so that it is up to date when it comes
back into view. You do not have to do anything for either.

---

## Choosing a structure

A thing worth deciding early, because it is annoying to change later:

**Desktop and web share their forms.** The same `.rfx` draws the same screen in a `JFrame` and in a
browser, so a program meant to be both should keep the decision — *which screen now* — out of the
forms. Put it in one place:

```
src/
  Main.rfx              ' desktop entry point: a JFrame
  WebMain.rfx           ' web entry point: createRoot
  Screens.rfx           ' which screen, for both
  SignInForm.rfx        ' the forms, which know nothing about either
  MainMenu.rfx
  CustomerList.rfx
```

**Mobile cannot share them**, because its widgets are a different set — `tsb.mobile.Button` and not
`javax.swing.JButton`. What it shares is everything below the screen: your model classes, your
database code, your calculations. Keep those in files that import neither toolkit and they compile
for all three.

---

[← The designer](02-designer.md) · [Contents](README.md) · [Desktop →](04-desktop.md)
