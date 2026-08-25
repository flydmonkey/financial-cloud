/*
 * Copyright [2025] [JinBooks of copyright http://www.jinbooks.com]
 * 
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * 
 *     http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 */
 

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
@RequestMapping("/base/init-balance")
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
