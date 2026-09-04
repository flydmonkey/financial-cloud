package com.financial.cloud.service.arap;

import com.financial.cloud.dto.arap.ArapMovementRow;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Pure open-item / write-off helpers (no persistence).
 */
public final class ArapWriteoffRules {

	public static final String AGING_OPEN_ITEM = "OPEN_ITEM";
	public static final String AGING_FIFO_ESTIMATE = "FIFO_ESTIMATE";

	private ArapWriteoffRules() {
	}

	public static Map<String, BigDecimal> remainingByItem(
			List<ArapMovementRow> movements,
			boolean receivableSide,
			Map<String, BigDecimal> writtenOffByItem) {
		Map<String, BigDecimal> remaining = new LinkedHashMap<>();
		for (ArapMovementRow row : movements) {
			String itemId = row.getVoucherItemId();
			if (itemId == null || itemId.isBlank()) {
				continue;
			}
			BigDecimal original = ArapAgingCalculator.signedAmount(row, receivableSide).abs();
			BigDecimal written = writtenOffByItem.getOrDefault(itemId, BigDecimal.ZERO);
			BigDecimal rem = original.subtract(written);
			if (rem.compareTo(BigDecimal.ZERO) < 0) {
				rem = BigDecimal.ZERO;
			}
			remaining.put(itemId, rem);
		}
		return remaining;
	}

	public static boolean isIncreaseSide(ArapMovementRow row, boolean receivableSide) {
		return ArapAgingCalculator.signedAmount(row, receivableSide).compareTo(BigDecimal.ZERO) > 0;
	}

	public static void validateMatchLegs(
			List<ArapMovementRow> movements,
			boolean receivableSide,
			Map<String, BigDecimal> remainingByItem,
			Map<String, BigDecimal> legAmounts,
			String expectedCounterpartId) {
		if (legAmounts == null || legAmounts.size() < 2) {
			throw new IllegalArgumentException("核销至少需要两行分录");
		}
		Map<String, ArapMovementRow> byId = indexByItem(movements);
		BigDecimal increaseSum = BigDecimal.ZERO;
		BigDecimal decreaseSum = BigDecimal.ZERO;
		boolean hasInc = false;
		boolean hasDec = false;
		for (Map.Entry<String, BigDecimal> e : legAmounts.entrySet()) {
			ArapMovementRow row = byId.get(e.getKey());
			if (row == null) {
				throw new IllegalArgumentException("分录不存在或不属于该往来: " + e.getKey());
			}
			String cp = row.getCounterpartId() == null ? "" : row.getCounterpartId();
			if (!cp.equals(expectedCounterpartId == null ? "" : expectedCounterpartId)) {
				throw new IllegalArgumentException("不能跨往来单位核销");
			}
			BigDecimal amt = nz(e.getValue());
			if (amt.compareTo(BigDecimal.ZERO) <= 0) {
				throw new IllegalArgumentException("核销金额必须大于0");
			}
			BigDecimal rem = remainingByItem.getOrDefault(e.getKey(), BigDecimal.ZERO);
			if (amt.compareTo(rem) > 0) {
				throw new IllegalArgumentException("核销金额超过未核销余额: " + e.getKey());
			}
			if (isIncreaseSide(row, receivableSide)) {
				increaseSum = increaseSum.add(amt);
				hasInc = true;
			} else if (ArapAgingCalculator.signedAmount(row, receivableSide).compareTo(BigDecimal.ZERO) < 0) {
				decreaseSum = decreaseSum.add(amt);
				hasDec = true;
			} else {
				throw new IllegalArgumentException("零金额分录不可核销");
			}
		}
		if (!hasInc || !hasDec) {
			throw new IllegalArgumentException("核销须同时包含挂账与冲减分录");
		}
		if (increaseSum.compareTo(decreaseSum) != 0) {
			throw new IllegalArgumentException("挂账侧与冲减侧核销合计必须相等");
		}
	}

