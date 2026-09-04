package com.financial.cloud.dto.fixedasset;

import lombok.Data;

import java.io.Serial;
import java.io.Serializable;
import java.math.BigDecimal;
import java.util.Date;

@Data
public class FixedAssetDepreciationStatusVo implements Serializable {
    @Serial
    private static final long serialVersionUID = 1L;

    private boolean accrued;
    /** false when book has no assets requiring depreciation this period */
    private boolean needed = true;
    private String voucherId;
    private String voucherWord;
    private BigDecimal totalAmount;
    private boolean canReaccrue;
    private String yearPeriod;
    private Date voucherDate;
    private String summary;
}
