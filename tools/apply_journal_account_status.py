import pymysql

conn = pymysql.connect(
    host="127.0.0.1",
    port=3307,
    user="financial_cloud",
    password="FinancialCloud321!",
    database="financial_cloud",
)
cur = conn.cursor()
cur.execute("SHOW COLUMNS FROM journal_account LIKE 'status'")
existing = cur.fetchall()
if not existing:
    cur.execute(
        "ALTER TABLE journal_account "
        "ADD COLUMN status tinyint NOT NULL DEFAULT 1 "
        "COMMENT '1=enabled 0=disabled' AFTER description"
    )
    conn.commit()
    print("added status column")
else:
    print("status already exists", existing)

cur.execute("SHOW COLUMNS FROM journal_account LIKE 'status'")
print("cols", cur.fetchall())
cur.execute("SELECT id, acc_name, status FROM journal_account LIMIT 10")
print("rows", cur.fetchall())
conn.close()
