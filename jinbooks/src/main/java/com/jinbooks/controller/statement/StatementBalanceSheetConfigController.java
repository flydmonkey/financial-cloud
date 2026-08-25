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
 

package com.jinbooks.controller.statement;

import com.jinbooks.service.config.ConfigService;
import com.jinbooks.authn.annotation.CurrentUser;
import com.jinbooks.common.Message;
import com.jinbooks.domain.idm.UserInfo;
import com.jinbooks.domain.statement.StatementBalanceSheetItem;
import com.jinbooks.dto.statement.StatementBalanceSheetItemListVo;
import com.jinbooks.service.statement.StatementBalanceSheetConfigService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;


/**
 * 报表资产负债表配置接口
 */

@RestController
@RequestMapping("/statement/config/balance-sheet")
@Slf4j
@RequiredArgsConstructor
public class StatementBalanceSheetConfigController {
    private final StatementBalanceSheetConfigService configService;

    /**
     * 获取资产负债表配置
     */
    @GetMapping(value = {"/{itemCode}"})
    public Message<StatementBalanceSheetItem> get(@PathVariable("itemCode") String itemCode,
                                                  @CurrentUser UserInfo userInfo) {
        return configService.get(userInfo.getBookId(), itemCode);
    }

    /**
     * 获取资产负债表配置
     */
    @GetMapping(value = {"/fetch"})
    public Message<StatementBalanceSheetItemListVo> list(@CurrentUser UserInfo userInfo) {
        return configService.list(userInfo.getBookId());
    }

    /**
     * 资产负债表配置
     *
     * @param dto 查询参数
     * @return 结果
     */
    @PostMapping
    public Message<StatementBalanceSheetItem> save(@Validated @RequestBody StatementBalanceSheetItem dto,
                                                   @CurrentUser UserInfo userInfo) {
        dto.setBookId(userInfo.getBookId());
        return configService.save(dto);
    }

    /**
     * 资产负债表配置
     *
     * @param id 被删除项ID
     * @return 结果
     */
    @DeleteMapping(value = {"/{id}"})
    public Message<Boolean> delete(@PathVariable("id") String id) {
        return configService.delete(id);
    }

}
