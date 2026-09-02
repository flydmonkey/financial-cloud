# 数据库初始化

## 全量初始化（新环境）

```bash
# 一键清空并重建（推荐）
python tools/run_init_sql.py

# 或手动
mysql -uroot -p < sql/financial_cloud_init.sql
```

`financial_cloud_init.sql` 由 `tools/build_init_sql.py` 生成，包含：

- **62+ 张表**完整结构（含固定资产扩展表）
- **系统种子数据**（菜单、权限、角色、准则、科目、凭证模板等）
- **菜单 seed**（账簿、总账、费用明细、固定资产、图标对齐等）
- **报表 rules**（资产负债表重分类、存货/固定资产、坏账准备）
- **不含**账套、凭证、员工、日记账、余额等业务测试数据

默认管理员：`admin` / `changeme`（首次启动后由 `PlainPasswordMigrator` 自动转为 bcrypt）

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
├── financial_cloud_init.sql       # 生成物，勿手改
├── financial_cloud_v1.0.1.sql     # schema 源 dump（参考）
├── patches/                       # build 读取的历史补丁
│   ├── cleanup-dead-menus.sql
│   ├── assist-acc-config.sql
│   └── menu-restructure.sql
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
