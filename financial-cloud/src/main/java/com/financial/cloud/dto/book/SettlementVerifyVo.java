package com.financial.cloud.dto.book;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class SettlementVerifyVo {
	int id;

	String item;

	/** true = passed or N/A; false = failed hard item */
	boolean result;

	boolean warning;

	/** hard system gate vs manual / not-system-checked placeholder */
	boolean hard;

	/** false when the check does not apply (e.g. no depreciable assets) */
	boolean applicable;

	String reason;

	/** Backward-compatible ctor used by older callers / tests */
	public SettlementVerifyVo(int id, String item, boolean result, boolean warning) {
		this(id, item, result, warning, true, true, null);
	}

	public static SettlementVerifyVo hardPass(int id, String item) {
		return new SettlementVerifyVo(id, item, true, false, true, true, null);
	}

	public static SettlementVerifyVo hardPassWarning(int id, String item, String reason) {
		return new SettlementVerifyVo(id, item, true, true, true, true, reason);
	}

	public static SettlementVerifyVo hardFail(int id, String item, String reason) {
		return new SettlementVerifyVo(id, item, false, false, true, true, reason);
	}

	public static SettlementVerifyVo hardNa(int id, String item, String reason) {
		return new SettlementVerifyVo(id, item, true, false, true, false, reason);
	}
}
