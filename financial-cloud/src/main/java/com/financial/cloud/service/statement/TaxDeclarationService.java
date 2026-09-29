package com.financial.cloud.service.statement;

import com.financial.cloud.dto.statement.TaxDeclarationVo;
import com.financial.cloud.dto.statement.TaxEstimateVo;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

/**
 * 增值税申报表（简版主表）：把税费测算结果按官方主表行次格式化为申报底稿。
 * 口径与 {@link TaxEstimateService} 一致；销售额按营业收入估算，
 * 账面无法拆分的行次（简易计税、免税、上期留抵等）留空由人工填报。
 */
@Service
@RequiredArgsConstructor
public class TaxDeclarationService {

    private static final String SECTION_SALES = "一、销售额（按账面营业收入估算）";
    private static final String SECTION_COMPUTE = "二、税款计算";
    private static final String SECTION_PAYMENT = "三、税款缴纳";
    private static final String SECTION_SURTAX = "四、附加税费（以本期应补增值税为计税依据）";
    private static final String SECTION_INCOME_TAX = "五、企业所得税（预缴参考）";

    private final TaxEstimateService taxEstimateService;

    public TaxDeclarationVo declaration(String bookId, String yearMonth,
                                        BigDecimal urbanRate, BigDecimal eduRate, BigDecimal localEduRate,
                                        BigDecimal incomeTaxRate) {
        TaxEstimateVo est = taxEstimateService.estimate(bookId, yearMonth,
                urbanRate, eduRate, localEduRate, incomeTaxRate, BigDecimal.ZERO);

        // 行 18 实际抵扣税额：应纳税额为正时全额抵扣，为负（留抵）时仅抵扣至销项为零
        BigDecimal deductible = nz(est.getInputTax()).subtract(nz(est.getInputTransferOut()));
        boolean payablePositive = nz(est.getVatPayable()).signum() > 0;
        BigDecimal actualDeduct = payablePositive
                ? deductible.max(BigDecimal.ZERO)
                : nz(est.getOutputTax());
        BigDecimal vatPositive = nz(est.getVatPayable()).max(BigDecimal.ZERO)
                .setScale(2, java.math.RoundingMode.HALF_UP);

        List<TaxDeclarationVo.Line> lines = new ArrayList<>();
        lines.add(line(SECTION_SALES, "1", "（一）按适用税率计税销售额", est.getRevenue()));
        lines.add(line(SECTION_SALES, "5", "（二）按简易办法计税销售额", null));
        lines.add(line(SECTION_SALES, "7", "（四）免税销售额", null));

        lines.add(line(SECTION_COMPUTE, "11", "销项税额", est.getOutputTax()));
        lines.add(line(SECTION_COMPUTE, "12", "进项税额", est.getInputTax()));
        lines.add(line(SECTION_COMPUTE, "13", "上期留抵税额", null));
        lines.add(line(SECTION_COMPUTE, "14", "进项税额转出", est.getInputTransferOut()));
        lines.add(line(SECTION_COMPUTE, "17", "应抵扣税额合计（12+13-14）", deductible.max(BigDecimal.ZERO)));
        lines.add(line(SECTION_COMPUTE, "18", "实际抵扣税额", actualDeduct));
        lines.add(line(SECTION_COMPUTE, "19", "应纳税额（11-18）", vatPositive));
        lines.add(line(SECTION_COMPUTE, "20", "期末留抵税额", est.getVatCredit()));
        lines.add(line(SECTION_COMPUTE, "24", "应纳税额合计", vatPositive));

        lines.add(line(SECTION_PAYMENT, "27", "本期已缴税额", est.getPaidTax()));
        lines.add(line(SECTION_PAYMENT, "34", "本期应补（退）税额", est.getVatDue()));

        lines.add(line(SECTION_SURTAX, "", "城市维护建设税（" + percent(est.getUrbanRate()) + "）", est.getUrbanTax()));
        lines.add(line(SECTION_SURTAX, "", "教育费附加（" + percent(est.getEduRate()) + "）", est.getEduTax()));
        lines.add(line(SECTION_SURTAX, "", "地方教育附加（" + percent(est.getLocalEduRate()) + "）", est.getLocalEduTax()));
        lines.add(line(SECTION_SURTAX, "", "附加税费合计", est.getSurtaxTotal()));

        lines.add(line(SECTION_INCOME_TAX, "", "利润总额（剔除所得税费用）", est.getProfitBeforeTax()));
        lines.add(line(SECTION_INCOME_TAX, "", "应纳所得税额（" + percent(est.getIncomeTaxRate()) + "）", est.getIncomeTax()));

        return TaxDeclarationVo.builder()
                .yearMonth(est.getYearMonth())
                .lines(lines)
                .build();
    }

    private static TaxDeclarationVo.Line line(String section, String rowNo, String item, BigDecimal amount) {
        return TaxDeclarationVo.Line.builder()
                .section(section)
                .rowNo(rowNo)
                .item(item)
                .amount(amount)
                .build();
    }

    private static String percent(BigDecimal rate) {
        if (rate == null) {
            return "";
        }
        return rate.multiply(new BigDecimal("100")).stripTrailingZeros().toPlainString() + "%";
    }

    private static BigDecimal nz(BigDecimal v) {
        return v != null ? v : BigDecimal.ZERO;
    }
}
