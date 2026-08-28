package com.financial.cloud.service.statement;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.financial.cloud.common.Message;
import com.financial.cloud.constants.system.ConstsSysConfig;
import com.financial.cloud.domain.book.Book;
import com.financial.cloud.domain.book.BookSubject;
import com.financial.cloud.domain.statement.StatementIncome;
import com.financial.cloud.domain.statement.StatementIncomeItem;
import com.financial.cloud.dto.statement.ExpenseReconciliationWarning;
import com.financial.cloud.dto.statement.StatementExpenseDetailItem;
import com.financial.cloud.dto.statement.StatementExpenseDetailReport;
import com.financial.cloud.dto.statement.StatementParamsDto;
import com.financial.cloud.dto.voucher.VoucherItemVo;
import com.financial.cloud.repository.book.BookMapper;
import com.financial.cloud.repository.book.BookSubjectMapper;
import com.financial.cloud.repository.voucher.VoucherItemMapper;
import com.financial.cloud.service.config.ConfigSysService;
import com.financial.cloud.util.StatementExpenseDetailRules;
import com.financial.cloud.util.StatementIncomeRules;
import com.financial.cloud.util.SubjectCodeCompat;
import com.financial.cloud.util.excel.ExcelExporter;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.math.BigDecimal;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

@Slf4j
@RequiredArgsConstructor
@Service
public class StatementExpenseDetailService {

    private static final List<String> DEFAULT_SUBJECT_CODES = List.of("5601", "5602", "5603");

    private final BookSubjectMapper bookSubjectMapper;
    private final BookMapper bookMapper;
    private final VoucherItemMapper voucherItemMapper;
    private final ConfigSysService configSysService;
    private final StatementIncomeService statementIncomeService;

    public Message<StatementExpenseDetailReport> query(StatementParamsDto dto) {
        if (dto.getPostedOnly() == null) {
            dto.setPostedOnly(true);
        }
        if (dto.getSubjectCodes() == null || dto.getSubjectCodes().isEmpty()) {
            dto.setSubjectCodes(DEFAULT_SUBJECT_CODES);
        }
        dto.parse();

        List<String> periods = dto.getAllMonths();
        List<BookSubject> allSubjects = bookSubjectMapper.selectList(
                Wrappers.<BookSubject>lambdaQuery().eq(BookSubject::getBookId, dto.getBookId()));
        List<String> prefixes = lookupPrefixes(expandSubjectCodeRanges(dto.getSubjectCodes()));
        List<VoucherItemVo> rows = voucherItemMapper.selectExpenseAmountByMonth(dto, prefixes);
        Map<String, Map<String, BigDecimal>> amountsByCode = aggregateAmounts(rows);

        List<BookSubject> preferredSubjects = preferExpenseCodingFamily(allSubjects);
        List<StatementExpenseDetailItem> roots = buildTree(preferredSubjects, prefixes, amountsByCode);
        for (StatementExpenseDetailItem root : roots) {
            StatementExpenseDetailRules.rollup(root, periods);
        }
        StatementExpenseDetailRules.filterZeroLeaves(roots, periods);
        roots.removeIf(root -> StatementExpenseDetailRules.isZeroLeaf(root, periods));
        if (dto.getMaxLevel() != null && dto.getMaxLevel() > 0) {
            StatementExpenseDetailRules.truncateLevel(roots, dto.getMaxLevel());
        }

        StatementExpenseDetailReport report = StatementExpenseDetailReport.builder()
                .periods(periods)
                .yearLabel(StatementExpenseDetailRules.yearLabel(periods))
                .items(roots)
                .totals(StatementExpenseDetailRules.computeTotals(roots, periods))
                .reconciliationWarnings(new ArrayList<>())
                .build();
        reconcileWithIncome(dto, report);
        return Message.ok(report);
    }

