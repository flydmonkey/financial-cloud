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
import com.jinbooks.validate.AddGroup;
import com.jinbooks.validate.EditGroup;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * @description:
 * @author: orangeBabu
 * @time: 2024/12/31 11:18
 */

@RestController
@RequestMapping("/book")
public class BookController {
    private static final Logger logger = LoggerFactory.getLogger(BookController.class);

    @Autowired
    BookService bookService;

    @GetMapping(value = { "/fetch" })
    public Message<Page<Book>> fetch(BookPageDto dto) {

        logger.debug("fetch {}",dto);

        return bookService.pageList(dto);
    }

    @GetMapping("/get/{id}")
    public Message<Book> getById(@PathVariable(name="id") String id) {

        logger.debug("get {}",id);

        return new Message<>(Message.SUCCESS, bookService.getById(id));
    }

    @PostMapping("/save")
    public Message<String> save(@Validated(value = AddGroup.class) @RequestBody BookChangeDto dto) {
        logger.debug("save {}",dto);
        return bookService.save(dto);
    }

    @PutMapping("/update")
    public Message<String> update(@Validated(value = EditGroup.class) @RequestBody BookChangeDto dto) {
        logger.debug("update {}",dto);
        return bookService.update(dto);
    }

    @DeleteMapping(value = { "/delete" })
    public Message<String> delete(@Validated @RequestBody ListIdsDto dto) {

        logger.debug("delete {}",dto);

        return bookService.delete(dto);
    }

    @GetMapping("/fetchAll")
    public Message<List<BookVo>> listStore(@CurrentUser UserInfo currentUser) {
        return Message.ok(bookService.listBooks(currentUser.getId()));
    }
}
