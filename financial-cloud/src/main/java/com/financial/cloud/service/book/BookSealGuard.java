package com.financial.cloud.service.book;

import com.financial.cloud.domain.book.Book;
import com.financial.cloud.enums.book.BookStatusEnum;
import com.financial.cloud.enums.error.BookBusinessExceptionEnum;
import com.financial.cloud.exception.BusinessException;
import com.financial.cloud.repository.book.BookMapper;
import lombok.RequiredArgsConstructor;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Component;

/**
 * 账套封存守卫：封存（归档）账套为只读，任何业务写操作一律拒绝。
 * 与期间锁正交——期间锁按账期拦截，封存按整个账套拦截。
 */
@Component
@RequiredArgsConstructor
public class BookSealGuard {

    private final BookMapper bookMapper;

    /**
     * 账套已封存时抛出业务异常；账套不存在或未封存时放行。
     */
    public void assertWritable(String bookId) {
        if (StringUtils.isBlank(bookId)) {
            return;
        }
        Book book = bookMapper.selectById(bookId);
        if (book != null && BookStatusEnum.isSealed(book.getStatus())) {
            throw new BusinessException(BookBusinessExceptionEnum.BOOK_SEALED);
        }
    }

    public boolean isSealed(String bookId) {
        if (StringUtils.isBlank(bookId)) {
            return false;
        }
        Book book = bookMapper.selectById(bookId);
        return book != null && BookStatusEnum.isSealed(book.getStatus());
    }
}
