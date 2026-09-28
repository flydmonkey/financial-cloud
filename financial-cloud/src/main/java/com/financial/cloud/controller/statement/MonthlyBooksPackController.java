package com.financial.cloud.controller.statement;

import com.financial.cloud.authn.annotation.CurrentUser;
import com.financial.cloud.domain.idm.UserInfo;
import com.financial.cloud.service.statement.MonthlyBooksPackService;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.io.IOException;

@RestController
@RequestMapping("/api/statement/books-pack")
@RequiredArgsConstructor
public class MonthlyBooksPackController {

    private final MonthlyBooksPackService booksPackService;

    @GetMapping("/export")
    public void export(@RequestParam String yearPeriod,
                       @RequestParam(defaultValue = "true") boolean includeVoucherList,
                       @CurrentUser UserInfo userInfo,
                       HttpServletResponse response) throws IOException {
        MonthlyBooksPackService.BooksPack pack = booksPackService.build(
                userInfo.getBookId(), yearPeriod, includeVoucherList, userInfo.getId());
        response.setContentType(MonthlyBooksPackService.CONTENT_TYPE);
        response.setContentLength(pack.content().length);
        response.setHeader("Content-Disposition", "attachment; filename*=UTF-8''" + pack.encodedFileName());
        response.getOutputStream().write(pack.content());
        response.getOutputStream().flush();
    }
}
