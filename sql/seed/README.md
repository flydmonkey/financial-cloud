# Standard subject seed data

系统预置会计科目（小企业会计准则、企业会计制度）由本目录维护，已内嵌到全量 init。

## 生成

```bash
pip install openpyxl
python tools/import_standard_subjects.py
```

输入：

- `docs/小企业会计准则科目.xlsx` → `standard_id = 1`
- `docs/企业会计制度科目.xlsx` → `standard_id = 2`

输出：

- `sql/seed/data/standard_subjects.sql` — 全量替换 standard_id 1/2
- `docs/subject-import-compatibility.md` — 兼容性说明

生成后需重新构建 init：

```bash
python tools/build_init_sql.py
python tools/run_init_sql.py
```

## 目录说明

| 子目录 | 内容 |
|--------|------|
| `data/` | 标准科目、现金流量模板 |
| `schema/` | 固定资产等 DDL 扩展 |
| `menus/` | 账簿、总账、费用明细、固定资产等菜单 |
| `rules/` | 资产负债表 rules 模板 |

菜单 seed 执行顺序（由 `build_init_sql.py` 固定）：

1. `ledger_books_menu.sql`
2. `general_ledger_menu.sql`
3. `expense_detail_menu.sql`
4. `fixed_asset_menu.sql`
5. `menu_salary_tax_and_rename_config.sql`
6. `menu_icons_align.sql`

## 已有账套

导入标准科目 **不会** 自动更新 `book_subject`。已有账套需删除账套后重建，或清空该账套 `book_subject` 后重新初始化科目。

详见 `docs/subject-import-compatibility.md`。

## 编码兼容

新模板使用点分编码（如 `2211.01`）。后端 `SubjectCodeCompat` 在工资/社保凭证逻辑中兼容旧定长码（如 `221101`）。
