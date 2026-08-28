# 费用明细表 Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** 新增费用明细表：科目树 × 多月份矩阵 + 最右年度合计列 + 底部合计 + Excel 导出，并与利润表期间费用行勾稽。

**Architecture:** 新建 `StatementExpenseDetailService` 编排取数；`VoucherItemMapper.selectExpenseAmountByMonth` 按科目×月聚合；`StatementExpenseDetailRules` 负责 rollup / 合计 / 零行过滤 / yearLabel / 勾稽比对；前端新建 `expense-detail.vue` 动态列树表。复用 `StatementIncomeRules.PROFIT_AND_LOSS_AMOUNT` 与利润表 config 行次做逐月勾稽。

**Tech Stack:** Java 17、Spring Boot、MyBatis、Apache POI（程序化动态列导出）、Vue3 + Element Plus、JUnit 5。

**Spec:** `docs/superpowers/specs/2026-08-28-expense-detail-design.md`

## Global Constraints

- 默认科目：`5601,5602,5603`（`SubjectCodeCompat` 兼容 6601/6602/6603）
- 发生额：`|借| − |贷|` = `StatementIncomeRules.normalizePeriodAmount(..., PROFIT_AND_LOSS_AMOUNT)`
- 仅已过账凭证（`postedOnly=true` 默认）
- 年度合计列始终显示；表头单年=`{year}年合计`，跨年=`区间合计`
- Phase 1 **不含**图表、同比、打印、显示零发生额开关
- 勾稽偏差默认 warn + `reconciliationWarnings`，不拦截出表
- 区间上限 24 个月
- Do not git commit unless the user explicitly asks
- 后端测试：`$env:JAVA_HOME='C:\Program Files\Java\jdk-17'; cd financial-cloud; .\mvnw.cmd -Dtest=... test`
- 编译：`cd financial-cloud; .\mvnw.cmd -DskipTests compile`

---

## File map

| Path | Responsibility |
|------|----------------|
| `dto/statement/StatementExpenseDetailReport.java` | 响应根：periods, yearLabel, items, totals, warnings |
| `dto/statement/StatementExpenseDetailItem.java` | 树节点：code/name/amounts/yearTotal/children |
| `dto/statement/ExpenseReconciliationWarning.java` | 勾稽告警项 |
| `dto/voucher/VoucherItemVo.java`（或新建 Vo） | SQL 行：subjectCode, yearPeriod, debit, credit |
| `util/StatementExpenseDetailRules.java` | 纯函数：rollup、totals、filterZero、yearLabel、reconcile |
| `service/statement/StatementExpenseDetailService.java` | 编排 + 导出 + 勾稽调用利润表 |
| `repository/voucher/VoucherItemMapper.java` + `.xml` | `selectExpenseAmountByMonth` |
| `dto/statement/StatementParamsDto.java` | `maxLevel`；完善 `between` parse |
| `enums/error/StatementErrorCode.java` + messages | `EXPENSE_DETAIL_PERIOD_TOO_LONG` |
| `controller/statement/StatementReportController.java` | GET expense-detail + export |
| `financial-cloud-ui/.../api/statement/statement-expense-detail.ts` | API |
| `financial-cloud-ui/.../views/statement/expense-detail.vue` | 页面 |
| `sql/seed/expense_detail_menu.sql`（或等价） | 菜单注册 |

**Phase 1 辅助核算：** UI 保留 `showAux` 开关并传参；后端 Phase 1 **先忽略展开**（与科目余额表当前行为一致），验收标准第 8 条放到 Task 9（可延后）。核心矩阵不依赖辅助核算。

---

### Task 1: Rules 纯函数 + 单元测试

**Files:**
- Create: `financial-cloud/src/main/java/com/financial/cloud/util/StatementExpenseDetailRules.java`
- Create: `financial-cloud/src/main/java/com/financial/cloud/dto/statement/StatementExpenseDetailItem.java`
- Create: `financial-cloud/src/main/java/com/financial/cloud/dto/statement/ExpenseReconciliationWarning.java`
- Test: `financial-cloud/src/test/java/com/financial/cloud/util/StatementExpenseDetailRulesTest.java`

