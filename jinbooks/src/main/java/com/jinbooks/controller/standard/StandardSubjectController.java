package com.jinbooks.controller.standard;



import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import com.baomidou.mybatisplus.core.toolkit.StringUtils;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.jinbooks.common.Message;
import com.jinbooks.dto.book.SubjectChangeDto;
import com.jinbooks.dto.book.SubjectPageDto;
import com.jinbooks.dto.book.BookSubjectTreeDto;
import com.jinbooks.dto.common.ListIdsDto;
import com.jinbooks.domain.standard.StandardSubject;
import com.jinbooks.service.standard.StandardSubjectService;
import com.jinbooks.validation.AddGroup;
import com.jinbooks.validation.EditGroup;
import org.dromara.hutool.core.tree.MapTree;
import org.springframework.http.MediaType;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * @description: ä¼è®¡ç§ç®æ¥å£
 * @author: orangeBabu
 * @time: 2024/12/19 15:43
 */

@RequiredArgsConstructor
@Slf4j
@RestController
@RequestMapping("/api/standardsubject")
public class StandardSubjectController {

    private final StandardSubjectService standardSubjectService;

    @GetMapping(value = {"/fetch"})
    public Message<Page<StandardSubject>> fetch(SubjectPageDto dto) {

        log.debug("fetch {}", dto);

        return standardSubjectService.pageList(dto);
    }

    @GetMapping(value = {"/tree"}, produces = {MediaType.APPLICATION_JSON_VALUE})
    public Message<List<MapTree<String>>> tree(BookSubjectTreeDto dto) {
        List<MapTree<String>> tree = standardSubjectService.tree(dto);
        return new Message<>(Message.SUCCESS, tree);
    }
    
    @GetMapping(value = {"/reorgDisplayName"}, produces = {MediaType.APPLICATION_JSON_VALUE})
    public Message<String> reorgDisplayName(BookSubjectTreeDto dto) {
    	return standardSubjectService.reorgDisplayName(dto);
    }

    @GetMapping("/get/{id}")
    public Message<StandardSubject> getById(@PathVariable(name = "id") String id) {
        StandardSubject subject = standardSubjectService.getById(id);
        return new Message<>(Message.SUCCESS, subject);
    }

    @PostMapping("/save")
    public Message<String> save(@Validated(value = AddGroup.class) @RequestBody SubjectChangeDto dto) {
        if (StringUtils.isBlank(dto.getStandardId())) {
            return Message.failed("所属会计准则不能为空");
        }
        return standardSubjectService.save(dto);
    }

    @PutMapping("/update")
    public Message<String> update(@Validated(value = EditGroup.class) @RequestBody SubjectChangeDto dto) {
        return standardSubjectService.update(dto);
    }

    @DeleteMapping("/delete")
    public Message<String> delete(@Validated @RequestBody ListIdsDto dto) {
        return standardSubjectService.delete(dto);
    }
}
