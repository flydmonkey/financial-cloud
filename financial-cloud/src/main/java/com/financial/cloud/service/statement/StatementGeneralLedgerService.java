package com.financial.cloud.service.statement;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.financial.cloud.common.Message;
import com.financial.cloud.domain.book.Book;
import com.financial.cloud.domain.book.BookSubject;
import com.financial.cloud.domain.statement.StatementSubjectBalance;
import com.financial.cloud.dto.statement.StatementGeneralLedgerItem;
import com.financial.cloud.dto.statement.StatementGeneralLedgerReport;
import com.financial.cloud.dto.statement.StatementParamsDto;
import com.financial.cloud.dto.voucher.VoucherItemVo;
import com.financial.cloud.enums.common.YesNoEnum;
import com.financial.cloud.repository.book.BookMapper;
import com.financial.cloud.repository.book.BookSubjectMapper;
import com.financial.cloud.repository.statement.StatementSubjectBalanceMapper;
import com.financial.cloud.repository.voucher.VoucherItemMapper;
import com.financial.cloud.util.StatementGeneralLedgerRules;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.beans.BeanUtils;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.math.BigDecimal;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.time.YearMonth;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

/**
 * 总账：已过账凭证实时汇总发生额 + 期初结转；默认一级科目；含试算平衡。
 */
@Slf4j
@RequiredArgsConstructor
@Service
public class StatementGeneralLedgerService {

    private final StatementSubjectBalanceMapper subjectBalanceMapper;
    private final BookSubjectMapper bookSubjectMapper;
    private final BookMapper bookMapper;
    private final VoucherItemMapper voucherItemMapper;

