package com.financial.cloud.service.statement;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.financial.cloud.domain.book.BookSubject;
import com.financial.cloud.dto.statement.TaxEstimateVo;
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
import java.time.YearMonth;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 税费测算：按期间的增值税、附加税、企业所得税测算与税负预警。
 * 取数口径与账簿一致（已过账凭证分录），科目按编码前缀 + 名称关键字分类，
 * 兼容新准则（2221.01.*）与小企业准则（2171.01.*）两套模板。
 */
@Service
@RequiredArgsConstructor
public class TaxEstimateService {

    /** 所得税费用科目前缀（测算利润总额时剔除）：新准则 6801 / 小企业准则 5801 */
    private static final String[] INCOME_TAX_EXPENSE_PREFIXES = {"6801", "5801"};
    /** 营业收入科目前缀：主营业务收入 + 其他业务收入（两套准则） */
    private static final String[] REVENUE_PREFIXES = {"6001", "6051", "5001", "5051"};

    private final VoucherItemMapper voucherItemMapper;
    private final BookSubjectMapper bookSubjectMapper;

    public TaxEstimateVo estimate(String bookId, String yearMonth,
                                  BigDecimal urbanRate, BigDecimal eduRate, BigDecimal localEduRate,
                                  BigDecimal incomeTaxRate, BigDecimal burdenThreshold) {
        if (StringUtils.isBlank(yearMonth)) {
            throw new BusinessException(400, "请选择测算期间");
        }
        YearMonth ym;
        try {
            ym = YearMonth.parse(yearMonth);
        } catch (Exception e) {
            throw new BusinessException(400, "期间格式应为 yyyy-MM");
        }
        LocalDate start = ym.atDay(1);
        LocalDate end = ym.atEndOfMonth();

        // 科目方向表（P&L 净额符号判定用）
        Map<String, BookSubject> subjectByCode = new HashMap<>();
        for (BookSubject s : bookSubjectMapper.selectList(
                Wrappers.<BookSubject>lambdaQuery().eq(BookSubject::getBookId, bookId))) {
            subjectByCode.putIfAbsent(s.getCode(), s);
        }

        // ---- 增值税：应交税费_（应交）增值税下级，按分录科目名称归类 ----
        BigDecimal output = BigDecimal.ZERO;
        BigDecimal input = BigDecimal.ZERO;
        BigDecimal transferOut = BigDecimal.ZERO;
        BigDecimal paid = BigDecimal.ZERO;
        for (String prefix : new String[]{"2221", "2171"}) {
            for (VoucherItemVo item : items(bookId, prefix, start, end)) {
                String name = StringUtils.defaultString(item.getSubjectName());
                BigDecimal debit = nz(item.getDebitAmount());
                BigDecimal credit = nz(item.getCreditAmount());
                if (name.contains("销项")) {
                    output = output.add(credit.subtract(debit));
                } else if (name.contains("进项税额转出")) {
                    transferOut = transferOut.add(credit.subtract(debit));
                } else if (name.contains("进项")) {
                    input = input.add(debit.subtract(credit));
                } else if (name.contains("已交")) {
                    paid = paid.add(debit.subtract(credit));
                }
            }
        }
        BigDecimal vatPayable = output.subtract(input).add(transferOut);
        BigDecimal vatCredit = vatPayable.signum() < 0 ? vatPayable.negate() : BigDecimal.ZERO;
        BigDecimal vatDue = vatPayable.signum() > 0
                ? vatPayable.subtract(paid).max(BigDecimal.ZERO)
                : BigDecimal.ZERO;

        // ---- 附加税 ----
        BigDecimal urbanTax = vatDue.multiply(urbanRate).setScale(2, RoundingMode.HALF_UP);
        BigDecimal eduTax = vatDue.multiply(eduRate).setScale(2, RoundingMode.HALF_UP);
        BigDecimal localEduTax = vatDue.multiply(localEduRate).setScale(2, RoundingMode.HALF_UP);
        BigDecimal surtaxTotal = urbanTax.add(eduTax).add(localEduTax);

        // ---- 利润总额与企业所得税 ----
        BigDecimal profit = BigDecimal.ZERO;
        BigDecimal revenue = BigDecimal.ZERO;
        for (String prefix : new String[]{"5", "6"}) {
            for (VoucherItemVo item : items(bookId, prefix, start, end)) {
                String code = StringUtils.defaultString(item.getSubjectCode());
                if (startsWithAny(code, INCOME_TAX_EXPENSE_PREFIXES)) {
                    continue;
                }
                boolean creditNature = isCreditNature(subjectByCode, code);
                BigDecimal net = creditNature
                        ? nz(item.getCreditAmount()).subtract(nz(item.getDebitAmount()))
                        : nz(item.getDebitAmount()).subtract(nz(item.getCreditAmount()));
                profit = profit.add(creditNature ? net : net.negate());
                if (startsWithAny(code, REVENUE_PREFIXES)) {
                    revenue = revenue.add(net);
                }
            }
        }
        BigDecimal incomeTax = (profit.signum() > 0 ? profit : BigDecimal.ZERO)
                .multiply(incomeTaxRate).setScale(2, RoundingMode.HALF_UP);

        // ---- 税负预警 ----
        BigDecimal vatBurdenRate = revenue.signum() > 0
                ? vatDue.divide(revenue, 4, RoundingMode.HALF_UP)
                : null;
        boolean warning = vatBurdenRate != null && vatBurdenRate.compareTo(burdenThreshold) < 0;

        return TaxEstimateVo.builder()
                .yearMonth(yearMonth)
                .outputTax(scale(output))
                .inputTax(scale(input))
                .inputTransferOut(scale(transferOut))
                .paidTax(scale(paid))
                .vatPayable(scale(vatPayable))
                .vatCredit(scale(vatCredit))
                .vatDue(scale(vatDue))
                .urbanRate(urbanRate)
                .urbanTax(urbanTax)
                .eduRate(eduRate)
                .eduTax(eduTax)
                .localEduRate(localEduRate)
                .localEduTax(localEduTax)
                .surtaxTotal(surtaxTotal)
                .profitBeforeTax(scale(profit))
                .incomeTaxRate(incomeTaxRate)
                .incomeTax(incomeTax)
                .revenue(scale(revenue))
                .vatBurdenRate(vatBurdenRate)
                .burdenThreshold(burdenThreshold)
                .burdenWarning(warning)
                .build();
    }

