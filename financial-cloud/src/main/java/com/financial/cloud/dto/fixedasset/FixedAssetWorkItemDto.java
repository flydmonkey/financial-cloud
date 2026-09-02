package com.financial.cloud.dto.fixedasset;

import lombok.Data;

import java.io.Serial;
import java.io.Serializable;
import java.math.BigDecimal;

@Data
public class FixedAssetWorkItemDto implements Serializable {
    @Serial
    private static final long serialVersionUID = 1L;

    private String assetId;
    private String code;
    private String name;
    private BigDecimal expectedTotalWork;
    private BigDecimal periodWork;
}
