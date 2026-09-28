# Design: 账套备份与恢复

## 1. 备份包格式

```
book-backup-<账套名>-<yyyyMMdd-HHmmss>.zip
├── manifest.json          # 格式版本、账套元信息、表清单、行数、SHA-256
└── data/<table>.jsonl     # 每行一个 JSON 对象（列名→值），按表名固定清单
```

`manifest.json`：

```json
{
  "format": "financial-cloud-book-backup",
  "formatVersion": 1,
  "appVersion": "1.1.0-ga",
  "exportedAt": "2026-09-29T02:10:00",
  "book": { "name": "...", "companyName": "...", "creditCode": "...", "industry": "...", "taxpayerType": "...", "standardId": 1, "startPeriod": "2026-01" },
  "tables": [ { "name": "voucher", "rows": 1523, "sha256": "..." } ]
}
```

- JSONL 而非单 JSON 数组：逐行流式读写，大账套不撑内存。
- 值编码：数值/布尔保持 JSON 原生类型；`LocalDate/LocalDateTime` 统一 ISO 字符串；`null` 显式保留。
- 每表 sha256 针对 JSONL 字节计算，恢复时逐表核对。

## 2. 备份范围（表规格清单是核心设计物）

**包含**（按恢复顺序排列；`B`=按 book_id 过滤，`V`=按 voucher id 集合过滤）：

| 组 | 表 | 过滤 | FK 重映射边 |
|----|----|------|-------------|
| 账套壳 | book | 单行 | 生成新 book_id |
| 科目 | book_subject | B | parent_id→book_subject.id |
| 期初 | book_init_balance | B | parent_id→book_init_balance.id |
| 辅助 | assist_acc | B | — |
| 字号 | voucher_word | B | — |
| 凭证 | voucher | B | —（audit/sender/manager 人员 ID 置空） |
| 凭证分录 | voucher_item | B | voucher_id→voucher.id；subject_id→book_subject.id |
| 分录辅助 | voucher_auxiliary | B | voucher_id→voucher.id；voucher_item_id→voucher_item.id；item_id→assist_acc.id |
| 分录现金流 | voucher_item_cash_flow | B | voucher_item_id→voucher_item.id |
| 审批 | approval_record | V | voucher_id→voucher.id；approver_id 置空 |
| 日记账 | journal_account / journal_entry / journal_summary | B | entry.acc_id→journal_account.id；entry.subject_id→book_subject.id；entry.voucher_id→voucher.id；account.subject_id→book_subject.id |
| 固定资产 | asset_category / fixed_asset / fixed_asset_work / fixed_asset_accrual / fixed_asset_depr / fixed_asset_change / fixed_asset_change_item | B | asset.category_id→asset_category.id；各 *_subject_id→book_subject.id；asset.purchase/dispose_voucher_id→voucher.id；work/depr/change.asset_id→fixed_asset.id；depr.accrual_id→fixed_asset_accrual.id；change_item.change_id→fixed_asset_change.id |
| 薪资 | employee / employee_salary / employee_salary_summary / employee_salary_temp / employee_tax_deduction / config_salary_formula / config_insurance_fund | B | salary.employee_id→employee.id；salary/summary.accrual_voucher_id、salary_voucher_id→voucher.id；employee.department_id、manager_id 置空（部门/组织不纳入 v1） |
| 结账 | settlement / settlement_carryforward | B | carryforward.voucher_id→voucher.id；voucher_template_id 置空（模板全局） |
| 报表 | statement_rules / statement_subject_balance / statement_balance_sheet(+_item) / statement_income(+_item) / statement_cash_flow / config_cash_flow_balance | B | item 表按头表 id 重映射 |
| 配置 | config | B | — |

**明确排除**：userinfo / permission* / role_member / organizations（用户权限与组织，实例级）；history_* / session_list / scheduled_lock（日志锁）；socials_* / config_email_senders / config_sms_provider / config_login_policy / config_password_policy / captcha 相关（实例级配置）；standard* / config_personal_tax（准则与个税税率模板，目标实例自带）；voucher_template(+_item)（全局模板）；file_storage（附件文件体，v1 凭证附件能力未建）；customer（全局往来单位档案——assist_acc 已内联名称，恢复不依赖）。

排除原则：**只带账套业务数据，不带实例身份与安全配置**。每条排除在代码注释中可追溯。

## 3. 导出流程

1. 校验：当前用户对 bookId 有授权；账套存在。
2. 按表规格逐表 `SELECT * FROM <table> WHERE book_id = ?`（approval_record 用 voucher id IN 子查询）；写 JSONL 临时流，累计行数与 sha256。
3. 写 manifest，打 ZIP 一次性响应（复用 MonthlyBooksPack 的整包响应模式；大账套 v2 再改异步）。
4. 记 history_event（操作人、账套、表数、总行数、包大小）。

## 4. 恢复流程（克隆式）

1. 解析 ZIP：校验 manifest（format/formatVersion=1、表齐全、逐表行数与 sha256 一致）；任一不符即拒绝，不产生任何写入。
2. 建新账套壳：复用账套创建逻辑（按 manifest.book 元信息 + standardId 初始化），得到 newBookId。**不复制原 book 行的 id**。
3. 单事务灌数：按 §2 顺序逐表插入。每张表先分配新主键（`idMap<table>[oldId] = newId`），再按 FK 边改写字段后 insert。人员/部门类外键按 §2 置空。
4. 完成后记 history_event；返回新账套 id 与名称，前端提示并刷新账套列表。
5. 任一步失败 → 整体回滚（含新账套壳），错误信息指明表与行号。

## 5. 关键取舍

- **整包内存组装**：与 MonthlyBooksPackService 一致，单账套数据量（数万行）可接受；超限场景（百万级分录）留给异步导出 v2。
- **声明式 FK 边表**：`BackupTableSpec(table, scopeColumn, fkEdges[])` 集中枚举，新增账套级表时必须登记，单测断言"schema 中所有含 book_id 的表都在规格清单或有显式排除注解"，防止未来加表漏备。
- **JSONL 自描述**：不依赖 MySQL 版本语法，跨版本恢复只需 formatVersion 协商。

## 6. API

- `POST /api/book/backup/export` → ZIP 下载（当前账套上下文）
- `POST /api/book/backup/restore`（multipart 上传 ZIP）→ `{ bookId, name, tables, rows }`
