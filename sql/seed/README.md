# Standard subject seed data

系统预置会计科目（小企业会计准则、企业会计制度）不再写入 `jinbooks_v1.0.1.sql`，改由本目录维护。

## 生成

```bash
pip install openpyxl
python tools/import_standard_subjects.py
```

输入：

- `docs/小企业会计准则科目.xlsx` → `standard_id = 1`
- `docs/企业会计制度科目.xlsx` → `standard_id = 2`

输出：

- `sql/seed/standard_subjects.sql` — 全量替换 standard_id 1/2
- `docs/subject-import-compatibility.md` — 兼容性说明

## 导入数据库

```bash
mysql -h127.0.0.1 -P3307 -ujinbooks -p jinbooks < sql/seed/standard_subjects.sql
```

脚本会：

1. `DELETE FROM standard_subject_cash_flow`
2. `DELETE FROM standard_subject WHERE standard_id IN (1,2)`
3. 更新 `standard` 表名称
4. 插入 369 条新科目

另：`balance_sheet_reclassification_rules.sql`、`balance_sheet_inventory_fixed_asset_rules.sql`、`balance_sheet_bad_debt_rules.sql` 在 `run_init_sql.py` 中于 init 之后执行，分别更新往来重分类、存货/固定资产、坏账准备模板 rules 并同步到已有账套。

费用明细表菜单（已有库增量）：`mysql ... < sql/seed/expense_detail_menu.sql` 或 `python tools/apply_expense_detail_menu.py`

总账菜单（已有库增量）：`python tools/apply_general_ledger_menu.py`（或 `mysql ... < sql/seed/general_ledger_menu.sql`）

账簿顶级菜单（迁入明细账/总账/科目余额表）：`python tools/apply_ledger_books_menu.py`（或 `mysql ... < sql/seed/ledger_books_menu.sql`）。**推荐顺序**：先 `ledger_books_menu`（会创建父菜单并迁入已存在子项），再按需跑 `general_ledger_menu`（现默认挂到账簿下）。

菜单图标语义对齐：`python tools/apply_menu_icons_align.py`（或 `mysql ... < sql/seed/menu_icons_align.sql`）。

冒烟：`python tools/smoke_general_ledger.py`（需后端 `http://localhost:2154`）

## 已有账套

导入标准科目 **不会** 自动更新 `book_subject`。已有账套需：

- 删除账套后重建，或
- 清空该账套 `book_subject` 后重新初始化科目

凭证、余额、现金流量映射等引用旧 `subject_id` 的数据需一并处理。

## 编码兼容

新模板使用点分编码（如 `2211.01`）。后端 `SubjectCodeCompat` 在工资/社保凭证逻辑中兼容旧定长码（如 `221101`）。

详见 `docs/subject-import-compatibility.md`。
