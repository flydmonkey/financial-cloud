# 数据库初始化

> **以后所有任务 / 新环境 / 空库重建：一律以本目录的 `financial_cloud_init.sql` 为唯一全量初始化入口。**  
> 不要只跑零散 `sql/patches/*.sql` 当「新库装库」；patch 用于已有库升级，或通过 `tools/build_init_sql.py` 汇入 init。

菜单与权限终态（截至 2026-10-01）已写入 init，包括：

- 仪表盘分组（首页 + 代账工作台）
- 业务菜单图标对齐与三处缺图标修复（费用报销 / 增值税申报表 / 银行对账）
- 系统设置仅保留「用户管理」「角色管理」；「日志审计」整组已移除

对应设计：`docs/superpowers/specs/2026-10-01-menu-trim-sys-audit-icons-design.md`、`docs/superpowers/specs/2026-09-30-menu-dashboard-nest-icons-design.md`。

## 全量初始化（新环境）

```bash
# 一键清空并重建（推荐）
python tools/run_init_sql.py

# 或手动
mysql -uroot -p < sql/financial_cloud_init.sql
```

`financial_cloud_init.sql` 由 `tools/build_init_sql.py` 生成（亦可经校验后手改末尾幂等对齐段），包含：

- **62+ 张表**完整结构（含固定资产扩展表）
- **系统种子数据**（菜单、权限、角色、准则、科目、凭证模板等）
- **菜单 seed**（账簿、总账、费用明细、固定资产、代账工作台、仪表盘嵌套、系统设置精简、图标对齐等）
- **报表 rules**（资产负债表重分类、存货/固定资产、坏账准备）
- **不含**账套、凭证、员工、日记账、余额等业务测试数据

默认管理员：`admin` / `changeme`（首次启动后由 `PlainPasswordMigrator` 自动转为 bcrypt）

## 以后改菜单 / Schema 时怎么做

1. **已有库升级**：新增 `sql/patches/YYYY-MM-DD-*.sql`（幂等），在目标库执行。
2. **保证新库也能装全**：把该 patch 注册进 `tools/build_init_sql.py` 的 `MENU_SEED_SQL` / `SCHEMA_EXTENSION_SQL` / `DATA_PATCH_SQL`（按依赖排序）。
3. **重新生成并校验 init**：

```bash
python tools/build_init_sql.py
python tools/run_init_sql.py   # 或在临时库验证菜单树
```

4. **提交**：同时提交 patch、`build_init_sql.py`（若有注册变更）、以及更新后的 `financial_cloud_init.sql`。

未注册进 `build_init_sql.py` 的 patch **不会**进入下次生成的 init，新环境会缺菜单或缺表。

## 重新生成 init SQL

```bash
# 1. 从 xlsx 更新标准科目
python tools/import_standard_subjects.py

# 2. （可选）从 Java 枚举更新现金流量模板
python tools/gen_cash_flow_seed.py

# 3. 生成全量 init SQL
python tools/build_init_sql.py

# 4. 应用到本地库
python tools/run_init_sql.py
```

## 目录结构

```
sql/
├── financial_cloud_init.sql       # 全量初始化入口（新环境只用这个）
├── financial_cloud_v1.0.1.sql     # schema 源 dump（参考）
├── patches/                       # build 读取的历史补丁 / 已有库升级
│   ├── 2026-09-30-menu-dashboard-nest-icons.sql
│   ├── 2026-10-01-menu-trim-sys-audit-icons.sql
│   └── …
└── seed/
    ├── schema/                    # DDL 扩展（固定资产等）
    ├── menus/                     # 菜单 seed（顺序敏感）
    ├── data/                      # standard_subjects、config_cash_flow
    └── rules/                     # balance_sheet_* rules
```

## 本地 MySQL

```bash
docker compose up -d
```

默认 `127.0.0.1:3307`，库名 `financial_cloud`，MySQL **9.7 LTS**（见 `docker-compose.yml`），用户/密码见 compose 配置。
