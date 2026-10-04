Four jars belong here. RapidXERPDesktop.rfxproj names them, and this folder is
empty, so the first build will say so.

    flatlaf-3.7.2.jar                   look and feel
    flatlaf-intellij-themes-3.7.2.jar   the themes it offers
    sqlite-jdbc-3.53.1.0.jar            the database
    tsbswing-1.0.jar                    the table and the toolbar widgets

Three of them are third party and are a download away: FlatLaf and its themes
from github.com/JFormDesigner/FlatLaf, the SQLite driver from
github.com/xerial/sqlite-jdbc. Any 3.x and any 3.5x will do; the version in the
project file is simply the one this was built against.

tsbswing is ours and comes with the plugin, in its lib folder. It is not free to
pass on, which is why this folder is empty rather than full: a sample that
shipped it would be shipping the product.

The quickest way to all four at once: let the wizard make a project —
**New Project ▸ RapidFX — Desktop** — and copy its lib folder over this one.

There was also a tools/tsbdesignerswx.jar here, which the two scripts in tools/
use. That one is the designer itself and is not published either. The designer
you already have: it is the Design tab in the IDE, and the scripts are only a
way to run its checks from a terminal.
