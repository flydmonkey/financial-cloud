package com.jinbooks.dto.voucher;

import com.jinbooks.domain.voucher.VoucherItemCashFlow;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import lombok.Data;

import java.util.List;

/**
 * @description:
 * @author: orangeBabu
 * @time: 2025/3/26 18:24
 */

@Data
public class VoucherItemCashFlowDto {

    @Valid
    @NotEmpty(message = "入参不能为空")
    private List<VoucherItemCashFlow> voucherItemCashFlowDtos;

    private String bookId;

    private String voucherId;

    private Boolean isEdit;

    private Integer cashFlowItemType;

    private String voucherDate;
}
