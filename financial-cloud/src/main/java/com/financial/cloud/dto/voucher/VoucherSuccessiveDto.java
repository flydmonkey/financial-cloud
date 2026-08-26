package com.financial.cloud.dto.voucher;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.fasterxml.jackson.annotation.JsonFormat;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.*;
import com.financial.cloud.constants.common.MessageKeys;

import java.io.Serializable;
import java.util.Date;

@Builder
@NoArgsConstructor
@AllArgsConstructor
@Data
public class VoucherSuccessiveDto implements Serializable {

    /**
	 * 
	 */
	private static final long serialVersionUID = -5435074135295463976L;

	/**
     * 凭证ID
     */
    @TableId(type = IdType.ASSIGN_ID)
    @NotBlank(message = MessageKeys.Validation.COMMON_EDIT_TARGET_REQUIRED)
    private String id;

    private String bookId;

    /**
     * 原始凭证字
     */
    @NotBlank(message = MessageKeys.Validation.VOUCHER_ORIGINAL_VOUCHER_WORD_REQUIRED)
    private String sourceWord;

    /**
     * 新凭证字
     */
    @NotBlank(message = MessageKeys.Validation.VOUCHER_NEW_VOUCHER_WORD_REQUIRED)
    private String targetWord;

    /**
     * 字头：“收”、“付”、“转”等
     */
    @NotBlank(message = MessageKeys.Validation.VOUCHER_PREFIX_REQUIRED)
    private String wordHead;

    /**
     * 号码
     */
    @NotNull(message = MessageKeys.Validation.COMMON_NUMBER_REQUIRED)
    private Integer wordNum;

    /**
     * 年份
     */
    @NotNull(message = MessageKeys.Validation.COMMON_YEAR_REQUIRED)
    private Integer voucherYear;

    /**
     * 月份
     */
    @NotNull(message = MessageKeys.Validation.COMMON_MONTH_REQUIRED)
    private Integer voucherMonth;

    /**
     * 日期
     */
    @JsonFormat(pattern = "yyyy-MM-dd", timezone = "GMT+8")
    private Date voucherDate;
}