**Interfaces:**
- Produces:
  - `String yearLabel(List<String> periods)`
  - `BigDecimal sumAmounts(Map<String, BigDecimal> amounts, List<String> periods)`
  - `void rollup(StatementExpenseDetailItem node, List<String> periods)` — 自底向上填父级 amounts + yearTotal
  - `Map<String, BigDecimal> computeTotals(List<StatementExpenseDetailItem> roots, List<String> periods)` — 含 key `"yearTotal"`
  - `void filterZeroLeaves(List<StatementExpenseDetailItem> roots, List<String> periods)`
  - `List<StatementExpenseDetailItem> truncateLevel(List<StatementExpenseDetailItem> roots, int maxLevel)` — `0`=不截断
  - `List<ExpenseReconciliationWarning> reconcile(Map<String, Map<String, BigDecimal>> detailByCodePeriod, Map<String, Map<String, BigDecimal>> incomeByCodePeriod, BigDecimal tolerance)`

- [ ] **Step 1: 写 DTO**

```java
// StatementExpenseDetailItem.java
@Data @Builder @NoArgsConstructor @AllArgsConstructor
public class StatementExpenseDetailItem {
    private String sourceId;
    private String parentId;
    private String subjectCode;
    private String subjectName;
    private Integer level;
    private Map<String, BigDecimal> amounts; // period -> amount
    private BigDecimal yearTotal;
    private List<StatementExpenseDetailItem> children;
}

// ExpenseReconciliationWarning.java
@Data @Builder @NoArgsConstructor @AllArgsConstructor
public class ExpenseReconciliationWarning {
    private String subjectCode;
    private String period;
    private BigDecimal detailAmount;
    private BigDecimal incomeAmount;
    private BigDecimal diff;
}
```

- [ ] **Step 2: 写失败测试**

```java
class StatementExpenseDetailRulesTest {
    @Test
    void yearLabel_sameYear() {
        assertEquals("2023年合计",
            StatementExpenseDetailRules.yearLabel(List.of("2023-01", "2023-12")));
    }

    @Test
    void yearLabel_crossYear() {
        assertEquals("区间合计",
            StatementExpenseDetailRules.yearLabel(List.of("2023-12", "2024-01")));
    }

    @Test
    void rollup_sumsChildren() {
        var child = item("5601.01", "房租", Map.of("2023-01", bd("100"), "2023-02", bd("50")));
        var parent = item("5601", "销售费用", new HashMap<>());
        parent.setChildren(List.of(child));
        StatementExpenseDetailRules.rollup(parent, List.of("2023-01", "2023-02"));
        assertEquals(0, bd("100").compareTo(parent.getAmounts().get("2023-01")));
        assertEquals(0, bd("50").compareTo(parent.getAmounts().get("2023-02")));
        assertEquals(0, bd("150").compareTo(parent.getYearTotal()));
    }

    @Test
    void computeTotals_sumsRootRows() {
        var a = item("5601", "销售", Map.of("2023-01", bd("10")));
        a.setYearTotal(bd("10"));
        var b = item("5602", "管理", Map.of("2023-01", bd("20")));
        b.setYearTotal(bd("20"));
        var totals = StatementExpenseDetailRules.computeTotals(List.of(a, b), List.of("2023-01"));
        assertEquals(0, bd("30").compareTo(totals.get("2023-01")));
        assertEquals(0, bd("30").compareTo(totals.get("yearTotal")));
    }

    @Test
    void filterZeroLeaves_removesZeroOnlyLeaves() {
        var zero = item("5601.01", "零", Map.of("2023-01", BigDecimal.ZERO));
        zero.setYearTotal(BigDecimal.ZERO);
        var nonzero = item("5601.02", "有", Map.of("2023-01", bd("1")));
        nonzero.setYearTotal(bd("1"));
        var parent = item("5601", "销", new HashMap<>());
        parent.setChildren(new ArrayList<>(List.of(zero, nonzero)));
        StatementExpenseDetailRules.rollup(parent, List.of("2023-01"));
        StatementExpenseDetailRules.filterZeroLeaves(List.of(parent), List.of("2023-01"));
        assertEquals(1, parent.getChildren().size());
        assertEquals("5601.02", parent.getChildren().get(0).getSubjectCode());
    }

    @Test
    void reconcile_emitsWarningWhenDiffExceedsTolerance() {
        var detail = Map.of("5601", Map.of("2023-01", bd("100")));
        var income = Map.of("5601", Map.of("2023-01", bd("100.02")));
        var warnings = StatementExpenseDetailRules.reconcile(
            detail, income, new BigDecimal("0.01"));
        assertEquals(1, warnings.size());
        assertEquals("5601", warnings.get(0).getSubjectCode());
    }

    private static BigDecimal bd(String s) { return new BigDecimal(s); }
    private static StatementExpenseDetailItem item(String code, String name, Map<String, BigDecimal> amounts) {
        return StatementExpenseDetailItem.builder()
            .subjectCode(code).subjectName(name)
            .amounts(new HashMap<>(amounts))
            .children(new ArrayList<>())
            .build();
    }
}
```