    public void export(StatementParamsDto dto, HttpServletResponse response) throws IOException {
        StatementExpenseDetailReport report = query(dto).getData();
        Book book = bookMapper.selectById(dto.getBookId());
        try (Workbook workbook = new XSSFWorkbook()) {
            Sheet sheet = workbook.createSheet("费用明细表");
            int rowIndex = 0;
            Row title = sheet.createRow(rowIndex++);
            title.createCell(0).setCellValue(book.getName() + " — 费用明细表");

            Row header = sheet.createRow(rowIndex++);
            int columnIndex = 0;
            header.createCell(columnIndex++).setCellValue("编码");
            header.createCell(columnIndex++).setCellValue("名称");
            for (String period : report.getPeriods()) {
                header.createCell(columnIndex++).setCellValue(formatPeriodLabel(period));
            }
            header.createCell(columnIndex).setCellValue(report.getYearLabel());

            rowIndex = writeItems(sheet, report.getItems(), report.getPeriods(), rowIndex);
            Row totals = sheet.createRow(rowIndex);
            totals.createCell(0).setCellValue("合计");
            totals.createCell(1).setCellValue("");
            columnIndex = 2;
            for (String period : report.getPeriods()) {
                setAmount(totals, columnIndex++, report.getTotals().get(period));
            }
            setAmount(totals, columnIndex, report.getTotals().get(StatementExpenseDetailRules.YEAR_TOTAL_KEY));

            for (int column = 0; column <= columnIndex; column++) {
                sheet.autoSizeColumn(column);
            }

            response.setContentType(ExcelExporter.APPLICATION_MS_EXCEL);
            String fileName = "费用明细表_" + book.getName() + "_"
                    + dto.getDateRange()[0] + "-" + dto.getDateRange()[1] + ".xlsx";
            response.setHeader("Content-Disposition", "attachment; filename="
                    + URLEncoder.encode(fileName, StandardCharsets.UTF_8));
            workbook.write(response.getOutputStream());
            response.getOutputStream().flush();
        }
    }

    private static int writeItems(
            Sheet sheet,
            List<StatementExpenseDetailItem> items,
            List<String> periods,
            int rowIndex) {
        if (items == null) {
            return rowIndex;
        }
        for (StatementExpenseDetailItem item : items) {
            Row row = sheet.createRow(rowIndex++);
            row.createCell(0).setCellValue(item.getSubjectCode());
            row.createCell(1).setCellValue(item.getSubjectName());
            int columnIndex = 2;
            for (String period : periods) {
                setAmount(row, columnIndex++, item.getAmounts().get(period));
            }
            setAmount(row, columnIndex, item.getYearTotal());
            rowIndex = writeItems(sheet, item.getChildren(), periods, rowIndex);
        }
        return rowIndex;
    }

    private static void setAmount(Row row, int columnIndex, BigDecimal amount) {
        row.createCell(columnIndex).setCellValue(defaultZero(amount).doubleValue());
    }

    private static String formatPeriodLabel(String period) {
        String[] parts = period.split("-");
        return parts[0] + "年" + Integer.parseInt(parts[1]) + "期";
    }

    private void reconcileWithIncome(StatementParamsDto dto, StatementExpenseDetailReport report) {
        Map<String, String> config = configSysService.getBookConfigMap(dto.getBookId());
        Map<String, String> codeToItem = new LinkedHashMap<>();
        codeToItem.put("5601", config.get(ConstsSysConfig.SYS_DEFAULT_SELLING_EXPENSES));
        codeToItem.put("5602", config.get(ConstsSysConfig.SYS_DEFAULT_ADMINISTRATIVE_EXPENSES));
        codeToItem.put("5603", config.get(ConstsSysConfig.SYS_DEFAULT_FINANCIAL_EXPENSES));

        Map<String, Map<String, BigDecimal>> detail = new LinkedHashMap<>();
        for (String topCode : DEFAULT_SUBJECT_CODES) {
            Map<String, BigDecimal> periodAmounts = new LinkedHashMap<>();
            for (String period : report.getPeriods()) {
                periodAmounts.put(period, BigDecimal.ZERO);
            }
            detail.put(topCode, periodAmounts);
        }
        for (StatementExpenseDetailItem root : report.getItems()) {
            String topCode = topExpenseCode(root.getSubjectCode());
            if (topCode == null) {
                continue;
            }
            Map<String, BigDecimal> periodAmounts = detail.get(topCode);
            for (Map.Entry<String, BigDecimal> amount : root.getAmounts().entrySet()) {
                periodAmounts.merge(amount.getKey(), defaultZero(amount.getValue()), BigDecimal::add);
            }
        }

        Map<String, Map<String, BigDecimal>> income = new LinkedHashMap<>();
        for (String period : report.getPeriods()) {
            StatementParamsDto monthDto = StatementParamsDto.builder()
                    .bookId(dto.getBookId())
                    .periodType("month")
                    .reportDate(period)
                    .postedOnly(true)
                    .build();
            monthDto.parse();
            Message<StatementIncome> incomeMessage =
                    statementIncomeService.generateIncomeStatement(monthDto, false);
            List<StatementIncomeItem> incomeItems = incomeMessage == null
                    || incomeMessage.getData() == null
                    || incomeMessage.getData().getItems() == null
                    ? List.of() : incomeMessage.getData().getItems();
            for (Map.Entry<String, String> entry : codeToItem.entrySet()) {
                BigDecimal currentBalance = BigDecimal.ZERO;
                if (StringUtils.isBlank(entry.getValue())) {
                    log.warn("Expense detail reconciliation config missing: bookId={} subjectCode={}",
                            dto.getBookId(), entry.getKey());
                } else {
                    currentBalance = incomeItems.stream()
                            .filter(item -> entry.getValue().equals(item.getItemCode()))
                            .map(StatementIncomeItem::getCurrentBalance)
                            .findFirst()
                            .orElse(BigDecimal.ZERO);
                }
                income.computeIfAbsent(entry.getKey(), ignored -> new LinkedHashMap<>())
                        .put(period, defaultZero(currentBalance));
            }
        }

        List<ExpenseReconciliationWarning> warnings = StatementExpenseDetailRules.reconcile(
                detail, income, StatementIncomeRules.FORMULA_TOLERANCE);
        for (ExpenseReconciliationWarning warning : warnings) {
            log.warn("Expense detail vs income: {} {} detail={} income={} diff={}",
                    warning.getSubjectCode(), warning.getPeriod(), warning.getDetailAmount(),
                    warning.getIncomeAmount(), warning.getDiff());
        }
        report.setReconciliationWarnings(warnings);
    }

