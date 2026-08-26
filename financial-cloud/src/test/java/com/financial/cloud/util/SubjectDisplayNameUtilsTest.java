package com.financial.cloud.util;

import com.financial.cloud.domain.book.BookSubject;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class SubjectDisplayNameUtilsTest {

    @Test
    void resolvePrefersDisplayName() {
        BookSubject subject = new BookSubject();
        subject.setDisplayName("库存现金");
        subject.setName("1001-库存现金");

        assertEquals("库存现金", SubjectDisplayNameUtils.resolve(subject));
    }

    @Test
    void resolveFallsBackToNameAfterDash() {
        BookSubject subject = new BookSubject();
        subject.setName("1001-库存现金");

        assertEquals("库存现金", SubjectDisplayNameUtils.resolve(subject));
    }

    @Test
    void formatVoucherSubjectNameUsesResolvedDisplayName() {
        BookSubject subject = new BookSubject();
        subject.setCode("1001");
        subject.setName("库存现金");

        assertEquals("1001-库存现金", SubjectDisplayNameUtils.formatVoucherSubjectName(subject));
    }

    @Test
    void normalizeSummaryRemovesPlaceholder() {
        assertEquals("", SubjectDisplayNameUtils.normalizeSummary("摘要"));
        assertEquals("工资", SubjectDisplayNameUtils.normalizeSummary(" 工资 "));
    }

    @Test
    void needsSubjectNameFixDetectsNullLiteral() {
        assertTrue(SubjectDisplayNameUtils.needsSubjectNameFix("1001 null"));
        assertFalse(SubjectDisplayNameUtils.needsSubjectNameFix("1001-库存现金"));
    }
}
