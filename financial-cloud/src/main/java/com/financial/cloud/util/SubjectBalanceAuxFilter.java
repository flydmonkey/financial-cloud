package com.financial.cloud.util;

import com.financial.cloud.domain.statement.StatementSubjectBalance;
import com.financial.cloud.enums.common.YesNoEnum;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Filters subject-balance rows by the product "显示辅助核算" (showAux) switch.
 */
public final class SubjectBalanceAuxFilter {

	private SubjectBalanceAuxFilter() {
	}

	/**
	 * @param showAux {@code true} keep auxiliary rows; {@code null}/{@code false} hide {@code is_auxiliary=y}
	 */
	public static List<StatementSubjectBalance> apply(List<StatementSubjectBalance> rows, Boolean showAux) {
		if (rows == null || rows.isEmpty()) {
			return Collections.emptyList();
		}
		if (Boolean.TRUE.equals(showAux)) {
			return new ArrayList<>(rows);
		}
		List<StatementSubjectBalance> out = new ArrayList<>();
		for (StatementSubjectBalance row : rows) {
			if (row == null) {
				continue;
			}
			if (!YesNoEnum.y.name().equals(row.getIsAuxiliary())) {
				out.add(row);
			}
		}
		return out;
	}
}
