package com.jinbooks.controller.book;



import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.jinbooks.authn.annotation.CurrentUser;
import com.jinbooks.common.Message;
import com.jinbooks.domain.book.Book;
import com.jinbooks.dto.book.BookChangeDto;
import com.jinbooks.dto.book.BookPageDto;
import com.jinbooks.dto.book.BookVo;
import com.jinbooks.dto.common.ListIdsDto;
import com.jinbooks.domain.idm.UserInfo;
import com.jinbooks.service.book.BookService;
import com.jinbooks.validation.AddGroup;
import com.jinbooks.validation.EditGroup;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * @description:
 * @author: orangeBabu
 * @time: 2024/12/31 11:18
 */

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
