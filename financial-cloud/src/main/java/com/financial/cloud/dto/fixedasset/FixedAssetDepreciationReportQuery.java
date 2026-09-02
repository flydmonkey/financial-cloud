package com.financial.cloud.dto.fixedasset;

import lombok.Data;

import java.io.Serial;
import java.io.Serializable;

@Data
public class FixedAssetDepreciationReportQuery implements Serializable {
    @Serial
    private static final long serialVersionUID = 1L;

    private String bookId;
    private String startPeriod;
    private String endPeriod;
    private Boolean includeDisposed;
    private Boolean groupByDept;
    /** 明细表：是否附带期间变动摘要 */
    private Boolean includeChangeInfo;
}
