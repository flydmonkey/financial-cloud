package com.jinbooks.entity;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.jinbooks.common.PageQuery;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.assertEquals;

class PageQueryTest {
    @Test
    void defaultPageSizeIsTwenty() {
        PageQuery q = new PageQuery();
        Page<Object> page = q.build();
        assertEquals(1, page.getCurrent());
        assertEquals(20, page.getSize());
    }

    @Test
    void clampsPageSizeToMax() {
        PageQuery q = new PageQuery();
        q.setPageSize(200000);
        q.setPageNumber(2);
        Page<Object> page = q.build();
        assertEquals(2, page.getCurrent());
        assertEquals(100000, page.getSize());
    }

    @Test
    void nonPositivePageSizeUsesDefault() {
        PageQuery q = new PageQuery();
        q.setPageSize(0);
        Page<Object> page = q.build();
        assertEquals(20, page.getSize());
    }
}
