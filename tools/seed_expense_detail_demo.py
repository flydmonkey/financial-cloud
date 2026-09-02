#!/usr/bin/env python3
"""Inspect books / expense subjects and seed sample expense vouchers for expense-detail demo."""
from __future__ import annotations

from datetime import date
from decimal import Decimal
import uuid

import pymysql
from pymysql.constants import CLIENT

HOST, PORT = "127.0.0.1", 3307
USER, PASSWORD, DB = "financial_cloud", "FinancialCloud321!", "financial_cloud"


def connect():
    return pymysql.connect(
        host=HOST,
        port=PORT,
        user=USER,
        password=PASSWORD,
        database=DB,
        charset="utf8mb4",
        client_flag=CLIENT.MULTI_STATEMENTS,
        autocommit=False,
        cursorclass=pymysql.cursors.DictCursor,
    )


def nid() -> str:
    return str(uuid.uuid4().int)[:19]


def pick_leaf(subjects: list[dict], prefix: str) -> dict | None:
    candidates = [s for s in subjects if s["code"].startswith(prefix)]
    if not candidates:
        return None
    # prefer deepest non-parent used as leaf: no other subject has this as parent of longer code under same prefix
    codes = {s["code"] for s in candidates}
    leaves = []
    for s in candidates:
        if not any(c != s["code"] and c.startswith(s["code"]) for c in codes):
            leaves.append(s)
    return leaves[0] if leaves else candidates[0]


