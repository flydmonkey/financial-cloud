package com.financial.cloud.dto.arap;

import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;
import java.util.Date;

@Data
@Builder
public class ArapOpenItemVo {
	private String voucherItemId;
	private String voucherId;
	private String voucherWord;
	private Date voucherDate;
	private Integer voucherYear;
	private Integer voucherMonth;
	private String summary;
	private String subjectCode;
	private String subjectName;
	/** true = 挂账侧（AR借/AP贷） */
	private boolean increaseSide;
	private BigDecimal originalAmount;
	private BigDecimal writtenOffAmount;
	private BigDecimal remainingAmount;
}
