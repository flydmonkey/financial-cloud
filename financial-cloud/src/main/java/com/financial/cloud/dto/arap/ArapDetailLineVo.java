package com.financial.cloud.dto.arap;

import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;
import java.util.Date;

@Data
@Builder
public class ArapDetailLineVo {
	private Date voucherDate;
	private String voucherId;
	private String voucherWord;
	private String summary;
	private String subjectCode;
	private String subjectName;
	private BigDecimal debitAmount;
	private BigDecimal creditAmount;
	private BigDecimal runningBalance;
}
