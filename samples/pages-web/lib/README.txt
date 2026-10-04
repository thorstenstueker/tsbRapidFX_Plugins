Three jars belong here, and they are not in this repository.

    server-0.1.0.jar
    session-0.1.0.jar
    mirror-0.1.0.jar

They are tsbWEB, they come with the plugin, and they are not free to pass on —
which is the whole reason this folder is empty rather than full. A sample that
shipped them would be shipping the product.

Where to find them: the plugin's own lib/rfxweb folder, or the Libraries list
of any project the wizard made with "RapidFX — Web".

Copy them in, and then:

    rfxc --project PagesWeb.rfxproj --jar PagesWeb.jar
    java -jar PagesWeb.jar

The desktop sample next door needs none of this. Swing is in the JDK, which is
why it has no lib folder at all.