    /** 期内已过账分录（科目前缀匹配，复用多栏账取数口径） */
    private List<VoucherItemVo> items(String bookId, String prefix, LocalDate start, LocalDate end) {
        return voucherItemMapper.multiColumnLedgerItems(
                bookId, prefix, start.toString(), end.toString());
    }

    /** 科目方向判定：账簿里查不到时按编码启发式（损益类 60/61/63/50/51/53 为贷方性质） */
    static boolean isCreditNature(Map<String, BookSubject> subjectByCode, String code) {
        BookSubject subject = subjectByCode.get(code);
        if (subject != null && StringUtils.isNotBlank(subject.getDirection())) {
            return SubjectDirectionEnum.CREDIT.getValue().equals(subject.getDirection());
        }
        return code.startsWith("60") || code.startsWith("61") || code.startsWith("63")
                || code.startsWith("50") || code.startsWith("51") || code.startsWith("53");
    }

    private static boolean startsWithAny(String code, String[] prefixes) {
        for (String prefix : prefixes) {
            if (code.startsWith(prefix)) {
                return true;
            }
        }
        return false;
    }

    private static BigDecimal nz(BigDecimal v) {
        return v != null ? v : BigDecimal.ZERO;
    }

    private static BigDecimal scale(BigDecimal v) {
        return v.setScale(2, RoundingMode.HALF_UP);
    }
}
