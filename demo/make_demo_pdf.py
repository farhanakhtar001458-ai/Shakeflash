#!/usr/bin/env python3
"""
Generate a clearly-marked DEMO / SAMPLE PDF with fictional data,
for testing PDF generation only.

Every detail in this file is made up. This is NOT a real document
and must not be used as identity or address proof anywhere.

Usage:
    pip install reportlab
    python3 demo/make_demo_pdf.py
Output:
    demo/demo-certificate.pdf
"""

from pathlib import Path

from reportlab.lib.colors import HexColor, white
from reportlab.lib.pagesizes import A4
from reportlab.pdfgen import canvas

OUTPUT = Path(__file__).parent / "demo-certificate.pdf"

# 100% fictional demo data (safe to change to other made-up values)
DEMO_FIELDS = [
    ("Name", "Test User"),
    ("Address", "123 Demo Street, Sample City, 000000"),
    ("Date of Birth", "01/01/1990"),
    ("Document No.", "DEMO-0000-TEST"),
    ("Gender", "- (Demo)"),
    ("Date of Issue", "01/01/2020"),
    ("Valid Upto", "Permanent"),
]

RED = HexColor("#B00020")
NAVY = HexColor("#1F3A5F")
GREY = HexColor("#5A6570")
LIGHT_BG = HexColor("#F4F6F8")
BORDER = HexColor("#B9C4CF")
PHOTO_BG = HexColor("#E8EEF3")


def draw_watermark(c, width, height):
    """Big diagonal watermark so the file can never pass as a real document."""
    c.saveState()
    c.translate(width / 2, height / 2)
    c.rotate(28)
    c.setFillColor(RED)
    try:
        c.setFillAlpha(0.10)
    except Exception:
        pass
    c.setFont("Helvetica-Bold", 58)
    c.drawCentredString(0, 28, "SAMPLE")
    c.setFont("Helvetica-Bold", 40)
    c.drawCentredString(0, -28, "FOR TESTING ONLY")
    c.restoreState()


def build():
    width, height = A4
    c = canvas.Canvas(str(OUTPUT), pagesize=A4)
    c.setTitle("Demo Sample Certificate - fictional data, for testing only")
    c.setAuthor("Demo generator (dummy data)")

    # Page background
    c.setFillColor(LIGHT_BG)
    c.rect(0, 0, width, height, stroke=0, fill=1)

    # Top red banner
    c.setFillColor(RED)
    c.rect(0, height - 64, width, 64, stroke=0, fill=1)
    c.setFillColor(white)
    c.setFont("Helvetica-Bold", 16)
    c.drawCentredString(width / 2, height - 30, "DEMO DOCUMENT - NOT A REAL / VALID DOCUMENT")
    c.setFont("Helvetica", 10.5)
    c.drawCentredString(width / 2, height - 50,
                        "Contains fictional data. Generated only to test PDF creation.")

    # Card
    card_x, card_top, card_w, card_h = 45, 750, 505, 370
    card_y = card_top - card_h
    c.setFillColor(white)
    c.setStrokeColor(BORDER)
    c.setLineWidth(1.2)
    c.roundRect(card_x, card_y, card_w, card_h, 12, stroke=1, fill=1)

    # Title
    c.setFillColor(NAVY)
    c.setFont("Helvetica-Bold", 22)
    c.drawString(card_x + 20, card_top - 38, "Sample Certificate")
    c.setFillColor(GREY)
    c.setFont("Helvetica", 11)
    c.drawString(card_x + 20, card_top - 58, "Demo layout for PDF testing - dummy details below")

    # Divider
    c.setStrokeColor(BORDER)
    c.setLineWidth(0.8)
    c.line(card_x + 20, card_top - 74, card_x + card_w - 20, card_top - 74)

    # Photo placeholder (top-right)
    px, py, pw, ph = card_x + card_w - 100, card_top - 180, 80, 96
    c.setFillColor(PHOTO_BG)
    c.setStrokeColor(BORDER)
    c.roundRect(px, py, pw, ph, 6, stroke=1, fill=1)
    c.setFillColor(GREY)
    c.setFont("Helvetica-Bold", 11)
    c.drawCentredString(px + pw / 2, py + ph / 2 + 4, "PHOTO")
    c.setFont("Helvetica", 8)
    c.drawCentredString(px + pw / 2, py + ph / 2 - 10, "(demo)")

    # Fields
    label_x = card_x + 20
    value_x = card_x + 140
    y = card_top - 104
    for label, value in DEMO_FIELDS:
        c.setFillColor(GREY)
        c.setFont("Helvetica-Bold", 11)
        c.drawString(label_x, y, label + ":")
        c.setFillColor(HexColor("#111417"))
        c.setFont("Helvetica", 12)
        c.drawString(value_x, y, value)
        y -= 34

    # Round DEMO stamp
    c.saveState()
    try:
        c.setStrokeAlpha(0.75)
        c.setFillAlpha(0.75)
    except Exception:
        pass
    c.translate(card_x + card_w - 70, card_y + 55)
    c.rotate(-14)
    c.setStrokeColor(RED)
    c.setLineWidth(2)
    c.circle(0, 0, 34, stroke=1, fill=0)
    c.setFillColor(RED)
    c.setFont("Helvetica-Bold", 13)
    c.drawCentredString(0, 2, "DEMO")
    c.setFont("Helvetica-Bold", 7)
    c.drawCentredString(0, -9, "TEST ONLY")
    c.restoreState()

    # Notes below the card
    ny = card_y - 34
    c.setFillColor(NAVY)
    c.setFont("Helvetica-Bold", 12)
    c.drawString(45, ny, "About this file")
    notes = [
        "- This PDF was generated only to test PDF creation / editing.",
        "- Every detail in it (name, address, dates, number) is fictional.",
        "- It has no legal value and cannot be used as identity or address proof.",
        "- For corrections in a real certificate, apply through the official portal / office.",
    ]
    c.setFillColor(HexColor("#22272C"))
    c.setFont("Helvetica", 10.5)
    for line in notes:
        ny -= 18
        c.drawString(45, ny, line)

    # Footer
    c.setFillColor(GREY)
    c.setFont("Helvetica", 9)
    c.drawCentredString(width / 2, 52, "Demo file | Fictional data | Not a government document")

    # Watermark on top of everything (still readable, clearly a sample)
    draw_watermark(c, width, height)

    c.showPage()
    c.save()
    print(f"Wrote {OUTPUT}")


if __name__ == "__main__":
    build()
