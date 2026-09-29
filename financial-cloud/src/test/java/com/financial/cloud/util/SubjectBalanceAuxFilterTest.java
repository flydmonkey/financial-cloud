package com.financial.cloud.util;

import com.financial.cloud.domain.statement.StatementSubjectBalance;
import com.financial.cloud.enums.common.YesNoEnum;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class SubjectBalanceAuxFilterTest {

	@Test
	void hideAuxWhenShowAuxFalseOrNull() {
		StatementSubjectBalance plain = row("1001", YesNoEnum.n.name());
		StatementSubjectBalance aux = row("1001_A1", YesNoEnum.y.name());
		assertEquals(1, SubjectBalanceAuxFilter.apply(List.of(plain, aux), false).size());
		assertEquals(1, SubjectBalanceAuxFilter.apply(List.of(plain, aux), null).size());
		assertEquals("1001", SubjectBalanceAuxFilter.apply(List.of(plain, aux), false).get(0).getSubjectCode());
	}

	@Test
	void keepAuxWhenShowAuxTrue() {
		StatementSubjectBalance plain = row("1001", YesNoEnum.n.name());
		StatementSubjectBalance aux = row("1001_A1", YesNoEnum.y.name());
		assertEquals(2, SubjectBalanceAuxFilter.apply(List.of(plain, aux), true).size());
	}

	@Test
	void nullSafeEmpty() {
		assertTrue(SubjectBalanceAuxFilter.apply(null, true).isEmpty());
	}

	private static StatementSubjectBalance row(String code, String isAux) {
		StatementSubjectBalance b = new StatementSubjectBalance();
		b.setSubjectCode(code);
		b.setIsAuxiliary(isAux);
		return b;
	}
}
