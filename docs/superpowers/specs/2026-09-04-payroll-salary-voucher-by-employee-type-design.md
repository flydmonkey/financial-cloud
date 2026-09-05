# 工资明细按员工类型生成凭证（普通员工走 jt_gz / zf_gz）

**状态：** 待实现  
**日期：** 2026-09-04  
**范围：** 工资明细「生成凭证」与算薪引导第 3 步；`EmployeeSalaryService.generateVoucher`

## 背景

小企业发薪最小闭环要求：算薪 → 推送明细 → **生成凭证** → 代发盘。

现状问题：

1. `EmployeeSalaryService.generateVoucher` 无论员工类型一律使用劳务模板 `fp_lwf`（收票）/ `zf_lwf`（发放）。
2. 工资明细 UI 仅对 `PARTTIME` 显示生成按钮；普通员工（`NORMAL` 等）在明细页无法生成凭证。
3. 账套标准库普遍已有 `jt_gz`（计提工资），期末处理也按汇总走 `jt_gz`；多数账套**没有** `fp_lwf` / `zf_lwf`。
4. 算薪页第 3 步「去生成凭证」仅跳转工资明细，对普通员工等于空转。

回归结果：普通员工推送后调用 `voucherType=3` 得到 `凭证模板[zf_lwf]未设置！`。

## 目标

1. **普通员工路径可用**：`NORMAL` / `INTERN` / `RETIREMENT` 在工资明细可生成计提、发放凭证，模板分别为 `jt_gz`、`zf_gz`。
2. **兼职路径不变**：`PARTTIME` 仍用 `fp_lwf` / `zf_lwf`。
3. 按**该行工资明细金额**填充模板分录（非期末汇总）。
4. 模板缺失时返回明确错误信息（含模板编码）。
5. 算薪第 3 步跳转后，普通员工可见可用的计提/发放操作。

## 非目标

- 不新建独立「工资凭证规则」数据表（继续复用凭证模板）。
- 不在本轮自动补种 `fp_lwf` / `zf_lwf` / `zf_gz` 到已有账套（缺模板时报错；`jt_gz` 依赖既有建账默认）。
- 不做与期末汇总计提的硬互斥锁（文档提示同一账期勿双路径重复计提）。
- 不改个税累计预扣、工资条、分险种基数。

## 方案

### 模板选择

| 员工类型 | voucherType | 含义（UI） | 模板编码 | 回写字段 |
|----------|-------------|------------|----------|----------|
| NORMAL / INTERN / RETIREMENT | 2 | 计提凭证 | `jt_gz` | `accrualVoucherId` |
| NORMAL / INTERN / RETIREMENT | 3 | 发放凭证 | `zf_gz` | `salaryVoucherId` |
| PARTTIME | 2 | 收票凭证 | `fp_lwf` | `accrualVoucherId` |
| PARTTIME | 3 | 发放凭证 | `zf_lwf` | `salaryVoucherId` |

`voucherType` 取值与现有 API/前端约定保持一致（2=计提侧，3=发放侧）。

### 后端：`EmployeeSalaryService.generateVoucher`

1. 加载 `EmployeeSalary` 与对应 `Employee`（取 `employeeType`）。
2. 按上表解析 `tplCode`；查账套模板（`relatedId=bookId`，`deleted=n`）；不存在则 `Message.failed("凭证模板["+code+"]未设置！")`。
3. 已生成幂等：计提侧已有 `accrualVoucherId` / 发放侧已有 `salaryVoucherId` 时返回已生成提示（文案按类型区分：普通「计提/发放」；兼职「收票/发放」）。
4. 分录填充：
   - **`jt_gz`**：对模板各分录项使用本行 `payAmount`（与期末汇总逻辑同形，金额源改为单行）。
   - **`zf_gz`**：复用期末 `SettlementCarryService` 中 `zf_gz` 的科目匹配思路（应付职工薪酬、个人社保、个税、银行存款等），金额取自**本行** `EmployeeSalary` 对应字段；若模板无匹配科目则跳过该行（与现劳务逻辑一致）。
   - **`fp_lwf` / `zf_lwf`**：保持现有实现。
5. 保存草稿凭证并回写对应 voucherId 字段。

### 前端

1. **`salary-detail.vue`**
   - 普通类员工显示「计提凭证」「发放凭证」列操作（生成/查看/删除），条件对称于现兼职列。
   - 兼职列标签仍为「收票凭证」「发放凭证」。
2. **`calc-salary.vue`**
   - 第 3 步仍校验已推送后跳转 `/hr/salary-detail`；引导文案可注明：普通员工在明细中点「计提/发放」，兼职点「收票/发放」。
3. **工资凭证规则页**筛选集合增加 `zf_gz`（若尚未包含），便于维护发放模板。

### 与期末处理的关系

- 期末 `jt_gz` / `zf_gz` 仍按**工资汇总**生成，本变更不删除该路径。
- 同一账期 `jt_gz`：**先到先得、双向硬拦**（明细计提 ↔ 期末汇总）。兼职收票 `fp_lwf` 不参与互斥。

### 测试与回归

1. 单元/服务级：按 `employeeType` 选择模板编码的纯函数或可测私有规则（推荐抽 `SalaryVoucherTemplateRules`）。
2. 扩展 `payroll-smb-regression.spec.ts`：普通员工推送后 `voucherType=2` 对有 `jt_gz` 的账套应 `code=0`；`voucherType=3` 在无 `zf_gz` 时允许明确失败信息（或账套有模板则成功）。
3. 兼职路径冒烟：仍请求 `fp_lwf`/`zf_lwf`（缺模板时错误文案不变）。

## 验收标准

- [ ] 普通员工：自定义基数 → 预览 → 推送 → **计提凭证成功**（`jt_gz`）→ 代发盘仍成功。
- [ ] 普通员工明细可见计提/发放操作；兼职仍为收票/发放。
- [ ] 缺 `zf_gz` 时发放失败信息含 `zf_gz`，不误报 `zf_lwf`。
- [ ] 兼职生成逻辑与模板编码未回退。
- [ ] `docs/product/09-payroll.md` 补充：普通 vs 兼职模板差异及勿与期末双计提的提示。

## 风险

| 风险 | 缓解 |
|------|------|
| `zf_gz` 未出现在多数账套种子 | 发放允许明确失败；计提（`jt_gz`）优先保证闭环 |
| 与期末汇总双计提 | 系统级先到先得互斥（`SalaryAccrualMutexRules`） |
| `zf_gz` 单人金额字段与汇总字段不完全同名 | 实现前对照 `EmployeeSalary` 字段映射表，单测覆盖 |
