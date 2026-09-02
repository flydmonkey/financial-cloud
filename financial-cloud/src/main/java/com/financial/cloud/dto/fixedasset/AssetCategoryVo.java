package com.financial.cloud.dto.fixedasset;

import com.financial.cloud.domain.fixedasset.AssetCategory;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.io.Serial;

@EqualsAndHashCode(callSuper = true)
@Data
public class AssetCategoryVo extends AssetCategory {
    @Serial
    private static final long serialVersionUID = 1L;

    private String fixedAssetSubjectName;
    private String accumDeprSubjectName;
    private String depreciationMethodLabel;
}
