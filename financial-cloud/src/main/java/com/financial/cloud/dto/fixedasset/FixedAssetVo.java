package com.financial.cloud.dto.fixedasset;

import com.financial.cloud.domain.fixedasset.FixedAsset;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.io.Serial;
import java.math.BigDecimal;

@EqualsAndHashCode(callSuper = true)
@Data
public class FixedAssetVo extends FixedAsset {
    @Serial
    private static final long serialVersionUID = 1L;

    private BigDecimal residualValue;
    private BigDecimal openingNetValue;
    private BigDecimal monthlyDepr;
    private BigDecimal endingAccumDepr;
    private BigDecimal endingNetValue;
    private String categoryName;
    private String deptName;
    private String methodLabel;
    private Boolean calcFieldsLocked;
    private String purchaseVoucherWord;
    private String disposeVoucherWord;
}
