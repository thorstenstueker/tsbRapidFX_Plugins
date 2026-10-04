import java.awt.Font;
import java.lang.reflect.Field;
import java.util.ArrayList;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTextField;
import javax.swing.SwingUtilities;
import javax.swing.UIManager;

/**
 * One round through the whole program, without anybody clicking.
 *
 * <p>The web project has {@code probe.py} for this: it speaks the browser's own protocol, so it
 * can work the application from outside. A desktop program has no such seam — there is no wire
 * between the buttons and the code, which is rather the point of a desktop program. So this test
 * goes in through the front door instead: it builds the real forms, reaches their real buttons
 * and calls {@code doClick()} on them, which runs exactly the listener a mouse would.
 *
 * <p>The fields it reaches for are private, and that is not an oversight in the forms. They are
 * private because nothing in the program should touch them; a test is not part of the program.
 *
 * <p>What is deliberately left out: the two confirmation dialogs. {@code JOptionPane} stops and
 * waits for an answer, and a test that has to answer its own dialogs is testing the JDK rather
 * than this program. The rule underneath — a customer who appears on a document is not deleted —
 * is checked directly, where it is actually written.
 *
 * <pre>
 *   sh tools/selftest.sh
 * </pre>
 */
public final class SelfTest {

    private static final ArrayList<String> FAILED = new ArrayList<>();

    public static void main(String[] args) throws Exception {
        System.out.println("database: " + Database.FilePath());
        Database.Start();

        dataLayer();
        userInterface();

        System.out.println();
        if (!FAILED.isEmpty()) {
            System.out.println(FAILED.size() + " checks failed:");
            for (String f : FAILED) System.out.println("  - " + f);
            System.exit(1);
        }
        System.out.println("All the way through.");
        System.exit(0);
    }

    // =====================================================================================
    //  Part one: everything below the screen
    // =====================================================================================

    private static void dataLayer() {
        System.out.println("the sample data");
        check(CustomerFile.All().size() == 8, "eight customers");
        check(ArticleFile.All().size() == 12, "twelve articles");

        System.out.println("passwords");
        String hash = Passwords.Hash("secret");
        check(!hash.contains("secret"), "the password is not in the hash");
        check(Passwords.Matches("secret", hash), "the right password fits");
        check(!Passwords.Matches("Secret", hash), "a wrong one does not");
        check(!Passwords.Hash("secret").equals(hash), "two hashes of one password differ (the salt)");
        check(!Passwords.Matches("secret", "nonsense"), "a broken row is a refusal, not a crash");

        System.out.println("signing in");
        User who = UserFile.SignIn("admin", "secret");
        check(who != null && who.getDisplayName().equals("Administration"), "admin / secret");
        check(who != null && who.IsAdmin(), "and is an administrator");
        check(UserFile.SignIn("admin", "wrong") == null, "a wrong password is refused");
        check(UserFile.SignIn("nobody", "secret") == null, "an unknown login is refused");

        System.out.println("searching");
        check(CustomerFile.Search("yacht").size() == 1, "'yacht' finds one customer");
        check(CustomerFile.Search("YACHT").size() == 1, "and so does 'YACHT'");
        check(CustomerFile.Search("ulm").size() == 1, "a town is searched too");
        check(ArticleFile.Search("panel").size() == 1, "'panel' finds one article");

        System.out.println("converting text");
        check(Util.ToNumber("12,50") == 12.5, "a comma is a decimal point");
        check(Util.ToNumber("12.50") == 12.5, "and so is a full stop");
        check(Util.ToNumber("rubbish") == 0, "nonsense is zero, not an error");
        check(Util.DateShown("2026-09-08").equals("2026-09-08"), "the date on screen");
        check(Util.DateToIso("2026-09-08").equals("2026-09-08"), "and back again");
        check(Util.DateToIso("08.09.2026").equals("2026-09-08"), "the old spelling still reads");

        System.out.println("a document adds up");
        Document doc = new Document();
        doc.setKind(Document.QUOTATION);
        doc.setNumber(DocumentFile.NextNumber(Document.QUOTATION));
        doc.setCustomerId(customerId("K-1003"));
        doc.setCustomerName("Southgate Building Centre KG");
        doc.setSubject("Umbau Werkhalle");
        doc.AddLine(DocumentLine.FromArticle(article("A-1005"), 10));
        doc.AddLine(DocumentLine.FromArticle(article("A-1008"), 8));

        check(doc.LineCount() == 2, "two lines");
        check(near(doc.Net(), 1181.0), "net 1181,00  (10 x 54,90 + 8 x 79,00)");
        check(near(doc.Tax(), 224.39), "VAT 224,39");
        check(near(doc.Gross(), 1405.39), "gross 1405,39");

        System.out.println("the price is frozen onto the line");
        Article a = article("A-1005");
        double was = a.getPrice();
        a.setPrice(99.0);
        ArticleFile.Save(a);
        check(near(doc.Gross(), 1405.39), "raising the article does not change the document");
        a.setPrice(was);
        ArticleFile.Save(a);

        System.out.println("saving and reading back");
        DocumentFile.Save(doc);
        check(doc.getId() > 0, "the document got an id");
        Document again = DocumentFile.Load(doc.getId());
        check(again != null && again.LineCount() == 2, "both lines came back");
        check(near(again.Gross(), 1405.39), "and the total is the same");
        check(again.getNumber().startsWith("QU-"), "a quotation number: " + again.getNumber());

        System.out.println("saving twice does not double the lines");
        DocumentFile.Save(again);
        check(DocumentFile.Load(again.getId()).LineCount() == 2, "still two lines");

        System.out.println("carrying over");
        Document invoice = DocumentFile.CarryOver(again, Document.INVOICE);
        check(invoice.getNumber().startsWith("IN-"), "an invoice number: " + invoice.getNumber());
        check(invoice.getCustomerId() == again.getCustomerId(), "the customer came along");
        check(invoice.LineCount() == 2, "and both lines");
        check(invoice.getSourceId() == again.getId(), "the origin is remembered");
        check(near(invoice.Gross(), again.Gross()), "the total matches");
        DocumentFile.Save(invoice);

        invoice.LineAt(0).setQuantity(99);
        check(near(again.Gross(), 1405.39), "changing the invoice leaves the quotation alone");

        System.out.println("the two rules");
        String no = CustomerFile.Delete(again.getCustomerId());
        check(no.contains("cannot be deleted"), "a customer with documents stays");
        String stays = ArticleFile.Delete(article("A-1005").getId());
        check(stays.contains("stays"), "an article on a line stays");

        System.out.println("tidying up");
        DocumentFile.Delete(invoice.getId());
        DocumentFile.Delete(again.getId());
        check(DocumentFile.Search(Document.QUOTATION, "").isEmpty(), "no quotations left");
        check(DocumentFile.Search(Document.INVOICE, "").isEmpty(), "no invoices left");
        check(CustomerFile.Delete(customerId("K-1003")).isEmpty(), "and now the customer may go");
        check(CustomerFile.All().size() == 7, "seven customers left");
    }

