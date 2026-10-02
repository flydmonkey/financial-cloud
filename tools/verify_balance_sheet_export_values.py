import json
import re
import sys
from decimal import Decimal
from pathlib import Path
import openpyxl
import pdfplumber

root = Path(sys.argv[1] if len(sys.argv) > 1 else '.e2e-run/export-verification')
for name in ('positive', 'negative'):
    data = json.loads((root / f'{name}.json').read_text(encoding='utf-8'))['items']
    workbook = openpyxl.load_workbook(root / f'{name}.xlsx', data_only=True)
    rows = list(workbook.active.values)
    checked = 0
    with pdfplumber.open(root / f'{name}.pdf') as pdf:
        text = '\n'.join(page.extract_text() or '' for page in pdf.pages)
        amounts = {int(n): (Decimal(a.replace(',', '')), Decimal(b.replace(',', '')))
                   for n, a, b in re.findall(r'(?<![\d.])(\d+)\s+(-?[\d,]+\.\d{2})\s+(-?[\d,]+\.\d{2})', text)}
        for side, offset in (('assets', 0), ('liability', 4)):
            for item in data[side]:
                row = next(r for r in rows if str(r[offset]).removeprefix('减：') == item['itemName'])
                expected = tuple(Decimal(str(item.get(field) or 0)) for field in ('currentBalance', 'initialBalance'))
                actual = tuple(Decimal(str(row[offset + col] or 0)) for col in (2, 3))
                assert actual == expected, (name, item['itemName'], 'xlsx', actual, expected)
                if item['sortIndex'] is not None:
                    assert amounts[item['sortIndex']] == expected, (name, item['itemName'], 'pdf')
                checked += 1
        for index, page in enumerate(pdf.pages):
            page.to_image(resolution=100).save(root / f'{name}-page-{index + 1}.png')
    print(f'{name}: {checked} XLSX rows and all numbered PDF rows match API; difference={data["balanceDifference"]}')
