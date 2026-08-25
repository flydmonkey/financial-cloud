package com.jinbooks.controller.book;

import com.baomidou.mybatisplus.core.toolkit.StringUtils;
import com.jinbooks.authn.annotation.CurrentUser;
import com.jinbooks.common.Message;
import com.jinbooks.dto.book.BookInitBalanceChangeDto;
import com.jinbooks.dto.book.BookInitBalancePageDto;
import com.jinbooks.dto.book.BookInitBalanceVo;
import com.jinbooks.domain.idm.UserInfo;
import com.jinbooks.service.book.BookInitBalanceService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 初始余额管理接口
 */

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
}
