package com.financial.cloud.controller.statement;

import com.financial.cloud.authn.annotation.CurrentUser;
import com.financial.cloud.constants.auth.ProductRoles;
import com.financial.cloud.domain.idm.UserInfo;
import com.financial.cloud.dto.workspace.BooksPackBatchRequest;
import com.financial.cloud.enums.error.UsersBusinessCode;
import com.financial.cloud.exception.BusinessException;
import com.financial.cloud.service.statement.MonthlyBooksPackService;
import com.financial.cloud.service.workspace.BooksBoardService;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.apache.commons.lang3.StringUtils;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.io.IOException;

@RestController
@RequestMapping("/api/statement/books-pack")
@RequiredArgsConstructor
public class MonthlyBooksPackController {

    private final MonthlyBooksPackService booksPackService;
    private final BooksBoardService booksBoardService;
    private final org.springframework.jdbc.core.JdbcTemplate jdbcTemplate;

    @GetMapping("/export")
    public void export(@RequestParam String yearPeriod,
                       @RequestParam(defaultValue = "true") boolean includeVoucherList,
                       @RequestParam(required = false) String bookId,
                       @CurrentUser UserInfo userInfo,
                       HttpServletResponse response) throws IOException {
        requireExportRole();
        String targetBookId = resolveBookId(bookId, userInfo);
        MonthlyBooksPackService.BooksPack pack = booksPackService.build(
                targetBookId, yearPeriod, includeVoucherList, userInfo.getId());
        writeZip(response, pack);
    }

    @PostMapping("/export-batch")
    public void exportBatch(@RequestBody BooksPackBatchRequest request,
                            @CurrentUser UserInfo userInfo,
                            HttpServletResponse response) throws IOException {
        requireExportRole();
        if (request == null || StringUtils.isBlank(request.getYearPeriod())) {
            throw new BusinessException(400, "账期不能为空");
        }
        MonthlyBooksPackService.BooksPack pack = booksPackService.buildBatch(
                request.getBookIds(),
                request.getYearPeriod(),
                request.isIncludeVoucherList(),
                userInfo.getId(),
                id -> canExportBook(userInfo.getId(), id));
        writeZip(response, pack);
    }

    private String resolveBookId(String bookId, UserInfo userInfo) {
        if (StringUtils.isBlank(bookId)) {
            return userInfo.getBookId();
        }
        if (!canExportBook(userInfo.getId(), bookId.trim())) {
            throw new BusinessException(UsersBusinessCode.PERMISSION_DENIED);
        }
        return bookId.trim();
    }

    private boolean canExportBook(String userId, String bookId) {
        return booksBoardService.userHasBook(userId, bookId)
                && jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM role_member WHERE member_id=? AND book_id=? AND type='USER' AND role_id IN (?,?,?)",
                Long.class, userId, bookId, ProductRoles.ADMINISTRATORS, ProductRoles.BOOKKEEPER, ProductRoles.REVIEWER) > 0;
    }

    private static void requireExportRole() {
        if (!ProductRoles.canWriteBusiness()) {
            throw new BusinessException(UsersBusinessCode.PERMISSION_DENIED);
        }
    }

    private static void writeZip(HttpServletResponse response, MonthlyBooksPackService.BooksPack pack)
            throws IOException {
        response.setContentType(MonthlyBooksPackService.CONTENT_TYPE);
        response.setContentLength(pack.content().length);
        response.setHeader("Content-Disposition", "attachment; filename*=UTF-8''" + pack.encodedFileName());
        response.getOutputStream().write(pack.content());
        response.getOutputStream().flush();
    }
}
