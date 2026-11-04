# Demo PDF (fiction – testing only)

`demo-certificate.pdf` is a **watermarked sample document** created to test
PDF generation. All details inside it — name, address, date of birth,
document number, dates — are **completely made up**
("Test User", "123 Demo Street", document no. `DEMO-0000-TEST`, etc.).

It is **not** a real certificate, has no legal value, and must never be
used or submitted as identity/address proof.

## Regenerate

```bash
pip install reportlab
python3 demo/make_demo_pdf.py
```

This recreates `demo/demo-certificate.pdf`. Edit the `DEMO_FIELDS` list in
`make_demo_pdf.py` if you want different *fictional* test values.
