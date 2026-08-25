package com.jinbooks.dto.voucher;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import lombok.Data;


@Data
public class GenerateVoucherDto {

    @Valid
    @NotNull(message = "凭证规则不能为空")
    Integer voucherType;

    @NotEmpty(message = "id不能为空")
    String id;

    String bookId;
    
    String yearPeriod;
    
    String templateId;
}
