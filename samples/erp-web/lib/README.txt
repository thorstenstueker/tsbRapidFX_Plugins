Seven jars belong here. RapidXERP.rfxproj names them, and this folder is empty,
so the first build will say so.

Third party, a download away:

    flatlaf-3.7.2.jar                   look and feel
    flatlaf-intellij-themes-3.7.2.jar   the themes it offers
    sqlite-jdbc-3.53.1.0.jar            the database

FlatLaf and its themes from github.com/JFormDesigner/FlatLaf, the SQLite driver
from github.com/xerial/sqlite-jdbc. Any 3.x and any 3.5x will do.

Ours, shipped with the plugin and not free to pass on:

    server-0.1.0.jar     the HTTP server and the session registry
    session-0.1.0.jar    tsbWebSession, sign-in, SessionStatic
    mirror-0.1.0.jar     what paints a Swing form and sends the drawing
    tsbswing-1.0.jar     the table and the toolbar widgets

That is why this folder is empty rather than full: a sample that shipped them
would be shipping the product.

The quickest way to all seven at once: let the wizard make a project —
**New Project ▸ RapidFX — Web** — and copy its lib folder over this one.

There was also a tools/tsbdesignerswx.jar here, which the scripts in tools/ use.
That one is the designer itself and is not published either. The designer you
already have: it is the Design tab in the IDE, and the scripts are only a way to
run its checks from a terminal.
