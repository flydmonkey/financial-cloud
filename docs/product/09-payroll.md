# 09 · 薪资与个税

> 状态：部分实现（员工、算薪、专项附加、社保比例、计提/发放凭证、导出可用；PRD 将本模块规划在 V1.2，代码已提前落地）

## 1. 模块定位

覆盖员工档案、当月工资计算、个税专项附加扣除、社保公积金比例、工资明细/汇总，以及生成工资相关会计凭证。

## 2. 典型场景

1. 维护员工与社保公积金缴费比例、个税税率档。
2. 录入专项附加扣除，执行当月工资计算。
3. 生成工资计提凭证与发放凭证，导出工资表。

## 3. 功能清单

| 能力 | 状态 | 说明 |
|------|------|------|
| 员工档案 CRUD | **已实现** | `employee.vue` |
| 当月工资计算 | **部分实现** | `calc-salary.vue` + 临时表重算 |
| 薪资结构 / 公式配置 | **部分实现** | `config/formula.vue`、`ConfigSalaryFormula` |
| 个税税率配置 | **已实现** | `config/tax.vue` |
| 专项附加扣除 | **部分实现** | `employee-tax-deduction.vue`、导入 |
| 社保公积金比例 | **部分实现** | `insurance-fund.vue`、`ConfigInsuranceFund` |
| 工资明细 / 总览 | **已实现** | `salary-detail`、`salary-summary` |
| 生成计提/发放凭证 | **已实现** | `EmployeeSalaryService.generateVoucher`（类型区分） |
| 删除工资凭证 | **已实现** | `delete-voucher` |
| 工资表 Excel 导出 | **已实现** | `exportSalary` |
| 工资凭证规则 | **部分实现** | `salary-voucher-rules` 相关页 |
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
| 工资凭证规则 | `/hr/salary-voucher-rules` | `salary-voucher-rules/` |
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
| `/api/employee/salary-summary` | 汇总保存 |
| `/api/salary/detail` | 临时表创建、重算、提交 |
| `/api/employee/taxdeduction` | 专项附加 |
| `/api/config/tax`、`/salary/formula`、`/insurance_fund` | 配置 |

## 7. 业务规则与约束

1. 工资凭证进入统一凭证模块后，遵循审核/过账/账期规则。
2. 个税与社保依赖配置表与扣除档案；税率变更影响后续计算期。
3. 与期末「工资计提类」结转模板可能并存，需避免重复计提（业务操作约定）。

## 8. 已知缺口

- 票据管理、费用报销（PRD V1.2）未做。
- 薪资结构的可视化配置与合规报表（个税申报表）未产品化。
- 部分 IAM 菜单资源与 HR 页面权限粒度可再梳理。

## 9. 证据索引

- `controller/hr/*`、`service` 下 Employee* / salary 相关
- `views/hr/*`、`views/config/tax.vue`、`insurance-fund.vue`、`formula.vue`
- E2E：`e2e/hr.spec.ts`
