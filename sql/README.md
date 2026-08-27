# 数据库初始化

## 全量初始化（推荐新环境）

```bash
# 一键清空并重建（推荐）
python tools/run_init_sql.py

# 或手动
mysql -uroot -p < sql/jinbooks_init.sql
```

`jinbooks_init.sql` 包含：

- **62 张表**的完整结构（含 `jinbooks_v1.0.1-add.sql` 补丁）
- **系统初始化数据**（菜单、权限、角色、准则、科目、凭证模板等）
- **不含**账套、凭证、员工、日记账、余额等业务测试数据

默认管理员：`admin` / `maxkey`（首次启动后由 `PlainPasswordMigrator` 自动转为 bcrypt）

## 重新生成

```bash
# 1. 从 xlsx 更新标准科目
python tools/import_standard_subjects.py

# 2. 生成全量 init SQL
python tools/build_init_sql.py
```

## 文件说明

| 文件 | 用途 |
|------|------|
| `jinbooks_init.sql` | 全量初始化（结构 + 种子数据） |
| `jinbooks_v1.0.1.sql` | 历史完整 dump（已剥离科目种子，仅作参考） |
| `seed/standard_subjects.sql` | 标准科目增量更新（已内嵌到 init） |
| `jinbooks_v1.1.0-cleanup-dead-menus.sql` | 菜单清理（已内嵌到 init） |
| `jinbooks_v1.1.1-add-assist-acc-config.sql` | 辅助核算配置（已内嵌到 init） |

## 已有库升级

若数据库已存在，请使用增量脚本，**不要**直接执行 `jinbooks_init.sql`（会 DROP 表）。
