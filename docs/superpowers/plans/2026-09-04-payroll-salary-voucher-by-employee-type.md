# Payroll Salary Voucher by Employee Type Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** 工资明细按员工类型选择凭证模板：普通员工用 `jt_gz`/`zf_gz`，兼职仍用 `fp_lwf`/`zf_lwf`，使算薪第 3 步对普通员工可真正生成计提凭证。

**Architecture:** 抽出纯函数 `SalaryVoucherTemplateRules` 决定模板编码与幂等文案；`EmployeeSalaryService.generateVoucher` 按编码填充分录（`jt_gz`/`zf_gz` 新增，劳务保持原逻辑）；前端工资明细对普通类员工开放计提/发放按钮；筛选集合与产品文档同步。

**Tech Stack:** Java 17 / Spring Boot / JUnit 5；Vue 3 + Element Plus；Playwright e2e。

## Global Constraints

- 不新建工资凭证规则表；复用账套 `voucher_template`。
- 不自动补种 `fp_lwf`/`zf_lwf`/`zf_gz`；缺模板返回 `凭证模板[code]未设置！`。
- 与期末汇总计提：**先到先得双向硬互斥**（`SalaryAccrualMutexRules`）。
- `voucherType`：`2`=计提/收票侧 → `accrualVoucherId`；`3`=发放侧 → `salaryVoucherId`。
- TDD：先写失败测试再写实现；提交仅在用户要求时执行（本计划 Step「Commit」改为「暂不 commit，除非用户要求」）。

---

## File Map

| File | Responsibility |
|------|----------------|
| `financial-cloud/.../hr/SalaryVoucherTemplateRules.java` | 按员工类型 + voucherType 解析模板编码与提示文案 |
| `financial-cloud/.../hr/SalaryVoucherTemplateRulesTest.java` | 规则单测 |
| `financial-cloud/.../hr/EmployeeSalaryService.java` | `generateVoucher` 接入规则 + `jt_gz`/`zf_gz` 分录 |
| `financial-cloud-ui/src/views/hr/salary-detail.vue` | 普通员工计提/发放列 |
| `financial-cloud-ui/src/views/hr/calc-salary.vue` | 第 3 步引导文案 |
| `financial-cloud-ui/src/views/voucher/voucher-template.vue` | `SALARY_TEMPLATE_CODES` 含 `zf_gz` |
| `financial-cloud-ui/e2e/payroll-smb-regression.spec.ts` | 普通员工计提成功断言 |
| `docs/product/09-payroll.md` | 普通/兼职模板与双路径提示 |

---

### Task 1: SalaryVoucherTemplateRules（模板选择）

**Files:**
- Create: `financial-cloud/src/main/java/com/financial/cloud/service/hr/SalaryVoucherTemplateRules.java`
- Test: `financial-cloud/src/test/java/com/financial/cloud/service/hr/SalaryVoucherTemplateRulesTest.java`

**Interfaces:**
- Consumes: `ConstsUser.EMPLOYEE_TYPE`（`NORMAL`/`INTERN`/`RETIREMENT`/`PARTTIME`）
- Produces:
  - `static String resolveTemplateCode(String employeeType, int voucherType)`
  - `static boolean isLaborEmployee(String employeeType)` — true only for `PARTTIME`
  - `static String alreadyGeneratedMessage(String employeeType, int voucherType)` — 普通「计提/发放凭证已生成」；兼职「收票/发放凭证已生成」

- [ ] **Step 1: Write the failing test**

```java
package com.financial.cloud.service.hr;

import org.junit.jupiter.api.Test;
import com.financial.cloud.constants.auth.ConstsUser;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class SalaryVoucherTemplateRulesTest {

    @Test
    void normalUsesJtGzAndZfGz() {
        assertEquals("jt_gz", SalaryVoucherTemplateRules.resolveTemplateCode(ConstsUser.EMPLOYEE_TYPE.NORMAL, 2));
        assertEquals("zf_gz", SalaryVoucherTemplateRules.resolveTemplateCode(ConstsUser.EMPLOYEE_TYPE.NORMAL, 3));
        assertEquals("jt_gz", SalaryVoucherTemplateRules.resolveTemplateCode(ConstsUser.EMPLOYEE_TYPE.INTERN, 2));
        assertEquals("jt_gz", SalaryVoucherTemplateRules.resolveTemplateCode(ConstsUser.EMPLOYEE_TYPE.RETIREMENT, 2));
    }

    @Test
    void parttimeUsesLaborTemplates() {
        assertEquals("fp_lwf", SalaryVoucherTemplateRules.resolveTemplateCode(ConstsUser.EMPLOYEE_TYPE.PARTTIME, 2));
        assertEquals("zf_lwf", SalaryVoucherTemplateRules.resolveTemplateCode(ConstsUser.EMPLOYEE_TYPE.PARTTIME, 3));
        assertTrue(SalaryVoucherTemplateRules.isLaborEmployee(ConstsUser.EMPLOYEE_TYPE.PARTTIME));
        assertFalse(SalaryVoucherTemplateRules.isLaborEmployee(ConstsUser.EMPLOYEE_TYPE.NORMAL));
    }

    @Test
    void alreadyGeneratedMessages() {
        assertEquals("计提凭证已生成", SalaryVoucherTemplateRules.alreadyGeneratedMessage(ConstsUser.EMPLOYEE_TYPE.NORMAL, 2));
        assertEquals("发放凭证已生成", SalaryVoucherTemplateRules.alreadyGeneratedMessage(ConstsUser.EMPLOYEE_TYPE.NORMAL, 3));
        assertEquals("收票凭证已生成", SalaryVoucherTemplateRules.alreadyGeneratedMessage(ConstsUser.EMPLOYEE_TYPE.PARTTIME, 2));
        assertEquals("发放凭证已生成", SalaryVoucherTemplateRules.alreadyGeneratedMessage(ConstsUser.EMPLOYEE_TYPE.PARTTIME, 3));
    }
}
```