- [ ] **Step 3: 运行测试 — 期望 FAIL（类不存在）**

```powershell
$env:JAVA_HOME='C:\Program Files\Java\jdk-17'
cd C:\Users\Administrator\Projects\jinbooks\financial-cloud
.\mvnw.cmd -Dtest=StatementExpenseDetailRulesTest test
```

Expected: compile error / FAIL

- [ ] **Step 4: 实现 `StatementExpenseDetailRules`**

实现上述全部静态方法；`rollup` 递归子节点后汇总；`filterZeroLeaves` 后序删除 yearTotal==0 且无子节点的叶子；`reconcile` 对双方共有的 (code, period) 比较绝对值差。

- [ ] **Step 5: 再跑测试 — PASS**

```powershell
.\mvnw.cmd -Dtest=StatementExpenseDetailRulesTest test
```

---

### Task 2: Params / 错误码 — between + maxLevel + 24 期上限

**Files:**
- Modify: `financial-cloud/src/main/java/com/financial/cloud/dto/statement/StatementParamsDto.java`
- Modify: `financial-cloud/src/main/java/com/financial/cloud/enums/error/StatementErrorCode.java`
- Modify: `financial-cloud/src/main/java/com/financial/cloud/constants/common/MessageKeys.java`
- Modify: `financial-cloud/src/main/resources/messages/messages.properties`
- Modify: `financial-cloud/src/main/resources/messages/messages_zh_CN.properties`
- Modify: `financial-cloud/src/main/resources/messages/messages_en.properties`
- Test: `financial-cloud/src/test/java/com/financial/cloud/dto/statement/StatementParamsDtoExpenseDetailTest.java`（新建）

**Interfaces:**
- Produces: `StatementParamsDto.maxLevel`（Integer，null/0=至末级）
- `parse()` 在 `periodType=between` 时：要求 `dateRange.length==2`，填充 `dateRangeStart/End`（月初/月末），校验月数 ≤ 24

- [ ] **Step 1: 写失败测试**

```java
@Test
void between_parsesDateRangeAndBounds() {
    StatementParamsDto dto = StatementParamsDto.builder()
        .bookId("b1")
        .periodType("between")
        .dateRange(new String[]{"2023-01", "2023-12"})
        .build();
    dto.parse();
    assertEquals("2023-01-01", dto.getDateRangeStart());
    assertEquals("2023-12-31", dto.getDateRangeEnd());
    assertEquals(12, dto.getAllMonths().size());
}

@Test
void between_rejectsMoreThan24Months() {
    StatementParamsDto dto = StatementParamsDto.builder()
        .bookId("b1")
        .periodType("between")
        .dateRange(new String[]{"2022-01", "2024-02"})
        .build();
    assertThrows(BusinessException.class, dto::parse);
}
```

