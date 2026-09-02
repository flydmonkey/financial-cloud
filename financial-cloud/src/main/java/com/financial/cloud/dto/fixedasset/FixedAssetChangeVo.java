package com.financial.cloud.dto.fixedasset;

import lombok.Data;

import java.io.Serial;
import java.io.Serializable;
import java.util.Date;

@Data
public class FixedAssetChangeVo implements Serializable {
    @Serial
    private static final long serialVersionUID = 1L;

    private String id;
    private String changeId;
    private String assetId;
    private String assetCode;
    private String assetName;
    private String fieldCode;
    private String fieldLabel;
    private String beforeValue;
    private String afterValue;
    private String yearPeriod;
    private String modifiedBy;
    private String modifiedByName;
    private Date changeTime;
}
