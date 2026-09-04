package com.financial.cloud.dto.arap;

import lombok.Data;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

@Data
public class ArapWriteoffConfirmDto {
	private String side;
	private String counterpartId;
	private String counterpartName;
	private List<Leg> legs = new ArrayList<>();

	@Data
	public static class Leg {
		private String voucherItemId;
		private BigDecimal amount;
	}
}