def main() -> int:
    conn = connect()
    cur = conn.cursor()

    cur.execute("SELECT id, name, standard_id FROM book WHERE deleted='n' LIMIT 10")
    books = cur.fetchall()
    print("BOOKS:", books)
    if not books:
        print("No book found")
        return 1
    book = books[0]
    book_id = book["id"]
    print("Using book:", book_id, book["name"])

    cur.execute(
        """
        SELECT id, code, name, parent_id, level
        FROM book_subject
        WHERE book_id=%s AND deleted='n'
          AND (code LIKE '5601%%' OR code LIKE '5602%%' OR code LIKE '5603%%'
               OR code LIKE '6601%%' OR code LIKE '6602%%' OR code LIKE '6603%%'
               OR code LIKE '1001%%' OR code LIKE '1002%%')
        ORDER BY code
        """,
        (book_id,),
    )
    subjects = cur.fetchall()
    print("EXPENSE/CASH subjects:", [(s["code"], s["name"]) for s in subjects])

    # current term
    cur.execute("SHOW COLUMNS FROM config")
    cols = [r["Field"] for r in cur.fetchall()]
    key_col = "conf_key" if "conf_key" in cols else ("config_key" if "config_key" in cols else None)
    val_col = "conf_value" if "conf_value" in cols else ("config_value" if "config_value" in cols else None)
    if key_col and val_col:
        cur.execute(
            f"""
            SELECT `{val_col}` AS conf_value FROM config
            WHERE (book_id=%s OR book_id='template') AND `{key_col}`='sys.payment.term.current'
            ORDER BY CASE WHEN book_id=%s THEN 0 ELSE 1 END LIMIT 1
            """,
            (book_id, book_id),
        )
        term_row = cur.fetchone()
        term = (term_row or {}).get("conf_value") or date.today().strftime("%Y-%m")
    else:
        term = date.today().strftime("%Y-%m")
    print("CURRENT_TERM:", term)
    year = int(term[:4])

    bank = pick_leaf(subjects, "1002") or pick_leaf(subjects, "1001")
    sell = pick_leaf(subjects, "5601") or pick_leaf(subjects, "6601")
    admin = pick_leaf(subjects, "5602") or pick_leaf(subjects, "6602")
    fin = pick_leaf(subjects, "5603") or pick_leaf(subjects, "6603")
    print("PICKED:", {
        "bank": bank and (bank["code"], bank["name"]),
        "sell": sell and (sell["code"], sell["name"]),
        "admin": admin and (admin["code"], admin["name"]),
        "fin": fin and (fin["code"], fin["name"]),
    })
    if not bank or not sell or not admin:
        print("Missing required subjects")
        return 1

    # months: Jan..current month of term year (cap 6 recent months for demo richness)
    end_m = int(term[5:7])
    months = list(range(max(1, end_m - 5), end_m + 1))

    # Sample amounts per month (sell, admin, fin)
    schedule = {
        # month: (sell, admin, fin)
        m: (
            Decimal("12000") + Decimal(m) * 500,
            Decimal("8000") + Decimal(m) * 300,
            Decimal("200") + Decimal(m) * 10,
        )
        for m in months
    }

    cur.execute(
        "SELECT COALESCE(MAX(word_num),0) AS n FROM voucher WHERE book_id=%s AND deleted='n'",
        (book_id,),
    )
    word_num = int(cur.fetchone()["n"] or 0)

    created = 0
    for month, (a_sell, a_admin, a_fin) in schedule.items():
        lines = [
            ("éĺŽč´šç?, sell, a_sell),
            ("çŽĄçč´šç¨", admin, a_admin),
        ]
        if fin is not None:
            lines.append(("č´˘ĺĄč´šç¨", fin, a_fin))
        for label, subj, amt in lines:
            word_num += 1
            vid = nid()
            vdate = date(year, month, 15)
            # header
            word = f"čŽ?{word_num}"
            cur.execute(
                """
                INSERT INTO voucher (
                  id, word, word_head, word_num, book_id, company_name, receipt_num,
                  voucher_date, voucher_year, voucher_month, debit_amount, credit_amount,
                  status, sender_id, sender_name, sender_date, deleted,
                  created_by, created_date, modified_by, modified_date
                ) VALUES (
                  %s,%s,'čŽ?,%s,%s,'ćźç¤şĺŹĺ¸',0,
                  %s,%s,%s,%s,%s,
                  'completed','seed-expense','seed',NOW(),'n',
                  '1', NOW(), '1', NOW()
                )
                """,
                (
                    vid,
                    word,
                    word_num,
                    book_id,
                    vdate,
                    year,
                    month,
                    amt,
                    amt,
                ),
            )
            # debit expense
            cur.execute(
                """
                INSERT INTO voucher_item (
                  id, book_id, voucher_id, subject_id, subject_code, subject_name,
                  summary, debit_amount, credit_amount, voucher_date, subject_balance,
                  deleted, created_by, created_date, modified_by, modified_date
                ) VALUES (
                  %s,%s,%s,%s,%s,%s,
                  %s,%s,0,%s,%s,
                  'n','1',NOW(),'1',NOW()
                )
                """,
                (
                    nid(),
                    book_id,
                    vid,
                    subj["id"],
                    subj["code"],
                    subj["name"],
                    f"ćźç¤ş-{label}-{year}ĺš´{month}ć?,
                    amt,
                    vdate,
                    amt,
                ),
            )
            # credit bank
            cur.execute(
                """
                INSERT INTO voucher_item (
                  id, book_id, voucher_id, subject_id, subject_code, subject_name,
                  summary, debit_amount, credit_amount, voucher_date, subject_balance,
                  deleted, created_by, created_date, modified_by, modified_date
                ) VALUES (
                  %s,%s,%s,%s,%s,%s,
                  %s,0,%s,%s,%s,
                  'n','1',NOW(),'1',NOW()
                )
                """,
                (
                    nid(),
                    book_id,
                    vid,
                    bank["id"],
                    bank["code"],
                    bank["name"],
                    f"ćźç¤ş-{label}-äťćŹž-{year}ĺš´{month}ć?,
                    amt,
                    vdate,
                    amt,
                ),
            )
            created += 1

    conn.commit()
    print(f"CREATED_VOUCHERS={created} months={months} year={year}")

    # sanity: expense debit sums
    cur.execute(
        """
        SELECT DATE_FORMAT(i.voucher_date,'%%Y-%%m') ym,
               SUM(i.debit_amount) debit
        FROM voucher_item i
        JOIN voucher v ON v.id=i.voucher_id
        WHERE i.book_id=%s AND i.deleted='n' AND v.deleted='n' AND v.status='completed'
          AND (i.subject_code LIKE '560%%' OR i.subject_code LIKE '660%%')
          AND YEAR(i.voucher_date)=%s
        GROUP BY ym ORDER BY ym
        """,
        (book_id, year),
    )
    print("EXPENSE_DEBIT_BY_MONTH:", cur.fetchall())
    conn.close()
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
