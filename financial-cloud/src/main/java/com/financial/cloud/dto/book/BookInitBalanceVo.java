package com.financial.cloud.dto.book;

import com.financial.cloud.domain.book.BookInitBalance;
import lombok.*;

import java.io.Serial;
import java.io.Serializable;

/**
 * 初期余额表视图对象
 *
 * @author Wuyan
 * {@code @date} 2025-03-11
 */
@EqualsAndHashCode(callSuper = true)
@Data
public class BookInitBalanceVo extends BookInitBalance implements Serializable {
    @Serial
    private static final long serialVersionUID = 1L;

    String parentCode;

    String parentName;

    /**
     * 原始ID
     */
    String originId;

    /**
     * 是否存在凭证
     */
    private boolean hasVoucher;
}
