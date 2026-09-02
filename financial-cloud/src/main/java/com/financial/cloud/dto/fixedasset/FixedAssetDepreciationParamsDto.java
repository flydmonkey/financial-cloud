package com.financial.cloud.dto.fixedasset;

import lombok.Data;

import java.io.Serial;
import java.io.Serializable;
import java.util.Date;

@Data
public class FixedAssetDepreciationParamsDto implements Serializable {
    @Serial
    private static final long serialVersionUID = 1L;

    private String yearPeriod;
    private Date voucherDate;
    private String voucherWord;
    private String summary;
}
