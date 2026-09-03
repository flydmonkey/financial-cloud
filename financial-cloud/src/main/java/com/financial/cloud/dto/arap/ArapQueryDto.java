package com.financial.cloud.dto.arap;

import lombok.Data;

@Data
public class ArapQueryDto {
	/** AR or AP */
	private String side;
	/** yyyy-MM period start inclusive (balance/detail/statement) */
	private String periodStart;
	/** yyyy-MM period end inclusive */
	private String periodEnd;
	/** as-of date yyyy-MM-dd for aging */
	private String asOfDate;
	private String counterpartId;
	private boolean includeZero = false;
}
