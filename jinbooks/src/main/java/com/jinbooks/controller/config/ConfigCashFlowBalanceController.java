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

import java.util.List;

import com.jinbooks.dto.report.CashFlowSubjectBalanceVo;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.jinbooks.authn.annotation.CurrentUser;
import com.jinbooks.common.Message;
import com.jinbooks.domain.config.ConfigCashFlowBalance;
import com.jinbooks.dto.config.ConfigCashFlowChangeDto;
import com.jinbooks.dto.config.ConfigCashFlowPageDto;
import com.jinbooks.domain.idm.UserInfo;
import com.jinbooks.service.config.ConfigCashFlowBalanceService;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * @description:
 * @author: orangeBabu
 * @time: 2025/3/19 10:41
 */

@RestController
@RequestMapping("/config/cash-flow-balance")
@Slf4j
@RequiredArgsConstructor
public class ConfigCashFlowBalanceController {
    static final Logger logger = LoggerFactory.getLogger(ConfigCashFlowBalanceController.class);

    private final ConfigCashFlowBalanceService configCashFlowService;

    @GetMapping(value = {"/fetch"})
    public Message<CashFlowSubjectBalanceVo> fetch(ConfigCashFlowPageDto dto,
                                                   @CurrentUser UserInfo currentUser) {
        logger.debug("fetch {}", dto);
        dto.setBookId(currentUser.getBookId());
        return configCashFlowService.pageList(dto);
    }

    @PostMapping("/save")
    public Message<String> save(@RequestBody ConfigCashFlowChangeDto dto,
                                                      @CurrentUser UserInfo currentUser) {
        logger.debug("save {}", dto);
        return configCashFlowService.save(dto);
    }


    @GetMapping(value = {"/get-select-item"})
    public Message<List<ConfigCashFlowBalance>> getSelectItem(@RequestParam(name="cashFlowItemType") Integer cashFlowItemType) {
        return configCashFlowService.getSelectItem(cashFlowItemType);
    }
}
