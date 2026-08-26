package com.financial.cloud.util;

import com.financial.cloud.common.BaseSubject;
import org.apache.commons.lang3.StringUtils;

public final class SubjectDisplayNameUtils {

    private SubjectDisplayNameUtils() {
    }

    public static String resolve(String displayName, String name) {
        if (StringUtils.isNotBlank(displayName)) {
            return displayName;
        }
        if (StringUtils.isBlank(name)) {
            return "";
        }
        int dashIndex = name.indexOf('-');
        if (dashIndex > -1) {
            return name.substring(dashIndex + 1);
        }
        return name;
    }

    public static String resolve(BaseSubject subject) {
        if (subject == null) {
            return "";
        }
        return resolve(subject.getDisplayName(), subject.getName());
    }

    public static String formatVoucherSubjectName(BaseSubject subject) {
        if (subject == null || StringUtils.isBlank(subject.getCode())) {
            return "";
        }
        String displayName = resolve(subject);
        if (StringUtils.isNotBlank(displayName)) {
            return subject.getCode() + "-" + displayName;
        }
        return subject.getCode();
    }

    public static boolean needsSubjectNameFix(String subjectName) {
        return StringUtils.isBlank(subjectName)
                || "null".equalsIgnoreCase(subjectName.trim())
                || subjectName.contains(" null");
    }

    public static String normalizeSummary(String summary) {
        if (summary == null) {
            return null;
        }
        String normalized = summary.trim();
        if ("摘要".equals(normalized)) {
            return "";
        }
        return normalized;
    }
}
