package com.financial.cloud.dto.arap;

import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;

@Data
@Builder
public class ArapMonthEndSummaryVo {
	private BigDecimal receivableTotal;
	private BigDecimal payableTotal;
	private BigDecimal overdueReceivable;
	private BigDecimal overduePayable;
	private boolean hasOverdue;
}
