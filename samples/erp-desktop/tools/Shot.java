import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.image.BufferedImage;
import java.io.File;
import javax.imageio.ImageIO;
import javax.swing.JFrame;
import javax.swing.JPanel;
import javax.swing.JRootPane;
import javax.swing.SwingUtilities;

/**
 * A picture of the running program, taken without a screen and without anybody looking.
 *
 * <pre>
 *   sh tools/shot.sh                       # build/tsberp-desktop.png
 *   sh tools/shot.sh ../../web/webseite/resources/tsberp-desktop.png
 * </pre>
 *
 * <p>The window is built exactly as {@code SelfTest} builds it — the real forms, the real
 * database, the real look and feel — and then painted into an image instead of onto a screen.
 * That is not a trick: {@code paint(Graphics)} is what Swing does to a window anyway, and the
 * one thing it needs is a laid-out component tree, which {@code pack()} provides whether the
 * window was ever shown or not.
 *
 * <p>So the picture cannot drift away from the program. A screenshot taken by hand is true on the
 * day it was taken; this one is true on the day it was run, and running it is one command.
 *
 * <p>It is a screenshot of the customer list, because that is the screen the program opens on and
 * the one that shows the most: the header with the signed-in user, the navigation, a table with
 * real rows, and the search box.
 */
public final class Shot {

    public static void main(String[] args) {
        try {
            take(args);
        } catch (Throwable e) {
            e.printStackTrace();
            // Explicitly, and in both directions: AWT's event thread is not a daemon, so a
            // program that only throws goes on running with nothing left to do. The first
            // version of this file hung for ten minutes that way.
            System.exit(1);
        }
        System.exit(0);
    }

    private static void take(String[] args) throws Exception {
        String target = args.length > 0 ? args[0] : "build/tsberp-desktop.png";

        // The program opens at 1180 × 760, and at that size a third of the picture is the empty
        // half of a table. Shorter, because a screenshot is not a window: it has to say what the
        // program looks like, not how much room it was given.
        int width = args.length > 1 ? Integer.parseInt(args[1]) : 1180;
        int height = args.length > 2 ? Integer.parseInt(args[2]) : 620;

        Database.Start();
        User who = UserFile.SignIn("anna", "secret");

        // Dark, because the site it goes onto is dark. A screenshot in the light theme on a dark
        // page looks like a hole cut into it.
        SwingUtilities.invokeAndWait(() -> Look.Switch(Look.DARK));

        JRootPane[] root = new JRootPane[1];
        SwingUtilities.invokeAndWait(() -> {
            JFrame frame = new JFrame();
            MainWindow shell = new MainWindow(frame, who);
            shell.ShowCustomers();

            // ShowIn() would do these three lines and then setVisible(true). The window is
            // deliberately never shown: a frame that is laid out is all a picture needs, and a
            // window flashing up on somebody's screen is not part of taking one.
            frame.setContentPane(shell.GetRootPane());

            // pack() before setSize, and both before validate. The order is not taste:
            // Container.validate() returns at once while the window is not displayable, and a
            // window becomes displayable in pack(). Without pack the layout never runs, every
            // component stays at 0 × 0, and the picture comes out empty — which is exactly what
            // the first two runs of this file produced.
            frame.pack();
            frame.setSize(width, height);
            frame.validate();

            // The root pane rather than the content pane: the menu bar hangs on the window, so
            // the content pane alone is the program with its menus cut off.
            root[0] = frame.getRootPane();
        });

        File file = new File(target);
        if (file.getParentFile() != null) file.getParentFile().mkdirs();

        SwingUtilities.invokeAndWait(() -> {
            try {
                JRootPane pane = root[0];
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