- [ ] **Step 2: 运行 — FAIL**

- [ ] **Step 3: 实现**

在 `parse()` 的 `BETWEEN_MONTH` 分支：

```java
} else if (StatementPeriodTypeEnum.BETWEEN_MONTH.getValue().equals(this.periodType)) {
    if (dateRange == null || dateRange.length != 2
            || StringUtils.isBlank(dateRange[0]) || StringUtils.isBlank(dateRange[1])) {
        throw new BusinessException(StatementErrorCode.DATE_RANGE_SIZE);
    }
    YearMonth start = YearMonth.parse(dateRange[0]);
    YearMonth end = YearMonth.parse(dateRange[1]);
    if (start.isAfter(end)) {
        throw new BusinessException(StatementErrorCode.START_DATE_AFTER_END);
    }
    long months = ChronoUnit.MONTHS.between(start, end) + 1;
    if (months > 24) {
        throw new BusinessException(StatementErrorCode.EXPENSE_DETAIL_PERIOD_TOO_LONG);
    }
    this.dateRangeStart = start.atDay(1).toString();
    this.dateRangeEnd = end.atEndOfMonth().toString();
    this.reportDate = dateRange[1];
    this.year = end.getYear();
    this.month = null;
}
```

新增错误码：

```java
EXPENSE_DETAIL_PERIOD_TOO_LONG(513016, MessageKeys.Statement.EXPENSE_DETAIL_PERIOD_TOO_LONG);
```

messages（三语）：`statement.error.expense_detail_period_too_long=费用明细表查询区间不能超过24个月`

字段：`private Integer maxLevel;`

- [ ] **Step 4: 测试 PASS**

---

### Task 3: Mapper SQL — 按科目×月聚合

**Files:**
- Modify: `financial-cloud/src/main/java/com/financial/cloud/repository/voucher/VoucherItemMapper.java`
- Modify: `financial-cloud/src/main/resources/com/financial/cloud/repository/voucher/VoucherItemMapper.xml`
- Ensure `VoucherItemVo` 有 `yearPeriod` 字段（若无则加上）

**Interfaces:**
- Produces: `List<VoucherItemVo> selectExpenseAmountByMonth(@Param("params") StatementParamsDto params, @Param("prefixes") List<String> prefixes);`
- 每行：`subjectCode`, `yearPeriod` (`yyyy-MM`), `debitAmount`, `creditAmount`

- [ ] **Step 1: 扩展接口与 Vo**

```java
List<VoucherItemVo> selectExpenseAmountByMonth(
    @Param("params") StatementParamsDto params,
    @Param("prefixes") List<String> prefixes);
```

- [ ] **Step 2: XML**

```xml
<select id="selectExpenseAmountByMonth" resultType="com.financial.cloud.dto.voucher.VoucherItemVo">
    SELECT
        i.subject_code AS subjectCode,
        DATE_FORMAT(i.voucher_date, '%Y-%m') AS yearPeriod,
        SUM(i.debit_amount) AS debitAmount,
        SUM(i.credit_amount) AS creditAmount
    FROM voucher_item i
    JOIN voucher v ON i.book_id = v.book_id AND i.voucher_id = v.id
    WHERE v.book_id = #{params.bookId}
      AND i.deleted = 'n'
      AND v.deleted = 'n'
      AND v.status = 'completed'
      AND i.voucher_date &gt;= str_to_date(#{params.dateRangeStart}, '%Y-%m-%d')
      AND str_to_date(#{params.dateRangeEnd}, '%Y-%m-%d') &gt;= i.voucher_date
      <if test="params.postedOnly != null and params.postedOnly">
          AND v.sender_id IS NOT NULL AND v.sender_id &lt;&gt; ''
      </if>
      <if test="prefixes != null and prefixes.size() &gt; 0">
          AND (
          <foreach collection="prefixes" item="p" separator=" OR ">
              i.subject_code LIKE CONCAT(#{p}, '%')
          </foreach>
          )
      </if>
    GROUP BY i.subject_code, DATE_FORMAT(i.voucher_date, '%Y-%m')
    ORDER BY i.subject_code, yearPeriod
</select>
```

