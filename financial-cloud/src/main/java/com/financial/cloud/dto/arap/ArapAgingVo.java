package com.financial.cloud.dto.arap;

import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;

@Data
@Builder
public class ArapAgingVo {
	private String counterpartId;
	private String counterpartName;
	private BigDecimal bucket0To30;
	private BigDecimal bucket31To60;
	private BigDecimal bucket61To90;
	private BigDecimal bucket91To180;
	private BigDecimal bucketOver180;
	private BigDecimal total;
	/** OPEN_ITEM or FIFO_ESTIMATE */
	private String agingMethod;
}
