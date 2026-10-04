import com.tsbdesignerswx.codegen.DesignerMarker;
import com.tsbdesignerswx.codegen.FormParser;
import com.tsbdesignerswx.codegen.FormWriter;
import com.tsbdesignerswx.model.DesignForm;
import com.tsbdesignerswx.model.SwingComponent;
import com.tsbdesignerswx.model.components.ComponentCatalog;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

/**
 * Checks that the designer can open every form again and save it unchanged.
 *
 * <p>That is the one promise this application makes to the designer, and the only one testable
 * without starting it: <b>read, write, compare</b>. If the same file comes out, then the generated
 * half is exactly what {@code FormParser} expects — and a save in the designer changes no line
 * that nobody changed.
 *
 * <p>The expensive failure this rules out: a form that opens in the designer as an empty canvas
 * because one {@code ' @tsbswx} line could not be read. The first save would write the empty
 * canvas back, and the screen would be gone.
 *
 * <pre>
 *   javac -cp tools/tsbdesignerswx.jar -d tools/build tools/CheckForms.java
 *   java  -cp tools/tsbdesignerswx.jar:tools/build CheckForms src
 * </pre>
 */
public final class CheckForms {

    public static void main(String[] args) throws IOException {
        Path folder = Path.of(args.length > 0 ? args[0] : "src");
        ComponentCatalog catalog = new ComponentCatalog();

        int checked = 0;
        int bad = 0;

        List<Path> files;
        try (var stream = Files.list(folder)) {
            files = stream.filter(p -> p.toString().endsWith(".rfx")).sorted().toList();
        }

        for (Path file : files) {
            String source = Files.readString(file, StandardCharsets.UTF_8);
            if (!DesignerMarker.isDesignerFile(source)) continue;
            checked++;

            FormParser parser = new FormParser(catalog);
            DesignForm form = parser.parseFile(source);
            if (form == null) {
                System.out.println("FAILED " + file + ": cannot be read");
                bad++;
                continue;
            }

            List<SwingComponent> parts = form.getAllComponents();
            String again = new FormWriter().write(form);
            boolean same = again.equals(source);

            System.out.printf("%-24s %2d components  %4dx%-4d  %s%s%n",
                    form.getClassName(),
                    parts.size(),
                    form.getScreenConfig().getWidth(),
                    form.getScreenConfig().getHeight(),
                    form.getScreenConfig().getTarget(),
                    same ? "  unchanged" : "  DIFFERS");

            for (String warning : parser.getWarnings()) {
                System.out.println("    warning: " + warning);
            }
            if (!same) {
                bad++;
                showFirstDifference(source, again);
            }
        }

        System.out.println();
        System.out.println(checked + " forms checked, " + bad + " with differences.");
        if (bad > 0) System.exit(1);
    }

    /** The first line where the original and the rewrite differ. */
    private static void showFirstDifference(String a, String b) {
        String[] left = a.split("\n", -1);
        String[] right = b.split("\n", -1);
        for (int i = 0; i < Math.max(left.length, right.length); i++) {
            String l = i < left.length ? left[i] : "<end>";
            String r = i < right.length ? right[i] : "<end>";
            if (!l.equals(r)) {
                System.out.println("    line " + (i + 1));
                System.out.println("      was:  " + l);
                System.out.println("      now:  " + r);
                return;
            }
        }
    }
}