    private static String topExpenseCode(String subjectCode) {
        String mappedCode = SubjectCodeCompat.mapIncomeRuleSubject(subjectCode);
        if (StringUtils.isBlank(mappedCode)) {
            return null;
        }
        for (String expenseCode : DEFAULT_SUBJECT_CODES) {
            if (mappedCode.startsWith(expenseCode)) {
                return expenseCode;
            }
        }
        return null;
    }

    private static BigDecimal defaultZero(BigDecimal amount) {
        return amount == null ? BigDecimal.ZERO : amount;
    }

    private static List<String> expandSubjectCodeRanges(List<String> subjectCodes) {
        List<String> expanded = new ArrayList<>();
        if (subjectCodes == null) {
            return expanded;
        }
        for (String token : subjectCodes) {
            String trimmed = StringUtils.trimToEmpty(token);
            if (!trimmed.matches("^\\d+-\\d+$")) {
                expanded.add(trimmed);
                continue;
            }
            String[] bounds = trimmed.split("-", 2);
            try {
                int start = Integer.parseInt(bounds[0]);
                int end = Integer.parseInt(bounds[1]);
                if (start <= end) {
                    for (int code = start; ; code++) {
                        expanded.add(Integer.toString(code));
                        if (code == end) {
                            break;
                        }
                    }
                }
            } catch (NumberFormatException ignored) {
                expanded.add(trimmed);
            }
        }
        return expanded;
    }

    private static List<String> lookupPrefixes(List<String> subjectCodes) {
        LinkedHashSet<String> prefixes = new LinkedHashSet<>();
        for (String code : SubjectCodeCompat.expandLookupCodes(subjectCodes)) {
            if (StringUtils.isNotBlank(code)) {
                String trimmedCode = code.trim();
                prefixes.add(trimmedCode);
                String enterpriseExpenseCode = expenseAliasCounterpart(trimmedCode);
                if (enterpriseExpenseCode != null) {
                    prefixes.add(enterpriseExpenseCode);
                }
            }
        }
        return new ArrayList<>(prefixes);
    }

    private static String expenseAliasCounterpart(String code) {
        return switch (code) {
            case "5601" -> "6601";
            case "6601" -> "5601";
            case "5602" -> "6602";
            case "6602" -> "5602";
            case "5603" -> "6603";
            case "6603" -> "5603";
            default -> null;
        };
    }

    private static Map<String, Map<String, BigDecimal>> aggregateAmounts(List<VoucherItemVo> rows) {
        Map<String, Map<String, BigDecimal>> amountsByCode = new LinkedHashMap<>();
        if (rows == null) {
            return amountsByCode;
        }
        for (VoucherItemVo row : rows) {
            if (row == null || StringUtils.isBlank(row.getSubjectCode())
                    || StringUtils.isBlank(row.getYearPeriod())) {
                continue;
            }
            BigDecimal amount = StatementIncomeRules.normalizePeriodAmount(
                    row.getDebitAmount(),
                    row.getCreditAmount(),
                    StatementIncomeRules.PROFIT_AND_LOSS_AMOUNT);
            amountsByCode.computeIfAbsent(row.getSubjectCode(), ignored -> new HashMap<>())
                    .merge(row.getYearPeriod(), amount, BigDecimal::add);
        }
        return amountsByCode;
    }

    private static List<BookSubject> preferExpenseCodingFamily(List<BookSubject> subjects) {
        if (subjects == null || subjects.isEmpty()) {
            return subjects;
        }
        boolean preferSmallBusiness = subjects.stream()
                .filter(java.util.Objects::nonNull)
                .map(BookSubject::getCode)
                .anyMatch(StatementExpenseDetailService::isSmallBusinessExpenseCode);
        String excludedPrefix = preferSmallBusiness ? "66" : "56";
        return subjects.stream()
                .filter(java.util.Objects::nonNull)
                .filter(subject -> !isExpenseFamilyCode(subject.getCode(), excludedPrefix))
                .toList();
    }

