package com.financial.cloud.controller.book;

import com.financial.cloud.service.book.BookOwnershipGuard;
import com.baomidou.mybatisplus.core.toolkit.StringUtils;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.financial.cloud.authn.annotation.CurrentUser;
import com.financial.cloud.common.Message;
import com.financial.cloud.constants.auth.ProductRoles;
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

@RestController
@RequestMapping("/api/base/assist-acc")
@Slf4j
@RequiredArgsConstructor
public class AssistAccController {
    private final BookOwnershipGuard bookOwnershipGuard;
    private final AssistAccService assistAccService;

    @GetMapping(value = {"/fetch"})
    public Message<Page<AssistAccVo>> fetch(AssistAccPageDto dto,
                                            @CurrentUser UserInfo userInfo) {
        bookOwnershipGuard.checkRequest("assist_acc", dto);
        dto.setBookId(userInfo.getBookId());
        if (StringUtils.isBlank(dto.getBookId())) {
            return Message.failed("所属账套ID不能为空");
        }
        return assistAccService.pageList(dto);
    }

    @GetMapping("/get/{id}")
    public Message<AssistAccVo> getById(@PathVariable(name = "id") String id) {
        bookOwnershipGuard.checkReference("assist_acc", "id", id);
        return assistAccService.getById(id);
    }

    @PostMapping("/save")
    public Message<String> save(@Validated(value = AddGroup.class) @RequestBody AssistAccChangeDto dto,
                                @CurrentUser UserInfo userInfo) {
        bookOwnershipGuard.checkRequest("assist_acc", dto);
        ProductRoles.requireWriteBusiness();
        dto.setBookId(userInfo.getBookId());
        return assistAccService.save(dto);
    }

    @PutMapping("/update")
    public Message<String> update(@Validated(value = EditGroup.class) @RequestBody AssistAccChangeDto dto,
                                  @CurrentUser UserInfo userInfo) {
        bookOwnershipGuard.checkRequest("assist_acc", dto);
        ProductRoles.requireWriteBusiness();
        dto.setBookId(userInfo.getBookId());
        return assistAccService.update(dto);
    }

    @DeleteMapping("/delete")
    public Message<String> delete(@RequestBody ListIdsDto dto) {
        bookOwnershipGuard.checkRequest("assist_acc", dto);
        ProductRoles.requireWriteBusiness();
        return assistAccService.delete(dto);
    }

}
