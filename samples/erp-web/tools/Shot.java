import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.image.BufferedImage;
import java.io.File;
import javax.imageio.ImageIO;
import javax.swing.JComponent;
import javax.swing.JFrame;
import javax.swing.SwingUtilities;

/**
 * A picture of the web version, painted the same way the server paints it.
 *
 * <pre>
 *   sh tools/build.sh
 *   sh tools/shot.sh ../webseite/resources/tsberp-web.png
 * </pre>
 *
 * <p>This is not a photograph of a browser, and it is not meant to be. A tsbWEB server draws the
 * Swing form and sends the drawing; what a visitor sees is that drawing and nothing else. So
 * painting the same form into a PNG here produces exactly what arrives in the browser — and it
 * needs neither a browser nor a running server to do it.
 *
 * <p>The form is built inside a session scope, because that is where it expects to live: it asks
 * the session which screen is on view. Outside one, the first SessionStatic it touches would have
 * nowhere to read from.
 */
public final class Shot {

    public static void main(String[] args) {
        try {
            take(args);
        } catch (Throwable e) {
            e.printStackTrace();
            System.exit(1);
        }
        System.exit(0);
    }

    private static void take(String[] args) throws Exception {
        String target = args.length > 0 ? args[0] : "build/tsberp-web.png";
        int width = args.length > 1 ? Integer.parseInt(args[1]) : 1180;
        int height = args.length > 2 ? Integer.parseInt(args[2]) : 620;

        Database.Start();

        // A session of its own, made the way the server makes one, against the same database
        // the program signs its users in against. The picture is of what a signed-in user sees,
        // so somebody has to sign in - and doing it through the real provider means the picture
        // cannot show a screen the program would refuse to build.
        com.tsbweb.session.tsbWebJdbcAuth.ConnectionSource source = () -> Database.Connect();
        com.tsbweb.session.tsbWebJdbcAuth auth = new com.tsbweb.session.tsbWebJdbcAuth(source,
                "SELECT displayname, password FROM users WHERE login = ?",
                "SELECT role FROM userrole WHERE login = ?");

        com.tsbweb.session.tsbWebSession session =
                com.tsbweb.session.tsbWebSessionRegistry.standard(auth)
                        .create(java.time.Instant.now());

        JComponent[] root = new JComponent[1];
        SwingUtilities.invokeAndWait(() ->
                com.tsbweb.session.tsbWebSessionScope.runWith(session, () -> {
                    if (!session.signIn("anna", "secret")) {
                        throw new IllegalStateException("anna could not sign in - is the "
                                + "database the one tools/build.sh made?");
                    }
                    root[0] = (JComponent) new MainForm().GetRootPane();
                }));

        File file = new File(target);
        if (file.getParentFile() != null) file.getParentFile().mkdirs();

        SwingUtilities.invokeAndWait(() -> {
            try {
                JFrame frame = new JFrame();
                frame.setContentPane(root[0]);
                frame.pack();
                frame.setSize(width, height);
                frame.validate();

                JComponent pane = frame.getRootPane();
                BufferedImage picture = new BufferedImage(
                        pane.getWidth(), pane.getHeight(), BufferedImage.TYPE_INT_RGB);
                Graphics2D g = picture.createGraphics();
                g.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING,
                        RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
                g.setRenderingHint(RenderingHints.KEY_ANTIALIASING,
                        RenderingHints.VALUE_ANTIALIAS_ON);
                pane.paint(g);
                g.dispose();

                ImageIO.write(picture, "png", file);
                System.out.println("written: " + file.getAbsolutePath()
                        + "  " + pane.getWidth() + " × " + pane.getHeight()
                        + "  " + (file.length() / 1024) + " kB");
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        });
    }
}
