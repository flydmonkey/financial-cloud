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
 

package com.jinbooks.controller.standard;

import com.baomidou.mybatisplus.core.toolkit.StringUtils;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.jinbooks.common.Message;
import com.jinbooks.dto.book.SubjectChangeDto;
import com.jinbooks.dto.book.SubjectPageDto;
import com.jinbooks.dto.book.BookSubjectTreeDto;
import com.jinbooks.dto.common.ListIdsDto;
import com.jinbooks.domain.standard.StandardSubject;
import com.jinbooks.service.standard.StandardSubjectService;
import com.jinbooks.validate.AddGroup;
import com.jinbooks.validate.EditGroup;
import org.dromara.hutool.core.tree.MapTree;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * @description: ä¼è®¡ç§ç®æ¥å£
 * @author: orangeBabu
 * @time: 2024/12/19 15:43
 */

@RestController
@RequestMapping("/standardsubject")
public class StandardSubjectController {
    private static final Logger logger = LoggerFactory.getLogger(StandardSubjectController.class);

    @Autowired
    StandardSubjectService standardSubjectService;

    @GetMapping(value = {"/fetch"})
    public Message<Page<StandardSubject>> fetch(SubjectPageDto dto) {

        logger.debug("fetch {}", dto);

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