    public Message<StatementGeneralLedgerReport> query(StatementParamsDto dto) {
        applyDefaults(dto);
        dto.parse();
        List<String> months = dto.getAllMonths();
        String firstPeriod = months.get(0);
        String lastPeriod = months.get(months.size() - 1);

        List<BookSubject> allSubjects = bookSubjectMapper.selectList(
                Wrappers.<BookSubject>lambdaQuery().eq(BookSubject::getBookId, dto.getBookId()));
        Map<String, BookSubject> byId = allSubjects.stream()
                .filter(s -> StringUtils.isNotBlank(s.getId()))
                .collect(Collectors.toMap(BookSubject::getId, s -> s, (a, b) -> a));
        Map<String, BookSubject> byCode = allSubjects.stream()
                .filter(s -> StringUtils.isNotBlank(s.getCode()))
                .collect(Collectors.toMap(BookSubject::getCode, s -> s, (a, b) -> a));

        int maxLevelCfg = dto.getMaxLevel() == null ? 1 : dto.getMaxLevel();
        List<BookSubject> displaySubjects;
        if (maxLevelCfg <= 0) {
            // 至末级：仅无子科目的末级
            displaySubjects = allSubjects.stream()
                    .filter(s -> StringUtils.isNotBlank(s.getCode()))
                    .filter(s -> allSubjects.stream().noneMatch(c -> s.getId().equals(c.getParentId())))
                    .filter(s -> inCodeRange(s.getCode(), dto.getSubjectCodeFrom(), dto.getSubjectCodeTo()))
                    .sorted(Comparator.comparing(BookSubject::getCode, Comparator.nullsLast(String::compareTo)))
                    .toList();
        } else {
            displaySubjects = allSubjects.stream()
                    .filter(s -> s.getLevel() != null && s.getLevel() > 0 && s.getLevel() <= maxLevelCfg)
                    .filter(s -> inCodeRange(s.getCode(), dto.getSubjectCodeFrom(), dto.getSubjectCodeTo()))
                    .sorted(Comparator.comparing(BookSubject::getCode, Comparator.nullsLast(String::compareTo)))
                    .toList();
        }

        // 期初：取区间首月快照 opening，rollup 到展示级（上月结转口径）
        Map<String, Amount> openings = loadOpeningsRolled(dto.getBookId(), firstPeriod, displaySubjects, byId, byCode);

        // 本期 / 本年累计：已过账凭证实时汇总后 rollup
        Map<String, Amount> periodAmounts = rollupVoucherAmounts(
                fetchPostedAmounts(dto, dto.getDateRangeStart(), dto.getDateRangeEnd()),
                displaySubjects, byId, byCode);
        YearMonth endYm = YearMonth.parse(lastPeriod);
        String ytdStart = endYm.getYear() + "-01-01";
        Map<String, Amount> ytdAmounts = rollupVoucherAmounts(
                fetchPostedAmounts(dto, ytdStart, dto.getDateRangeEnd()),
                displaySubjects, byId, byCode);

        boolean hideZero = Boolean.TRUE.equals(dto.getHideZeroBalance());
        boolean hideNoActZero = !Boolean.FALSE.equals(dto.getHideNoActivityAndZeroBalance());
        boolean hidePeriodRows = Boolean.TRUE.equals(dto.getHidePeriodRowsWhenNoActivity());

        List<StatementGeneralLedgerItem> items = new ArrayList<>();
        int subjectCount = 0;
        List<String> warnings = new ArrayList<>();

        for (BookSubject subject : displaySubjects) {
            String code = subject.getCode();
            Amount open = openings.getOrDefault(code, Amount.ZERO);
            Amount period = periodAmounts.getOrDefault(code, Amount.ZERO);
            Amount ytd = ytdAmounts.getOrDefault(code, Amount.ZERO);

            StatementGeneralLedgerRules.FoldedBalance folded = StatementGeneralLedgerRules.fromParts(
                    open.debit, open.credit,
                    period.debit, period.credit,
                    ytd.debit, ytd.credit,
                    subject.getDirection());

            if (StatementGeneralLedgerRules.shouldHideGroup(folded, hideZero, hideNoActZero)) {
                continue;
            }
            List<StatementGeneralLedgerItem> rows = StatementGeneralLedgerRules.expandRows(
                    code, subject.getName(), lastPeriod, folded, hidePeriodRows);
            items.addAll(rows);
            subjectCount++;
        }

        // 试算只合计「展示集中的叶子」（避免父+子重复）
        java.util.Set<String> nonLeafCodes = displaySubjects.stream()
                .filter(s -> displaySubjects.stream().anyMatch(c -> s.getId().equals(c.getParentId())))
                .map(BookSubject::getCode)
                .collect(Collectors.toSet());
        List<StatementGeneralLedgerItem> trialRows = items.stream()
                .filter(i -> !nonLeafCodes.contains(i.getSubjectCode()))
                .toList();
        StatementGeneralLedgerRules.TrialBalance tb = StatementGeneralLedgerRules.trialBalance(trialRows);
        if (!tb.balanced) {
            if (!tb.periodBalanced) {
                warnings.add(String.format(
                        "试算不平衡：本期借方合计 %s ≠ 贷方合计 %s",
                        tb.periodDebitTotal.toPlainString(), tb.periodCreditTotal.toPlainString()));
            }
            if (!tb.balanceBalanced) {
                warnings.add(String.format(
                        "试算不平衡：期末借方余额合计 %s ≠ 贷方余额合计 %s",
                        tb.closingDebitTotal.toPlainString(), tb.closingCreditTotal.toPlainString()));
            }
        }

        // 账证同源：本期发生额直接由已过账凭证汇总派生
        if (tb.balanced) {
            warnings.add("试算平衡通过；本期/本年累计来自已过账凭证，期初来自区间首月结转。");
        } else {
            warnings.add("取数口径：本期/本年累计来自已过账凭证实时汇总；期初来自区间首月结转快照。");
        }
        return Message.ok(StatementGeneralLedgerReport.builder()
                .items(items)
                .subjectCount(subjectCount)
                .trialBalanced(tb.balanced)
                .periodTrialBalanced(tb.periodBalanced)
                .balanceTrialBalanced(tb.balanceBalanced)
                .periodDebitTotal(tb.periodDebitTotal)
                .periodCreditTotal(tb.periodCreditTotal)
                .closingDebitTotal(tb.closingDebitTotal)
                .closingCreditTotal(tb.closingCreditTotal)
                .warnings(warnings)
                .build());
    }

