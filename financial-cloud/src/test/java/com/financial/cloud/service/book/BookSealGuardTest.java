package com.financial.cloud.service.book;

import com.financial.cloud.domain.book.Book;
import com.financial.cloud.enums.error.BookBusinessExceptionEnum;
import com.financial.cloud.exception.BusinessException;
import com.financial.cloud.repository.book.BookMapper;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class BookSealGuardTest {

    @Mock
    private BookMapper bookMapper;

    @InjectMocks
    private BookSealGuard guard;

    private Book bookWithStatus(Integer status) {
        Book book = new Book();
        book.setId("book-1");
        book.setStatus(status);
        return book;
    }

    @Test
    void sealedBookRejectsWrites() {
        when(bookMapper.selectById("book-1")).thenReturn(bookWithStatus(2));

        BusinessException ex = assertThrows(BusinessException.class,
                () -> guard.assertWritable("book-1"));
        assertEquals(BookBusinessExceptionEnum.BOOK_SEALED.getCode(), ex.getCode());
        assertTrue(guard.isSealed("book-1"));
    }

    @Test
    void activeAndDisabledBooksAllowWrites() {
        when(bookMapper.selectById("book-1")).thenReturn(bookWithStatus(1));
        assertDoesNotThrow(() -> guard.assertWritable("book-1"));
        assertFalse(guard.isSealed("book-1"));

        when(bookMapper.selectById("book-1")).thenReturn(bookWithStatus(0));
        assertDoesNotThrow(() -> guard.assertWritable("book-1"));
    }

    @Test
    void blankIdOrMissingBookPasses() {
        assertDoesNotThrow(() -> guard.assertWritable(null));
        assertDoesNotThrow(() -> guard.assertWritable(""));
        when(bookMapper.selectById("ghost")).thenReturn(null);
        assertDoesNotThrow(() -> guard.assertWritable("ghost"));
        assertFalse(guard.isSealed("ghost"));
    }
}
