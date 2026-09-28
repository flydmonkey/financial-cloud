package com.financial.cloud.service.voucher;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.financial.cloud.domain.book.BookSubject;
import com.financial.cloud.dto.voucher.MultiColumnLedgerVo;
import com.financial.cloud.dto.voucher.VoucherItemVo;
import com.financial.cloud.enums.book.SubjectDirectionEnum;
import com.financial.cloud.exception.BusinessException;
import com.financial.cloud.repository.book.BookSubjectMapper;
import com.financial.cloud.repository.voucher.VoucherItemMapper;
import lombok.RequiredArgsConstructor;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * 多栏账：以某科目为栏母、其直接子科目为栏位，逐凭证展开各栏净额。
 * 数据源与明细账一致（已过账凭证分录），栏位净额 = 按栏母方向取「借-贷」或「贷-借」。
 */
@Service
@RequiredArgsConstructor
public class MultiColumnLedgerService {

    private final VoucherItemMapper voucherItemMapper;
    private final BookSubjectMapper bookSubjectMapper;

    public MultiColumnLedgerVo query(String bookId, String subjectCode, String startDate, String endDate) {
        if (StringUtils.isBlank(subjectCode)) {
            throw new BusinessException(400, "请选择栏母科目");
        }

        List<BookSubject> subjects = bookSubjectMapper.selectList(
                Wrappers.<BookSubject>lambdaQuery().eq(BookSubject::getBookId, bookId));
        Map<String, BookSubject> byCode = subjects.stream()
                .collect(Collectors.toMap(BookSubject::getCode, s -> s, (a, b) -> a));
        BookSubject parent = byCode.get(subjectCode);
        if (parent == null) {
            throw new BusinessException(404, "科目不存在或不属于当前账套");
        }
        boolean creditNature = SubjectDirectionEnum.CREDIT.getValue().equals(parent.getDirection());

        // 栏位：账簿中的直接子科目（按编码排序）
        List<BookSubject> childSubjects = subjects.stream()
                .filter(s -> parent.getId().equals(s.getParentId()))
                .sorted(Comparator.comparing(BookSubject::getCode))
                .toList();
        Map<String, String> childNames = childSubjects.stream()
                .collect(Collectors.toMap(BookSubject::getCode, BookSubject::getName, (a, b) -> a));

        // 期初：起始日之前的净额累计
        BigDecimal opening = BigDecimal.ZERO;
        if (StringUtils.isNotBlank(startDate)) {
            String beforeStart = LocalDate.parse(startDate).minusDays(1).toString();
            List<VoucherItemVo> openingItems = voucherItemMapper.multiColumnLedgerItems(
                    bookId, subjectCode, null, beforeStart);
            for (VoucherItemVo item : openingItems) {
                opening = opening.add(netAmount(item, creditNature));
            }
        }

        // 本期分录
        List<VoucherItemVo> items = voucherItemMapper.multiColumnLedgerItems(
                bookId, subjectCode, startDate, endDate);

        // 逐凭证聚合（保持 SQL 排序）
        Map<String, MultiColumnLedgerVo.Row> rowByVoucher = new LinkedHashMap<>();
        Map<String, BigDecimal> columnTotals = new LinkedHashMap<>();
        BigDecimal balance = opening;
        for (VoucherItemVo item : items) {
            MultiColumnLedgerVo.Row row = rowByVoucher.computeIfAbsent(item.getVoucherId(), id ->
                    MultiColumnLedgerVo.Row.builder()
                            .voucherId(id)
                            .voucherDate(item.getVoucherDate() != null
                                    ? new java.text.SimpleDateFormat("yyyy-MM-dd").format(item.getVoucherDate()) : "")
                            .word(StringUtils.defaultString(item.getWord()))
                            .summary(StringUtils.defaultString(item.getSummary()))
                            .amounts(new LinkedHashMap<>())
                            .total(BigDecimal.ZERO)
                            .balance(BigDecimal.ZERO)
                            .build());
            if (StringUtils.isBlank(row.getSummary()) && StringUtils.isNotBlank(item.getSummary())) {
                row.setSummary(item.getSummary());
            }

            String columnCode = directChildCode(subjectCode, item.getSubjectCode());
            if (columnCode == null) {
                columnCode = subjectCode;
            }
            BigDecimal net = netAmount(item, creditNature);
            row.getAmounts().merge(columnCode, net, BigDecimal::add);
            row.setTotal(row.getTotal().add(net));
            columnTotals.merge(columnCode, net, BigDecimal::add);
            // 列名兜底：账簿里找不到的子科目用分录上的名称
            childNames.putIfAbsent(columnCode, item.getSubjectName());
        }
        // 滚动余额
        List<MultiColumnLedgerVo.Row> rows = new ArrayList<>(rowByVoucher.values());
        for (MultiColumnLedgerVo.Row row : rows) {
            balance = balance.add(row.getTotal());
            row.setBalance(balance);
        }

        // 栏位清单：账簿直接子科目 + 实际出现但不在账簿的编码 + 本级栏（如有）
        List<MultiColumnLedgerVo.Column> columns = new ArrayList<>();
        for (BookSubject child : childSubjects) {
            columns.add(MultiColumnLedgerVo.Column.builder()
                    .code(child.getCode()).name(child.getName()).selfColumn(false).build());
        }
        List<String> knownCodes = columns.stream().map(MultiColumnLedgerVo.Column::getCode).toList();
        for (String code : columnTotals.keySet()) {
            if (!knownCodes.contains(code) && !code.equals(subjectCode)) {
                columns.add(MultiColumnLedgerVo.Column.builder()
                        .code(code).name(childNames.getOrDefault(code, code)).selfColumn(false).build());
            }
        }
        if (columnTotals.containsKey(subjectCode)) {
            columns.add(MultiColumnLedgerVo.Column.builder()
                    .code(subjectCode).name(parent.getName() + "（本级）").selfColumn(true).build());
        }

        BigDecimal periodTotal = columnTotals.values().stream()
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        return MultiColumnLedgerVo.builder()
                .subjectCode(subjectCode)
                .subjectName(parent.getName())
                .direction(parent.getDirection())
                .startDate(startDate)
                .endDate(endDate)
                .openingBalance(opening)
                .closingBalance(opening.add(periodTotal))
                .columns(columns)
                .rows(rows)
                .columnTotals(columnTotals)
                .periodTotal(periodTotal)
                .build();
    }

