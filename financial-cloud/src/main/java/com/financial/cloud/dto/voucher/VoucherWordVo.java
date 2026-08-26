package com.financial.cloud.dto.voucher;

import lombok.Data;
import lombok.EqualsAndHashCode;

import java.io.Serial;

import com.financial.cloud.domain.voucher.VoucherWord;

@EqualsAndHashCode(callSuper = true)
@Data
public class VoucherWordVo extends VoucherWord {
    @Serial
    private static final long serialVersionUID = 1L;
}
