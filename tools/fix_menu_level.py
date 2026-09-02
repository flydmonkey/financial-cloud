#!/usr/bin/env python3
import pymysql

conn = pymysql.connect(
    host="127.0.0.1", port=3307, user="financial_cloud",
    password="FinancialCloud321!", database="financial_cloud", autocommit=True,
)
cur = conn.cursor()

print("before:")
cur.execute(
    "SELECT id, res_name, parent_id, sort_index FROM resources "
    "WHERE id IN (%s, %s, %s) ORDER BY sort_index",
    ("1869692874272862209", "1886357455563137026", "1917420357065609218"),
)
for row in cur.fetchall():
    print(row)

# 报表、结账与凭证同级（parent_id=1），排在凭证后面
cur.execute(
    "UPDATE resources SET parent_id = %s, sort_index = %s WHERE id = %s",
    ("1", 3, "1886357455563137026"),
)
cur.execute(
    "UPDATE resources SET parent_id = %s, sort_index = %s WHERE id = %s",
    ("1", 4, "1917420357065609218"),
)

print("\nafter:")
cur.execute(
    "SELECT id, res_name, parent_id, sort_index FROM resources "
    "WHERE parent_id = %s AND classify = 'MENU' AND status = '1' ORDER BY sort_index",
    ("1",),
)
for row in cur.fetchall():
    print(row)

conn.close()
print("done")