    /** 栏位净额：借方栏母取 借-贷，贷方栏母取 贷-借 */
    static BigDecimal netAmount(VoucherItemVo item, boolean creditNature) {
        BigDecimal debit = item.getDebitAmount() != null ? item.getDebitAmount() : BigDecimal.ZERO;
        BigDecimal credit = item.getCreditAmount() != null ? item.getCreditAmount() : BigDecimal.ZERO;
        return creditNature ? credit.subtract(debit) : debit.subtract(credit);
    }

    /**
     * 取分录科目相对栏母的直接子编码：
     * 点分制（5602.01.02 → 5602.01）、拼接制（66020103 → 660201）、等于栏母 → 栏母自身（本级栏）。
     */
    static String directChildCode(String parentCode, String code) {
        if (StringUtils.isBlank(code) || StringUtils.isBlank(parentCode)) {
            return null;
        }
        if (code.equals(parentCode)) {
            return parentCode;
        }
        if (!code.startsWith(parentCode)) {
            return null;
        }
        String rest = code.substring(parentCode.length());
        if (rest.startsWith(".")) {
            String segment = rest.substring(1);
            int dot = segment.indexOf('.');
            return parentCode + "." + (dot >= 0 ? segment.substring(0, dot) : segment);
        }
        // 拼接制子编码：取栏母后再两位（如 660201）
        return code.substring(0, Math.min(code.length(), parentCode.length() + 2));
    }
}
