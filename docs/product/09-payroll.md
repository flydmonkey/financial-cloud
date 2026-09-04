# 09 · 薪资与个税

> 状态：部分实现（员工、算薪、专项附加、社保比例、计提/发放凭证、导出可用；已补齐小企业发薪最小闭环：按人统一基数、算薪引导、银行代发盘）

## 1. 模块定位

覆盖员工档案、当月工资计算、个税专项附加扣除、社保公积金比例、工资明细/汇总，以及生成工资相关会计凭证。面向小企业「算薪 → 进账 → 代发」最小闭环，而非完整 HR/个税申报系统。

## 2. 典型场景

1. 维护员工与社保公积金缴费比例、个税税率档；可按人设置统一缴费基数。
2. 录入专项附加扣除，执行当月工资计算。
3. 生成工资计提凭证与发放凭证，导出工资表与银行代发盘。

## 2.1 小企业发薪最小闭环（SMB）

推荐路径（账期取当前账套期间）：

1. **选月预览**：`/hr/calc-salary` → 生成工资预览；核对缴费基数（账套默认/自定义）与社保公积金。
2. **调整推送**：编辑应增应扣后「推送工资明细」写入确认明细。
3. **生成凭证**：在工资明细生成计提/发放凭证。
4. **导出代发盘**：从确认明细导出银行批量支付文件（缺银行卡号则阻断）。

### 工资凭证模板

- 普通员工（含实习、返聘）：`jt_gz` 计提、`zf_gz` 发放。
- 兼职员工：`fp_lwf` 收票、`zf_lwf` 发放。
- 同一账期勿与期末结转中的 `jt_gz` / `zf_gz` 对同一批工资重复生成凭证。

### 统一缴费基数范围

- **本期支持**：账套默认基数，或员工级**统一**自定义基数（`payBaseRule` + `payBaseNumber`）。
- **本期不启用**：员工表上分险种自定义规则字段不参与算薪。
- 启用自定义基数但未填正数时，算薪拒绝并提示员工姓名。

### 社保公积金默认值（全国最低比例）

新建账套或账套尚无配置时，系统自动写入：

| 项 | 默认 |
|----|------|
| 缴费基数 | 2500 |
| 养老 | 单位 16% / 个人 8% |
| 医疗（含生育） | 单位 6% / 个人 2%；生育分项 0 |
| 失业 | 单位 0.3% / 个人 0.2% |
| 工伤 | 单位 0.2% / 个人 0% |
| 公积金 | 单位 5% / 个人 5% |

各地实际政策不同，可在「社保公积金」页按参保地调整。

### 代发盘列

| 列 | 说明 |
|----|------|
| 工号 | 可选 |
| 姓名 | 必填来源 |
| 开户行 | 有则导出 |
| 账号 | 必填；缺失则整单阻断 |
| 实发金额 | `totalAmount` |
| 所属月 | 所属期间 |

## 3. 功能清单

| 能力 | 状态 | 说明 |
|------|------|------|
| 员工档案 CRUD | **已实现** | `employee.vue` |
| 当月工资计算 | **部分实现** | `calc-salary.vue` + 临时表重算；含四步引导 |
| 薪资结构 / 公式配置 | **部分实现** | `config/formula.vue`、`ConfigSalaryFormula` |
| 个税税率配置 | **已实现** | `config/tax.vue` |
| 专项附加扣除 | **部分实现** | `employee-tax-deduction.vue`、导入 |
| 社保公积金比例 | **部分实现** | `insurance-fund.vue`、`ConfigInsuranceFund` |
| 按人统一缴费基数 | **已实现** | 校验 + 预览可见 |
| 工资明细 / 总览 | **已实现** | `salary-detail`、`salary-summary` |
| 生成计提/发放凭证 | **已实现** | `EmployeeSalaryService.generateVoucher`（类型区分） |
| 删除工资凭证 | **已实现** | `delete-voucher` |
| 工资表 Excel 导出 | **已实现** | `exportSalary` |
| 银行代发盘导出 | **已实现** | `export-payment`（确认明细） |
| 工资凭证规则 | **已实现** | 入口复用「凭证模板」，筛选当前账套工资/劳务相关模板 |
| 完整「自定义薪资结构」产品化 | **部分实现** | 有公式与明细项，体验相对专业财务系统仍简化 |
| 简易费用报销 | **未实现** | PRD V1.2 项 |

