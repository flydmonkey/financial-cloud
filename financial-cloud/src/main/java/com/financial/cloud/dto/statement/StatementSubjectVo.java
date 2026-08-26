package com.financial.cloud.dto.statement;

import com.financial.cloud.domain.book.BookSubject;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.math.BigDecimal;

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
