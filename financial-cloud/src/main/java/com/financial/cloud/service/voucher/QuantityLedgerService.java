package com.financial.cloud.service.voucher;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.financial.cloud.domain.book.BookSubject;
import com.financial.cloud.dto.voucher.QuantityLedgerVo;
import com.financial.cloud.dto.voucher.VoucherItemVo;
import com.financial.cloud.enums.book.SubjectDirectionEnum;
import com.financial.cloud.exception.BusinessException;
import com.financial.cloud.repository.book.BookSubjectMapper;
import com.financial.cloud.repository.voucher.VoucherItemMapper;
import lombok.RequiredArgsConstructor;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

/**
 * 数量金额账：某科目（含下级）逐分录展示收入/发出/结存的数量、单价、金额。
 * 借方科目：借=收入、贷=发出；贷方科目相反。数量取分录 num 列，未填按 0 计。
 */
@Service
@RequiredArgsConstructor
public class QuantityLedgerService {

    private final VoucherItemMapper voucherItemMapper;
    private final BookSubjectMapper bookSubjectMapper;

    public QuantityLedgerVo query(String bookId, String subjectCode, String startDate, String endDate) {
        if (StringUtils.isBlank(subjectCode)) {
            throw new BusinessException(400, "请选择科目");
        }
        BookSubject subject = bookSubjectMapper.selectOne(Wrappers.<BookSubject>lambdaQuery()
                .eq(BookSubject::getBookId, bookId)
                .eq(BookSubject::getCode, subjectCode)
                .last("limit 1"));
        if (subject == null) {
            throw new BusinessException(404, "科目不存在或不属于当前账套");
        }
        boolean creditNature = SubjectDirectionEnum.CREDIT.getValue().equals(subject.getDirection());

        // 期初结存：起始日之前的数量/金额净额
        int openingQty = 0;
        BigDecimal openingAmt = BigDecimal.ZERO;
        if (StringUtils.isNotBlank(startDate)) {
            String beforeStart = LocalDate.parse(startDate).minusDays(1).toString();
            for (VoucherItemVo item : voucherItemMapper.multiColumnLedgerItems(
                    bookId, subjectCode, null, beforeStart)) {
                openingQty += netQuantity(item, creditNature);
                openingAmt = openingAmt.add(netAmount(item, creditNature));
            }
        }

        List<VoucherItemVo> items = voucherItemMapper.multiColumnLedgerItems(
                bookId, subjectCode, startDate, endDate);

        List<QuantityLedgerVo.Row> rows = new ArrayList<>();
        int balanceQty = openingQty;
        BigDecimal balanceAmt = openingAmt;
        int inQty = 0;
        int outQty = 0;
        BigDecimal inAmt = BigDecimal.ZERO;
        BigDecimal outAmt = BigDecimal.ZERO;

        for (VoucherItemVo item : items) {
            boolean isIn = isIncomeSide(item, creditNature);
            int qty = item.getNum() != null ? Math.abs(item.getNum()) : 0;
            BigDecimal sideAmount = sideAmount(item, creditNature, isIn);

            QuantityLedgerVo.Row.RowBuilder row = QuantityLedgerVo.Row.builder()
                    .voucherId(item.getVoucherId())
                    .voucherDate(item.getVoucherDate() != null
                            ? new java.text.SimpleDateFormat("yyyy-MM-dd").format(item.getVoucherDate()) : "")
                    .word(StringUtils.defaultString(item.getWord()))
                    .summary(StringUtils.defaultString(item.getSummary()))
                    .subjectName(StringUtils.defaultString(item.getSubjectName()));
            if (isIn) {
                row.inQuantity(qty).inAmount(sideAmount).inPrice(unitPrice(sideAmount, qty));
                inQty += qty;
                inAmt = inAmt.add(sideAmount);
                balanceQty += qty;
                balanceAmt = balanceAmt.add(sideAmount);
            } else {
                row.outQuantity(qty).outAmount(sideAmount).outPrice(unitPrice(sideAmount, qty));
                outQty += qty;
                outAmt = outAmt.add(sideAmount);
                balanceQty -= qty;
                balanceAmt = balanceAmt.subtract(sideAmount);
            }
            row.balanceQuantity(balanceQty)
                    .balanceAmount(balanceAmt)
                    .balancePrice(unitPrice(balanceAmt, balanceQty));
            rows.add(row.build());
        }

        return QuantityLedgerVo.builder()
                .subjectCode(subjectCode)
                .subjectName(subject.getName())
                .direction(subject.getDirection())
                .startDate(startDate)
                .endDate(endDate)
                .openingQuantity(openingQty)
                .openingAmount(openingAmt)
                .closingQuantity(balanceQty)
                .closingAmount(balanceAmt)
                .periodInQuantity(inQty)
                .periodInAmount(inAmt)
                .periodOutQuantity(outQty)
                .periodOutAmount(outAmt)
                .rows(rows)
                .build();
    }

    /** 收入侧判定：借方科目借=收入；贷方科目贷=收入。按金额列是否有值判定所在侧。 */
    static boolean isIncomeSide(VoucherItemVo item, boolean creditNature) {
        boolean hasDebit = item.getDebitAmount() != null && item.getDebitAmount().compareTo(BigDecimal.ZERO) != 0;
        boolean hasCredit = item.getCreditAmount() != null && item.getCreditAmount().compareTo(BigDecimal.ZERO) != 0;
        if (hasDebit && !hasCredit) {
            return !creditNature;
        }
        if (hasCredit && !hasDebit) {
            return creditNature;
        }
        // 双侧都有或都为 0 的异常分录：按净额方向归入一侧
        BigDecimal net = nz(item.getDebitAmount()).subtract(nz(item.getCreditAmount()));
        boolean debitDominant = net.compareTo(BigDecimal.ZERO) >= 0;
        return creditNature != debitDominant;
    }

    /** 该侧的金额（收入/发出取正数展示） */
    static BigDecimal sideAmount(VoucherItemVo item, boolean creditNature, boolean isIn) {
        if (isIn) {
            return creditNature ? nz(item.getCreditAmount()) : nz(item.getDebitAmount());
        }
        return creditNature ? nz(item.getDebitAmount()) : nz(item.getCreditAmount());
    }

    /** 净数量：收入为正、发出为负 */
    static int netQuantity(VoucherItemVo item, boolean creditNature) {
        int qty = item.getNum() != null ? Math.abs(item.getNum()) : 0;
        return isIncomeSide(item, creditNature) ? qty : -qty;
    }

    /** 净金额：借方科目 借-贷；贷方科目 贷-借 */
    static BigDecimal netAmount(VoucherItemVo item, boolean creditNature) {
        return creditNature
                ? nz(item.getCreditAmount()).subtract(nz(item.getDebitAmount()))
                : nz(item.getDebitAmount()).subtract(nz(item.getCreditAmount()));
    }

    /** 结存单价 = 金额 / 数量，数量为 0 时返回 null */
    static BigDecimal unitPrice(BigDecimal amount, int quantity) {
        if (quantity == 0 || amount == null) {
            return null;
        }
        return amount.divide(BigDecimal.valueOf(quantity), 2, RoundingMode.HALF_UP);
    }

    private static BigDecimal nz(BigDecimal v) {
        return v != null ? v : BigDecimal.ZERO;
    }
}
