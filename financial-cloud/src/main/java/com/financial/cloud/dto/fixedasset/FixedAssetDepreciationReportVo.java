package com.financial.cloud.dto.fixedasset;

import lombok.Data;

import java.io.Serial;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

@Data
public class FixedAssetDepreciationReportVo implements Serializable {
    @Serial
    private static final long serialVersionUID = 1L;

    private String startPeriod;
    private String endPeriod;
    private String periodDeprColumnLabel;
    private Boolean includeChangeInfo;
    private List<FixedAssetDepreciationReportRow> rows = new ArrayList<>();
}