- [ ] **Step 3: 编译通过**

```powershell
.\mvnw.cmd -DskipTests compile
```

---

### Task 4: Report DTO + Service 核心（建树 / rollup / 合计）

**Files:**
- Create: `financial-cloud/src/main/java/com/financial/cloud/dto/statement/StatementExpenseDetailReport.java`
- Create: `financial-cloud/src/main/java/com/financial/cloud/service/statement/StatementExpenseDetailService.java`
- Test: `financial-cloud/src/test/java/com/financial/cloud/service/statement/StatementExpenseDetailServiceTest.java`（Mockito）

**Interfaces:**
- Consumes: Mapper、`BookSubjectMapper`（或现有 book subject repo）、`StatementExpenseDetailRules`、`SubjectCodeCompat`、`StatementIncomeRules`
- Produces: `Message<StatementExpenseDetailReport> query(StatementParamsDto dto)`

```java
@Data @Builder @NoArgsConstructor @AllArgsConstructor
public class StatementExpenseDetailReport {
    private List<String> periods;
    private String yearLabel;
    private List<StatementExpenseDetailItem> items;
    private Map<String, BigDecimal> totals;
    private List<ExpenseReconciliationWarning> reconciliationWarnings;
}
```

- [ ] **Step 1: 写 Mockito 测试骨架**

覆盖：
1. 默认 prefixes `5601,5602,5603`；空 subjectCodes 时用默认
2. 子科目金额 normalize 后 rollup 到父级
3. totals / yearLabel 正确
4. `postedOnly` 默认 true

```java
@Test
void query_buildsTreeAndTotals() {
    // mock book subjects: 5601, 5601.01
    // mock voucher rows: 5601.01 / 2023-01 debit 100 credit 0
    // assert root 5601 amounts 2023-01 == 100, yearTotal == 100
}
```

- [ ] **Step 2: 实现 Service.query 流程**

```
1. dto.setPostedOnly(true) if null
2. 若 subjectCodes 空 → ["5601","5602","5603"]
3. dto.parse(); periods = dto.getAllMonths()
4. prefixes = SubjectCodeCompat.expandLookupCodes(subjectCodes) 展开后取顶级前缀列表
5. rows = voucherItemMapper.selectExpenseAmountByMonth(dto, prefixes)
6. amountMap: subjectCode -> (period -> normalizeAmount)
7. 加载 book_subject（bookId），过滤 code 匹配任一 prefix，补全祖先
8. 建 StatementExpenseDetailItem 树（sourceId=subject.id, parentId=subject.parentId）
9. 末级填 amounts；对每个 root 调用 rollup
10. filterZeroLeaves；若 maxLevel>0 truncateLevel
11. totals = computeTotals；yearLabel = yearLabel(periods)
12. reconciliationWarnings = 空列表（Task 5 填充）
13. return Message.ok(report)
```

**科目匹配：** `code.startsWith(prefix)` 或 `SubjectCodeCompat.incomeRuleMatchesVoucherSubject(prefix, code)`。

**normalize：**

```java
StatementIncomeRules.normalizePeriodAmount(debit, credit, StatementIncomeRules.PROFIT_AND_LOSS_AMOUNT)
```

- [ ] **Step 3: 测试 PASS**

---

### Task 5: 利润表勾稽

**Files:**
- Modify: `StatementExpenseDetailService.java`
- Modify: `StatementExpenseDetailServiceTest.java`
- Depends: `StatementIncomeService`、`ConfigSysService`

**Interfaces:**
- Produces: `reconcileWithIncome(dto, report)` 填充 `reconciliationWarnings`

