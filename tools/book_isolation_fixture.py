"""Fake records and read-only snapshots for isolated book-isolation acceptance tests."""
import json
import sys
import base64
import io
import zipfile
import pymysql
from clear_books import HOST, PORT, USER, PASSWORD, DB

if not DB.startswith('financial_cloud_e2e_'):
    raise SystemExit('Book isolation fixtures require FC_DB_NAME=financial_cloud_e2e_<suffix>')
mode, book = sys.argv[1:3]
connection = pymysql.connect(host=HOST, port=PORT, user=USER, password=PASSWORD, database=DB,
                             cursorclass=pymysql.cursors.DictCursor)
with connection.cursor() as q:
    if mode == 'empty-tax-import':
        buffer = io.BytesIO()
        with zipfile.ZipFile(buffer, 'w') as workbook:
            workbook.writestr('[Content_Types].xml', '<Types xmlns="http://schemas.openxmlformats.org/package/2006/content-types"><Default Extension="rels" ContentType="application/vnd.openxmlformats-package.relationships+xml"/><Default Extension="xml" ContentType="application/xml"/><Override PartName="/xl/workbook.xml" ContentType="application/vnd.openxmlformats-officedocument.spreadsheetml.sheet.main+xml"/><Override PartName="/xl/worksheets/sheet1.xml" ContentType="application/vnd.openxmlformats-officedocument.spreadsheetml.worksheet+xml"/></Types>')
            workbook.writestr('_rels/.rels', '<Relationships xmlns="http://schemas.openxmlformats.org/package/2006/relationships"><Relationship Id="rId1" Type="http://schemas.openxmlformats.org/officeDocument/2006/relationships/officeDocument" Target="xl/workbook.xml"/></Relationships>')
            workbook.writestr('xl/workbook.xml', '<workbook xmlns="http://schemas.openxmlformats.org/spreadsheetml/2006/main" xmlns:r="http://schemas.openxmlformats.org/officeDocument/2006/relationships"><sheets><sheet name="Empty import" sheetId="1" r:id="rId1"/></sheets></workbook>')
            workbook.writestr('xl/_rels/workbook.xml.rels', '<Relationships xmlns="http://schemas.openxmlformats.org/package/2006/relationships"><Relationship Id="rId1" Type="http://schemas.openxmlformats.org/officeDocument/2006/relationships/worksheet" Target="worksheets/sheet1.xml"/></Relationships>')
            workbook.writestr('xl/worksheets/sheet1.xml', '<worksheet xmlns="http://schemas.openxmlformats.org/spreadsheetml/2006/main"><sheetData><row r="1"><c r="A1" t="inlineStr"><is><t>Header</t></is></c></row></sheetData></worksheet>')
        result = {'file': base64.b64encode(buffer.getvalue()).decode('ascii')}
    elif mode == 'snapshot':
        q.execute("SELECT TABLE_NAME FROM information_schema.COLUMNS WHERE TABLE_SCHEMA=%s AND COLUMN_NAME='book_id' ORDER BY TABLE_NAME", (DB,))
        tables = [r['TABLE_NAME'] for r in q.fetchall()]
        result = {}
        for table in tables:
            # identifiers are obtained from server metadata, never request input
            q.execute('SELECT * FROM `' + table + '` WHERE book_id=%s', (book,))
            result[table] = sorted(q.fetchall(), key=lambda r: str(r.get('id', r.get('config_id', ''))))
        q.execute('SELECT * FROM voucher_template WHERE related_id=%s ORDER BY id', (book,))
        result['voucher_template'] = q.fetchall()
        q.execute('SELECT * FROM voucher_template_item WHERE related_id=%s ORDER BY id', (book,))
        result['voucher_template_item'] = q.fetchall()
        q.execute('SELECT id,created_by,file_name,HEX(data_stored) AS content FROM file_storage WHERE id IN (SELECT file_id FROM expense_claim_attachment WHERE book_id=%s UNION SELECT file_id FROM voucher_attachment WHERE book_id=%s) ORDER BY id', (book,book))
        result['linked_files'] = q.fetchall()
        q.execute('SELECT * FROM config_personal_tax ORDER BY id')
        result['global_tax'] = q.fetchall()
        q.execute('SELECT * FROM roles ORDER BY id')
        result['global_roles'] = q.fetchall()
    elif mode == 'link-file':
        attachment, file_id = sys.argv[3:5]
        q.execute('UPDATE expense_claim_attachment SET file_id=%s WHERE id=%s AND book_id=%s', (file_id, attachment, book))
        assert q.rowcount == 1
        connection.commit()
        result = {'updated': True}
    elif mode == 'remove-files':
        # Deliberately simulate lost files only in the guarded isolated database.
        q.execute('DELETE FROM file_storage WHERE id IN (SELECT file_id FROM voucher_attachment WHERE book_id=%s UNION SELECT file_id FROM expense_claim_attachment WHERE book_id=%s)', (book, book))
        result = {'removed': q.rowcount}
        connection.commit()
    elif mode == 'link-entry':
        entry, account = sys.argv[3:5]
        q.execute('UPDATE journal_entry SET acc_id=%s WHERE id=%s AND book_id=%s',(account,entry,book))
        assert q.rowcount == 1
        connection.commit()
        result = {'updated': True}
    elif mode == 'seed':
        term, prefix = sys.argv[3:5]
        result = {name: prefix + '-' + name for name in ('asset','category','employee','salary','salaryTemp','taxDeduction','account','entry','assist','org','file','expense','attachment')}
        q.execute("SELECT id FROM book_subject WHERE book_id=%s AND code='1601'", (book,)); fixed_subject=q.fetchone()['id']
        q.execute("SELECT id FROM book_subject WHERE book_id=%s AND code='1602'", (book,)); depreciation_subject=q.fetchone()['id']
        q.execute('INSERT INTO asset_category(id,book_id,code,name,useful_life_months,fixed_asset_subject_id,accum_depr_subject_id) VALUES(%s,%s,%s,%s,60,%s,%s)',(result['category'],book,prefix,'Isolation fixture',fixed_subject,depreciation_subject))
        q.execute('INSERT INTO fixed_asset(id,book_id,code,name,category_id,start_use_date,entry_period,depreciation_method) VALUES(%s,%s,%s,%s,%s,%s,%s,%s)',(result['asset'],book,prefix,'Isolation fixture',result['category'],term+'-01',term,'STRAIGHT_LINE'))
        q.execute('INSERT INTO employee(id,book_id,display_name,employee_number,bank_card_no) VALUES(%s,%s,%s,%s,%s)',(result['employee'],book,'Isolation fixture',prefix,'AUDIT-ONLY'))
        q.execute('INSERT INTO employee_salary(id,book_id,employee_id,belong_date,pay_basic) VALUES(%s,%s,%s,%s,1234)',(result['salary'],book,result['employee'],term))
        result['formula'] = prefix + '-formula'
        q.execute('INSERT INTO config_salary_formula(id,book_id,rule_name,status) VALUES(%s,%s,%s,0)',(result['formula'],book,prefix + ' salary formula'))
        # A dedicated non-business tax type avoids changing normal payroll tax brackets.
        result['tax'] = prefix + '-tax'
        q.execute('INSERT INTO config_personal_tax(id,level,min_num,max_num,tax_rate,type) VALUES(%s,1,0,100,10,999)',(result['tax'],))
        q.execute('INSERT INTO employee_salary_temp(id,book_id,employee_id,belong_date,pay_basic) VALUES(%s,%s,%s,%s,1234)',(result['salaryTemp'],book,result['employee'],term))
        q.execute("INSERT INTO employee_tax_deduction(id,book_id,employee_name,id_card_type,id_card_no,year_period,years,periods,deleted) VALUES(%s,%s,'Isolation fixture','audit',%s,%s,%s,%s,'n')",(result['taxDeduction'],book,prefix,int(term.replace('-','')),int(term[:4]),int(term[5:7])))
        q.execute('INSERT INTO journal_account(id,book_id,acc_name,acc_code,balance) VALUES(%s,%s,%s,%s,100)',(result['account'],book,'Isolation fixture',prefix))
        q.execute('INSERT INTO journal_entry(id,book_id,acc_id,remark,income,trade_date) VALUES(%s,%s,%s,%s,100,%s)',(result['entry'],book,result['account'],'Isolation fixture',term+'-01'))
        q.execute('INSERT INTO assist_acc(id,book_id,assist_type,assist_code,assist_name,created_by,created_date) VALUES(%s,%s,%s,%s,%s,%s,NOW())',(result['assist'],book,'1','audit','Isolation fixture','1'))
        q.execute('INSERT INTO organizations(id,book_id,org_name) VALUES(%s,%s,%s)',(result['org'],book,'Isolation fixture'))
        q.execute('INSERT INTO file_storage(id,data_stored,created_by,file_name) VALUES(%s,%s,%s,%s)',(result['file'],b'ISOLATION-REGRESSION-ONLY','admin','isolation-fixture.txt'))
        q.execute('INSERT INTO expense_claim(id,book_id,claim_no,claimant,claim_date,expense_subject_code,fund_subject_code,amount) VALUES(%s,%s,%s,%s,%s,%s,%s,100)',(result['expense'],book,prefix,'Audit only',term+'-15','5602','1002'))
        q.execute('INSERT INTO expense_claim_attachment(id,book_id,claim_id,file_id,file_name) VALUES(%s,%s,%s,%s,%s)',(result['attachment'],book,result['expense'],result['file'],'isolation-fixture.txt'))
        for key, sql in {
            'subject':"SELECT id FROM book_subject WHERE book_id=%s AND code='1001'",
            'config':"SELECT config_id AS id FROM config WHERE book_id=%s LIMIT 1",
            'cashFlow':"SELECT id FROM config_cash_flow_balance WHERE book_id=%s AND is_edit=1 LIMIT 1",
            'insurance':"SELECT id FROM config_insurance_fund WHERE book_id=%s LIMIT 1",
            'income':"SELECT id FROM statement_income_item WHERE book_id=%s AND sort_index=1 LIMIT 1",
            'balanceSheet':"SELECT id FROM statement_balance_sheet_item WHERE book_id=%s AND sort_index=1 LIMIT 1",
            'rule':"SELECT id FROM statement_rules WHERE book_id=%s LIMIT 1",
            'template':"SELECT id FROM voucher_template WHERE related_id=%s LIMIT 1",
        }.items():
            q.execute(sql,(book,)); row=q.fetchone(); result[key]=row['id'] if row else None
        connection.commit()
    else:
        raise SystemExit('Unsupported fixture mode')
print(json.dumps(result, default=str, sort_keys=True))
connection.close()
