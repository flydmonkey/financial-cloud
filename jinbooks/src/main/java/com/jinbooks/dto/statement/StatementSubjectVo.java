package com.jinbooks.dto.statement;

import com.jinbooks.domain.book.BookSubject;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.math.BigDecimal;

/**
 * 简介说明: 统计报表中的科目对象
 *
 * @author wuyan
 * {@code @date} 2025/03/18 13:49:13
 * {@code @version} 1.0
 */

@EqualsAndHashCode(callSuper = true)
@Data
public class StatementSubjectVo extends BookSubject {
	

    /**
	 * 
	 */
	private static final long serialVersionUID = -3175152469296124678L;

	/**
     * 借方金额
     */
    private BigDecimal debitAmount;

    /**
     * 贷方金额
     */
    private BigDecimal creditAmount;

}
