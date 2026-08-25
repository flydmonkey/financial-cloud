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
 

package com.jinbooks.controller.config;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.jinbooks.authn.annotation.CurrentUser;
import com.jinbooks.common.Message;
import com.jinbooks.domain.config.ConfigPersonalTax;
import com.jinbooks.dto.config.ConfigPersonalTaxChangeDto;
import com.jinbooks.dto.config.ConfigPersonalTaxPageDto;
import com.jinbooks.dto.common.ListIdsDto;
import com.jinbooks.domain.idm.UserInfo;
import com.jinbooks.service.config.ConfigPersonalTaxService;
import com.jinbooks.validate.AddGroup;
import com.jinbooks.validate.EditGroup;
import lombok.extern.slf4j.Slf4j;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

/**
 * @description:
 * @author: orangeBabu
 * @time: 2025/2/6 17:18
 */

@RestController
@RequestMapping("/config/tax")
@Slf4j
public class ConfigPersonalTaxController {
    static final Logger logger = LoggerFactory.getLogger(ConfigPersonalTaxController.class);

    @Autowired
    ConfigPersonalTaxService configJbxTaxService;

    @GetMapping(value = {"/fetch"})
    public Message<Page<ConfigPersonalTax>> fetch(ConfigPersonalTaxPageDto dto, @CurrentUser UserInfo currentUser) {
        dto.setBookId(currentUser.getBookId());
        logger.debug("fetch {}", dto);
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
        logger.debug("save {}", dto);
        return configJbxTaxService.save(dto);
    }

    @PutMapping("/update")
    public Message<String> update(@Validated(value = EditGroup.class) @RequestBody ConfigPersonalTaxChangeDto dto) {
        logger.debug("update {}", dto);

        return configJbxTaxService.update(dto);
    }

    @DeleteMapping("/delete")
    public Message<String> delete(@RequestBody ListIdsDto dto) {
        return configJbxTaxService.delete(dto);
    }
}
