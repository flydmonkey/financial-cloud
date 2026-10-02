"""Pure in-memory XLSX/ZIP helpers for isolation regression; no database access."""
import base64
import hashlib
import io
import json
import sys
import zipfile
from xml.sax.saxutils import escape

payload = json.load(sys.stdin)
output = io.BytesIO()
if payload['mode'] == 'xlsx':
    rows = payload['rows']
    def column(index):
        name = ''
        while index:
            index, remainder = divmod(index - 1, 26)
            name = chr(65 + remainder) + name
        return name
    cells = ''.join('<row r="%d">%s</row>' % (r, ''.join(
        '<c r="%s%d" t="inlineStr"><is><t>%s</t></is></c>' %
        (column(c), r, escape(str(value))) for c, value in enumerate(row, 1)))
        for r, row in enumerate(rows, 1))
    with zipfile.ZipFile(output, 'w') as z:
        z.writestr('[Content_Types].xml', '<Types xmlns="http://schemas.openxmlformats.org/package/2006/content-types"><Default Extension="rels" ContentType="application/vnd.openxmlformats-package.relationships+xml"/><Default Extension="xml" ContentType="application/xml"/><Override PartName="/xl/workbook.xml" ContentType="application/vnd.openxmlformats-officedocument.spreadsheetml.sheet.main+xml"/><Override PartName="/xl/worksheets/sheet1.xml" ContentType="application/vnd.openxmlformats-officedocument.spreadsheetml.worksheet+xml"/></Types>')
        z.writestr('_rels/.rels', '<Relationships xmlns="http://schemas.openxmlformats.org/package/2006/relationships"><Relationship Id="rId1" Type="http://schemas.openxmlformats.org/officeDocument/2006/relationships/officeDocument" Target="xl/workbook.xml"/></Relationships>')
        z.writestr('xl/workbook.xml', '<workbook xmlns="http://schemas.openxmlformats.org/spreadsheetml/2006/main" xmlns:r="http://schemas.openxmlformats.org/officeDocument/2006/relationships"><sheets><sheet name="Data" sheetId="1" r:id="rId1"/></sheets></workbook>')
        z.writestr('xl/_rels/workbook.xml.rels', '<Relationships xmlns="http://schemas.openxmlformats.org/package/2006/relationships"><Relationship Id="rId1" Type="http://schemas.openxmlformats.org/officeDocument/2006/relationships/worksheet" Target="worksheets/sheet1.xml"/></Relationships>')
        z.writestr('xl/worksheets/sheet1.xml', '<worksheet xmlns="http://schemas.openxmlformats.org/spreadsheetml/2006/main"><sheetData>' + cells + '</sheetData></worksheet>')
    result = {'file': base64.b64encode(output.getvalue()).decode()}
else:
    with zipfile.ZipFile(io.BytesIO(base64.b64decode(payload['file']))) as z:
        entries = {name: z.read(name) for name in z.namelist()}
    if payload['mode'] == 'inspect':
        result = {name: data.decode('utf-8', 'replace') for name, data in entries.items()}
    elif payload['mode'] == 'tamper-file':
        name = 'data/expense_claim_attachment.jsonl'
        rows = [json.loads(line) for line in entries[name].splitlines()]
        assert rows
        rows[0]['file_id'] = payload['fileId']
        entries[name] = ''.join(json.dumps(row) + '\n' for row in rows).encode()
        manifest = json.loads(entries['manifest.json'])
        for table in manifest['tables']:
            if table['name'] == 'expense_claim_attachment':
                table['sha256'] = hashlib.sha256(entries[name]).hexdigest()
        entries['manifest.json'] = json.dumps(manifest).encode()
        with zipfile.ZipFile(output, 'w') as z:
            for name, data in entries.items():
                z.writestr(name, data)
        result = {'file': base64.b64encode(output.getvalue()).decode()}
    else:
        raise SystemExit('Unsupported format mode')
print(json.dumps(result))
