#!/usr/bin/env python3
import pymysql

conn = pymysql.connect(
    host="127.0.0.1",
    port=3307,
    user="root",
    password="root",
    database="jinbooks",
    charset="utf8mb4",
)
with conn.cursor() as cur:
    cur.execute("UPDATE userinfo SET password=%s WHERE username='admin'", ("{plain}maxkey",))
conn.commit()
with conn.cursor() as cur:
    cur.execute("SELECT username, password FROM userinfo WHERE username='admin'")
    print(cur.fetchone())
conn.close()
