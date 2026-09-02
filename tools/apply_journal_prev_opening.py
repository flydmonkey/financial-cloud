"""Apply journal_account.prev_opening_balance if missing."""
import pymysql

conn = pymysql.connect(
    host="127.0.0.1",
    port=3307,
    user="financial_cloud",
    password="FinancialCloud321!",
    database="financial_cloud",
    charset="utf8mb4",
)
cur = conn.cursor()
cur.execute(
    "SELECT COUNT(*) FROM information_schema.COLUMNS "
    "WHERE TABLE_SCHEMA=%s AND TABLE_NAME='journal_account' AND COLUMN_NAME='prev_opening_balance'",
    ("financial_cloud",),
)
exists = cur.fetchone()[0]
if exists:
    print("column already exists")
else:
    cur.execute(
        "ALTER TABLE journal_account ADD COLUMN prev_opening_balance decimal(10,2) "
        "DEFAULT NULL COMMENT 'prev opening snapshot' AFTER opening_balance"
    )
    conn.commit()
    print("ALTER OK")
cur.execute("SHOW COLUMNS FROM journal_account LIKE 'prev_opening_balance'")
print(cur.fetchall())
conn.close()
