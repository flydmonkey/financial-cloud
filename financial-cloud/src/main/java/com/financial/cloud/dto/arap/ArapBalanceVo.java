package com.financial.cloud.dto.arap;

import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;

@Data
@Builder
public class ArapBalanceVo {
	private String counterpartId;
	private String counterpartName;
	private BigDecimal opening;
	private BigDecimal periodDebit;
	private BigDecimal periodCredit;
	private BigDecimal ending;
}
