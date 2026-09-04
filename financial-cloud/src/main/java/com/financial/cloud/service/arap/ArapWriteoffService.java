package com.financial.cloud.service.arap;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.financial.cloud.common.Message;
import com.financial.cloud.domain.arap.ArapWriteoff;
import com.financial.cloud.domain.arap.ArapWriteoffLine;
import com.financial.cloud.dto.arap.ArapMovementRow;
import com.financial.cloud.dto.arap.ArapOpenItemVo;
import com.financial.cloud.dto.arap.ArapQueryDto;
import com.financial.cloud.dto.arap.ArapWriteoffConfirmDto;
import com.financial.cloud.dto.arap.ArapWriteoffVo;
import com.financial.cloud.repository.arap.ArapMapper;
import com.financial.cloud.repository.arap.ArapWriteoffLineMapper;
import com.financial.cloud.repository.arap.ArapWriteoffMapper;
import com.financial.cloud.service.config.ConfigSysService;
import lombok.RequiredArgsConstructor;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.YearMonth;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class ArapWriteoffService {

	private final ArapMapper arapMapper;
	private final ArapWriteoffMapper writeoffMapper;
	private final ArapWriteoffLineMapper writeoffLineMapper;
	private final ConfigSysService configSysService;

	public Message<List<ArapOpenItemVo>> openItems(String bookId, ArapQueryDto dto) {
		String side = normalizeSide(dto.getSide());
		if (StringUtils.isBlank(dto.getCounterpartId())) {
			return Message.failed("请选择往来单位");
		}
		boolean receivable = ArapRules.isReceivableSide(side);
		List<ArapMovementRow> movements = loadFiltered(bookId, side, dto.getCounterpartId());
		LocalDate asOf = parseAsOf(dto.getAsOfDate());
		movements = movements.stream()
				.filter(r -> r.getVoucherDate() != null
						&& !toLocalDate(r.getVoucherDate()).isAfter(asOf))
				.collect(Collectors.toList());
		Map<String, BigDecimal> written = loadWrittenOff(bookId);
		Map<String, BigDecimal> remaining = ArapWriteoffRules.remainingByItem(movements, receivable, written);
		List<ArapOpenItemVo> list = new ArrayList<>();
		for (ArapMovementRow row : movements) {
			String itemId = row.getVoucherItemId();
			BigDecimal original = ArapAgingCalculator.signedAmount(row, receivable).abs();
			if (original.compareTo(BigDecimal.ZERO) == 0) {
				continue;
			}
			BigDecimal rem = remaining.getOrDefault(itemId, BigDecimal.ZERO);
			if (!dto.isIncludeZero() && rem.compareTo(BigDecimal.ZERO) <= 0) {
				continue;
			}
			BigDecimal wo = written.getOrDefault(itemId, BigDecimal.ZERO);
			list.add(ArapOpenItemVo.builder()
					.voucherItemId(itemId)
					.voucherId(row.getVoucherId())
					.voucherWord(row.getVoucherWord())
					.voucherDate(row.getVoucherDate())
					.voucherYear(row.getVoucherYear())
					.voucherMonth(row.getVoucherMonth())
					.summary(row.getSummary())
					.subjectCode(row.getSubjectCode())
					.subjectName(row.getSubjectName())
					.increaseSide(ArapWriteoffRules.isIncreaseSide(row, receivable))
					.originalAmount(original)
					.writtenOffAmount(wo.min(original))
					.remainingAmount(rem)
					.build());
		}
		return Message.ok(list);
	}

	public Message<List<ArapWriteoffConfirmDto.Leg>> suggest(String bookId, ArapQueryDto dto) {
		String side = normalizeSide(dto.getSide());
		if (StringUtils.isBlank(dto.getCounterpartId())) {
			return Message.failed("请选择往来单位");
		}
		boolean receivable = ArapRules.isReceivableSide(side);
		List<ArapMovementRow> movements = loadFiltered(bookId, side, dto.getCounterpartId());
		Map<String, BigDecimal> written = loadWrittenOff(bookId);
		Map<String, BigDecimal> remaining = ArapWriteoffRules.remainingByItem(movements, receivable, written);
		List<Map.Entry<String, BigDecimal>> legs = ArapWriteoffRules.suggestLegs(movements, receivable, remaining);
		List<ArapWriteoffConfirmDto.Leg> result = new ArrayList<>();
		for (Map.Entry<String, BigDecimal> e : legs) {
			ArapWriteoffConfirmDto.Leg leg = new ArapWriteoffConfirmDto.Leg();
			leg.setVoucherItemId(e.getKey());
			leg.setAmount(e.getValue());
			result.add(leg);
		}
		return Message.ok(result);
	}

	@Transactional(rollbackFor = Exception.class)
	public Message<String> confirm(String bookId, String userId, ArapWriteoffConfirmDto dto) {
		String side = normalizeSide(dto.getSide());
		if (StringUtils.isBlank(dto.getCounterpartId())) {
			return Message.failed("请选择往来单位");
		}
		if (dto.getLegs() == null || dto.getLegs().isEmpty()) {
			return Message.failed("请选择核销分录");
		}
		boolean receivable = ArapRules.isReceivableSide(side);
		List<ArapMovementRow> movements = loadFiltered(bookId, side, dto.getCounterpartId());
		Map<String, BigDecimal> written = loadWrittenOff(bookId);
		Map<String, BigDecimal> remaining = ArapWriteoffRules.remainingByItem(movements, receivable, written);
		Map<String, BigDecimal> legs = new LinkedHashMap<>();
		for (ArapWriteoffConfirmDto.Leg leg : dto.getLegs()) {
			legs.merge(leg.getVoucherItemId(), nz(leg.getAmount()), BigDecimal::add);
		}
		try {
			ArapWriteoffRules.validateMatchLegs(movements, receivable, remaining, legs, dto.getCounterpartId());
		} catch (IllegalArgumentException ex) {
			return Message.failed(ex.getMessage());
		}
		Map<String, ArapMovementRow> byId = movements.stream()
				.filter(r -> r.getVoucherItemId() != null)
				.collect(Collectors.toMap(ArapMovementRow::getVoucherItemId, r -> r, (a, b) -> a));
		BigDecimal headerAmt = BigDecimal.ZERO;
		for (Map.Entry<String, BigDecimal> e : legs.entrySet()) {
			if (ArapWriteoffRules.isIncreaseSide(byId.get(e.getKey()), receivable)) {
				headerAmt = headerAmt.add(e.getValue());
			}
		}
		ArapWriteoff header = ArapWriteoff.builder()
				.bookId(bookId)
				.side(side)
				.counterpartId(dto.getCounterpartId())
				.counterpartName(StringUtils.defaultIfBlank(dto.getCounterpartName(),
						byId.values().stream().findFirst().map(ArapMovementRow::getCounterpartName).orElse("")))
				.amount(headerAmt)
				.status(ArapWriteoff.STATUS_ACTIVE)
				.writeoffDate(new Date())
				.build();
		writeoffMapper.insert(header);
		for (Map.Entry<String, BigDecimal> e : legs.entrySet()) {
			ArapMovementRow row = byId.get(e.getKey());
			ArapWriteoffLine line = ArapWriteoffLine.builder()
					.writeoffId(header.getId())
					.bookId(bookId)
					.voucherItemId(e.getKey())
					.voucherId(row.getVoucherId())
					.voucherYear(row.getVoucherYear())
					.voucherMonth(row.getVoucherMonth())
					.amount(e.getValue())
					.build();
			writeoffLineMapper.insert(line);
		}
		return Message.ok(header.getId());
	}

	@Transactional(rollbackFor = Exception.class)
	public Message<String> reverse(String bookId, String writeoffId) {
		ArapWriteoff header = writeoffMapper.selectById(writeoffId);
		if (header == null || !bookId.equals(header.getBookId())) {
			return Message.failed("核销单不存在");
		}
		if (ArapWriteoff.STATUS_REVERSED.equals(header.getStatus())) {
			return Message.failed("核销单已撤销");
		}
		List<ArapWriteoffLine> lines = writeoffLineMapper.selectList(
				new LambdaQueryWrapper<ArapWriteoffLine>().eq(ArapWriteoffLine::getWriteoffId, writeoffId));
		String currentTerm = configSysService.getCurrentTerm(bookId);
		YearMonth open = YearMonth.parse(currentTerm);
		for (ArapWriteoffLine line : lines) {
			if (line.getVoucherYear() == null || line.getVoucherMonth() == null) {
				return Message.failed("核销分录缺少账期，无法撤销");
			}
			YearMonth ym = YearMonth.of(line.getVoucherYear(), line.getVoucherMonth());
			if (ym.isBefore(open)) {
				return Message.failed("存在已结账期分录，不能撤销核销");
			}
		}
		header.setStatus(ArapWriteoff.STATUS_REVERSED);
		writeoffMapper.updateById(header);
		return Message.ok(writeoffId);
	}

	public Message<List<ArapWriteoffVo>> list(String bookId, ArapQueryDto dto) {
		String side = normalizeSide(dto.getSide());
		LambdaQueryWrapper<ArapWriteoff> q = new LambdaQueryWrapper<ArapWriteoff>()
				.eq(ArapWriteoff::getBookId, bookId)
				.eq(ArapWriteoff::getSide, side)
				.orderByDesc(ArapWriteoff::getCreatedDate);
		if (StringUtils.isNotBlank(dto.getCounterpartId())) {
			q.eq(ArapWriteoff::getCounterpartId, dto.getCounterpartId());
		}
		List<ArapWriteoff> headers = writeoffMapper.selectList(q);
		List<ArapWriteoffVo> result = new ArrayList<>();
		for (ArapWriteoff h : headers) {
			List<ArapWriteoffLine> lines = writeoffLineMapper.selectList(
					new LambdaQueryWrapper<ArapWriteoffLine>().eq(ArapWriteoffLine::getWriteoffId, h.getId()));
			result.add(ArapWriteoffVo.builder()
					.id(h.getId())
					.side(h.getSide())
					.counterpartId(h.getCounterpartId())
					.counterpartName(h.getCounterpartName())
					.amount(h.getAmount())
					.status(h.getStatus())
					.writeoffDate(h.getWriteoffDate())
					.lines(lines.stream().map(l -> ArapWriteoffVo.Line.builder()
							.voucherItemId(l.getVoucherItemId())
							.voucherId(l.getVoucherId())
							.amount(l.getAmount())
							.build()).toList())
					.build());
		}
		return Message.ok(result);
	}

	public Map<String, BigDecimal> loadWrittenOff(String bookId) {
		List<Map<String, Object>> rows = writeoffLineMapper.sumActiveByItem(bookId);
		Map<String, BigDecimal> map = new HashMap<>();
		for (Map<String, Object> row : rows) {
			Object id = row.get("voucherItemId");
			if (id == null) {
				id = row.get("voucher_item_id");
			}
			Object amt = row.get("writtenOff");
			if (amt == null) {
				amt = row.get("written_off");
			}
			if (id != null && amt != null) {
				map.put(String.valueOf(id), new BigDecimal(amt.toString()));
			}
		}
		return map;
	}

	public boolean hasActiveWriteoffs(String bookId, String counterpartId, String side) {
		return writeoffLineMapper.countActive(bookId, counterpartId, side) > 0;
	}

	public List<ArapMovementRow> loadFiltered(String bookId, String side, String counterpartId) {
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

	private static LocalDate parseAsOf(String asOfDate) {
		if (StringUtils.isBlank(asOfDate)) {
			return LocalDate.now();
		}
		return LocalDate.parse(asOfDate.trim());
	}

	private static LocalDate toLocalDate(Date date) {
		return date.toInstant().atZone(ZoneId.systemDefault()).toLocalDate();
	}

	private static BigDecimal nz(BigDecimal v) {
		return v == null ? BigDecimal.ZERO : v;
	}
}
