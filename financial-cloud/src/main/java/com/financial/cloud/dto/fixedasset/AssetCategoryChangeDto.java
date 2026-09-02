package com.financial.cloud.dto.fixedasset;

import com.financial.cloud.constants.common.MessageKeys;
import com.financial.cloud.validation.AddGroup;
import com.financial.cloud.validation.EditGroup;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.io.Serial;
import java.io.Serializable;
import java.math.BigDecimal;

@Data
public class AssetCategoryChangeDto implements Serializable {
    @Serial
    private static final long serialVersionUID = 1L;

    @NotBlank(message = MessageKeys.Validation.COMMON_ID_REQUIRED, groups = {EditGroup.class})
    private String id;

    @NotBlank(message = MessageKeys.Validation.BOOK_OWNER_BOOK_ID_REQUIRED, groups = {AddGroup.class, EditGroup.class})
    private String bookId;

    @NotBlank(message = MessageKeys.Validation.FIXED_ASSET_CATEGORY_CODE_REQUIRED, groups = {AddGroup.class, EditGroup.class})
    private String code;

    @NotBlank(message = MessageKeys.Validation.FIXED_ASSET_CATEGORY_NAME_REQUIRED, groups = {AddGroup.class, EditGroup.class})
    private String name;

    @NotBlank(message = MessageKeys.Validation.FIXED_ASSET_METHOD_REQUIRED, groups = {AddGroup.class, EditGroup.class})
    private String depreciationMethod;

    private Integer usefulLifeYears;

    @NotNull(message = MessageKeys.Validation.FIXED_ASSET_LIFE_MONTHS_REQUIRED, groups = {AddGroup.class, EditGroup.class})
    private Integer usefulLifeMonths;

    @NotNull(message = MessageKeys.Validation.FIXED_ASSET_RESIDUAL_RATE_REQUIRED, groups = {AddGroup.class, EditGroup.class})
    private BigDecimal residualRate;

    @NotBlank(message = MessageKeys.Validation.FIXED_ASSET_SUBJECT_REQUIRED, groups = {AddGroup.class, EditGroup.class})
    private String fixedAssetSubjectId;

    @NotBlank(message = MessageKeys.Validation.FIXED_ASSET_ACCUM_SUBJECT_REQUIRED, groups = {AddGroup.class, EditGroup.class})
    private String accumDeprSubjectId;

    private String remark;
}
