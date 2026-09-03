package com.financial.cloud.dto.arap;

import lombok.Data;

import java.math.BigDecimal;
import java.util.Date;

@Data
public class ArapMovementRow {
	private String counterpartId;
	private String counterpartName;
	private String assistType;
	private String subjectCode;
	private String subjectName;
	private BigDecimal debitAmount;
	private BigDecimal creditAmount;
	private String summary;
	private String voucherId;
	private String voucherWord;
	private Date voucherDate;
	private Integer voucherYear;
	private Integer voucherMonth;
	private String voucherItemId;
}
