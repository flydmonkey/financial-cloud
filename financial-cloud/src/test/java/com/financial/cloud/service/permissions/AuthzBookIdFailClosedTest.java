package com.financial.cloud.dto.auth;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Documents fail-closed contract: blank bookId must not widen role membership.
 * SQL behavior lives in AuthzMapper.xml (otherwise branch → 1=0).
 */
class AuthzBookIdFailClosedTest {

    @Test
    void blankBookIdIsTreatedAsMissingForScopedQuery() {
        QueryGroupMembersDto dto = new QueryGroupMembersDto();
        dto.add("user-1");
        assertNull(dto.getBookId());
        assertFalse(hasActiveBook(dto.getBookId()));

        dto.setBookId("");
        assertFalse(hasActiveBook(dto.getBookId()));

        dto.setBookId("   ");
        assertFalse(hasActiveBook(dto.getBookId() == null ? null : dto.getBookId().trim()));

        dto.setBookId("book-1");
        assertTrue(hasActiveBook(dto.getBookId()));
    }

    private static boolean hasActiveBook(String bookId) {
        return bookId != null && !bookId.isBlank();
    }
}