- [ ] **Step 1: 写测试**

- mock config：selling→`105`，admin→`106`，financial→`107`
- mock 利润表单月生成返回对应 currentBalance
- 金额一致 → warnings 空；故意偏差 → warnings 非空

- [ ] **Step 2: 实现**

```java
private void reconcileWithIncome(StatementParamsDto dto, StatementExpenseDetailReport report) {
    Map<String, String> config = configSysService.getBookConfigMap(dto.getBookId());
    Map<String, String> codeToItem = Map.of(
        "5601", config.get(ConstsSysConfig.SYS_DEFAULT_SELLING_EXPENSES),
        "5602", config.get(ConstsSysConfig.SYS_DEFAULT_ADMINISTRATIVE_EXPENSES),
        "5603", config.get(ConstsSysConfig.SYS_DEFAULT_FINANCIAL_EXPENSES)
    );
    Map<String, Map<String, BigDecimal>> detail = new HashMap<>();
    Map<String, Map<String, BigDecimal>> income = new HashMap<>();
    for (StatementExpenseDetailItem root : report.getItems()) {
        String top = topExpenseCode(root.getSubjectCode()); // 5601/5602/5603
        if (!codeToItem.containsKey(top)) continue;
        detail.computeIfAbsent(top, k -> new HashMap<>())
              .putAll(/* 该 root 的 amounts */);
    }
    for (String period : report.getPeriods()) {
        StatementParamsDto monthDto = StatementParamsDto.builder()
            .bookId(dto.getBookId())
            .periodType("month")
            .reportDate(period)
            .postedOnly(true)
            .build();
        monthDto.parse();
        StatementIncome si = statementIncomeService.generateIncomeStatement(monthDto, false).getData();
        // 按 itemCode 取 currentBalance，写入 income[560x][period]
    }
    List<ExpenseReconciliationWarning> warnings =
        StatementExpenseDetailRules.reconcile(detail, income, StatementIncomeRules.FORMULA_TOLERANCE);
    for (ExpenseReconciliationWarning w : warnings) {
        log.warn("Expense detail vs income: {} {} detail={} income={} diff={}",
            w.getSubjectCode(), w.getPeriod(), w.getDetailAmount(), w.getIncomeAmount(), w.getDiff());
    }
    report.setReconciliationWarnings(warnings);
}
```

注意：避免 N+1 性能问题——若 periods 很长，可对利润表取数做批量优化；Phase 1 可接受逐月调用（上限 24）。

- [ ] **Step 3: 测试 PASS**

---

### Task 6: Controller 端点

**Files:**
- Modify: `financial-cloud/src/main/java/com/financial/cloud/controller/statement/StatementReportController.java`

**Interfaces:**
- Produces:
  - `GET /api/statement/expense-detail` → `Message<StatementExpenseDetailReport>`
  - `GET /api/statement/expense-detail/export` → Excel stream（Task 7 实现方法体）

- [ ] **Step 1: 注入 Service，增加端点**

```java
private final StatementExpenseDetailService statementExpenseDetailService;

@GetMapping("/expense-detail")
public Message<StatementExpenseDetailReport> expenseDetail(
        StatementParamsDto dto, @CurrentUser UserInfo userInfo) {
    dto.setBookId(userInfo.getBookId());
    if (StringUtils.isBlank(dto.getPeriodType())) {
        dto.setPeriodType("between");
    }
    validParams(dto); // 若 validParams 强制 reportDate，对 between 放宽：允许 dateRange 替代
    return statementExpenseDetailService.query(dto);
}

@GetMapping("/expense-detail/export")
public void expenseDetailExport(HttpServletResponse response,
        StatementParamsDto dto, @CurrentUser UserInfo userInfo) throws IOException {
    dto.setBookId(userInfo.getBookId());
    if (StringUtils.isBlank(dto.getPeriodType())) {
        dto.setPeriodType("between");
    }
    validParams(dto);
    statementExpenseDetailService.export(dto, response);
}
```

