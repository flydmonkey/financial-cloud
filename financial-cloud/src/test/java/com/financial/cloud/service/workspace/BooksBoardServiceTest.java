package com.financial.cloud.service.workspace;

import com.baomidou.mybatisplus.core.conditions.Wrapper;
import com.financial.cloud.domain.book.Book;
import com.financial.cloud.domain.permissions.PermissionBook;
import com.financial.cloud.dto.report.DashboardTodoVo;
import com.financial.cloud.dto.workspace.BooksBoardVo;
import com.financial.cloud.repository.book.BookMapper;
import com.financial.cloud.service.permissions.PermissionBookService;
import com.financial.cloud.service.report.DashboardTodoService;
import com.financial.cloud.util.BooksBoardCloseStatus;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

import java.util.ArrayList;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class BooksBoardServiceTest {

    @Mock private PermissionBookService permissionBookService;
    @Mock private BookMapper bookMapper;
    @Mock private DashboardTodoService dashboardTodoService;

    private BooksBoardService service;

    @BeforeEach
    void setUp() {
        service = new BooksBoardService(permissionBookService, bookMapper, dashboardTodoService);
    }

    @Test
    void emptyWhenNoGrants() {
        doReturn(List.of()).when(permissionBookService).list(any(Wrapper.class));

        BooksBoardVo vo = service.list("u1", "2026-09", false, null);

        assertThat(vo.getRows()).isEmpty();
        assertThat(vo.getTotalGranted()).isZero();
        assertThat(vo.isTruncated()).isFalse();
        assertThat(vo.getFocusPeriod()).isEqualTo("2026-09");
    }

    @Test
    void aggregatesTwoBooksWithMixedCloseStatus() {
        stubGrants("b1", "b2");
        Book open = book("b1", "甲公司", 1);
        Book closed = book("b2", "乙公司", 1);
        when(bookMapper.selectBatchIds(any())).thenReturn(List.of(open, closed));
        when(dashboardTodoService.todo("b1")).thenReturn(DashboardTodoVo.builder()
                .currentTerm("2026-09")
                .voucherReviewed(true)
                .pendingAuditCount(2)
                .pendingPostCount(0)
                .depreciationPending(false)
                .build());
        when(dashboardTodoService.todo("b2")).thenReturn(DashboardTodoVo.builder()
                .currentTerm("2026-10")
                .voucherReviewed(false)
                .pendingAuditCount(0)
                .pendingPostCount(0)
                .depreciationPending(false)
                .build());

        BooksBoardVo vo = service.list("u1", "2026-09", false, null);

        assertThat(vo.getRows()).hasSize(2);
        assertThat(vo.getRows()).anySatisfy(row -> {
            assertThat(row.getBookName()).isEqualTo("甲公司");
            assertThat(row.getCloseStatus()).isEqualTo("OPEN");
            assertThat(row.getBlocker()).isEqualTo("AUDIT");
        });
        assertThat(vo.getRows()).anySatisfy(row -> {
            assertThat(row.getBookName()).isEqualTo("乙公司");
            assertThat(row.getCloseStatus()).isEqualTo("CLOSED");
            assertThat(row.getBlocker()).isEqualTo("READY_PACK");
        });
    }

    @Test
    void onlyTodoFiltersClosedIdle() {
        stubGrants("b1", "b2");
        when(bookMapper.selectBatchIds(any())).thenReturn(List.of(
                book("b1", "有待办", 1), book("b2", "已结闲", 1)));
        when(dashboardTodoService.todo("b1")).thenReturn(DashboardTodoVo.builder()
                .currentTerm("2026-09").pendingPostCount(1).build());
        when(dashboardTodoService.todo("b2")).thenReturn(DashboardTodoVo.builder()
                .currentTerm("2026-10").build());

        BooksBoardVo vo = service.list("u1", "2026-09", true, null);

        assertThat(vo.getRows()).extracting(r -> r.getBookId()).containsExactly("b1");
    }

    @Test
    void keywordFiltersName() {
        stubGrants("b1", "b2");
        when(bookMapper.selectBatchIds(any())).thenReturn(List.of(
                book("b1", "阳光商贸", 1), book("b2", "夜色咖啡", 1)));
        when(dashboardTodoService.todo(anyString())).thenReturn(DashboardTodoVo.builder()
                .currentTerm("2026-09").build());

        BooksBoardVo vo = service.list("u1", "2026-09", false, "阳光");

        assertThat(vo.getRows()).hasSize(1);
        assertThat(vo.getRows().get(0).getBookId()).isEqualTo("b1");
    }

    @Test
    void truncatedWhenOverCap() {
        List<PermissionBook> grants = new ArrayList<>();
        List<Book> books = new ArrayList<>();
        for (int i = 0; i < BooksBoardService.MAX_BOOKS + 1; i++) {
            String id = "b" + i;
            grants.add(new PermissionBook("u1", id));
            books.add(book(id, "账套" + String.format("%03d", i), 1));
        }
        doReturn(grants).when(permissionBookService).list(any(Wrapper.class));
        when(bookMapper.selectBatchIds(any())).thenReturn(books);
        when(dashboardTodoService.todo(anyString())).thenReturn(DashboardTodoVo.builder()
                .currentTerm("2026-09").build());

        BooksBoardVo vo = service.list("u1", "2026-09", false, null);

        assertThat(vo.isTruncated()).isTrue();
        assertThat(vo.getTotalGranted()).isEqualTo(BooksBoardService.MAX_BOOKS + 1);
        assertThat(vo.getRows()).hasSize(BooksBoardService.MAX_BOOKS);
    }

    @Test
    void resolveBlockerPriority() {
        DashboardTodoVo todo = DashboardTodoVo.builder()
                .voucherReviewed(true)
                .pendingAuditCount(1)
                .pendingPostCount(5)
                .depreciationPending(true)
                .build();
        assertThat(BooksBoardService.resolveBlocker(todo, BooksBoardCloseStatus.OPEN))
                .isEqualTo("AUDIT");
        assertThat(BooksBoardService.resolveBlocker(todo, BooksBoardCloseStatus.BEHIND))
                .isEqualTo("BEHIND");
    }

    private void stubGrants(String... ids) {
        List<PermissionBook> grants = new ArrayList<>();
        for (String id : ids) {
            grants.add(new PermissionBook("u1", id));
        }
        doReturn(grants).when(permissionBookService).list(any(Wrapper.class));
    }

    private static Book book(String id, String name, int status) {
        Book book = new Book();
        book.setId(id);
        book.setName(name);
        book.setCompanyName(name);
        book.setStatus(status);
        return book;
    }
}