- [ ] **Step 2: Run test to verify it fails**

Run (from `financial-cloud`):

```bash
mvn -q -Dtest=SalaryVoucherTemplateRulesTest test
```

Expected: FAIL（类不存在 / 编译失败）

- [ ] **Step 3: Write minimal implementation**

```java
package com.financial.cloud.service.hr;

import com.financial.cloud.constants.auth.ConstsUser;
import org.apache.commons.lang3.StringUtils;

public final class SalaryVoucherTemplateRules {
    private SalaryVoucherTemplateRules() {}

    public static boolean isLaborEmployee(String employeeType) {
        return ConstsUser.EMPLOYEE_TYPE.PARTTIME.equals(employeeType);
    }

    public static String resolveTemplateCode(String employeeType, int voucherType) {
        boolean labor = isLaborEmployee(employeeType);
        if (voucherType == 2) {
            return labor ? "fp_lwf" : "jt_gz";
        }
        // voucherType 3 (and any other payment-side code path)
        return labor ? "zf_lwf" : "zf_gz";
    }

    public static String alreadyGeneratedMessage(String employeeType, int voucherType) {
        if (voucherType == 2) {
            return isLaborEmployee(employeeType) ? "收票凭证已生成" : "计提凭证已生成";
        }
        return "发放凭证已生成";
    }
}
```

（未知 `employeeType` 按非劳务处理，走 `jt_gz`/`zf_gz`，与 NORMAL 同类。）

- [ ] **Step 4: Run test to verify it passes**

```bash
mvn -q -Dtest=SalaryVoucherTemplateRulesTest test
```

Expected: PASS

- [ ] **Step 5: Commit**

暂不 commit（除非用户要求）。

---

### Task 2: generateVoucher 接入规则 + jt_gz / zf_gz 分录

**Files:**
- Modify: `financial-cloud/src/main/java/com/financial/cloud/service/hr/EmployeeSalaryService.java`（`generateVoucher` 方法，约 373–478 行）
- Test: 可在 Task 1 同包增加纯金额辅助测试，或抽私有逻辑到 rules；本任务以改造 `generateVoucher` 为主，并用现有 e2e 在 Task 4 验证。可选补充：

**Interfaces:**
- Consumes: `SalaryVoucherTemplateRules.resolveTemplateCode` / `alreadyGeneratedMessage`
- Produces: 更新后的 `generateVoucher` 行为（模板随员工类型变化；`jt_gz`/`zf_gz` 分录）

**金额映射（单行 `EmployeeSalary`）：**

| 模板 | 分录规则 |
|------|----------|
| `jt_gz` | 模板每一项：金额 = `salary.getPayAmount()`（方向用模板 `direction`） |
| `zf_gz` | `2211*` 或 `221101`：`payAmount`；`122102`：`totalSocialInsurance`；`222114`：`personalTax`；科目以 `1002` 开头：贷方银行 = `payAmount - totalSocialInsurance - personalTax`（负数当 0）；用 `SubjectCodeCompat.mapContains` / `resolveFromMap` 与现有劳务风格一致 |
| `fp_lwf` / `zf_lwf` | 保持现有代码块不变 |

注意：期末 `SettlementCarryService` 里 `222114` 曾用 `businessSocialInsurance`；**本路径按个税语义使用 `personalTax`**。

- [ ] **Step 1: Write a focused unit test for zf_gz bank amount helper（可选但推荐）**

在 `SalaryVoucherTemplateRules` 增加：

