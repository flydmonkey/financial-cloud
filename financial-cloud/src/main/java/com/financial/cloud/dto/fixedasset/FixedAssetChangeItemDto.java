package com.financial.cloud.dto.fixedasset;

import lombok.Data;

import java.io.Serial;
import java.io.Serializable;

@Data
public class FixedAssetChangeItemDto implements Serializable {
    @Serial
    private static final long serialVersionUID = 1L;

    private String fieldCode;
    private String afterValue;
}
