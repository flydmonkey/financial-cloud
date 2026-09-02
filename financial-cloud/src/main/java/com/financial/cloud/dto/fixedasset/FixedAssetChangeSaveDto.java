package com.financial.cloud.dto.fixedasset;

import lombok.Data;

import java.io.Serial;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

@Data
public class FixedAssetChangeSaveDto implements Serializable {
    @Serial
    private static final long serialVersionUID = 1L;

    private String bookId;
    private String assetId;
    private String yearPeriod;
    private String remark;
    private List<FixedAssetChangeItemDto> items = new ArrayList<>();
}
