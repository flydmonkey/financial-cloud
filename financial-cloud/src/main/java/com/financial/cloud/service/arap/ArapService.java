package com.financial.cloud.service.arap;

import com.financial.cloud.common.Message;
import com.financial.cloud.dto.arap.ArapAgingVo;
import com.financial.cloud.dto.arap.ArapBalanceVo;
import com.financial.cloud.dto.arap.ArapDetailLineVo;
import com.financial.cloud.dto.arap.ArapMonthEndSummaryVo;
import com.financial.cloud.dto.arap.ArapMovementRow;
import com.financial.cloud.dto.arap.ArapQueryDto;
import com.financial.cloud.repository.arap.ArapMapper;
import com.financial.cloud.util.DateUtils;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.apache.commons.lang3.StringUtils;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.math.BigDecimal;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.time.YearMonth;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.Date;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class ArapService {

	private final ArapMapper arapMapper;

	public Message<List<ArapBalanceVo>> balances(String bookId, ArapQueryDto dto) {
		String side = normalizeSide(dto.getSide());
		YearMonth start = parsePeriod(dto.getPeriodStart(), true);
		YearMonth end = parsePeriod(dto.getPeriodEnd(), false);
		if (end.isBefore(start)) {
			return Message.failed("期间结束不能早于开始");
		}
		List<ArapMovementRow> rows = loadFiltered(bookId, side, null);
		Map<String, Acc> acc = new LinkedHashMap<>();
		boolean receivable = ArapRules.isReceivableSide(side);
		for (ArapMovementRow row : rows) {
			YearMonth ym = yearMonthOf(row);
			if (ym == null) {
				continue;
			}
			String id = nullToEmpty(row.getCounterpartId());
			Acc a = acc.computeIfAbsent(id, k -> new Acc(row.getCounterpartName()));
			BigDecimal signed = ArapAgingCalculator.signedAmount(row, receivable);
			if (ym.isBefore(start)) {
				a.opening = a.opening.add(signed);
			} else if (!ym.isAfter(end)) {
				a.periodDebit = a.periodDebit.add(nz(row.getDebitAmount()));
				a.periodCredit = a.periodCredit.add(nz(row.getCreditAmount()));
				a.periodSigned = a.periodSigned.add(signed);
			}
		}
		List<ArapBalanceVo> list = new ArrayList<>();
		for (Map.Entry<String, Acc> e : acc.entrySet()) {
			Acc a = e.getValue();
			BigDecimal ending = a.opening.add(a.periodSigned);
			if (!dto.isIncludeZero()
					&& a.opening.compareTo(BigDecimal.ZERO) == 0
					&& a.periodDebit.compareTo(BigDecimal.ZERO) == 0
					&& a.periodCredit.compareTo(BigDecimal.ZERO) == 0
					&& ending.compareTo(BigDecimal.ZERO) == 0) {
				continue;
			}
			list.add(ArapBalanceVo.builder()
					.counterpartId(e.getKey())
					.counterpartName(a.name)
					.opening(a.opening)
					.periodDebit(a.periodDebit)
					.periodCredit(a.periodCredit)
					.ending(ending)
					.build());
		}
		list.sort(Comparator.comparing(ArapBalanceVo::getCounterpartName, Comparator.nullsLast(String::compareTo)));
		return Message.ok(list);
	}

	public Message<List<ArapDetailLineVo>> detail(String bookId, ArapQueryDto dto) {
		if (StringUtils.isBlank(dto.getCounterpartId())) {
			return Message.failed("请选择往来单位");
		}
		String side = normalizeSide(dto.getSide());
		YearMonth start = parsePeriod(dto.getPeriodStart(), true);
		YearMonth end = parsePeriod(dto.getPeriodEnd(), false);
		boolean receivable = ArapRules.isReceivableSide(side);
		List<ArapMovementRow> rows = loadFiltered(bookId, side, dto.getCounterpartId());
		BigDecimal running = BigDecimal.ZERO;
		for (ArapMovementRow row : rows) {
			YearMonth ym = yearMonthOf(row);
			if (ym != null && ym.isBefore(start)) {
				running = running.add(ArapAgingCalculator.signedAmount(row, receivable));
			}
		}
		List<ArapDetailLineVo> lines = new ArrayList<>();
		for (ArapMovementRow row : rows) {
			YearMonth ym = yearMonthOf(row);
			if (ym == null || ym.isBefore(start) || ym.isAfter(end)) {
				continue;
			}
			running = running.add(ArapAgingCalculator.signedAmount(row, receivable));
			lines.add(ArapDetailLineVo.builder()
					.voucherDate(row.getVoucherDate())
					.voucherId(row.getVoucherId())
					.voucherWord(row.getVoucherWord())
					.summary(row.getSummary())
					.subjectCode(row.getSubjectCode())
					.subjectName(row.getSubjectName())
					.debitAmount(nz(row.getDebitAmount()))
					.creditAmount(nz(row.getCreditAmount()))
					.runningBalance(running)
					.build());
		}
		return Message.ok(lines);
	}

	public Message<List<ArapAgingVo>> aging(String bookId, ArapQueryDto dto) {
		String side = normalizeSide(dto.getSide());
		LocalDate asOf = parseAsOf(dto.getAsOfDate());
		List<ArapMovementRow> rows = loadFiltered(bookId, side, dto.getCounterpartId());
		List<ArapAgingVo> list = ArapAgingCalculator.age(
				rows, asOf, ArapRules.isReceivableSide(side), dto.isIncludeZero());
		return Message.ok(list);
	}

	public ArapMonthEndSummaryVo monthEndSummary(String bookId, String yearPeriod) {
		YearMonth ym = YearMonth.parse(yearPeriod);
		LocalDate asOf = ym.atEndOfMonth();
		ArapQueryDto arDto = new ArapQueryDto();
		arDto.setSide(ArapRules.SIDE_RECEIVABLE);
		arDto.setPeriodStart(yearPeriod);
		arDto.setPeriodEnd(yearPeriod);
		arDto.setIncludeZero(true);
		List<ArapBalanceVo> arBalances = balances(bookId, arDto).getData();
		ArapQueryDto apDto = new ArapQueryDto();
		apDto.setSide(ArapRules.SIDE_PAYABLE);
		apDto.setPeriodStart(yearPeriod);
		apDto.setPeriodEnd(yearPeriod);
		apDto.setIncludeZero(true);
		List<ArapBalanceVo> apBalances = balances(bookId, apDto).getData();

		BigDecimal arTotal = sumEnding(arBalances);
		BigDecimal apTotal = sumEnding(apBalances);

		ArapQueryDto arAge = new ArapQueryDto();
		arAge.setSide(ArapRules.SIDE_RECEIVABLE);
		arAge.setAsOfDate(asOf.toString());
		List<ArapAgingVo> arAging = aging(bookId, arAge).getData();
		ArapQueryDto apAge = new ArapQueryDto();
		apAge.setSide(ArapRules.SIDE_PAYABLE);
		apAge.setAsOfDate(asOf.toString());
		List<ArapAgingVo> apAging = aging(bookId, apAge).getData();

		BigDecimal overdueAr = sumOverdue(arAging);
		BigDecimal overdueAp = sumOverdue(apAging);
		return ArapMonthEndSummaryVo.builder()
				.receivableTotal(arTotal)
				.payableTotal(apTotal)
				.overdueReceivable(overdueAr)
				.overduePayable(overdueAp)
				.hasOverdue(overdueAr.compareTo(BigDecimal.ZERO) > 0 || overdueAp.compareTo(BigDecimal.ZERO) > 0)
				.build();
	}

	public void exportStatement(String bookId, ArapQueryDto dto, HttpServletResponse response) throws IOException {
		if (StringUtils.isBlank(dto.getCounterpartId())) {
			throw new IllegalArgumentException("请选择往来单位");
		}
		Message<List<ArapDetailLineVo>> detailMsg = detail(bookId, dto);
		if (detailMsg.getCode() != Message.SUCCESS) {
			throw new IllegalArgumentException(detailMsg.getMessage());
		}
		Message<List<ArapBalanceVo>> balMsg = balances(bookId, dto);
		ArapBalanceVo bal = balMsg.getData() == null ? null : balMsg.getData().stream()
				.filter(b -> dto.getCounterpartId().equals(b.getCounterpartId()))
				.findFirst()
				.orElse(null);
		String sideLabel = ArapRules.isReceivableSide(normalizeSide(dto.getSide())) ? "应收" : "应付";
		String fileName = URLEncoder.encode("对账单-" + sideLabel + ".xlsx", StandardCharsets.UTF_8)
				.replace("+", "%20");
		response.setContentType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet");
		response.setHeader("Content-Disposition", "attachment; filename*=UTF-8''" + fileName);
		try (XSSFWorkbook wb = new XSSFWorkbook()) {
			Sheet sheet = wb.createSheet("对账单");
			int r = 0;
			Row title = sheet.createRow(r++);
			title.createCell(0).setCellValue(sideLabel + "对账单");
			Row meta = sheet.createRow(r++);
			meta.createCell(0).setCellValue("期间");
			meta.createCell(1).setCellValue(dto.getPeriodStart() + " ~ " + dto.getPeriodEnd());
			meta.createCell(2).setCellValue("往来单位");
			meta.createCell(3).setCellValue(bal != null ? bal.getCounterpartName() : dto.getCounterpartId());
			Row open = sheet.createRow(r++);
			open.createCell(0).setCellValue("期初余额");
			open.createCell(1).setCellValue(bal == null ? 0 : bal.getOpening().doubleValue());
			Row header = sheet.createRow(r++);
			header.createCell(0).setCellValue("日期");
			header.createCell(1).setCellValue("凭证字号");
			header.createCell(2).setCellValue("摘要");
			header.createCell(3).setCellValue("借方");
			header.createCell(4).setCellValue("贷方");
			header.createCell(5).setCellValue("余额");
			for (ArapDetailLineVo line : detailMsg.getData()) {
				Row row = sheet.createRow(r++);
				row.createCell(0).setCellValue(line.getVoucherDate() == null ? ""
						: DateUtils.format(line.getVoucherDate(), DateUtils.FORMAT_DATE_YYYY_MM_DD));
				row.createCell(1).setCellValue(line.getVoucherWord());
				row.createCell(2).setCellValue(line.getSummary());
				row.createCell(3).setCellValue(nz(line.getDebitAmount()).doubleValue());
				row.createCell(4).setCellValue(nz(line.getCreditAmount()).doubleValue());
				row.createCell(5).setCellValue(nz(line.getRunningBalance()).doubleValue());
			}
			Row end = sheet.createRow(r);
			end.createCell(0).setCellValue("期末余额");
			end.createCell(1).setCellValue(bal == null ? 0 : bal.getEnding().doubleValue());
			wb.write(response.getOutputStream());
		}
	}

	private List<ArapMovementRow> loadFiltered(String bookId, String side, String counterpartId) {
		String assist = ArapRules.assistTypeForSide(side);
		List<String> prefixes = ArapRules.subjectPrefixesForSide(side);
		List<ArapMovementRow> raw = arapMapper.selectMovements(bookId, assist, counterpartId, prefixes);
		List<ArapMovementRow> filtered = new ArrayList<>();
		for (ArapMovementRow row : raw) {
			if (ArapRules.subjectMatchesSide(row.getSubjectCode(), side)) {
				filtered.add(row);
			}
		}
		return filtered;
	}

	private static String normalizeSide(String side) {
		return ArapRules.SIDE_PAYABLE.equalsIgnoreCase(side)
				? ArapRules.SIDE_PAYABLE
				: ArapRules.SIDE_RECEIVABLE;
	}

	private static YearMonth parsePeriod(String period, boolean startDefault) {
		if (StringUtils.isBlank(period)) {
			YearMonth now = YearMonth.now();
			return startDefault ? now.withMonth(1) : now;
		}
		return YearMonth.parse(period.trim());
	}

	private static LocalDate parseAsOf(String asOfDate) {
		if (StringUtils.isBlank(asOfDate)) {
			return LocalDate.now();
		}
		return LocalDate.parse(asOfDate.trim());
	}

	private static YearMonth yearMonthOf(ArapMovementRow row) {
		if (row.getVoucherYear() != null && row.getVoucherMonth() != null) {
			return YearMonth.of(row.getVoucherYear(), row.getVoucherMonth());
		}
		if (row.getVoucherDate() != null) {
			LocalDate d = row.getVoucherDate().toInstant().atZone(ZoneId.systemDefault()).toLocalDate();
			return YearMonth.from(d);
		}
		return null;
	}

	private static BigDecimal sumEnding(List<ArapBalanceVo> list) {
		if (list == null) {
			return BigDecimal.ZERO;
		}
		return list.stream().map(ArapBalanceVo::getEnding).reduce(BigDecimal.ZERO, BigDecimal::add);
	}

	private static BigDecimal sumOverdue(List<ArapAgingVo> list) {
		if (list == null) {
			return BigDecimal.ZERO;
		}
		BigDecimal sum = BigDecimal.ZERO;
		for (ArapAgingVo a : list) {
			sum = sum.add(nz(a.getBucket31To60()))
					.add(nz(a.getBucket61To90()))
					.add(nz(a.getBucket91To180()))
					.add(nz(a.getBucketOver180()));
		}
		return sum;
	}

	private static BigDecimal nz(BigDecimal v) {
		return v == null ? BigDecimal.ZERO : v;
	}

	private static String nullToEmpty(String v) {
		return v == null ? "" : v;
	}

	private static final class Acc {
		private final String name;
		private BigDecimal opening = BigDecimal.ZERO;
		private BigDecimal periodDebit = BigDecimal.ZERO;
		private BigDecimal periodCredit = BigDecimal.ZERO;
		private BigDecimal periodSigned = BigDecimal.ZERO;

		private Acc(String name) {
			this.name = name;
		}
	}
}
