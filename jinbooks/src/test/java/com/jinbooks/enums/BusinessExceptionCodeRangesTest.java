package com.jinbooks.enums;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class BusinessExceptionCodeRangesTest {
    @Test
    void bookCodesAre51xxxx() {
        for (BookBusinessExceptionEnum e : BookBusinessExceptionEnum.values()) {
            assertTrue(e.getCode() >= 510001 && e.getCode() <= 519999, e.name());
        }
    }

    @Test
    void orgsCodesAre52xxxx() {
        for (OrgsBusinessExceptionEnum e : OrgsBusinessExceptionEnum.values()) {
            assertTrue(e.getCode() >= 520001 && e.getCode() <= 529999, e.name());
        }
    }

    @Test
    void bookAndOrgsDoNotOverlap() {
        for (BookBusinessExceptionEnum b : BookBusinessExceptionEnum.values()) {
            for (OrgsBusinessExceptionEnum o : OrgsBusinessExceptionEnum.values()) {
                assertNotEquals(b.getCode(), o.getCode(), b.name() + " vs " + o.name());
            }
        }
    }
}