```java
public static BigDecimal paymentBankAmount(BigDecimal payAmount, BigDecimal personalSi, BigDecimal personalTax) {
    BigDecimal pay = payAmount == null ? BigDecimal.ZERO : payAmount;
    BigDecimal si = personalSi == null ? BigDecimal.ZERO : personalSi;
    BigDecimal tax = personalTax == null ? BigDecimal.ZERO : personalTax;
    BigDecimal bank = pay.subtract(si).subtract(tax);
    return bank.compareTo(BigDecimal.ZERO) < 0 ? BigDecimal.ZERO : bank;
}
```

测试：

```java
@Test
void paymentBankAmountSubtractsSiAndTax() {
    assertEquals(0, new BigDecimal("7000").compareTo(
        SalaryVoucherTemplateRules.paymentBankAmount(
            new BigDecimal("8000"), new BigDecimal("500"), new BigDecimal("500"))));
}
```

先加测试 → 跑失败 → 加方法 → 跑通过。

- [ ] **Step 2: Wire generateVoucher**

将硬编码：

```java
String tplCode = (voucherType == 2 ? "fp_lwf" : "zf_lwf");
```

改为：

```java
Employee employee = employeeMapper.selectById(salary.getEmployeeId());
String employeeType = employee != null ? employee.getEmployeeType() : null;
String tplCode = SalaryVoucherTemplateRules.resolveTemplateCode(employeeType, voucherType);
```

幂等提示改为 `SalaryVoucherTemplateRules.alreadyGeneratedMessage(employeeType, voucherType)`。

在 `fp_lwf` / `zf_lwf` 分支旁增加：

```java
} else if ("jt_gz".equals(voucherTemplate.getCode())) {
    BigDecimal amount = salary.getPayAmount() != null ? salary.getPayAmount() : BigDecimal.ZERO;
    for (VoucherTemplateItem item : items) {
        voucherItems.add(createVoucherItemDto(bookId, item, amount));
        if (item.getDirection() != null && item.getDirection() == 1) {
            debitAmount = debitAmount.add(amount);
        } else {
            creditAmount = creditAmount.add(amount);
        }
    }
} else if ("zf_gz".equals(voucherTemplate.getCode())) {
    BigDecimal pay = salary.getPayAmount() != null ? salary.getPayAmount() : BigDecimal.ZERO;
    BigDecimal personalSi = salary.getTotalSocialInsurance() != null ? salary.getTotalSocialInsurance() : BigDecimal.ZERO;
    BigDecimal personalTax = salary.getPersonalTax() != null ? salary.getPersonalTax() : BigDecimal.ZERO;
    if (SubjectCodeCompat.mapContains(itemsMap, "221101")
            || SubjectCodeCompat.mapContains(itemsMap, "2211")) {
        String code = SubjectCodeCompat.mapContains(itemsMap, "221101") ? "221101" : "2211";
        debitAmount = debitAmount.add(pay);
        voucherItems.add(createVoucherItemDto(bookId, SubjectCodeCompat.resolveFromMap(itemsMap, code), pay));
    }
    if (SubjectCodeCompat.mapContains(itemsMap, "122102")) {
        voucherItems.add(createVoucherItemDto(bookId, SubjectCodeCompat.resolveFromMap(itemsMap, "122102"), personalSi));
    }
    if (SubjectCodeCompat.mapContains(itemsMap, "222114")) {
        voucherItems.add(createVoucherItemDto(bookId, SubjectCodeCompat.resolveFromMap(itemsMap, "222114"), personalTax));
    }
    BigDecimal bank = SalaryVoucherTemplateRules.paymentBankAmount(pay, personalSi, personalTax);
    for (VoucherTemplateItem item : items) {
        if (item.getSubjectCode() != null && item.getSubjectCode().startsWith("1002")) {
            voucherItems.add(createVoucherItemDto(bookId, item, bank));
        }
    }
    creditAmount = debitAmount;
}
```

将原先方法末尾的 `Employee employee = employeeMapper.selectById(...)` 上移到模板解析前（避免重复查询）；备注替换仍用该 `employee`。

若 `SubjectCodeCompat` 对 `2211` 前缀已有兼容方法，优先用现有 API，不要发明第二套解析。

- [ ] **Step 3: Compile / unit tests**

```bash
mvn -q -Dtest=SalaryVoucherTemplateRulesTest test
```

Expected: PASS。若有编译错误，先修再进入前端任务。

- [ ] **Step 4: Commit**

暂不 commit。

---

### Task 3: 前端工资明细 + 引导 + 模板筛选

**Files:**
- Modify: `financial-cloud-ui/src/views/hr/salary-detail.vue`（收票/发放列模板约 388–448 行）
- Modify: `financial-cloud-ui/src/views/hr/calc-salary.vue`（引导 hint）
- Modify: `financial-cloud-ui/src/views/voucher/voucher-template.vue`（`SALARY_TEMPLATE_CODES`）

