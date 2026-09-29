package com.financial.cloud.dto.voucher;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serial;
import java.io.Serializable;
import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

/**
 * 多栏账：以某科目为栏母，其直接子科目为栏位，逐凭证展示各栏净额与余额。
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class MultiColumnLedgerVo implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    /** 栏母科目编码 */
    private String subjectCode;

    /** 栏母科目名称 */
    private String subjectName;

    /** 栏母方向：1 借方 / 2 贷方 */
    private String direction;

    /** 查询起止（yyyy-MM-dd） */
    private String startDate;
    private String endDate;

    /** 期初余额（按栏母方向带符号） */
    private BigDecimal openingBalance;

    /** 期末余额（期初 + 本期净额合计） */
    private BigDecimal closingBalance;

    /** 栏位（直接子科目 + 本级） */
    private List<Column> columns;

    /** 逐凭证行 */
    private List<Row> rows;

    /** 各栏本期合计（key 为栏位编码） */
    private Map<String, BigDecimal> columnTotals;

    /** 本期净额合计 */
    private BigDecimal periodTotal;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class Column implements Serializable {
        @Serial
        private static final long serialVersionUID = 1L;

        private String code;
        private String name;
        /** true 表示栏母自身的「本级」栏 */
        private Boolean selfColumn;
    }

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
        /** 各栏净额（key 为栏位编码；借方栏母=借-贷，贷方栏母=贷-借） */
        private Map<String, BigDecimal> amounts;
        /** 本行净额合计 */
        private BigDecimal total;
        /** 截至本行的余额 */
        private BigDecimal balance;
    }
}
