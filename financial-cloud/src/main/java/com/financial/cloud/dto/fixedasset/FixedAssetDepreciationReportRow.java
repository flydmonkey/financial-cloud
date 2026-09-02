package com.financial.cloud.dto.fixedasset;

import lombok.Data;

import java.io.Serial;
import java.io.Serializable;
import java.math.BigDecimal;

@Data
public class FixedAssetDepreciationReportRow implements Serializable {
    @Serial
    private static final long serialVersionUID = 1L;

    /** ASSET | SUBTOTAL | TOTAL */
    private String rowType;
    private String assetId;
    private String assetCode;
    private String assetName;
    private String categoryId;
    private String categoryName;
    private String deptId;
    private String deptName;
    private BigDecimal originalValue;
    private BigDecimal openingAccumDepr;
    private BigDecimal periodDepr;
    private BigDecimal yearDepr;
    private BigDecimal endingAccumDepr;
    private BigDecimal endingImpairment;
    private BigDecimal endingNetValue;
    /** 查询期内变动摘要（仅 includeChangeInfo 时有值） */
    private String changeInfo;
}