- [ ] **Step 2: 调整 `validParams`**

现有若要求 `reportDate` 非空，对 `between` 改为：`dateRange` 非空即可。

- [ ] **Step 3: 编译**

```powershell
.\mvnw.cmd -DskipTests compile
```

---

### Task 7: 动态列 Excel 导出

**Files:**
- Modify: `StatementExpenseDetailService.java` — `export(...)`
- 不使用固定模板；用 Apache POI `XSSFWorkbook` 程序化写表（与现有 `ExcelExporter` 模板模式不同，因列数动态）

**Interfaces:**
- Produces: `void export(StatementParamsDto dto, HttpServletResponse response)`

- [ ] **Step 1: 实现导出**

```java
public void export(StatementParamsDto dto, HttpServletResponse response) throws IOException {
    StatementExpenseDetailReport report = query(dto).getData();
    Book book = bookMapper.selectById(dto.getBookId());
    try (Workbook wb = new XSSFWorkbook()) {
        Sheet sheet = wb.createSheet("费用明细表");
        int r = 0;
        Row title = sheet.createRow(r++);
        title.createCell(0).setCellValue(book.getName() + " — 费用明细表");
        Row header = sheet.createRow(r++);
        int c = 0;
        header.createCell(c++).setCellValue("编码");
        header.createCell(c++).setCellValue("名称");
        for (String p : report.getPeriods()) {
            header.createCell(c++).setCellValue(formatPeriodLabel(p)); // 2023年1期
        }
        header.createCell(c).setCellValue(report.getYearLabel());
        // flatten tree depth-first 写行
        writeItems(sheet, report.getItems(), r, /* out row cursor */);
        // 合计行
        ...
        response.setContentType(ExcelExporter.APPLICATION_MS_EXCEL);
        response.setHeader("Content-Disposition",
            "attachment; filename=" + URLEncoder.encode(
                "费用明细表_" + book.getName() + "_" + dto.getDateRange()[0]
                + "-" + dto.getDateRange()[1] + ".xlsx", StandardCharsets.UTF_8));
        wb.write(response.getOutputStream());
    }
}
```

金额用 `BigDecimal` 写入 numeric cell；空月写 0 或空白（与页面一致：0 显示为空串可选）。

- [ ] **Step 2: 手工或集成冒烟（可选）** — 至少 compile PASS

---

### Task 8: 前端 API + 页面

**Files:**
- Create: `financial-cloud-ui/src/api/statement/statement-expense-detail.ts`
- Create: `financial-cloud-ui/src/views/statement/expense-detail.vue`
- 参考: `subject-balance.vue`（树表、导出 blob）

**Interfaces:**
- Produces: `getExpenseDetail(query)`, `expenseDetailExport(query)`

- [ ] **Step 1: API**

```typescript
import request from '@/utils/Request'

export function getExpenseDetail(query: any): any {
  return request({ url: '/statement/expense-detail', method: 'get', params: query })
}

export function expenseDetailExport(query: any): any {
  return request({
    url: '/statement/expense-detail/export',
    method: 'get',
    params: query,
    responseType: 'blob'
  })
}
```

- [ ] **Step 2: 页面核心逻辑**

查询参数：

```typescript
queryParams = {
  periodType: 'between',
  dateRange: [yearStart, currentTerm], // monthrange
  subjectCodes: ['5601', '5602', '5603'], // 或输入框拆分
  maxLevel: 0,
  showAux: false,
  postedOnly: true
}
```

表格：

