package com.financial.cloud.controller.workspace;

import com.financial.cloud.authn.annotation.CurrentUser;
import com.financial.cloud.common.Message;
import com.financial.cloud.domain.idm.UserInfo;
import com.financial.cloud.dto.workspace.BooksBoardVo;
import com.financial.cloud.service.workspace.BooksBoardService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/workspace")
@RequiredArgsConstructor
public class BooksBoardController {

    private final BooksBoardService booksBoardService;

    @GetMapping("/books-board")
    public Message<BooksBoardVo> booksBoard(
            @RequestParam(required = false) String focusPeriod,
            @RequestParam(defaultValue = "false") boolean onlyTodo,
            @RequestParam(required = false) String keyword,
            @CurrentUser UserInfo userInfo) {
        return Message.ok(booksBoardService.list(userInfo.getId(), focusPeriod, onlyTodo, keyword));
    }
}