    private static boolean isSmallBusinessExpenseCode(String code) {
        return isExpenseFamilyCode(code, "56");
    }

    private static boolean isExpenseFamilyCode(String code, String familyPrefix) {
        return StringUtils.isNotBlank(code)
                && (code.startsWith(familyPrefix + "01")
                || code.startsWith(familyPrefix + "02")
                || code.startsWith(familyPrefix + "03"));
    }

    private static List<StatementExpenseDetailItem> buildTree(
            List<BookSubject> subjects,
            List<String> prefixes,
            Map<String, Map<String, BigDecimal>> amountsByCode) {
        if (subjects == null || subjects.isEmpty()) {
            return new ArrayList<>();
        }
        Map<String, BookSubject> subjectsById = new LinkedHashMap<>();
        for (BookSubject subject : subjects) {
            if (subject != null && subject.getId() != null) {
                subjectsById.put(subject.getId(), subject);
            }
        }

        Set<String> includedIds = new LinkedHashSet<>();
        for (BookSubject subject : subjects) {
            if (subject != null && matchesAnyPrefix(subject.getCode(), prefixes)) {
                includeWithAncestors(subject, subjectsById, includedIds);
            }
        }

        Map<String, StatementExpenseDetailItem> itemsById = new LinkedHashMap<>();
        for (BookSubject subject : subjects) {
            if (subject == null || !includedIds.contains(subject.getId())) {
                continue;
            }
            itemsById.put(subject.getId(), StatementExpenseDetailItem.builder()
                    .sourceId(subject.getId())
                    .parentId(subject.getParentId())
                    .subjectCode(subject.getCode())
                    .subjectName(subject.getName())
                    .level(subject.getLevel())
                    .amounts(new HashMap<>())
                    .yearTotal(BigDecimal.ZERO)
                    .children(new ArrayList<>())
                    .build());
        }

        List<StatementExpenseDetailItem> roots = new ArrayList<>();
        for (StatementExpenseDetailItem item : itemsById.values()) {
            StatementExpenseDetailItem parent = itemsById.get(item.getParentId());
            if (parent == null) {
                roots.add(item);
            } else {
                parent.getChildren().add(item);
            }
        }
        for (StatementExpenseDetailItem item : itemsById.values()) {
            if (item.getChildren().isEmpty()) {
                item.setAmounts(findAmounts(item.getSubjectCode(), amountsByCode));
            }
        }
        return roots;
    }

    private static boolean matchesAnyPrefix(String subjectCode, List<String> prefixes) {
        if (StringUtils.isBlank(subjectCode)) {
            return false;
        }
        for (String prefix : prefixes) {
            if (subjectCode.startsWith(prefix)
                    || SubjectCodeCompat.incomeRuleMatchesVoucherSubject(prefix, subjectCode)) {
                return true;
            }
        }
        return false;
    }

    private static void includeWithAncestors(
            BookSubject subject,
            Map<String, BookSubject> subjectsById,
            Set<String> includedIds) {
        BookSubject current = subject;
        while (current != null && current.getId() != null && includedIds.add(current.getId())) {
            current = subjectsById.get(current.getParentId());
        }
    }

    private static Map<String, BigDecimal> findAmounts(
            String subjectCode,
            Map<String, Map<String, BigDecimal>> amountsByCode) {
        Map<String, BigDecimal> merged = new HashMap<>();
        for (Map.Entry<String, Map<String, BigDecimal>> entry : amountsByCode.entrySet()) {
            if (subjectCode.equals(entry.getKey())
                    || canonicalExpenseCode(subjectCode).equals(canonicalExpenseCode(entry.getKey()))
                    || SubjectCodeCompat.incomeRuleMatchesVoucherSubject(subjectCode, entry.getKey())
                    || SubjectCodeCompat.incomeRuleMatchesVoucherSubject(entry.getKey(), subjectCode)) {
                for (Map.Entry<String, BigDecimal> amount : entry.getValue().entrySet()) {
                    merged.merge(amount.getKey(), amount.getValue(), BigDecimal::add);
                }
            }
        }
        return merged;
    }

    private static String canonicalExpenseCode(String code) {
        if (StringUtils.isBlank(code)) {
            return StringUtils.defaultString(code);
        }
        if (code.startsWith("6601") || code.startsWith("6602") || code.startsWith("6603")) {
            return "56" + code.substring(2);
        }
        return code;
    }
}
