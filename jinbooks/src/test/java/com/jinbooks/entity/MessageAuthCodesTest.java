package com.jinbooks.entity;

import com.jinbooks.common.Message;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.assertEquals;

class MessageAuthCodesTest {
    @Test
    void unauthorizedAndForbiddenCodes() {
        assertEquals(401, Message.UNAUTHORIZED);
        assertEquals(403, Message.FORBIDDEN);
        assertEquals(0, Message.SUCCESS);
        assertEquals(2, Message.FAIL);
    }
}
