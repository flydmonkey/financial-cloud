package com.financial.cloud.service.workspace;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.financial.cloud.domain.book.Book;
import com.financial.cloud.domain.permissions.PermissionBook;
import com.financial.cloud.dto.report.DashboardTodoVo;
import com.financial.cloud.dto.workspace.BooksBoardRowVo;
import com.financial.cloud.dto.workspace.BooksBoardVo;
import com.financial.cloud.enums.book.BookStatusEnum;
import com.financial.cloud.repository.book.BookMapper;
import com.financial.cloud.service.permissions.PermissionBookService;
import com.financial.cloud.service.report.DashboardTodoService;
import com.financial.cloud.util.BooksBoardCloseStatus;
import lombok.RequiredArgsConstructor;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Service;

import java.time.YearMonth;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class BooksBoardService {

    public static final int MAX_BOOKS = 200;

    private final PermissionBookService permissionBookService;
    private final BookMapper bookMapper;
    private final DashboardTodoService dashboardTodoService;

    public BooksBoardVo list(String userId, String focusPeriod, boolean onlyTodo, String keyword) {
        String focus = StringUtils.isBlank(focusPeriod)
                ? YearMonth.now().minusMonths(1).toString()
                : focusPeriod.trim();

        List<PermissionBook> grants = permissionBookService.list(new LambdaQueryWrapper<PermissionBook>()
                .eq(PermissionBook::getUserId, userId));
        Set<String> bookIds = grants.stream()
                .map(PermissionBook::getBookId)
                .filter(StringUtils::isNotBlank)
                .collect(Collectors.toSet());
        int totalGranted = bookIds.size();
        if (bookIds.isEmpty()) {
            return BooksBoardVo.builder()
                    .focusPeriod(focus)
                    .rows(List.of())
                    .totalGranted(0)
                    .truncated(false)
                    .build();
        }

        List<Book> books = bookMapper.selectBatchIds(bookIds).stream()
                .filter(Objects::nonNull)
                .sorted(Comparator.comparing(b -> StringUtils.defaultString(b.getName()), String.CASE_INSENSITIVE_ORDER))
                .toList();

        boolean truncated = books.size() > MAX_BOOKS;
        if (truncated) {
            books = books.subList(0, MAX_BOOKS);
        }

        String kw = StringUtils.trimToEmpty(keyword).toLowerCase(Locale.ROOT);
        List<BooksBoardRowVo> rows = new ArrayList<>();
        for (Book book : books) {
            BooksBoardRowVo row = toRow(book, focus);
            if (StringUtils.isNotBlank(kw)) {
                String hay = (StringUtils.defaultString(row.getBookName()) + " "
                        + StringUtils.defaultString(row.getCompanyName())).toLowerCase(Locale.ROOT);
                if (!hay.contains(kw)) {
                    continue;
                }
            }
            if (onlyTodo && !hasTodo(row)) {
                continue;
            }
            rows.add(row);
        }

        rows.sort(Comparator
                .comparingInt((BooksBoardRowVo r) -> closePriority(r.getCloseStatus()))
                .thenComparing(r -> StringUtils.defaultString(r.getBookName()), String.CASE_INSENSITIVE_ORDER));

        return BooksBoardVo.builder()
                .focusPeriod(focus)
                .rows(rows)
                .totalGranted(totalGranted)
                .truncated(truncated)
                .build();
    }

    static int closePriority(String closeStatus) {
        if ("BEHIND".equals(closeStatus)) return 0;
        if ("OPEN".equals(closeStatus)) return 1;
        if ("UNKNOWN".equals(closeStatus)) return 2;
        if ("CLOSED".equals(closeStatus)) return 3;
        return 9;
    }

    private BooksBoardRowVo toRow(Book book, String focus) {
        DashboardTodoVo todo = dashboardTodoService.todo(book.getId());
        String currentTerm = todo.getCurrentTerm();
        BooksBoardCloseStatus close = BooksBoardCloseStatus.resolve(currentTerm, focus);
        boolean sealed = BookStatusEnum.isSealed(book.getStatus());
        String blocker = resolveBlocker(todo, close);
        return BooksBoardRowVo.builder()
                .bookId(book.getId())
                .bookName(book.getName())
                .companyName(book.getCompanyName())
                .currentTerm(currentTerm)
                .voucherReviewed(todo.isVoucherReviewed())
                .pendingAuditCount(todo.getPendingAuditCount())
                .pendingPostCount(todo.getPendingPostCount())
                .depreciationPending(todo.isDepreciationPending())
                .closeStatus(close.name())
                .bookStatus(book.getStatus())
                .sealed(sealed)
                .blocker(blocker)
                .build();
    }

    static String resolveBlocker(DashboardTodoVo todo, BooksBoardCloseStatus close) {
        if (close == BooksBoardCloseStatus.BEHIND) {
            return "BEHIND";
        }
        if (todo.isVoucherReviewed() && todo.getPendingAuditCount() > 0) {
            return "AUDIT";
        }
        if (todo.getPendingPostCount() > 0) {
            return "POST";
        }
        if (todo.isDepreciationPending()) {
            return "DEPRECIATION";
        }
        if (close == BooksBoardCloseStatus.OPEN) {
            return "READY_CLOSE";
        }
        if (close == BooksBoardCloseStatus.CLOSED) {
            return "READY_PACK";
        }
        return "NONE";
    }

    private static boolean hasTodo(BooksBoardRowVo row) {
        if (row.getPendingAuditCount() > 0 || row.getPendingPostCount() > 0 || row.isDepreciationPending()) {
            return true;
        }
        return "OPEN".equals(row.getCloseStatus()) || "BEHIND".equals(row.getCloseStatus());
    }

    public boolean userHasBook(String userId, String bookId) {
        return permissionBookService.userHasBook(userId, bookId);
    }
}