    public void export(StatementParamsDto dto, HttpServletResponse response) throws IOException {
        StatementGeneralLedgerReport report = query(dto).getData();
        Book book = bookMapper.selectById(dto.getBookId());
        String bookName = book != null ? book.getName() : "";
        try (Workbook workbook = new XSSFWorkbook()) {
            Sheet sheet = workbook.createSheet("总账");
            Row header = sheet.createRow(0);
            String[] titles = {"科目编码", "科目名称", "期间", "摘要", "借方", "贷方", "方向", "余额"};
            for (int i = 0; i < titles.length; i++) {
                header.createCell(i).setCellValue(titles[i]);
            }
            int r = 1;
            for (StatementGeneralLedgerItem item : report.getItems()) {
                Row row = sheet.createRow(r++);
                row.createCell(0).setCellValue(nullToEmpty(item.getSubjectCode()));
                row.createCell(1).setCellValue(nullToEmpty(item.getSubjectName()));
                row.createCell(2).setCellValue(nullToEmpty(item.getPeriod()));
                row.createCell(3).setCellValue(nullToEmpty(item.getSummary()));
                setAmount(row, 4, item.getDebit());
                setAmount(row, 5, item.getCredit());
                row.createCell(6).setCellValue(nullToEmpty(item.getDirection()));
                setAmount(row, 7, item.getBalance());
            }
            String fileName = URLEncoder.encode(bookName + "-总账.xlsx", StandardCharsets.UTF_8)
                    .replace("+", "%20");
            response.setContentType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet");
            response.setHeader("Content-Disposition", "attachment; filename*=UTF-8''" + fileName);
            workbook.write(response.getOutputStream());
        }
    }

    private List<VoucherItemVo> fetchPostedAmounts(StatementParamsDto base, String start, String end) {
        StatementParamsDto q = new StatementParamsDto();
        BeanUtils.copyProperties(base, q);
        q.setPostedOnly(true);
        q.setDateRangeStart(start);
        q.setDateRangeEnd(end);
        if (StringUtils.isBlank(q.getCountType())) {
            q.setCountType("SUM");
        }
        return voucherItemMapper.selectSubjectAmount(q);
    }

    private Map<String, Amount> rollupVoucherAmounts(
            List<VoucherItemVo> rows,
            List<BookSubject> displaySubjects,
            Map<String, BookSubject> byId,
            Map<String, BookSubject> byCode) {
        Map<String, Amount> result = new HashMap<>();
        Map<String, BookSubject> displayByCode = displaySubjects.stream()
                .collect(Collectors.toMap(BookSubject::getCode, s -> s, (a, b) -> a, LinkedHashMap::new));

        for (VoucherItemVo row : rows) {
            if (row == null || StringUtils.isBlank(row.getSubjectCode())) {
                continue;
            }
            BookSubject leaf = byCode.get(row.getSubjectCode());
            BookSubject mapped = mapToDisplay(leaf, row.getSubjectCode(), displayByCode, byId, byCode);
            if (mapped == null) {
                continue;
            }
            Amount amt = result.computeIfAbsent(mapped.getCode(), k -> new Amount());
            amt.debit = amt.debit.add(nz(row.getDebitAmount()));
            amt.credit = amt.credit.add(nz(row.getCreditAmount()));
        }
        return result;
    }

    private BookSubject mapToDisplay(
            BookSubject leaf,
            String rawCode,
            Map<String, BookSubject> displayByCode,
            Map<String, BookSubject> byId,
            Map<String, BookSubject> byCode) {
        if (leaf != null) {
            BookSubject cur = leaf;
            while (cur != null) {
                if (displayByCode.containsKey(cur.getCode())) {
                    return cur;
                }
                if (StringUtils.isBlank(cur.getParentId())) {
                    break;
                }
                cur = byId.get(cur.getParentId());
            }
        }
        // 编码前缀回退：最长匹配展示科目
        String best = null;
        for (String code : displayByCode.keySet()) {
            if (rawCode.equals(code) || rawCode.startsWith(code)) {
                if (best == null || code.length() > best.length()) {
                    best = code;
                }
            }
        }
        return best == null ? null : displayByCode.get(best);
    }

