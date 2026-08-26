package com.financial.cloud.dto.voucher;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import lombok.Data;
import com.financial.cloud.constants.common.MessageKeys;


@Data
public class GenerateVoucherDto {

    @Valid
    @NotNull(message = MessageKeys.Validation.VOUCHER_VOUCHER_RULE_REQUIRED)
    Integer voucherType;

    @NotEmpty(message = MessageKeys.Validation.COMMON_ID_LOWERCASE_REQUIRED)
    String id;

    String bookId;
    
    String yearPeriod;
    
    String templateId;
}
