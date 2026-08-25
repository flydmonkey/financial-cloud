package com.jinbooks.dto.voucher;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.fasterxml.jackson.annotation.JsonFormat;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.*;

import java.io.Serializable;
import java.util.Date;

/**
 * 凭证字号连续性视图对象
 *
 * @author wuyan
 * {@code @date} 2025-04-20
 */

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
    @NotBlank(message = "编辑对象不能为空")
    private String id;

    private String bookId;

    /**
     * 原始凭证字
     */
    @NotBlank(message = "原始凭证字不能为空")
    private String sourceWord;

    /**
     * 新凭证字
     */
    @NotBlank(message = "新凭证字不能为空")
    private String targetWord;

    /**
     * 字头：“收”、“付”、“转”等
     */
    @NotBlank(message = "字头不能为空")
    private String wordHead;

    /**
     * 号码
     */
    @NotNull(message = "号码不能为空")
    private Integer wordNum;

    /**
     * 年份
     */
    @NotNull(message = "年份不能为空")
    private Integer voucherYear;

    /**
     * 月份
     */
    @NotNull(message = "月份不能为空")
    private Integer voucherMonth;

    /**
     * 日期
     */
    @JsonFormat(pattern = "yyyy-MM-dd", timezone = "GMT+8")
    private Date voucherDate;
}
