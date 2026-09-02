#!/usr/bin/env python3
"""Apply menu icon semantic alignment seed and print before/after for changed rows."""
from pathlib import Path
import pymysql
from pymysql.constants import CLIENT

ROOT = Path(__file__).resolve().parents[1]
SQL = ROOT / "sql" / "seed" / "menu_icons_align.sql"

IDS = [
    "1869692874272862209",
    "2026082817000000001",
    "1903024792422047745",
    "2026082816300000001",
    "1886384516205912065",
    "1886366126259052545",
    "2026082814300000001",
    "981334321270882304",
    "1894665979168575489",
    "981334814802051072",
    "1899369820127911938",
    "1913072049310191618",
    "1902625741973843969",
    "981334679749656576",
    "1920446221202178049",
    "2026082818000000001",
    "2026082818000000011",
    "2026082818000000021",
    "2026082818000000031",
    "2026082818000000041",
    "2026082818000000051",
    "2026082818000000061",
]


def dump(cur, label: str) -> None:
    placeholders = ",".join(["%s"] * len(IDS))
    cur.execute(
        f"SELECT id, res_name, res_style, icon FROM resources "
        f"WHERE id IN ({placeholders}) ORDER BY FIELD(id, {placeholders})",
        IDS + IDS,
    )
    print(label)
    for row in cur.fetchall():
        print(f"  {row[1]}: res_style={row[2]!r} icon={row[3]!r}")


def main() -> int:
    sql = SQL.read_text(encoding="utf-8")
    for user, password in (("jinbooks", "Jinbooks321!"), ("root", "root")):
        try:
            conn = pymysql.connect(
                host="127.0.0.1",
                port=3307,
                user=user,
                password=password,
                database="jinbooks",
                charset="utf8mb4",
                client_flag=CLIENT.MULTI_STATEMENTS,
                autocommit=True,
            )
            with conn.cursor() as cur:
                dump(cur, "BEFORE")
                cur.execute(sql)
                dump(cur, "AFTER")
            conn.close()
            print(f"OK applied {SQL.name} as {user}")
            return 0
        except Exception as e:
            print(f"fail {user}: {e}")
    return 1


if __name__ == "__main__":
    raise SystemExit(main())