	/** FIFO suggest: consume increase remainders against decrease remainders. */
	public static List<Map.Entry<String, BigDecimal>> suggestLegs(
			List<ArapMovementRow> movements,
			boolean receivableSide,
			Map<String, BigDecimal> remainingByItem) {
		List<ArapMovementRow> sorted = new ArrayList<>(movements);
		sorted.sort((a, b) -> {
			java.util.Date da = a.getVoucherDate();
			java.util.Date db = b.getVoucherDate();
			if (da == null && db == null) {
				return 0;
			}
			if (da == null) {
				return 1;
			}
			if (db == null) {
				return -1;
			}
			int c = da.compareTo(db);
			if (c != 0) {
				return c;
			}
			return String.valueOf(a.getVoucherItemId()).compareTo(String.valueOf(b.getVoucherItemId()));
		});
		List<ItemRem> increases = new ArrayList<>();
		List<ItemRem> decreases = new ArrayList<>();
		for (ArapMovementRow row : sorted) {
			BigDecimal rem = remainingByItem.getOrDefault(row.getVoucherItemId(), BigDecimal.ZERO);
			if (rem.compareTo(BigDecimal.ZERO) <= 0) {
				continue;
			}
			if (isIncreaseSide(row, receivableSide)) {
				increases.add(new ItemRem(row.getVoucherItemId(), rem));
			} else if (ArapAgingCalculator.signedAmount(row, receivableSide).compareTo(BigDecimal.ZERO) < 0) {
				decreases.add(new ItemRem(row.getVoucherItemId(), rem));
			}
		}
		Map<String, BigDecimal> legs = new LinkedHashMap<>();
		int i = 0;
		int d = 0;
		while (i < increases.size() && d < decreases.size()) {
			ItemRem inc = increases.get(i);
			ItemRem dec = decreases.get(d);
			BigDecimal use = inc.remaining.min(dec.remaining);
			if (use.compareTo(BigDecimal.ZERO) <= 0) {
				break;
			}
			legs.merge(inc.itemId, use, BigDecimal::add);
			legs.merge(dec.itemId, use, BigDecimal::add);
			inc.remaining = inc.remaining.subtract(use);
			dec.remaining = dec.remaining.subtract(use);
			if (inc.remaining.compareTo(BigDecimal.ZERO) <= 0) {
				i++;
			}
			if (dec.remaining.compareTo(BigDecimal.ZERO) <= 0) {
				d++;
			}
		}
		return new ArrayList<>(legs.entrySet());
	}

	/** Scale movements by remaining / original for FIFO aging after write-offs. */
	public static List<ArapMovementRow> scaleMovementsToRemaining(
			List<ArapMovementRow> movements,
			boolean receivableSide,
			Map<String, BigDecimal> remainingByItem) {
		List<ArapMovementRow> out = new ArrayList<>();
		for (ArapMovementRow row : movements) {
			String itemId = row.getVoucherItemId();
			BigDecimal original = ArapAgingCalculator.signedAmount(row, receivableSide).abs();
			if (original.compareTo(BigDecimal.ZERO) == 0) {
				continue;
			}
			BigDecimal rem = remainingByItem.getOrDefault(itemId, BigDecimal.ZERO);
			if (rem.compareTo(BigDecimal.ZERO) <= 0) {
				continue;
			}
			ArapMovementRow copy = new ArapMovementRow();
			copy.setCounterpartId(row.getCounterpartId());
			copy.setCounterpartName(row.getCounterpartName());
			copy.setAssistType(row.getAssistType());
			copy.setSubjectCode(row.getSubjectCode());
			copy.setSubjectName(row.getSubjectName());
			copy.setSummary(row.getSummary());
			copy.setVoucherId(row.getVoucherId());
			copy.setVoucherWord(row.getVoucherWord());
			copy.setVoucherDate(row.getVoucherDate());
			copy.setVoucherYear(row.getVoucherYear());
			copy.setVoucherMonth(row.getVoucherMonth());
			copy.setVoucherItemId(row.getVoucherItemId());
			boolean increase = isIncreaseSide(row, receivableSide);
			if (receivableSide) {
				if (increase) {
					copy.setDebitAmount(rem);
					copy.setCreditAmount(BigDecimal.ZERO);
				} else {
					copy.setDebitAmount(BigDecimal.ZERO);
					copy.setCreditAmount(rem);
				}
			} else {
				if (increase) {
					copy.setCreditAmount(rem);
					copy.setDebitAmount(BigDecimal.ZERO);
				} else {
					copy.setCreditAmount(BigDecimal.ZERO);
					copy.setDebitAmount(rem);
				}
			}
			out.add(copy);
		}
		return out;
	}

	private static Map<String, ArapMovementRow> indexByItem(List<ArapMovementRow> movements) {
		Map<String, ArapMovementRow> map = new HashMap<>();
		for (ArapMovementRow row : movements) {
			if (row.getVoucherItemId() != null) {
				map.put(row.getVoucherItemId(), row);
			}
		}
		return map;
	}

	private static BigDecimal nz(BigDecimal v) {
		return v == null ? BigDecimal.ZERO : v;
	}

	private static final class ItemRem {
		private final String itemId;
		private BigDecimal remaining;

		private ItemRem(String itemId, BigDecimal remaining) {
			this.itemId = itemId;
			this.remaining = remaining;
		}
	}
}
