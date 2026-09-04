package com.financial.cloud.dto.arap;

import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;
import java.util.Date;
import java.util.List;

@Data
@Builder
public class ArapWriteoffVo {
	private String id;
	private String side;
	private String counterpartId;
	private String counterpartName;
	private BigDecimal amount;
	private String status;
	private Date writeoffDate;
	private List<Line> lines;

	@Data
	@Builder
	public static class Line {
		private String voucherItemId;
		private String voucherId;
		private BigDecimal amount;
	}
}
