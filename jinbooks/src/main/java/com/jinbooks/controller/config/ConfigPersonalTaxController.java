package com.jinbooks.controller.config;


import lombok.RequiredArgsConstructor;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.jinbooks.authn.annotation.CurrentUser;
import com.jinbooks.common.Message;
import com.jinbooks.domain.config.ConfigPersonalTax;
import com.jinbooks.dto.config.ConfigPersonalTaxChangeDto;
import com.jinbooks.dto.config.ConfigPersonalTaxPageDto;
import com.jinbooks.dto.common.ListIdsDto;
import com.jinbooks.domain.idm.UserInfo;
import com.jinbooks.service.config.ConfigPersonalTaxService;
import com.jinbooks.validation.AddGroup;
import com.jinbooks.validation.EditGroup;
import lombok.extern.slf4j.Slf4j;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

/**
 * @description:
 * @author: orangeBabu
 * @time: 2025/2/6 17:18
 */

@RequiredArgsConstructor
@RestController
@RequestMapping("/api/config/tax")
@Slf4j
public class ConfigPersonalTaxController {

    private final ConfigPersonalTaxService configJbxTaxService;

    @GetMapping(value = {"/fetch"})
    public Message<Page<ConfigPersonalTax>> fetch(ConfigPersonalTaxPageDto dto, @CurrentUser UserInfo currentUser) {
        dto.setBookId(currentUser.getBookId());
        log.debug("fetch {}", dto);
        return configJbxTaxService.pageList(dto);
    }

    @GetMapping("/get/{id}")
    public Message<ConfigPersonalTax> getById(@PathVariable(name = "id") String id) {
        return Message.ok(configJbxTaxService.getById(id));
    }

    @PostMapping("/save")
    public Message<String> save(@Validated(value = AddGroup.class) @RequestBody ConfigPersonalTaxChangeDto dto,
                                @CurrentUser UserInfo currentUser) {
        dto.setBookId(currentUser.getBookId());
        log.debug("save {}", dto);
        return configJbxTaxService.save(dto);
    }

    @PutMapping("/update")
    public Message<String> update(@Validated(value = EditGroup.class) @RequestBody ConfigPersonalTaxChangeDto dto) {
        log.debug("update {}", dto);

        return configJbxTaxService.update(dto);
    }

    @DeleteMapping("/delete")
    public Message<String> delete(@RequestBody ListIdsDto dto) {
        return configJbxTaxService.delete(dto);
    }
}
