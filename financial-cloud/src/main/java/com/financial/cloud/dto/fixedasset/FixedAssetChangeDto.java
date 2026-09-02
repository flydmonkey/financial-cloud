package com.financial.cloud.dto.fixedasset;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.financial.cloud.constants.common.MessageKeys;
import com.financial.cloud.validation.AddGroup;
import com.financial.cloud.validation.EditGroup;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.io.Serial;
import java.io.Serializable;
import java.math.BigDecimal;
import java.util.Date;

@Data
public class FixedAssetChangeDto implements Serializable {
    @Serial
    private static final long serialVersionUID = 1L;

    @NotBlank(message = MessageKeys.Validation.COMMON_ID_REQUIRED, groups = {EditGroup.class})
    private String id;

    @NotBlank(message = MessageKeys.Validation.BOOK_OWNER_BOOK_ID_REQUIRED, groups = {AddGroup.class, EditGroup.class})
    private String bookId;

    @NotBlank(message = MessageKeys.Validation.FIXED_ASSET_CODE_REQUIRED, groups = {AddGroup.class, EditGroup.class})
    private String code;

    @NotBlank(message = MessageKeys.Validation.FIXED_ASSET_NAME_REQUIRED, groups = {AddGroup.class, EditGroup.class})
    private String name;

    @NotBlank(message = MessageKeys.Validation.FIXED_ASSET_CATEGORY_REQUIRED, groups = {AddGroup.class, EditGroup.class})
    private String categoryId;

    private String deptId;

    @NotNull(message = MessageKeys.Validation.FIXED_ASSET_START_DATE_REQUIRED, groups = {AddGroup.class, EditGroup.class})
    @JsonFormat(pattern = "yyyy-MM-dd", timezone = "GMT+8")
    private Date startUseDate;

    private String entryPeriod;
    private Integer quantity;
    private String spec;
    private String location;
    private String userId;
    private String status;
    private String disposedPeriod;

    private String depreciationMethod;
    private Integer usefulLifeMonths;
    private BigDecimal expectedTotalWork;

    private BigDecimal residualRate;

    @NotNull(message = MessageKeys.Validation.FIXED_ASSET_ORIGINAL_VALUE_REQUIRED, groups = {AddGroup.class, EditGroup.class})
    private BigDecimal originalValue;

    private BigDecimal taxAmount;
    private BigDecimal impairment;
    private Integer depreciatedPeriods;
    private BigDecimal openingAccumDepr;
    private BigDecimal accumDepr;
    private BigDecimal yearDepr;

    private String fixedAssetSubjectId;
    private String purchaseCounterpartSubjectId;
    private String taxSubjectId;
    private String accumDeprSubjectId;
    private String expenseSubjectId;

    private String disposalSubjectId;
    private String impairmentSubjectId;
    private String impairmentCounterpartSubjectId;
    private String remark;
}
