package com.jinbooks.controller.book;

import com.baomidou.mybatisplus.core.toolkit.StringUtils;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.jinbooks.authn.annotation.CurrentUser;
import com.jinbooks.common.Message;
import com.jinbooks.dto.book.AssistAccVo;
import com.jinbooks.dto.common.ListIdsDto;
import com.jinbooks.dto.book.AssistAccChangeDto;
import com.jinbooks.dto.book.AssistAccPageDto;
import com.jinbooks.domain.idm.UserInfo;
import com.jinbooks.service.book.AssistAccService;
import com.jinbooks.validation.AddGroup;
import com.jinbooks.validation.EditGroup;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

/**
 * 辅助核算项目管理接口
 */

@RestController
@RequestMapping("/api/base/assist-acc")
@Slf4j
@RequiredArgsConstructor
public class AssistAccController {
    private final AssistAccService assistAccService;

    @GetMapping(value = {"/fetch"})
    public Message<Page<AssistAccVo>> fetch(AssistAccPageDto dto,
                                            @CurrentUser UserInfo userInfo) {
        dto.setBookId(userInfo.getBookId());
        if (StringUtils.isBlank(dto.getBookId())) {
            return Message.failed("所属账套ID不能为空");
        }
        return assistAccService.pageList(dto);
    }

    @GetMapping("/get/{id}")
    public Message<AssistAccVo> getById(@PathVariable(name = "id") String id) {
        return assistAccService.getById(id);
    }

    @PostMapping("/save")
    public Message<String> save(@Validated(value = AddGroup.class) @RequestBody AssistAccChangeDto dto,
                                @CurrentUser UserInfo userInfo) {
        dto.setBookId(userInfo.getBookId());
        return assistAccService.save(dto);
    }

    @PutMapping("/update")
    public Message<String> update(@Validated(value = EditGroup.class) @RequestBody AssistAccChangeDto dto,
                                  @CurrentUser UserInfo userInfo) {
        dto.setBookId(userInfo.getBookId());
        return assistAccService.update(dto);
    }

    @DeleteMapping("/delete")
    public Message<String> delete(@RequestBody ListIdsDto dto) {
        return assistAccService.delete(dto);
    }

}
