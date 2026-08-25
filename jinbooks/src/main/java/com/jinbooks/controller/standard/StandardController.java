package com.jinbooks.controller.standard;



import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.jinbooks.common.Message;
import com.jinbooks.dto.common.ListIdsDto;
import com.jinbooks.domain.standard.Standard;
import com.jinbooks.dto.standard.StandardChangeDto;
import com.jinbooks.dto.standard.StandardPageDto;
import com.jinbooks.service.standard.StandardService;
import com.jinbooks.validation.AddGroup;
import com.jinbooks.validation.EditGroup;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Objects;

/**
 * @description:
 * @author: orangeBabu
 * @time: 2024/12/26 17:32
 */

@RequiredArgsConstructor
@Slf4j
@RestController
@RequestMapping("/api/standard")
public class StandardController {

    private final StandardService standardService;

    @GetMapping("/get/{id}")
    public Message<Standard> getById(@PathVariable(name="id") String id) {
        return new Message<>(Message.SUCCESS, standardService.getById(id));
    }

    @GetMapping(value = { "/fetch" })
    public Message<Page<Standard>> fetch(StandardPageDto dto) {

        log.debug("fetch {}",dto);

        return standardService.pageList(dto);
    }

    @GetMapping(value = { "/fetchAll" })
    public Message<List<Standard>> fetchAll(@RequestParam(name="status",required = false) Integer status) {
        LambdaQueryWrapper<Standard> wrapper = new LambdaQueryWrapper<>();
        if (Objects.nonNull(status)) {
            wrapper.eq(Standard::getStatus, status);
        }else {
        	wrapper.eq(Standard::getStatus, 1);
        }
        return new Message<>(standardService.list(wrapper));
    }

    @PostMapping(value = { "/save" })
    public Message<String> save(@Validated(value = AddGroup.class) @RequestBody StandardChangeDto dto) {

        log.debug("save {}",dto);

        return standardService.save(dto);
    }

    @PutMapping(value = { "/update" })
    public Message<String> update(@Validated(value = EditGroup.class) @RequestBody StandardChangeDto dto) {

        log.debug("update {}",dto);

        return standardService.update(dto);
    }

    @DeleteMapping(value = { "/delete" })
    public Message<String> delete(@Validated @RequestBody ListIdsDto dto) {

        log.debug("delete {}",dto);

        return standardService.delete(dto);
    }
}
