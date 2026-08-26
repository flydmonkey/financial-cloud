package com.financial.cloud.controller.book;



import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.financial.cloud.common.Message;
import com.financial.cloud.domain.book.BookSubject;
import com.financial.cloud.dto.book.BookSubjectTreeDto;
import com.financial.cloud.dto.book.SubjectChangeDto;
import com.financial.cloud.dto.book.SubjectPageDto;
import com.financial.cloud.dto.common.BookQueryDto;
import com.financial.cloud.dto.common.ListIdsDto;
import com.financial.cloud.service.book.BookSubjectService;
import com.financial.cloud.validation.AddGroup;
import com.financial.cloud.validation.EditGroup;

import java.util.List;

import cn.hutool.core.lang.tree.Tree;
import org.springframework.http.MediaType;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

/**
 * @description:
 * @author: orangeBabu
 * @time: 2025/1/17 14:40
 */

@RequiredArgsConstructor
@Slf4j
@RestController
@RequestMapping("/api/booksubject")
public class BookSubjectController {

    private final BookSubjectService bookSubjectService;

    @GetMapping(value = {"/tree/{bookId}"}, produces = {MediaType.APPLICATION_JSON_VALUE})
    public Message<List<Tree<String>>> treeByBookId(@PathVariable(name = "bookId") String bookId) {
        List<Tree<String>> tree = bookSubjectService.tree(bookId);
        return new Message<>(Message.SUCCESS, tree);
    }

    @GetMapping(value = {"/fetch"})
    public Message<Page<BookSubject>> fetch(SubjectPageDto dto) {

        log.debug("fetchBySet {}", dto);

        return bookSubjectService.pageList(dto);
    }

    @PostMapping("/save")
    public Message<String> save(@Validated(value = AddGroup.class) @RequestBody SubjectChangeDto dto) {
        return bookSubjectService.save(dto);
    }

    @GetMapping("/get")
    public Message<BookSubject> getById(BookQueryDto dto) {
        return new Message<>(Message.SUCCESS, bookSubjectService.getById(dto.getBookId(), dto.getId()));
    }

    @PutMapping("/update")
    public Message<String> update(@Validated(value = EditGroup.class) @RequestBody SubjectChangeDto dto) {
        return bookSubjectService.update(dto);
    }


    @DeleteMapping("/delete")
    public Message<String> delete(@Validated @RequestBody ListIdsDto dto) {
        return bookSubjectService.delete(dto);
    }

    @GetMapping(value = {"/reorgDisplayName"}, produces = {MediaType.APPLICATION_JSON_VALUE})
    public Message<String> reorgDisplayName(BookSubjectTreeDto dto) {
        return bookSubjectService.reorgDisplayName(dto.getBookId());
    }
}
