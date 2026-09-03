# 02 · 基础设置

> 状态：已实现（科目、辅助核算、期初、准则模板、参数均有闭环；部分高级校验与 PRD「凭证参数/结账参数」粒度不同）

## 1. 模块定位

为凭证、账簿、报表、资产、往来维度提供标准化主数据：账套科目、辅助档案、期初余额、现金流量期初、会计准则模板及系统参数。

## 2. 典型场景

1. 建账后检查准则带出的科目树，按需增改三级明细科目。
2. 为「应收账款」启用客户辅助核算，维护客户档案。
3. 录入科目期初与现金流量期初，保证首月报表平衡。
4. 管理员维护准则级科目/报表模板（影响后续新建账套）。

## 3. 功能清单

| 能力 | 状态 | 说明 |
|------|------|------|
| 账套科目树 CRUD | **已实现** | `/api/booksubject`，`views/books/subject.vue` |
| 科目辅助核算配置 | **已实现** | 科目上挂接辅助类型 |
| 禁止删改一级标准科目 | **部分实现** | 以业务校验为准，需结合服务层规则 |
| 辅助档案 6 类 | **已实现** | 项目/客户/供应商/部门/员工/存货 |
| 期初余额录入 | **已实现** | `/api/base/init-balance`，`config/initBalance` |
| 现金流量期初 | **已实现** | `config/cash-flow-balance` |
| 科目-现金流量项映射 | **已实现** | `config/subject-cash-flow` |
| 会计准则模板（2 套） | **已实现** | 小企业会计准则、企业会计制度 |
| 准则科目 / 资产负债/利润表模板 / 取数规则 | **已实现** | `standard_*` 表族 |
| 系统参数（账期、编码规则等） | **已实现** | `config` / `ConfigSysController` |
| PRD 级「凭证断号规则开关」独立页 | **部分实现** | 断号整理在凭证模块，非独立参数中心 |
| 部门/员工组织档案 | **部分实现** | 组织在 IDM；辅助核算可建部门/员工档案 |

## 4. 页面与路由

| 菜单/页面 | 组件 | 用途 |
|-----------|------|------|
| 会计科目（账套） | `views/books/subject.vue` | 账套级科目 |
| 辅助核算 | `views/config/assistAcc/index.vue` | 六类档案 |
| 初始余额 | `views/config/initBalance/index.vue` | 科目期初 |
| 现金流量初始余额 | `views/config/cash-flow-balance.vue` | CF 期初 |
| 科目现金流量项 | `views/config/subject-cash-flow.vue` | 映射 |
| 会计准则 | `views/config/standard.vue` | 准则主数据 |
| 会计科目（准则） | `views/config/standard-subject.vue` | 模板科目 |
| 资产负债表模板 | `views/config/standard-balance-sheet.vue` | 模板行 |
| 利润表模板 | `views/config/standard-income-statement.vue` | 模板行 |
| 系统参数 | `views/config/sys.vue` | 键值配置 |
| 机构管理 | `views/config/institutions.vue` | 见系统管理 |

> 注意：菜单 URL `/books/subjects` 对应文件名为 `subject.vue`（非 `subjects.vue`）。

## 5. 数据模型

### 辅助核算 `assist_acc`

| 字段 | 说明 |
|------|------|
| `assist_type` | 1 项目 / 2 客户 / 3 供应商 / 4 部门 / 5 员工 / 6 存货 |
| `assist_code` / `assist_name` | 编码、名称 |
| `dept` / `spec` / `unit` | 部门、规格、单位等扩展 |
| `book_id` / `status` | 账套隔离、启用状态 |

前端字典：`DistData.ts` → `subjects_auxiliary`。

### 其他关键表

| 表 | 用途 |
|----|------|
| `book_subject` | 账套科目树 |
| `book_init_balance` | 期初余额 |
| `config` | 账套级系统参数 |
| `config_cash_flow_balance` | 现金流量期初 |
| `standard` | 准则主表 |
| `standard_subject` | 准则科目 |
| `standard_statement_balance_sheet` / `standard_statement_income` | 报表模板 |
| `standard_statement_rules` / `statement_rules` | 取数/重分类等规则 |
| `standard_subject_cash_flow` | 科目-现金流量模板关系 |

内置准则种子：`standard_id=1` 小企业会计准则；`standard_id=2` 企业会计制度（科目 xlsx 与 `sql/seed`）。

## 6. 核心接口

| 前缀 | 说明 |
|------|------|
| `/api/booksubject` | 账套科目树、CRUD、显示名重整 |
| `/api/base/assist-acc` | 辅助档案 CRUD |
| `/api/base/init-balance` | 期初 list/save |
| `/api/config/sys` | 系统参数 |
| `/api/config/cash-flow-balance` | 现金流量期初 |
| `/api/config/subject-cash-flow` | 科目 CF 映射 |
| `/api/standard`、`/api/standardsubject` | 准则与准则科目 |
| `/api/standard/balance-sheet`、`/api/standardstatementincome/` | 准则报表模板 |

## 7. 业务规则与约束

1. **建账复制**：新账套从准则复制科目与报表结构，用户再在账套级微调。
2. **辅助核算录入**：凭证分录选择科目后，按科目配置弹出辅助维度（见 [03-voucher.md](03-voucher.md)）；按往来单位查询余额/明细见 [11-arap.md](11-arap.md)。
3. **期初**：影响科目余额表与首期报表；与结账推进后的期间逻辑配合使用。
4. **准则变更**：主要影响**新建**账套；已建账套科目不自动全量同步（需人工维护）。

## 8. 已知缺口

- 往来核销 L3、收款/付款业务单据仍未做（L1+L2 查询与账龄已有，见 [11-arap.md](11-arap.md)）。
- PRD「结账参数/异常拦截规则」配置页未单独产品化（逻辑散落在结账校验中）。
- 无独立「往来单位分类并强绑定科目」的主数据改造（仍用辅助档案类型 客户=2 / 供应商=3）。

## 9. 证据索引

- `AssistAccController`、`BookSubjectController`、`BookInitBalanceController`
- `StandardController`、`StandardSubjectService`
- `views/config/assistAcc/index.vue`、`DistData.ts`（`subjects_auxiliary`）
- `docs/subject-import-compatibility.md`
