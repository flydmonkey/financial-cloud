package com.financial.cloud.util;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

class SubjectBalanceAncestorsTest {

    @Test
    void sourceIdsFromIdPathIgnoresLeadingSlashEmptySegment() {
        assertEquals(List.of("subj-1002"), SubjectBalanceAncestors.sourceIdsFromIdPath("/subj-1002"));
        assertEquals(List.of("p1", "c1"), SubjectBalanceAncestors.sourceIdsFromIdPath("/p1/c1"));
        assertEquals(List.of(), SubjectBalanceAncestors.sourceIdsFromIdPath("/"));
        assertEquals(List.of(), SubjectBalanceAncestors.sourceIdsFromIdPath(null));
    }

    @Test
    void ancestorSourceIdsExcludesSelf() {
        assertEquals(List.of("p1"), SubjectBalanceAncestors.ancestorSourceIds("/p1/c1", "c1"));
        assertEquals(List.of(), SubjectBalanceAncestors.ancestorSourceIds("/leaf", "leaf"));
    }
}
