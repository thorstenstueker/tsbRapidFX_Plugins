#!/usr/bin/env python3
"""One round through the whole application, without a browser.

Signs in, searches, creates a customer and an article, writes a quotation with two lines, carries
it over into an invoice and tidies up afterwards. Every step checks what is on the screen
afterwards - that is the difference between "it does not crash" and "it is right".

    java -jar build/RapidXERP.jar &
    python3 tools/probe.py

It creates data and deletes it again, but documents stay behind, so two runs in a row against the
same database fail on the counts. For a clean run, delete data/rapidxerp.db first.

The two buttons of a confirmation dialog read "Yes" and "No". They come from tsbWebOptionPane
and not from this application, so they stay German here as well.
"""
import sys
import time

sys.path.insert(0, "tools")
import driver

OPEN = []


def check(condition, what):
    if condition:
        print("  ok  ", what)
    else:
        print("  BAD ", what)
        OPEN.append(what)


def has(s, text):
    return any(text in (t or "") for t in s.texts())


def main():
    s = driver.Session()
    s.start()

    print("signing in")
    s.type_in(s.at("JTextField", 158, 150), "admin")
    s.type_in(s.at("JPasswordField", 158, 190), "secret")
    s.click("Sign in")
    time.sleep(1.0)
    check(has(s, "Signed in: Administration"), "signed in as Administration")
    check("Customers" in s.button_texts(), "the navigation is there")

    # --- customers ------------------------------------------------------------
    print("searching for customers")
    _, t = s.table()
    check(t["n"] == 8, f"eight customers in the list (were {t['n']})")

    search = s.at("JTextField", 0, 40)            # the search field of the list
    s.type_in(search, "yacht")
    s.click("Search")
    time.sleep(0.5)
    _, t = s.table()
    check(t["n"] == 1 and t["cells"][0][0] == "K-1002", "searching for 'yacht' finds K-1002")

    s.click("All")
    time.sleep(0.5)
    _, t = s.table()
    check(t["n"] == 8, "All shows all eight again")

    print("creating a customer")
    s.click("New")
    time.sleep(0.8)
    check(has(s, "New customer"), "the customer sheet for a new customer")
    fields = [id_ for id_, _ in s.every("JTextField")]
    # The order the designer laid them out in: Number, Name, Contact, Street, Postcode,
    # City, Country, E-Mail, Phone, VAT ID.
    s.type_in(fields[1], "Probe Handels GmbH")
    s.type_in(fields[2], "Herr Test")
    s.type_in(fields[3], "Teststrasse 1")
    s.type_in(fields[4], "12345")
    s.type_in(fields[5], "Testhausen")
    s.click("Save")
    time.sleep(0.8)
    _, t = s.table()
    check(t["n"] == 9, f"nine customers after creating one (there are {t['n']})")
    check(has(s, "saved"), "a message above the content")

    print("editing a customer")
    s.select_row(8)
    s.click("Edit")
    time.sleep(0.8)
    fields = [id_ for id_, _ in s.every("JTextField")]
    s.type_in(fields[8], "0800 111222")
    s.click("Save")
    time.sleep(0.8)
    _, t = s.table()
    check(t["cells"][8][4] == "0800 111222", "the phone number changed and shows in the list")

    # --- articles -------------------------------------------------------------
    print("articles")
    s.click("Articles")
    time.sleep(0.8)
    _, t = s.table()
    check(t["n"] == 12, f"twelve articles (there are {t['n']})")

    search = s.at("JTextField", 0, 40)
    s.type_in(search, "panel")
    s.click("Search")
    time.sleep(0.5)
    _, t = s.table()
    check(t["n"] == 1 and t["cells"][0][0] == "A-1005", "searching for 'panel' finds A-1005")
    s.click("All")
    time.sleep(0.5)

    print("creating an article")
    s.click("New")
    time.sleep(0.8)
    fields = [id_ for id_, _ in s.every("JTextField")]
    s.type_in(fields[1], "Sample article")
    s.type_in(fields[2], "Piece")
    s.type_in(fields[3], "12,50")
    s.click("Save")
    time.sleep(0.8)
    _, t = s.table()
    check(t["n"] == 13, f"thirteen articles (there are {t['n']})")
    check(t["cells"][12][3] == "12.50",
          "a price typed with a comma is read and shown the English way")

    # --- a quotation ----------------------------------------------------------
    print("writing a quotation")
    s.click("Quotations")
    time.sleep(0.8)
    check(has(s, "Quotations"), "the quotation list")
    s.click("New")
    time.sleep(1.0)
    check(has(s, "Quotation QU-"), "a new quotation with a number")

    combos = [id_ for id_, _ in s.every("JComboBox")]
    check(len(combos) == 2, "two combo boxes: customer and article")
    _, box = list(s.every("JComboBox"))[0]
    check(box["items"][0].startswith("—"), "the customer box asks to be chosen from first")
    s.choose(combos[0], 3)                        # K-1003 (0 is the placeholder)
    time.sleep(0.4)

    subject = s.at("JTextField", 120, 84)          # the subject line
    s.type_in(subject, "Umbau Werkhalle")

    s.choose(combos[1], 4)                        # A-1005 LED panel
    quantity = s.at("JTextField", 670, 404)
    s.type_in(quantity, "10")
    s.click("Add line")
    time.sleep(0.6)

    s.choose(combos[1], 7)                        # A-1008 Electrical fitting work
    s.type_in(quantity, "8")
    s.click("Add line")
    time.sleep(0.6)

    _, t = s.table()
    check(t["n"] == 2, f"two lines (there are {t['n']})")
    check(t["cells"][0][7] == "549.00", f"10 x 54.90 = 549.00 (it says: {t['cells'][0][7]})")
    check(t["cells"][1][7] == "632.00", f"8 x 79.00 = 632.00 (it says: {t['cells'][1][7]})")
    check(has(s, "Total: 1,405.39 EUR"), "gross total 1,405.39 EUR")

    s.click("Save")
    time.sleep(1.0)
    _, t = s.table()
    check(t["n"] == 1, "one quotation in the list")
    check(t["cells"][0][2] == "Southgate Building Centre KG", "the customer is on the quotation")
    check(t["cells"][0][6] == "1,405.39", "the gross amount in the list")
    quotation_number = t["cells"][0][0]

    # --- an invoice -----------------------------------------------------------
    print("carrying the quotation over into an invoice")
    s.select_row(0)
    s.click("Convert to invoice")
    time.sleep(1.0)
    check(has(s, "Invoice IN-"), "a new invoice out of the quotation")
    _, t = s.table()
    check(t["n"] == 2, "both lines came along")
    _, box = list(s.every("JComboBox"))[0]
    check(box["items"][box["idx"]].startswith("K-1003"),
          f"the customer came along (it says: {box['items'][box['idx']]})")
    s.click("Save")
    time.sleep(1.0)

    s.click("Invoices")
    time.sleep(0.8)
    _, t = s.table()
    check(t["n"] == 1, "one invoice in the list")
    check(t["cells"][0][6] == "1,405.39", "the invoice total matches the quotation")

    s.click("Quotations")
    time.sleep(0.8)
    _, t = s.table()
    check(t["cells"][0][0] == quotation_number, "the quotation is still there")

    # --- a delivery note, and the way out of the sheet itself -------------------
    print("carrying the quotation over into a delivery note")
    s.select_row(0)
    s.click("Convert to delivery note")
    time.sleep(1.0)
    check(has(s, "Delivery note DN-"), "a new delivery note out of the quotation")
    s.click("Save")
    time.sleep(1.0)

    s.click("Delivery notes")
    time.sleep(0.8)
    _, t = s.table()
    check(t["n"] == 1, "one delivery note in the list")
    check(len(t["cells"][0][1]) == 10 and t["cells"][0][1][4] == "-",
          f"the date written as ISO (it says: {t['cells'][0][1]})")

    print("making an invoice from inside the delivery note")
    s.select_row(0)
    s.click("Open")
    time.sleep(1.0)
    check("Convert to invoice" in s.button_texts(), "the carry-over button is there")
    s.click("Convert to invoice")
    time.sleep(1.2)
    check(has(s, "Invoice IN-"), "the delivery note became an invoice")
    s.click("Save")
    time.sleep(1.0)
    _, t = s.table()
    check(t["n"] == 2, f"two invoices (there are {t['n']})")

    print("an invoice has nothing left to become")
    s.select_row(0)
    s.click("Open")
    time.sleep(1.0)
    check("Convert to invoice" not in s.button_texts(),
          "no carry-over button on an invoice")
    s.click("Back")
    time.sleep(0.8)

    # --- the rules ----------------------------------------------------------------
    print("a customer with documents stays")
    s.click("Customers")
    time.sleep(0.8)
    search = s.at("JTextField", 0, 40)
    s.type_in(search, "Southgate")
    s.click("Search")
    time.sleep(0.6)
    s.select_row(0)
    s.click("Delete")
    time.sleep(0.8)
    layer = [n for n in s.layers if n != "form"]
    if layer:
        s.click("Yes", layer[0])
        time.sleep(0.8)
    check(has(s, "cannot be deleted"), "the deletion is refused, with a reason")
    _, t = s.table()
    check(t["n"] == 1, "the customer is still there")

    print("tidying up")
    s.click("All")
    time.sleep(0.6)
    s.select_row(8)
    s.click("Delete")
    time.sleep(0.8)
    layer = [n for n in s.layers if n != "form"]
    check(bool(layer), "the confirmation appears as a dialog")
    if layer:
        s.click("Yes", layer[0])
        time.sleep(0.8)
        _, t = s.table()
        check(t["n"] == 8, "the test customer is gone")

    # --- two users at once ---------------------------------------------------------
    #
    # The real test for SessionStatic. Both are working on a quotation, each with a draft of
    # their own - were the variables Shared, the second would see the first one's lines.
    print("two users at once")
    b = driver.Session()
    b.start()
    b.type_in(b.at("JTextField", 158, 150), "anna")
    b.type_in(b.at("JPasswordField", 158, 190), "secret")
    b.click("Sign in")
    time.sleep(1.0)
    check(has(b, "Signed in: Anna Berger"), "the second session is Anna Berger")
    check(has(s, "Signed in: Administration"), "the first session is still Administration")

    s.click("Quotations")
    time.sleep(0.8)
    s.click("New")
    time.sleep(1.0)
    combos = [id_ for id_, _ in s.every("JComboBox")]
    s.choose(combos[0], 1)
    s.choose(combos[1], 0)
    s.type_in(s.at("JTextField", 670, 404), "3")
    s.click("Add line")
    time.sleep(0.8)
    _, t = s.table()
    check(t["n"] == 1, "Administration has one line in the draft")

    b.click("Quotations")
    time.sleep(0.8)
    b.click("New")
    time.sleep(1.0)
    _, t = b.table()
    check(t["n"] == 0, "Anna's draft is empty - she cannot see Administration's line")

    combos = [id_ for id_, _ in b.every("JComboBox")]
    b.choose(combos[0], 2)
    b.choose(combos[1], 1)
    b.type_in(b.at("JTextField", 670, 404), "5")
    b.click("Add line")
    time.sleep(0.8)
    _, t = b.table()
    check(t["n"] == 1, "Anna now has a line of her own")

    _, t = s.table()
    check(t["n"] == 1 and t["cells"][0][3] == "3",
          "Administration's draft is untouched, still quantity 3")

    b.click("Back")
    time.sleep(0.8)

    print("signing out")
    s.click("Back")
    time.sleep(0.8)
    s.click("Sign out")
    time.sleep(1.2)
    check("Sign in" in s.button_texts(), "back on the sign-in screen")
    check(has(b, "Signed in: Anna Berger"), "Anna is untouched by it")

    print()
    if OPEN:
        print(f"{len(OPEN)} checks failed:")
        for f in OPEN:
            print("  -", f)
        sys.exit(1)
    print("All the way through.")


if __name__ == "__main__":
    main()
