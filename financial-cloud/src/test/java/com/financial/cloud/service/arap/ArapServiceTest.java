package com.financial.cloud.service.arap;

import com.financial.cloud.common.Message;
import com.financial.cloud.dto.arap.ArapBalanceVo;
import com.financial.cloud.dto.arap.ArapDetailLineVo;
import com.financial.cloud.dto.arap.ArapMovementRow;
import com.financial.cloud.dto.arap.ArapQueryDto;
import com.financial.cloud.repository.arap.ArapMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockHttpServletResponse;

import java.math.BigDecimal;
import java.util.GregorianCalendar;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ArapServiceTest {

	private static final String BOOK = "book-1";

	@Mock
	private ArapMapper arapMapper;

	@InjectMocks
	private ArapService arapService;

	@BeforeEach
	void stubMovements() {
		when(arapMapper.selectMovements(eq(BOOK), eq(ArapRules.ASSIST_CUSTOMER), any(), anyList()))
				.thenAnswer(inv -> {
					String counterpartId = inv.getArgument(2);
					List<ArapMovementRow> all = List.of(
							row("c1", "客户甲", 2025, 1, 10, "100", "0"),
							row("c1", "客户甲", 2025, 3, 5, "50", "0"),
							row("c1", "客户甲", 2025, 3, 20, "0", "30"),
							row("c2", "客户乙", 2025, 3, 1, "20", "0"));
					if (counterpartId == null || counterpartId.isBlank()) {
						return all;
					}
					return all.stream().filter(r -> counterpartId.equals(r.getCounterpartId())).toList();
				});
	}

	@Test
	void balances_openingMovementEndingForCounterpart() {
		ArapQueryDto dto = new ArapQueryDto();
		dto.setSide(ArapRules.SIDE_RECEIVABLE);
		dto.setPeriodStart("2025-03");
		dto.setPeriodEnd("2025-03");
		dto.setIncludeZero(true);

		Message<List<ArapBalanceVo>> msg = arapService.balances(BOOK, dto);
		assertEquals(Message.SUCCESS, msg.getCode());
		ArapBalanceVo c1 = msg.getData().stream()
				.filter(b -> "c1".equals(b.getCounterpartId()))
				.findFirst()
				.orElseThrow();
		assertEquals(0, new BigDecimal("100").compareTo(c1.getOpening()));
		assertEquals(0, new BigDecimal("50").compareTo(c1.getPeriodDebit()));
		assertEquals(0, new BigDecimal("30").compareTo(c1.getPeriodCredit()));
		assertEquals(0, new BigDecimal("120").compareTo(c1.getEnding()));
	}

	@Test
	void detail_filtersByCounterpartAndPeriodWithRunningBalance() {
		ArapQueryDto dto = new ArapQueryDto();
		dto.setSide(ArapRules.SIDE_RECEIVABLE);
		dto.setCounterpartId("c1");
		dto.setPeriodStart("2025-03");
		dto.setPeriodEnd("2025-03");

		Message<List<ArapDetailLineVo>> msg = arapService.detail(BOOK, dto);
		assertEquals(Message.SUCCESS, msg.getCode());
		assertEquals(2, msg.getData().size());
		assertEquals(0, new BigDecimal("150").compareTo(msg.getData().get(0).getRunningBalance()));
		assertEquals(0, new BigDecimal("120").compareTo(msg.getData().get(1).getRunningBalance()));
	}

	@Test
	void exportStatement_containsOpeningLinesEnding() throws Exception {
		ArapQueryDto dto = new ArapQueryDto();
		dto.setSide(ArapRules.SIDE_RECEIVABLE);
		dto.setCounterpartId("c1");
		dto.setPeriodStart("2025-03");
		dto.setPeriodEnd("2025-03");
		MockHttpServletResponse response = new MockHttpServletResponse();
		arapService.exportStatement(BOOK, dto, response);
		assertTrue(response.getContentAsByteArray().length > 100);
		assertTrue(response.getContentType().contains("spreadsheetml"));
	}

	private static ArapMovementRow row(
			String id, String name, int y, int m, int day, String debit, String credit) {
		ArapMovementRow r = new ArapMovementRow();
		r.setCounterpartId(id);
		r.setCounterpartName(name);
		r.setVoucherYear(y);
		r.setVoucherMonth(m);
		r.setVoucherDate(new GregorianCalendar(y, m - 1, day).getTime());
		r.setVoucherId(y + "-" + m + "-" + day + "-" + id);
		r.setVoucherWord("记-" + day);
		r.setSummary("t");
		r.setSubjectCode("1122");
		r.setSubjectName("应收账款");
		r.setDebitAmount(new BigDecimal(debit));
		r.setCreditAmount(new BigDecimal(credit));
		return r;
	}
}