## 4. 页面与路由

| 页面 | 路由 | 组件 |
|------|------|------|
| 员工管理 | `/hr/employee` | `employee.vue` |
| 税务个人扣除 | `/hr/employee-tax-deduction` | `employee-tax-deduction.vue` |
| 工资明细 | `/hr/salary-detail` | `salary-detail.vue` |
| 工资总览 | `/hr/salary-summary` | `salary-summary.vue` |
| 当月工资计算 | `/hr/calc-salary` | `calc-salary.vue` |
| 工资凭证规则 | `/hr/salary-voucher-rules` | 复用 `voucher/voucher-template.vue`（薪资筛选） |
| 社保公积金 | `/config/insurance-fund` | `insurance-fund.vue` |
| 个人税率 | `/config/tax` | `tax.vue` |
| 薪资计算公式 | `/config/formula` | `formula.vue` |

## 5. 数据模型

| 表 | 用途 |
|----|------|
| `employee` | 员工档案 |
| `employee_salary` | 工资明细 |
| `employee_salary_summary` | 汇总 |
| `employee_salary_temp` | 计算临时数据 |
| `employee_tax_deduction` | 专项附加扣除 |
| `config_personal_tax` | 税率 |
| `config_salary_formula` | 公式 |
| `config_insurance_fund` | 社保公积金 |

## 6. 核心接口

| 前缀 | 说明 |
|------|------|
| `/api/salary/employee` | 员工 |
| `/api/employee/salary` | 明细、汇总、导出、生成凭证 |
| `/api/employee/salary/export-payment` | 银行代发盘 |
| `/api/employee/salary/count` | 按所属月统计确认明细数 |
| `/api/employee/salary-summary` | 汇总保存 |
| `/api/salary/detail` | 临时表创建、重算、提交 |
| `/api/employee/taxdeduction` | 专项附加 |
| `/api/config/tax`、`/salary/formula`、`/insurance_fund` | 配置 |

## 7. 业务规则与约束

1. 工资凭证进入统一凭证模块后，遵循审核/过账/账期规则。
2. 个税与社保依赖配置表与扣除档案；税率变更影响后续计算期。
3. 与期末「工资计提类」结转模板可能并存，需避免重复计提（业务操作约定）。
4. 代发盘仅基于已推送的 `employee_salary`，不使用预览临时表。
5. **工资凭证规则**菜单复用「凭证模板」编辑器：展示当前账套中编码/名称与工资、劳务相关的模板（如 `jt_gz`、`zf_gz`、`fp_lwf`、`zf_lwf`），不再维护独立规则表。

## 8. 已知缺口 / Non-goals

- 票据管理、费用报销（PRD V1.2）未做。
- **工资条 / 员工自助**未做（本期明确不做）。
- **综合所得累计预扣法**纠偏另案；当前仍为按期应税工资套税率。
- **分险种自定义基数**未启用。
- 自然人电子税务局申报表未产品化。
- 部分 IAM 菜单资源与 HR 页面权限粒度可再梳理。

## 9. 证据索引

- `controller/hr/*`、`service` 下 Employee* / salary 相关
- `SalaryContributionBaseRules`、`SalaryPaymentExportRules`
- `views/hr/*`、`views/config/tax.vue`、`insurance-fund.vue`、`formula.vue`
- E2E：`e2e/hr.spec.ts`
- OpenSpec：`openspec/changes/payroll-smb-min-loop`