**Interfaces:**
- Consumes: 现有 `generateVoucher(row, 2|3)` API
- Produces: 普通类员工可见计提/发放按钮

- [ ] **Step 1: salary-detail 按钮条件**

定义脚本内常量：

```ts
const SALARY_VOUCHER_TYPES = ['NORMAL', 'INTERN', 'RETIREMENT']
function isSalaryEmployee(type: string) {
  return SALARY_VOUCHER_TYPES.includes(type)
}
function isLaborEmployee(type: string) {
  return type === 'PARTTIME'
}
```

- 「收票凭证」列：仅 `isLaborEmployee`（保持现逻辑）。
- 新增或改造「计提凭证」列：对 `isSalaryEmployee` 显示生成/查看/删除（`voucherType` 2，字段 `accrualVoucherId`）。
- 「发放凭证」列：`isLaborEmployee || isSalaryEmployee` 均显示（`voucherType` 3）。

列标题可用两列并排，或同一「发放」列兼容两类；推荐：

1. 列「计提/收票」：劳动显示收票按钮，工资类显示计提按钮。  
2. 列「发放凭证」：两类都显示。

最小改法示例（计提/收票列）：

```vue
<el-button
  v-if="isSalaryEmployee(scope.row.employeeType) && !scope.row.accrualVoucherId"
  type="text"
  @click="generateVoucher(scope.row, 2)"
>生成</el-button>
<!-- 现有 PARTTIME 条件保留 -->
```

- [ ] **Step 2: calc-salary 文案**

将 `guide-hint` 改为类似：

```text
当前账期：{{ currentTerm || '—' }}。请按步骤完成；第 3 步进入工资明细后，普通员工点「计提/发放」，兼职点「收票/发放」。代发盘仅针对已推送确认的工资明细。
```

- [ ] **Step 3: voucher-template 筛选**

```ts
const SALARY_TEMPLATE_CODES = new Set(['jt_gz', 'zf_gz', 'fp_lwf', 'zf_lwf'])
```

empty-text 同步提及 `zf_gz`。

- [ ] **Step 4: 手工/浏览器冒烟（可选）**

打开 `/hr/salary-detail`，确认普通员工行出现计提按钮。

- [ ] **Step 5: Commit**

暂不 commit。

---

### Task 4: 回归 e2e + 产品文档

**Files:**
- Modify: `financial-cloud-ui/e2e/payroll-smb-regression.spec.ts`
- Modify: `docs/product/09-payroll.md`

**Interfaces:**
- Consumes: Task 2 后端行为（`jt_gz` 计提成功）

- [ ] **Step 1: 更新回归断言**

将「允许 zf_lwf 失败」改为：

```ts
const accrual = await jsonPost(request, '/api/employee/salary/generate-voucher', headers, {
  id: salary.id,
  bookId,
  voucherType: 2,
})
expect(accrual.code, accrual.message || 'accrual voucher').toBe(0)

const payVoucher = await jsonPost(request, '/api/employee/salary/generate-voucher', headers, {
  id: salary.id,
  bookId,
  voucherType: 3,
})
if (payVoucher.code !== 0) {
  expect(String(payVoucher.message || '')).toMatch(/凭证模板\[zf_gz\]未设置/)
}
```

- [ ] **Step 2: 跑 e2e**

前置：后端 `:2154`、前端 `:3154` 已启动。

```bash
cd financial-cloud-ui
npx playwright test e2e/payroll-smb-regression.spec.ts --reporter=list
```

Expected: PASS（计提成功；发放在无 `zf_gz` 时允许明确失败）

- [ ] **Step 3: 更新 `docs/product/09-payroll.md`**

在生成凭证相关段落增加：

- 普通员工（含实习/返聘）：`jt_gz` 计提、`zf_gz` 发放。  
- 兼职：`fp_lwf` 收票、`zf_lwf` 发放。  
- 同一账期勿与期末汇总 `jt_gz`/`zf_gz` 对同一批工资重复生成。

- [ ] **Step 4: Commit**

暂不 commit。

---

## Spec coverage (self-review)

| Spec 要求 | Task |
|-----------|------|
| NORMAL 等用 `jt_gz`/`zf_gz` | 1–2 |
| PARTTIME 保持劳务模板 | 1–2 |
| 单行金额填充 | 2 |
| 缺模板明确错误 | 2（沿用现有 failed 文案） |
| 明细 UI 普通员工按钮 | 3 |
| 算薪第 3 步可操作 | 3 |
| 筛选含 `zf_gz` | 3 |
| 回归 + 文档 | 4 |
| 不做硬互斥 / 不自动补种 | Global Constraints |

无 TBD 占位；`paymentBankAmount` 与 `resolveTemplateCode` 命名前后一致。
