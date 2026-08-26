package com.financial.cloud.controller.book;



import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.financial.cloud.authn.annotation.CurrentUser;
import com.financial.cloud.common.Message;
import com.financial.cloud.domain.book.Book;
import com.financial.cloud.dto.book.BookChangeDto;
import com.financial.cloud.dto.book.BookPageDto;
import com.financial.cloud.dto.book.BookVo;
import com.financial.cloud.dto.common.ListIdsDto;
import com.financial.cloud.domain.idm.UserInfo;
import com.financial.cloud.service.book.BookService;
import com.financial.cloud.validation.AddGroup;
import com.financial.cloud.validation.EditGroup;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RequiredArgsConstructor
@Slf4j
@RestController
@RequestMapping("/api/book")
public class BookController {

    private final BookService bookService;

    @GetMapping(value = { "/fetch" })
    public Message<Page<Book>> fetch(BookPageDto dto) {

        log.debug("fetch {}",dto);

        return bookService.pageList(dto);
    }

    @GetMapping("/get/{id}")
    public Message<Book> getById(@PathVariable(name="id") String id) {

        log.debug("get {}",id);

        return new Message<>(Message.SUCCESS, bookService.getById(id));
    }

    @PostMapping("/save")
    public Message<String> save(@Validated(value = AddGroup.class) @RequestBody BookChangeDto dto) {
        log.debug("save {}",dto);
        return bookService.save(dto);
    }

    @PutMapping("/update")
    public Message<String> update(@Validated(value = EditGroup.class) @RequestBody BookChangeDto dto) {
        log.debug("update {}",dto);
        return bookService.update(dto);
    }

    @DeleteMapping(value = { "/delete" })
    public Message<String> delete(@Validated @RequestBody ListIdsDto dto) {

        log.debug("delete {}",dto);

        return bookService.delete(dto);
    }

    @GetMapping("/fetchAll")
    public Message<List<BookVo>> listStore(@CurrentUser UserInfo currentUser) {
        return Message.ok(bookService.listBooks(currentUser.getId()));
    }
}
