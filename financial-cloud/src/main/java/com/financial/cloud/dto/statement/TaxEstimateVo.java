package com.financial.cloud.dto.statement;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serial;
import java.io.Serializable;
import java.math.BigDecimal;

/**
 * 税费测算：按期间的增值税、附加税、企业所得税测算与税负预警。
 * 数据来自已过账凭证分录（与账簿口径一致），测算结果仅供申报前参考。
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TaxEstimateVo implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    /** 测算期间 yyyy-MM */
    private String yearMonth;

    // ---- 增值税（一般纳税人口径） ----
    /** 销项税额（贷方净额） */
    private BigDecimal outputTax;
    /** 进项税额（借方净额） */
    private BigDecimal inputTax;
    /** 进项税额转出 */
    private BigDecimal inputTransferOut;
    /** 已交税金 */
    private BigDecimal paidTax;
    /** 应纳税额 = 销项 - 进项 + 进项转出（负数表示留抵） */
    private BigDecimal vatPayable;
    /** 期末留抵税额（应纳税额为负时） */
    private BigDecimal vatCredit;
    /** 本期应补税额 = max(应纳税额 - 已交, 0) */
    private BigDecimal vatDue;

    // ---- 附加税（以本期应补增值税为计税依据） ----
    private BigDecimal urbanRate;
    private BigDecimal urbanTax;
    private BigDecimal eduRate;
    private BigDecimal eduTax;
    private BigDecimal localEduRate;
    private BigDecimal localEduTax;
    /** 附加税合计 */
    private BigDecimal surtaxTotal;

    // ---- 企业所得税 ----
    /** 利润总额（损益类净额，剔除所得税费用） */
    private BigDecimal profitBeforeTax;
    private BigDecimal incomeTaxRate;
    /** 测算企业所得税 = max(利润总额, 0) × 税率 */
    private BigDecimal incomeTax;

    // ---- 税负预警 ----
    /** 营业收入（主营业务收入 + 其他业务收入贷方净额） */
    private BigDecimal revenue;
    /** 增值税税负率 = 本期应补 / 营业收入 */
    private BigDecimal vatBurdenRate;
    /** 预警阈值（如 0.01 表示 1%） */
    private BigDecimal burdenThreshold;
    /** 税负率低于阈值且有收入时为 true */
    private Boolean burdenWarning;
}
