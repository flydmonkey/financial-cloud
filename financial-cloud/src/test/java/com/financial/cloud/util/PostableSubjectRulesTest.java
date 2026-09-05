package com.financial.cloud.util;

import com.financial.cloud.domain.book.BookSubject;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;

class PostableSubjectRulesTest {

    @Test
    void prefersWagePayableLeafFor2211() {
        BookSubject parent = subject("2211", "应付职工薪酬");
        BookSubject wage = subject("2211.01", "工资");
        BookSubject bonus = subject("2211.02", "奖金");
        BookSubject picked = PostableSubjectRules.pickPostable("2211", List.of(parent, wage, bonus));
        assertNotNull(picked);
        assertEquals("2211.01", picked.getCode());
    }

    @Test
    void prefersStaffCostLeafFor5602ByName() {
        BookSubject parent = subject("5602", "管理费用");
        BookSubject office = subject("5602.01", "办公费");
        BookSubject salary = subject("5602.07", "5602.07-职工薪酬");
        BookSubject picked = PostableSubjectRules.pickPostable("5602", List.of(parent, office, salary));
        assertNotNull(picked);
        assertEquals("5602.07", picked.getCode());
    }

    @Test
    void exactLeafWins() {
        BookSubject leaf = subject("2211.01", "工资");
        BookSubject picked = PostableSubjectRules.pickPostable("2211.01", List.of(leaf));
        assertEquals("2211.01", picked.getCode());
    }

    @Test
    void returnsNullWhenEmpty() {
        assertNull(PostableSubjectRules.pickPostable("5602", List.of()));
    }

    private static BookSubject subject(String code, String name) {
        BookSubject s = new BookSubject();
        s.setId(code);
        s.setCode(code);
        s.setName(name);
        return s;
    }
}
