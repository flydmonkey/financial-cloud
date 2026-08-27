#!/usr/bin/env python3
"""Patch jinbooks_init.sql: move voucher summary menu parent to voucher menu."""
from pathlib import Path

INIT = Path(__file__).resolve().parents[1] / "sql" / "jinbooks_init.sql"
OLD = "('1891486309700673537','凭证汇总表'"
# parent_id and parent_name fields in resources tuple
OLD_FRAGMENT = (
    "('1891486309700673537','凭证汇总表','凭证汇总表','MENU',"
    "'1891486309700673537','/statement/voucher-summary','GET',NULL,'r',NULL,NULL,"
    "'menus-caiwu-pingzhenghuizongbiao','n','n','n','y','1886357455563137026','财务报表',5"
)
NEW_FRAGMENT = (
    "('1891486309700673537','凭证汇总表','凭证汇总表','MENU',"
    "'1891486309700673537','/statement/voucher-summary','GET',NULL,'r',NULL,NULL,"
    "'menus-caiwu-pingzhenghuizongbiao','n','n','n','y','1869692874272862209','凭证',3"
)

text = INIT.read_text(encoding="utf-8")
if OLD_FRAGMENT not in text:
    raise SystemExit("expected fragment not found in jinbooks_init.sql")
INIT.write_text(text.replace(OLD_FRAGMENT, NEW_FRAGMENT, 1), encoding="utf-8")
print("patched jinbooks_init.sql")
