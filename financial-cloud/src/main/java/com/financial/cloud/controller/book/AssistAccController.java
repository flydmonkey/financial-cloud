package com.financial.cloud.controller.book;

import com.baomidou.mybatisplus.core.toolkit.StringUtils;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.financial.cloud.authn.annotation.CurrentUser;
import com.financial.cloud.common.Message;
import com.financial.cloud.dto.book.AssistAccVo;
import com.financial.cloud.dto.common.ListIdsDto;
import com.financial.cloud.dto.book.AssistAccChangeDto;
import com.financial.cloud.dto.book.AssistAccPageDto;
import com.financial.cloud.domain.idm.UserInfo;
import com.financial.cloud.service.book.AssistAccService;
import com.financial.cloud.validation.AddGroup;
import com.financial.cloud.validation.EditGroup;
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
