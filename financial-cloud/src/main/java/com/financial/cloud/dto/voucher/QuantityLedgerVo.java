package com.financial.cloud.dto.voucher;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serial;
import java.io.Serializable;
import java.math.BigDecimal;
import java.util.List;

/**
 * 数量金额账：某科目逐分录展示收入/发出/结存的数量、单价、金额。
 * 数据源与明细账一致（已过账凭证分录的 num/price/金额列）。
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class QuantityLedgerVo implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    /** 科目编码 */
    private String subjectCode;

    /** 科目名称 */
    private String subjectName;

    /** 科目方向：1 借方 / 2 贷方 */
    private String direction;

    /** 查询起止（yyyy-MM-dd） */
    private String startDate;
    private String endDate;

    /** 期初结存数量 */
    private Integer openingQuantity;

    /** 期初结存金额 */
    private BigDecimal openingAmount;

    /** 期末结存数量 */
    private Integer closingQuantity;

    /** 期末结存金额 */
    private BigDecimal closingAmount;

    /** 本期收入合计 */
    private Integer periodInQuantity;
    private BigDecimal periodInAmount;

    /** 本期发出合计 */
    private Integer periodOutQuantity;
    private BigDecimal periodOutAmount;

    /** 逐分录行 */
    private List<Row> rows;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class Row implements Serializable {
        @Serial
        private static final long serialVersionUID = 1L;

        private String voucherId;
        private String voucherDate;
        /** 凭证字号，如 记-3 */
        private String word;
        private String summary;
        /** 对方科目（分录上的科目名称） */
        private String subjectName;

        /** 收入（借方科目=借，贷方科目=贷） */
        private Integer inQuantity;
        private BigDecimal inPrice;
        private BigDecimal inAmount;

        /** 发出 */
        private Integer outQuantity;
        private BigDecimal outPrice;
        private BigDecimal outAmount;

        /** 截至本行的结存 */
        private Integer balanceQuantity;
        private BigDecimal balancePrice;
        private BigDecimal balanceAmount;
    }
}
