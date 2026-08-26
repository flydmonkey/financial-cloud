package com.financial.cloud.dto.voucher;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 凭证号非连续性查询参数
 *
 * @author wuyan
 * {@code @date} 2025-04-20
 */

@Builder
@AllArgsConstructor
@NoArgsConstructor
@Data
public class VoucherSuccessiveQueryDto {
    public static final String[] WORD_HEADS = {"记", "收", "付", "转"};

    /**
     * 凭证字字头：“记”、“收”、“付”、“转”等
     */
    private String wordHead;

    /**
     * 起始凭证号
     */
    private Integer startWordNumber;

    /**
     * 所属账套
     */
    private String bookId;

    /**
     * 凭证号修复模式:sequential(顺序补齐)|date（日期重排）
     */
    private String successiveMethod;

    /**
     * 作废凭证参与凭证整理
     */
    private Boolean nullify;
}