    // =====================================================================================
    //  Part two: the screens
    // =====================================================================================

    private static void userInterface() throws Exception {
        System.out.println();
        System.out.println("building the window");

        JFrame frame = new JFrame();
        User who = UserFile.SignIn("anna", "secret");
        MainWindow[] made = new MainWindow[1];

        // Everything Swing has to happen on the event dispatch thread, in a test as much as in
        // the program. invokeAndWait runs it there and waits, so what follows sees a finished
        // screen rather than half of one.
        onEdt(() -> {
            made[0] = new MainWindow(frame, who);
            made[0].ShowCustomers();
        });
        MainWindow shell = made[0];

        check(label(shell, "lblSignedIn").startsWith("Signed in: Anna Berger"),
                "the header names the user");
        check(panel(shell, "pnlContent").getComponentCount() == 1,
                "one screen in the content area");

        System.out.println("the customer list");
        CustomerListForm list = (CustomerListForm) field(shell, "customerList");
        check(list != null, "the list was kept, not thrown away");
        check(rows(list, "tblCustomers") == 7, "seven customers in the table");

        onEdt(() -> {
            text(list, "txtSearch").setText("yacht");
            button(list, "btnSearch").doClick();
        });
        check(rows(list, "tblCustomers") == 1, "searching narrows it to one");
        check(cell(list, "tblCustomers", 0, 0).equals("K-1002"), "and it is K-1002");

        System.out.println("the search text survives a change of screen");
        onEdt(() -> {
            shell.ShowArticles();
            shell.ShowCustomers();
        });
        check(field(shell, "customerList") == list, "it is the same form object");
        check(text(list, "txtSearch").getText().equals("yacht"), "still 'yacht' in the box");
        check(rows(list, "tblCustomers") == 1, "and still one row");

        onEdt(() -> button(list, "btnAll").doClick());
        check(rows(list, "tblCustomers") == 7, "All brings them back");

        System.out.println("a new customer");
        CustomerSheetForm sheet = onEdt(() -> new CustomerSheetForm(shell, 0));
        check(label(sheet, "lblTitle").equals("New customer"), "the sheet says it is a new one");
        check(text(sheet, "txtNumber").getText().startsWith("K-"), "with a number of its own");

        onEdt(() -> {
            text(sheet, "txtName").setText("Probe Handels GmbH");
            text(sheet, "txtCity").setText("Testhausen");
            button(sheet, "btnSave").doClick();
        });
        check(CustomerFile.All().size() == 8, "eight customers after saving");
        check(label(shell, "lblMessage").contains("saved"), "a message above the content");
        check(rows(list, "tblCustomers") == 8, "and the list behind it was refreshed");

        System.out.println("saving without a name is refused");
        CustomerSheetForm empty = onEdt(() -> new CustomerSheetForm(shell, 0));
        onEdt(() -> button(empty, "btnSave").doClick());
        check(label(empty, "lblHint").contains("name is required"), "and it says why");
        check(CustomerFile.All().size() == 8, "nothing was written");

        System.out.println("an article with a comma in its price");
        ArticleSheetForm art = onEdt(() -> new ArticleSheetForm(shell, 0));
        onEdt(() -> {
            text(art, "txtName").setText("Probeartikel");
            text(art, "txtUnit").setText("Piece");
            text(art, "txtPrice").setText("12,50");
            button(art, "btnSave").doClick();
        });
        check(ArticleFile.All().size() == 13, "thirteen articles");
        check(near(article("Probeartikel").getPrice(), 12.5), "12,50 was read as twelve fifty");

        System.out.println("writing a quotation");
        Document draft = new Document();
        draft.setKind(Document.QUOTATION);
        draft.setNumber(DocumentFile.NextNumber(Document.QUOTATION));
        DocumentSheetForm ds = onEdt(() -> new DocumentSheetForm(shell, draft));

        JComboBox<?> customers = combo(ds, "cmbCustomer");
        JComboBox<?> articles = combo(ds, "cmbArticle");
        check(customers.getItemCount() == 9, "eight customers and one placeholder");
        check(customers.getSelectedIndex() == 0, "the placeholder is what is selected");
        check(customers.getItemAt(0).toString().startsWith("—"),
                "and it asks to be chosen from");

        onEdt(() -> {
            customers.setSelectedIndex(1);
            text(ds, "txtSubject").setText("Umbau Werkhalle");
            articles.setSelectedIndex(indexOf(articles, "A-1005"));
            text(ds, "txtQuantity").setText("10");
            button(ds, "btnAdd").doClick();
        });
        check(rows(ds, "tblLines") == 1, "one line");
        check(text(ds, "txtQuantity").getText().equals("1"),
                "the quantity box is ready for the next one");

        onEdt(() -> {
            articles.setSelectedIndex(indexOf(articles, "A-1008"));
            text(ds, "txtQuantity").setText("8");
            button(ds, "btnAdd").doClick();
        });
        check(rows(ds, "tblLines") == 2, "two lines");
        check(cell(ds, "tblLines", 0, 7).equals(Util.Money(549.0)), "10 x 54,90 = 549,00");
        check(cell(ds, "tblLines", 1, 7).equals(Util.Money(632.0)), "8 x 79,00 = 632,00");
        check(label(ds, "lblGross").equals("Total: " + Util.MoneyWithEuro(1405.39)),
                "the total underneath: " + label(ds, "lblGross"));

        System.out.println("and it is still only a draft");
        check(DocumentFile.Search(Document.QUOTATION, "").isEmpty(),
                "nothing in the database yet");

        onEdt(() -> button(ds, "btnSave").doClick());
        check(DocumentFile.Search(Document.QUOTATION, "").size() == 1, "and now there is");
        check(label(shell, "lblMessage").contains("saved"), "with a message");

        DocumentListForm docs = (DocumentListForm) field(shell, "documentList");
        check(docs != null, "the document list was built on the way back");
        check(rows(docs, "tblDocuments") == 1, "one quotation in it");
        check(cell(docs, "tblDocuments", 0, 2).equals("Miller Electrical Systems GmbH"),
                "the customer is on it");
        String shown = cell(docs, "tblDocuments", 0, 1);
        check(shown.length() == 10 && shown.charAt(4) == '-',
                "the date as ISO, which is what the screen shows: " + shown);
        check(cell(docs, "tblDocuments", 0, 6).equals(Util.Money(1405.39)),
                "and the gross amount");

        System.out.println("carrying it over from the list");
        onEdt(() -> {
            table(docs, "tblDocuments").Table().setRowSelectionInterval(0, 0);
            button(docs, "btnToInvoice").doClick();
        });
        check(shell.Kind().equals(Document.INVOICE), "the shell followed to invoices");
        check(label(shell, "lblMessage").contains("carried over"), "and said so");
        check(panel(shell, "pnlContent").getComponentCount() == 1, "an invoice sheet is on screen");

        System.out.println("the carried-over invoice");
        Document quotation = DocumentFile.Load(
                ((Document) DocumentFile.Search(Document.QUOTATION, "").get(0)).getId());
        Document carried = DocumentFile.CarryOver(quotation, Document.INVOICE);
        DocumentSheetForm inv = onEdt(() -> new DocumentSheetForm(shell, carried));

        check(label(inv, "lblTitle").startsWith("Invoice IN-"), "an invoice number in the title");
        check(label(inv, "lblTitle").endsWith("(new)"), "marked as not yet saved");
        check(rows(inv, "tblLines") == 2, "both lines came along");
        check(combo(inv, "cmbCustomer").getSelectedIndex() == 1, "and so did the customer");
        check(!button(inv, "btnConvert").isVisible(), "an invoice has nothing left to become");

        onEdt(() -> button(inv, "btnSave").doClick());
        check(DocumentFile.Search(Document.INVOICE, "").size() == 1, "the invoice is saved");
        check(docs.Kind().equals(Document.INVOICE), "the list switched to invoices");
        check(rows(docs, "tblDocuments") == 1, "and shows the one");
        check(DocumentFile.Search(Document.QUOTATION, "").size() == 1,
                "the quotation is still there");

        System.out.println("the navigation");
        onEdt(() -> shell.ShowDocuments(Document.QUOTATION));
        check(rows(docs, "tblDocuments") == 1, "one quotation");
        check(text(docs, "txtSearch").getText().isEmpty(),
                "a change of kind clears the search box");
        check(font(shell, "btnQuotations").isBold(), "the Quotations button is bold");
        check(!font(shell, "btnCustomers").isBold(), "and Customers is not");

        onEdt(shell::ShowCustomers);
        check(font(shell, "btnCustomers").isBold(), "and now the other way round");
        check(!font(shell, "btnQuotations").isBold(), "and Quotations is not");

        System.out.println("the look can be changed while it runs");
        String before = UIManager.getLookAndFeel().getName();
        onEdt(() -> Look.Switch(Look.DARK));
        check(!UIManager.getLookAndFeel().getName().equals(before), "the look and feel changed");
        check("Yes".equals(UIManager.getString("OptionPane.yesButtonText")),
                "and the dialog words survived it");
        onEdt(() -> Look.Switch(Look.LIGHT));

        onEdt(frame::dispose);
    }

