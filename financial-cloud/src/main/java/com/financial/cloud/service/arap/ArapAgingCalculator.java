package com.financial.cloud.service.arap;

import com.financial.cloud.dto.arap.ArapAgingVo;
import com.financial.cloud.dto.arap.ArapMovementRow;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.Date;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * FIFO open-item aging by voucher date.
 */
public final class ArapAgingCalculator {

	private ArapAgingCalculator() {
	}

	public static List<ArapAgingVo> age(
			List<ArapMovementRow> movements,
			LocalDate asOf,
			boolean receivableSide,
			boolean includeZero) {
		return age(movements, asOf, receivableSide, includeZero, ArapWriteoffRules.AGING_FIFO_ESTIMATE);
	}

	public static List<ArapAgingVo> age(
			List<ArapMovementRow> movements,
			LocalDate asOf,
			boolean receivableSide,
			boolean includeZero,
			String agingMethod) {
		Map<String, List<ArapMovementRow>> byCounterpart = new LinkedHashMap<>();
		for (ArapMovementRow row : movements) {
			if (row.getVoucherDate() == null) {
				continue;
			}
			LocalDate d = toLocalDate(row.getVoucherDate());
			if (d.isAfter(asOf)) {
				continue;
			}
			String id = row.getCounterpartId() == null ? "" : row.getCounterpartId();
			byCounterpart.computeIfAbsent(id, k -> new ArrayList<>()).add(row);
		}
		List<ArapAgingVo> result = new ArrayList<>();
		for (Map.Entry<String, List<ArapMovementRow>> e : byCounterpart.entrySet()) {
			List<ArapMovementRow> rows = e.getValue();
			rows.sort(Comparator.comparing(ArapMovementRow::getVoucherDate)
					.thenComparing(r -> r.getVoucherId() == null ? "" : r.getVoucherId()));
			List<Layer> layers = new ArrayList<>();
			for (ArapMovementRow row : rows) {
				BigDecimal signed = signedAmount(row, receivableSide);
				if (signed.compareTo(BigDecimal.ZERO) > 0) {
					layers.add(new Layer(toLocalDate(row.getVoucherDate()), signed));
				} else if (signed.compareTo(BigDecimal.ZERO) < 0) {
					consume(layers, signed.abs());
				}
			}
			BigDecimal b0 = BigDecimal.ZERO;
			BigDecimal b31 = BigDecimal.ZERO;
			BigDecimal b61 = BigDecimal.ZERO;
			BigDecimal b91 = BigDecimal.ZERO;
			BigDecimal b180 = BigDecimal.ZERO;
			for (Layer layer : layers) {
				if (layer.remaining.compareTo(BigDecimal.ZERO) <= 0) {
					continue;
				}
				long days = ChronoUnit.DAYS.between(layer.date, asOf);
				if (days <= 30) {
					b0 = b0.add(layer.remaining);
				} else if (days <= 60) {
					b31 = b31.add(layer.remaining);
				} else if (days <= 90) {
					b61 = b61.add(layer.remaining);
				} else if (days <= 180) {
					b91 = b91.add(layer.remaining);
				} else {
					b180 = b180.add(layer.remaining);
				}
			}
			BigDecimal total = b0.add(b31).add(b61).add(b91).add(b180);
			if (!includeZero && total.compareTo(BigDecimal.ZERO) == 0) {
				continue;
			}
			String name = rows.isEmpty() ? e.getKey() : rows.get(0).getCounterpartName();
			result.add(ArapAgingVo.builder()
					.counterpartId(e.getKey())
					.counterpartName(name)
					.bucket0To30(b0)
					.bucket31To60(b31)
					.bucket61To90(b61)
					.bucket91To180(b91)
					.bucketOver180(b180)
					.total(total)
					.agingMethod(agingMethod)
					.build());
		}
		return result;
	}

	static BigDecimal signedAmount(ArapMovementRow row, boolean receivableSide) {
		BigDecimal debit = nz(row.getDebitAmount());
		BigDecimal credit = nz(row.getCreditAmount());
		if (receivableSide) {
			return debit.subtract(credit);
		}
		return credit.subtract(debit);
	}

	private static void consume(List<Layer> layers, BigDecimal amount) {
		BigDecimal left = amount;
		for (Layer layer : layers) {
			if (left.compareTo(BigDecimal.ZERO) <= 0) {
				break;
			}
			if (layer.remaining.compareTo(BigDecimal.ZERO) <= 0) {
				continue;
			}
			BigDecimal use = layer.remaining.min(left);
			layer.remaining = layer.remaining.subtract(use);
			left = left.subtract(use);
		}
	}

	private static BigDecimal nz(BigDecimal v) {
		return v == null ? BigDecimal.ZERO : v;
	}

	private static LocalDate toLocalDate(Date date) {
		return date.toInstant().atZone(ZoneId.systemDefault()).toLocalDate();
	}

	private static final class Layer {
		private final LocalDate date;
		private BigDecimal remaining;

		private Layer(LocalDate date, BigDecimal remaining) {
			this.date = date;
			this.remaining = remaining;
		}
	}
}
