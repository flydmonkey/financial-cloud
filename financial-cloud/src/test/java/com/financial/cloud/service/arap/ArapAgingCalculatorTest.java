package com.financial.cloud.service.arap;

import com.financial.cloud.dto.arap.ArapAgingVo;
import com.financial.cloud.dto.arap.ArapMovementRow;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Calendar;
import java.util.Date;
import java.util.GregorianCalendar;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ArapAgingCalculatorTest {

	@Test
	void fifoBucketsSumToEndingBalance() {
		List<ArapMovementRow> rows = List.of(
				row("c1", "客户A", date(2025, 1, 10), "100", "0"),
				row("c1", "客户A", date(2025, 2, 15), "50", "0"),
				row("c1", "客户A", date(2025, 3, 1), "0", "40")
		);
		List<ArapAgingVo> aged = ArapAgingCalculator.age(rows, LocalDate.of(2025, 3, 31), true, true);
		assertEquals(1, aged.size());
		ArapAgingVo vo = aged.get(0);
		assertEquals(0, new BigDecimal("110").compareTo(vo.getTotal()));
		BigDecimal sum = vo.getBucket0To30()
				.add(vo.getBucket31To60())
				.add(vo.getBucket61To90())
				.add(vo.getBucket91To180())
				.add(vo.getBucketOver180());
		assertEquals(0, vo.getTotal().compareTo(sum));
	}

	@Test
	void subjectMatchCustomerRoots() {
		assertTrue(ArapRules.subjectMatchesSide("1122", ArapRules.SIDE_RECEIVABLE));
		assertTrue(ArapRules.subjectMatchesSide("1122.01", ArapRules.SIDE_RECEIVABLE));
		assertTrue(ArapRules.subjectMatchesSide("2202", ArapRules.SIDE_PAYABLE));
		assertEquals(ArapRules.ASSIST_CUSTOMER, ArapRules.assistTypeForSide(ArapRules.SIDE_RECEIVABLE));
		assertEquals(ArapRules.ASSIST_SUPPLIER, ArapRules.assistTypeForSide(ArapRules.SIDE_PAYABLE));
	}

	private static ArapMovementRow row(String id, String name, Date date, String debit, String credit) {
		ArapMovementRow r = new ArapMovementRow();
		r.setCounterpartId(id);
		r.setCounterpartName(name);
		r.setVoucherDate(date);
		r.setVoucherId(date.getTime() + "");
		r.setDebitAmount(new BigDecimal(debit));
		r.setCreditAmount(new BigDecimal(credit));
		r.setSubjectCode("1122");
		return r;
	}

	private static Date date(int y, int m, int d) {
		return new GregorianCalendar(y, m - 1, d).getTime();
	}
}
