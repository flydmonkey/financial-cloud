package com.financial.cloud.dto.voucher;

import com.financial.cloud.domain.voucher.VoucherItemCashFlow;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import lombok.Data;
import com.financial.cloud.constants.common.MessageKeys;

import java.util.List;

@Data
public class VoucherItemCashFlowDto {

    @Valid
    @NotEmpty(message = MessageKeys.Validation.COMMON_INPUT_PARAM_REQUIRED)
    private List<VoucherItemCashFlow> voucherItemCashFlowDtos;

    private String bookId;

    private String voucherId;

    private Boolean isEdit;

    private Integer cashFlowItemType;

    private String voucherDate;
}
