package com.financial.cloud.controller.book;

import com.baomidou.mybatisplus.core.toolkit.StringUtils;
import com.financial.cloud.authn.annotation.CurrentUser;
import com.financial.cloud.common.ExcelImport;
import com.financial.cloud.common.Message;
import com.financial.cloud.constants.auth.ProductRoles;
import com.financial.cloud.dto.book.BookInitBalanceChangeDto;
import com.financial.cloud.dto.book.BookInitBalanceImportResultVo;
import com.financial.cloud.dto.book.BookInitBalancePageDto;
import com.financial.cloud.dto.book.BookInitBalanceVo;
import com.financial.cloud.domain.idm.UserInfo;
import com.financial.cloud.service.book.BookInitBalanceService;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.io.IOException;
import java.util.List;

@RestController
@RequestMapping("/api/base/init-balance")
@Slf4j
@RequiredArgsConstructor
public class BookInitBalanceController {
    private final BookInitBalanceService bookInitBalanceService;

    @GetMapping(value = {"/list"})
    public Message<List<BookInitBalanceVo>> fetch(BookInitBalancePageDto dto,
                                                  @CurrentUser UserInfo userInfo) {
        dto.setBookId(userInfo.getBookId());
        if (StringUtils.isBlank(dto.getBookId())) {
            return Message.failed("所属账套ID不能为空");
        }
        return bookInitBalanceService.list(dto);
    }

    @PostMapping("/save")
    public Message<String> save(@Validated @RequestBody List<BookInitBalanceChangeDto> dtos,
                                @CurrentUser UserInfo userInfo) {
        for (BookInitBalanceChangeDto dto : dtos) {
            dto.setBookId(userInfo.getBookId());
        }
        return bookInitBalanceService.save(dtos);
    }

    @GetMapping("/export")
    public void export(BookInitBalancePageDto dto,
                       @CurrentUser UserInfo userInfo,
                       HttpServletResponse response) throws IOException {
        dto.setBookId(userInfo.getBookId());
        bookInitBalanceService.export(dto, response);
    }

    @GetMapping("/import-template")
    public void importTemplate(HttpServletResponse response) throws IOException {
        bookInitBalanceService.downloadImportTemplate(response);
    }

    @PostMapping("/import")
    public Message<BookInitBalanceImportResultVo> importExcel(
            @ModelAttribute("excelImportFile") ExcelImport excelImportFile,
            @CurrentUser UserInfo userInfo) {
        ProductRoles.requireWriteBusiness();
        return bookInitBalanceService.importFromExcel(userInfo.getBookId(), excelImportFile);
    }
}