    private Map<String, Amount> loadOpeningsRolled(
            String bookId,
            String firstPeriod,
            List<BookSubject> displaySubjects,
            Map<String, BookSubject> byId,
            Map<String, BookSubject> byCode) {
        List<StatementSubjectBalance> rows = subjectBalanceMapper.selectList(
                Wrappers.<StatementSubjectBalance>lambdaQuery()
                        .eq(StatementSubjectBalance::getBookId, bookId)
                        .eq(StatementSubjectBalance::getYearPeriod, firstPeriod)
                        .and(w -> w.isNull(StatementSubjectBalance::getIsAuxiliary)
                                .or()
                                .eq(StatementSubjectBalance::getIsAuxiliary, YesNoEnum.n.name())));

        Map<String, BookSubject> displayByCode = displaySubjects.stream()
                .collect(Collectors.toMap(BookSubject::getCode, s -> s, (a, b) -> a, LinkedHashMap::new));
        Map<String, Amount> result = new HashMap<>();
        for (BookSubject s : displaySubjects) {
            result.put(s.getCode(), new Amount());
        }
        for (StatementSubjectBalance row : rows) {
            if (row == null || StringUtils.isBlank(row.getSubjectCode())) {
                continue;
            }
            // 有下级余额行时只取末级，避免与父级快照重复加总
            if (hasChildBalance(row.getSubjectCode(), rows)) {
                continue;
            }
            BookSubject leaf = byCode.get(row.getSubjectCode());
            BookSubject mapped = mapToDisplay(leaf, row.getSubjectCode(), displayByCode, byId, byCode);
            if (mapped == null) {
                continue;
            }
            Amount amt = result.computeIfAbsent(mapped.getCode(), k -> new Amount());
            amt.debit = amt.debit.add(nz(row.getOpeningBalanceDebit()));
            amt.credit = amt.credit.add(nz(row.getOpeningBalanceCredit()));
        }
        return result;
    }

    private static boolean hasChildBalance(String parentCode, List<StatementSubjectBalance> rows) {
        return rows.stream().anyMatch(r -> r.getSubjectCode() != null
                && !r.getSubjectCode().equals(parentCode)
                && r.getSubjectCode().startsWith(parentCode));
    }

    private static void applyDefaults(StatementParamsDto dto) {
        if (StringUtils.isBlank(dto.getPeriodType())) {
            dto.setPeriodType("between");
        }
        if (dto.getHideNoActivityAndZeroBalance() == null) {
            dto.setHideNoActivityAndZeroBalance(true);
        }
        if (dto.getHideZeroBalance() == null) {
            dto.setHideZeroBalance(false);
        }
        if (dto.getHidePeriodRowsWhenNoActivity() == null) {
            dto.setHidePeriodRowsWhenNoActivity(false);
        }
        if (dto.getShowAux() == null) {
            dto.setShowAux(false);
        }
        // 规则一：默认仅一级科目
        if (dto.getMaxLevel() == null) {
            dto.setMaxLevel(1);
        }
        if (dto.getPostedOnly() == null) {
            dto.setPostedOnly(true);
        }
    }

    private static boolean inCodeRange(String code, String from, String to) {
        if (StringUtils.isBlank(code)) {
            return false;
        }
        if (StringUtils.isNotBlank(from) && code.compareTo(from) < 0) {
            return false;
        }
        if (StringUtils.isNotBlank(to) && code.compareTo(to) > 0) {
            return false;
        }
        return true;
    }

    private static void setAmount(Row row, int col, BigDecimal amount) {
        if (amount == null) {
            row.createCell(col).setCellValue("");
        } else {
            row.createCell(col).setCellValue(amount.doubleValue());
        }
    }

    private static String nullToEmpty(String v) {
        return Objects.toString(v, "");
    }

    private static BigDecimal nz(BigDecimal v) {
        return v == null ? BigDecimal.ZERO : v;
    }

    private static final class Amount {
        static final Amount ZERO = new Amount();
        BigDecimal debit = BigDecimal.ZERO;
        BigDecimal credit = BigDecimal.ZERO;
    }
}
