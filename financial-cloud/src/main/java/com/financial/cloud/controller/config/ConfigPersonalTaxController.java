package com.financial.cloud.controller.config;


import lombok.RequiredArgsConstructor;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.financial.cloud.authn.annotation.CurrentUser;
import com.financial.cloud.common.Message;
import com.financial.cloud.domain.config.ConfigPersonalTax;
import com.financial.cloud.dto.config.ConfigPersonalTaxChangeDto;
import com.financial.cloud.dto.config.ConfigPersonalTaxPageDto;
import com.financial.cloud.dto.common.ListIdsDto;
import com.financial.cloud.domain.idm.UserInfo;
import com.financial.cloud.service.config.ConfigPersonalTaxService;
import com.financial.cloud.validation.AddGroup;
import com.financial.cloud.validation.EditGroup;
import lombok.extern.slf4j.Slf4j;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

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
