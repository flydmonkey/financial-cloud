package com.financial.cloud.dto.statement;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class StatementGeneralLedgerItem {
    private String subjectCode;
    private String subjectName;
    /** yyyyMM */
    private String period;
    /** 期初余额 | 本期合计 | 本年累计 */
    private String summary;
    private BigDecimal debit;
    private BigDecimal credit;
    /** 借 | 贷 | 平 */
    private String direction;
    private BigDecimal balance;
    private String groupKey;
    /** 组首行=行数(1或3)，同组后续=0 */
    private Integer rowSpan;
}
