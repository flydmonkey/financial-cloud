package com.financial.cloud.controller.standard;



import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import com.baomidou.mybatisplus.core.toolkit.StringUtils;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.financial.cloud.common.Message;
import com.financial.cloud.dto.book.SubjectChangeDto;
import com.financial.cloud.dto.book.SubjectPageDto;
import com.financial.cloud.dto.book.BookSubjectTreeDto;
import com.financial.cloud.dto.common.ListIdsDto;
import com.financial.cloud.domain.standard.StandardSubject;
import com.financial.cloud.service.standard.StandardSubjectService;
import com.financial.cloud.validation.AddGroup;
import com.financial.cloud.validation.EditGroup;
import cn.hutool.core.lang.tree.Tree;
import org.springframework.http.MediaType;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.List;


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
    public Message<List<Tree<String>>> tree(BookSubjectTreeDto dto) {
        List<Tree<String>> tree = standardSubjectService.tree(dto);
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
