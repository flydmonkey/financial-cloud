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

import com.jinbooks.service.statement.StatementBalanceSheetService;
import com.jinbooks.common.Message;
import com.jinbooks.domain.standard.StandardStatementBalanceSheet;
import com.jinbooks.dto.standard.StandardStatementBalanceSheetListVo;
import com.jinbooks.service.standard.StandardStatementBalanceSheetService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

/**
 * 报表资产负债表配置接口
 */

@RestController
@RequestMapping("/standard/balance-sheet")
@Slf4j
@RequiredArgsConstructor
public class StandardStatementBalanceSheetController {
    private final StandardStatementBalanceSheetService statementBalanceSheetService;

    /**
     * 获取资产负债表配信
     */
    @GetMapping(value = {"/get"})
    public Message<StandardStatementBalanceSheet> get(StandardStatementBalanceSheet dto) {
        return statementBalanceSheetService.get(dto);
    }

    /**
     * 获取资产负债表配置
     */
    @GetMapping(value = {"/fetch"})
    public Message<StandardStatementBalanceSheetListVo> listBalanceSheet(StandardStatementBalanceSheet dto) {
        return statementBalanceSheetService.list(dto);
    }

    /**
     * 资产负债表配置
     *
     * @param dto 查询参数
     * @return 结果
     */
    @PostMapping(value = {"/save"})
    public Message<StandardStatementBalanceSheet> save(@Validated @RequestBody StandardStatementBalanceSheet dto) {
        return statementBalanceSheetService.save(dto);
    }

    /**
     * 资产负债表配置
     *
     * @param id 被删除项ID
     * @return 结果
     */
    @DeleteMapping(value = {"/delete/{id}"})
    public Message<Boolean> delete(@PathVariable("id") String id) {
        return statementBalanceSheetService.delete(id);
    }

}
