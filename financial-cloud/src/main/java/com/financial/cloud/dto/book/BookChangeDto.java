package com.financial.cloud.dto.book;

import com.financial.cloud.validation.AddGroup;
import com.financial.cloud.validation.EditGroup;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;
import com.financial.cloud.constants.common.MessageKeys;

import java.time.YearMonth;

@Data
public class BookChangeDto {
    @NotEmpty(message = MessageKeys.Validation.COMMON_EDIT_TARGET_REQUIRED, groups = {EditGroup.class})
    String id;

    @NotEmpty(message = MessageKeys.Validation.BOOK_NAME_REQUIRED, groups = {AddGroup.class, EditGroup.class})
    @Size(max = 21, message = MessageKeys.Validation.BOOK_NAME_MAX_LENGTH, groups = {AddGroup.class, EditGroup.class})
    String name;

    @NotEmpty(message = MessageKeys.Validation.ORG_UNIT_NAME_REQUIRED, groups = {AddGroup.class, EditGroup.class})
    @Size(max = 85, message = MessageKeys.Validation.ORG_UNIT_NAME_MAX_LENGTH, groups = {AddGroup.class, EditGroup.class})
    String companyName;

    @NotNull(message = MessageKeys.Validation.BOOK_INIT_PERIOD_REQUIRED, groups = {AddGroup.class, EditGroup.class})
    YearMonth enableDate;

    @NotEmpty(message = MessageKeys.Validation.STANDARD_ACCOUNTING_SYSTEM_REQUIRED, groups = {AddGroup.class, EditGroup.class})
    String standardId;

    @NotNull(message = MessageKeys.Validation.BOOK_TAX_NATURE_REQUIRED, groups = {AddGroup.class, EditGroup.class})
    Integer vatType;

    String address;

    Integer industry;

    String creditCode;

    @NotNull(message = MessageKeys.Validation.VOUCHER_AUDIT_REQUIRED_REQUIRED, groups = {AddGroup.class, EditGroup.class})
    Integer voucherReviewed;

    @NotNull(message = MessageKeys.Validation.COMMON_STATUS_REQUIRED, groups = {AddGroup.class, EditGroup.class})
    Integer status;
}
