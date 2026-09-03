package com.financial.cloud.service.arap;

import com.financial.cloud.util.SubjectCodeCompat;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

/**
 * Assist types and AR/AP subject roots for L1/L2 counterpart ledgers.
 * <p>
 * Discovery (task 1.1): {@code assist_acc.assist_type} / {@code voucher_auxiliary.auxiliary}
 * use string codes {@code "2"}=客户, {@code "3"}=供应商 (see DistData subjects_auxiliary /
 * SQL comments). Subject roots follow 小企业 + 企业准则 aliases via {@link SubjectCodeCompat}.
 */
public final class ArapRules {

	public static final String ASSIST_CUSTOMER = "2";
	public static final String ASSIST_SUPPLIER = "3";

	public static final String SIDE_RECEIVABLE = "AR";
	public static final String SIDE_PAYABLE = "AP";

	/** 应收账款 / 预付 / 其他应收 */
	private static final List<String> AR_ROOTS = List.of("1122", "1123", "1221", "1131", "1151");
	/** 应付账款 / 预收 / 其他应付 */
	private static final List<String> AP_ROOTS = List.of("2202", "2203", "2241", "2121", "2131");

	private ArapRules() {
	}

	public static String assistTypeForSide(String side) {
		return SIDE_PAYABLE.equalsIgnoreCase(side) ? ASSIST_SUPPLIER : ASSIST_CUSTOMER;
	}

	public static boolean isReceivableSide(String side) {
		return !SIDE_PAYABLE.equalsIgnoreCase(side);
	}

	public static List<String> subjectPrefixesForSide(String side) {
		List<String> roots = isReceivableSide(side) ? AR_ROOTS : AP_ROOTS;
		Set<String> expanded = new LinkedHashSet<>();
		for (String root : roots) {
			expanded.addAll(SubjectCodeCompat.lookupCandidates(root));
		}
		return new ArrayList<>(expanded);
	}

	public static boolean subjectMatchesSide(String subjectCode, String side) {
		if (subjectCode == null || subjectCode.isBlank()) {
			return false;
		}
		String normalized = subjectCode.trim();
		for (String prefix : subjectPrefixesForSide(side)) {
			if (normalized.equals(prefix) || normalized.startsWith(prefix + ".")
					|| normalized.startsWith(prefix)) {
				// avoid 11220 matching 1122 wrongly for non-dot codes: require next char non-digit OR end
				if (normalized.equals(prefix) || normalized.startsWith(prefix + ".")) {
					return true;
				}
				if (normalized.startsWith(prefix) && normalized.length() > prefix.length()) {
					char next = normalized.charAt(prefix.length());
					if (!Character.isDigit(next)) {
						return true;
					}
					// fixed-length child codes e.g. 112201 under 1122
					if (Character.isDigit(next) && prefix.length() >= 4) {
						return true;
					}
				}
			}
		}
		return false;
	}
}
