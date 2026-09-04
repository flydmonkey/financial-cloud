package com.financial.cloud.service.arap;

import com.financial.cloud.dto.arap.ArapMovementRow;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.GregorianCalendar;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ArapWriteoffRulesTest {

	@Test
	void partialWriteoffReducesRemaining() {
		List<ArapMovementRow> rows = List.of(
				row("i1", "c1", 100, 0),
				row("i2", "c1", 0, 60));
		Map<String, BigDecimal> written = Map.of("i1", new BigDecimal("40"), "i2", new BigDecimal("40"));
		Map<String, BigDecimal> rem = ArapWriteoffRules.remainingByItem(rows, true, written);
		assertEquals(0, new BigDecimal("60").compareTo(rem.get("i1")));
		assertEquals(0, new BigDecimal("20").compareTo(rem.get("i2")));
	}

	@Test
	void rejectCrossCounterpart() {
		List<ArapMovementRow> rows = List.of(
				row("i1", "c1", 100, 0),
				row("i2", "c2", 0, 100));
		Map<String, BigDecimal> rem = ArapWriteoffRules.remainingByItem(rows, true, Map.of());
		Map<String, BigDecimal> legs = Map.of("i1", new BigDecimal("50"), "i2", new BigDecimal("50"));
		IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
				() -> ArapWriteoffRules.validateMatchLegs(rows, true, rem, legs, "c1"));
		assertTrue(ex.getMessage().contains("跨往来"));
	}

	@Test
	void suggestCreatesBalancedLegs() {
		List<ArapMovementRow> rows = List.of(
				row("i1", "c1", 100, 0),
				row("i2", "c1", 0, 40));
		Map<String, BigDecimal> rem = ArapWriteoffRules.remainingByItem(rows, true, Map.of());
		List<Map.Entry<String, BigDecimal>> legs = ArapWriteoffRules.suggestLegs(rows, true, rem);
		Map<String, BigDecimal> map = new HashMap<>();
		for (Map.Entry<String, BigDecimal> e : legs) {
			map.put(e.getKey(), e.getValue());
		}
		assertEquals(0, new BigDecimal("40").compareTo(map.get("i1")));
		assertEquals(0, new BigDecimal("40").compareTo(map.get("i2")));
	}

	private static ArapMovementRow row(String itemId, String cp, double debit, double credit) {
		ArapMovementRow r = new ArapMovementRow();
		r.setVoucherItemId(itemId);
		r.setCounterpartId(cp);
		r.setCounterpartName(cp);
		r.setVoucherDate(new GregorianCalendar(2026, 7, 15).getTime());
		r.setVoucherId(itemId);
		r.setDebitAmount(BigDecimal.valueOf(debit));
		r.setCreditAmount(BigDecimal.valueOf(credit));
		r.setSubjectCode("1122");
		return r;
	}
}
