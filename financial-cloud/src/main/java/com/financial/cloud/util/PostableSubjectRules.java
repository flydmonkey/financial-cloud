package com.financial.cloud.util;

import com.financial.cloud.domain.book.BookSubject;
import org.apache.commons.lang3.StringUtils;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Pick a postable (leaf) book subject from a template subject code that may be a parent.
 * Accrual templates often store {@code 5602}/{@code 2211} while vouchers require leaves
 * such as {@code 5602.07}/{@code 2211.01}.
 */
public final class PostableSubjectRules {

    private static final Map<String, List<String>> PREFERRED_LEAVES = Map.of(
            "2211", List.of("2211.01", "221101"),
            "2151", List.of("2151.01", "215101"),
            "5602", List.of("5602.07", "560207"),
            "5502", List.of("5502.07", "550207"),
            "6602", List.of("6602.07", "660207")
    );

    private PostableSubjectRules() {
    }

    public static BookSubject pickPostable(String templateCode, List<BookSubject> subjects) {
        if (subjects == null || subjects.isEmpty() || StringUtils.isBlank(templateCode)) {
            return null;
        }
        Map<String, BookSubject> byCode = new LinkedHashMap<>();
        for (BookSubject subject : subjects) {
            if (subject != null && StringUtils.isNotBlank(subject.getCode())) {
                byCode.putIfAbsent(subject.getCode(), subject);
            }
        }
        List<BookSubject> all = new ArrayList<>(byCode.values());
        List<BookSubject> leaves = all.stream()
                .filter(s -> isLeaf(s, all))
                .sorted(Comparator.comparing(BookSubject::getCode))
                .toList();
        if (leaves.isEmpty()) {
            return null;
        }

        for (String candidate : SubjectCodeCompat.lookupCandidates(templateCode)) {
            BookSubject exact = byCode.get(candidate);
            if (exact != null && isLeaf(exact, all)) {
                return exact;
            }
        }

        List<String> preferred = new ArrayList<>();
        preferred.addAll(PREFERRED_LEAVES.getOrDefault(templateCode, List.of()));
        for (String candidate : SubjectCodeCompat.lookupCandidates(templateCode)) {
            preferred.addAll(PREFERRED_LEAVES.getOrDefault(candidate, List.of()));
        }
        for (String pref : preferred) {
            for (String candidate : SubjectCodeCompat.lookupCandidates(pref)) {
                BookSubject hit = byCode.get(candidate);
                if (hit != null && isLeaf(hit, all)) {
                    return hit;
                }
            }
        }

        for (BookSubject leaf : leaves) {
            String name = leaf.getName() == null ? "" : leaf.getName();
            String display = leaf.getDisplayName() == null ? "" : leaf.getDisplayName();
            if (name.contains("职工薪酬") || name.contains("工资")
                    || display.contains("职工薪酬") || display.contains("工资")) {
                return leaf;
            }
        }

        String prefix = templateCode + ".";
        for (BookSubject leaf : leaves) {
            if (leaf.getCode().startsWith(prefix)) {
                return leaf;
            }
        }
        return leaves.get(0);
    }

    static boolean isLeaf(BookSubject subject, List<BookSubject> subjectList) {
        if (subjectList.size() == 1) {
            return true;
        }
        for (BookSubject other : subjectList) {
            if (subject.getCode().equals(other.getCode())) {
                continue;
            }
            if (other.getCode().startsWith(subject.getCode())) {
                return false;
            }
        }
        return true;
    }
}
