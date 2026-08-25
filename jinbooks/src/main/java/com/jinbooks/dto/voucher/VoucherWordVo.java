package com.jinbooks.dto.voucher;

import lombok.Data;
import lombok.EqualsAndHashCode;

import java.io.Serial;

import com.jinbooks.domain.voucher.VoucherWord;

/**
 * 凭证字视图对象
 *
 * @author wuyan
 * {@code @date} 2025-01-14
 */

@EqualsAndHashCode(callSuper = true)
@Data
public class VoucherWordVo extends VoucherWord {
    @Serial
    private static final long serialVersionUID = 1L;
}