    // =====================================================================================
    //  The small helpers
    // =====================================================================================

    private static void check(boolean ok, String what) {
        System.out.println((ok ? "  ok   " : "  BAD  ") + what);
        if (!ok) FAILED.add(what);
    }

    private static boolean near(double a, double b) {
        return Math.abs(a - b) < 0.005;
    }

    private static void onEdt(Runnable r) throws Exception {
        SwingUtilities.invokeAndWait(r);
    }

    /** The same, for the calls that make something and hand it back. */
    private static <T> T onEdt(java.util.function.Supplier<T> s) throws Exception {
        Object[] out = new Object[1];
        SwingUtilities.invokeAndWait(() -> out[0] = s.get());
        @SuppressWarnings("unchecked")
        T value = (T) out[0];
        return value;
    }

    private static Object field(Object owner, String name) {
        try {
            Field f = owner.getClass().getDeclaredField(name);
            f.setAccessible(true);
            return f.get(owner);
        } catch (ReflectiveOperationException e) {
            throw new IllegalStateException("no field " + name + " on " + owner.getClass(), e);
        }
    }

    private static JButton button(Object form, String name) {
        return (JButton) field(form, name);
    }

    private static JTextField text(Object form, String name) {
        return (JTextField) field(form, name);
    }

    private static JComboBox<?> combo(Object form, String name) {
        return (JComboBox<?>) field(form, name);
    }

    private static JPanel panel(Object form, String name) {
        return (JPanel) field(form, name);
    }

    private static String label(Object form, String name) {
        return ((JLabel) field(form, name)).getText();
    }

    private static Font font(Object form, String name) {
        return button(form, name).getFont();
    }

    private static tsbSmartTable table(Object form, String name) {
        return (tsbSmartTable) field(form, name);
    }

    private static int rows(Object form, String name) {
        return table(form, name).RowCount();
    }

    private static String cell(Object form, String name, int row, int column) {
        return table(form, name).Value(row, column);
    }

    private static int indexOf(JComboBox<?> box, String start) {
        for (int i = 0; i < box.getItemCount(); i++) {
            if (box.getItemAt(i).toString().startsWith(start)) return i;
        }
        throw new IllegalStateException("no entry beginning with " + start);
    }

    private static Article article(String search) {
        return (Article) ArticleFile.Search(search).get(0);
    }

    private static int customerId(String number) {
        return ((Customer) CustomerFile.Search(number).get(0)).getId();
    }
}