```vue
<el-table
  :data="recordsList"
  row-key="sourceId"
  :tree-props="{ children: 'children' }"
  :expand-row-keys="expandsIds"
  show-summary
  :summary-method="summaryMethod"
  height="590"
>
  <el-table-column label="编码" prop="subjectCode" width="140" />
  <el-table-column label="名称" prop="subjectName" width="200" />
  <el-table-column
    v-for="p in periods"
    :key="p"
    :label="formatPeriod(p)"
    align="right"
    min-width="120"
  >
    <template #default="{ row }">
      {{ formatAmount(row.amounts?.[p]) }}
    </template>
  </el-table-column>
  <el-table-column
    :label="yearLabel"
    prop="yearTotal"
    fixed="right"
    align="right"
    min-width="130"
    class-name="col-year-total"
  >
    <template #default="{ row }">
      {{ formatAmount(row.yearTotal) }}
    </template>
  </el-table-column>
</el-table>
```

`summaryMethod`：首格「合计」；月列取 `totals[p]`；末列 `totals.yearTotal`。

工具栏：展开所有级次、导出（复用科目余额 blob download 模式）。

科目输入：文本框默认 `5601,5602,5603`，查询时 `split(/[,，\s]+/)`。

级次下拉：`0` 至末级 / `1` / `2` / `3`。

- [ ] **Step 3: 本地打开页面冒烟（需菜单 Task 9）**

---

### Task 9: 菜单注册

**Files:**
- Create: `sql/seed/expense_detail_menu.sql`
- Update: `sql/seed/README.md`（一行说明如何执行）

- [ ] **Step 1: 编写 seed**

插入 `resources` 一条 MENU：

- name: `费用明细表`
- path / component: `statement/expense-detail`
- parent: 与「科目余额表 / 利润表」同一报表父菜单（查现有 `resources` 中 `statement/subject-balance` 的 `parentId`）
- 同步 `permission` 给 `ROLE_ADMINISTRATORS`（仿其它报表菜单）

```sql
-- 先查出父菜单 ID 与排序，再 INSERT resources + permission
-- resource_id 使用新雪花 ID，避免冲突
```

- [ ] **Step 2: 执行 seed（开发环境）并确认前端动态路由可加载 `statement/expense-detail`**

---

### Task 10: 验收核对清单

- [ ] **后端单测全绿**

```powershell
$env:JAVA_HOME='C:\Program Files\Java\jdk-17'
cd C:\Users\Administrator\Projects\jinbooks\financial-cloud
.\mvnw.cmd -Dtest=StatementExpenseDetailRulesTest,StatementParamsDtoExpenseDetailTest,StatementExpenseDetailServiceTest test
```

- [ ] **对照 spec 验收标准 1–7**（第 8 条辅助核算延后）

| # | 标准 | 验证方式 |
|---|------|----------|
| 1 | 矩阵 + 年度合计列 | UI / API |
| 2 | 末级=凭证净额 | 手算或 Golden |
| 3 | 父级=子级和；底部=一级和 | 单测 + UI |
| 4 | yearTotal=各月和 | 单测 |
| 5 | 导出列一致 | 导出打开 Excel |
| 6 | 零发生额末级隐藏 | 单测 |
| 7 | 与利润表勾稽 | Service 单测 + warnings |

- [ ] **更新 spec 状态**（若全部完成）：`状态：Phase 1 已实现`（仅在实现完成时）

---

## Spec coverage (self-review)

| Spec 要求 | Task |
|-----------|------|
| between 区间 + 动态月份列 | 2, 4, 8 |
| 年度合计列始终显示 | 1, 4, 8 |
| 5601/5602/5603 默认 + Compat | 4 |
| PROFIT_AND_LOSS_AMOUNT | 4 |
| 树 rollup / 零行过滤 / maxLevel | 1, 4 |
| 利润表勾稽 + warnings | 1, 5 |
| API + export | 6, 7 |
| 前端页面 | 8 |
| 菜单 | 9 |
| 24 月上限 | 2 |
| 图表/同比/打印 | 明确不做 |
| 辅助核算展开 | Task 9 备注延后（UI 开关保留） |

## 执行说明

实现时按 Task 1 → 10 顺序；每 Task 结束后跑对应测试再进入下一 Task。  
推荐使用 **subagent-driven-development**（每 Task 新 subagent + 两轮 review）。
