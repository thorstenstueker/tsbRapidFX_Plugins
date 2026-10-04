[← Desktop](04-desktop.md) · [Contents](README.md) · [Mobile →](06-mobile.md)

# 5 Web

**A web application is a Swing application.** There is no web-only component, no annotation, no
markup and no template language. The same form shows the same screen in a browser as it does in a
`JFrame`.

The server paints every component with the look and feel you chose and sends the drawing. What the
browser shows is Swing's own rendering, pixel for pixel.

## The entry point

```basic
Imports com.tsbweb.server
Imports com.tsbweb.session

Module Main
    Sub Main()
        Dim config As tsbWebConfig = New tsbWebConfig()
        config.Port = 8099
        config.Title = "My web project"
        config.Theme = "FlatLightLaf"
        config.IdleMinutes = 30

        Dim users As tsbWebFixedAuth = tsbWebFixedAuth.builder() _
                .user("anna", "Anna Berger", "secret", java.util.Set.of("admin")) _
                .build()

        tsbWebStart.Go(config, New Application(), users)
    End Sub
End Module
```

One line and the program is a server you can double-click. `tsbWebStart.Go` sets
`java.awt.headless`, reads a `tsbweb.properties` beside the jar if there is one, installs the look
and feel and holds the main thread — **in that order**, because three of those fail silently when
they happen in the wrong one.

An operator can override the port, the title and the theme in that properties file without
recompiling anything.

## The application: one decision

```basic
Public Class Application
    Implements tsbWebApp

    Public Function title() As String
        Return "My web project"
    End Function

    Public Function createRoot(session As tsbWebSession) As Component
        If Not session.isSignedIn() Then
            Return New SignInForm().GetRootPane()
        End If
        Return New MainMenu().GetRootPane()
    End Function
End Class
```

`createRoot` runs once per session and again on every screen change, on the session thread, with the
session in reach. **A new instance per session, never a shared one** — two users editing the same
`JTextField` would watch each other type.

Everything else is a form. How you get from one to the next is
[3 Several pages](03-pages.md#web-there-is-no-navigation), and the short of it is that there is no
navigation: you write into the session and ask for a rebuild.

## Sessions

One per user, created on the first request, let go when it has been idle too long.

```basic
tsbWebSession.setValue("screen", "customers")
Dim screen As String = CStr(tsbWebSession.getValue("screen"))
tsbWebSession.hasValue("customerId")
tsbWebSession.removeValue("draft")
tsbWebSession.rebuild()
```

A session costs about **1.2 MB**, measured, so `IdleMinutes` buys memory and nothing else. When it
runs out, the session thread, the component tree and the registry entry are all released, and the
next click lands on the sign-in form.

### SessionStatic and Shared

Declared at the top of `Main.rfx`, together, so that the distinction is on one screen:

```basic
Module Main
    SessionStatic UserName As String          ' one per user
    Shared PriceListVersion As String         ' one for everybody
```

A `SessionStatic` reads like an ordinary variable and is one per signed-in user, unreachable from
any other session. A plain `Shared` is one value for every user at once — right for a catalogue or a
configuration, **a data leak for anything belonging to a person**: the second person to sign in
would read the first one's.

## Signing in

```basic
If tsbWebSession.signIn(txtUser.getText(), txtPassword.getPassword()) Then
    tsbWebSession.rebuild()
Else
    lblHint.setText("Wrong name or password")
End If
```

Where credentials come from sits behind an interface. `tsbWebFixedAuth` holds a list in the program,
which is right while you are building; `tsbWebJdbcAuth` goes against SQLite or the customer's
database, which is right afterwards.

**Passwords are hashed even in a fixed list**, because a fixed list ends up inside a jar and a jar
opens with any zip tool.

Two-factor is there when you want it: `startSignIn` answers an attempt,
`isAwaitingSecondFactor()` says whether a code is expected, `finishSignIn(code)` completes it, and
`abandonSignIn()` drops it.

```basic
tsbWebSession.signOut()
tsbWebSession.rebuild()
```

### Who may see what

```basic
If tsbWebSession.getUser().isPresent() Then
    Dim user As tsbWebUser = tsbWebSession.getUser().get()
    btnAdmin.setVisible(user.hasRole("admin"))
End If
```

Roles come from the authentication source. **Check them where the decision is made, not only where
the button is drawn** — hiding a button is a courtesy to the user, not a protection against anybody
who sends a request by hand.

## What a form may and may not do

A form is a Swing form, with two differences that follow from being on a server:

**Never block.** A handler that waits on a slow query holds that user's session thread, and the
browser shows nothing until it returns. Other users are unaffected — one thread each — but that user
is stuck.

**Nothing shared between sessions unless you mean it.** A `Shared` field, a static cache, a
singleton: all of them are one for everybody. Read the SessionStatic paragraph above twice.

File dialogs, `System.exit`, native windows and anything else that assumes a person sits at the
machine do not apply — the machine is a server in a rack.

## Running and deploying

```bash
java -jar MyWebProject.jar
```

Then `http://localhost:8099/`. There is no application server, no WAR, no servlet container: it is a
program with a port.

For a real deployment put it behind a reverse proxy that terminates TLS — nginx, Caddy, Apache — and
let that forward to the port. Nothing in the program changes.

---

[← Desktop](04-desktop.md) · [Contents](README.md) · [Mobile →](06-mobile.md)
