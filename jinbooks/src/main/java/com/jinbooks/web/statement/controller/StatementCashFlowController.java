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
 

package com.jinbooks.web.statement.controller;

import com.jinbooks.authn.annotation.CurrentUser;
import com.jinbooks.entity.Message;
import com.jinbooks.entity.idm.UserInfo;
import com.jinbooks.entity.statement.StatementCashFlow;
import com.jinbooks.entity.voucher.dto.VoucherItemCashFlowDto;
import com.jinbooks.entity.voucher.dto.VoucherItemPageDto;
import com.jinbooks.entity.voucher.vo.VoucherItemVo;
import com.jinbooks.exception.BusinessException;
import com.jinbooks.persistence.service.ConfigSysService;
import com.jinbooks.persistence.service.StatementCashFlowService;
import com.jinbooks.persistence.service.VoucherItemCashFlowService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springdoc.core.annotations.ParameterObject;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * @description:
 * @author: orangeBabu
 * @time: 2025/3/25 15:00
 */


@RestController
@RequestMapping("/statement/cash-flow")
@Slf4j
@RequiredArgsConstructor
public class StatementCashFlowController {
    private final VoucherItemCashFlowService voucherItemCashFlowService;

    private final StatementCashFlowService statementCashFlowService;

    private final ConfigSysService configSysService;


    @PostMapping("/specify")
    public Message<String> specifyCashFlowItems(@Validated @RequestBody VoucherItemCashFlowDto dto, @CurrentUser UserInfo user) {
        dto.setBookId(user.getBookId());
        String voucherDate = dto.getVoucherDate();
        String currentTerm = configSysService.getCurrentTerm(dto.getBookId());
        if (!currentTerm.equals(voucherDate)) {
            throw new BusinessException(510001, "不能修改非当前账套期间的凭证项的现金流量项");
        }
        return voucherItemCashFlowService.specifyCashFlowItems(dto);
    }

    @GetMapping("/get")
    public Message<List<VoucherItemVo>> getCashFlowItems(@ParameterObject VoucherItemPageDto paramsDto, @CurrentUser UserInfo user) {
        paramsDto.setBookId(user.getBookId());
        return voucherItemCashFlowService.getCashFlowItems(paramsDto);
    }

    @PostMapping("/save")
    public Message<String> changeSpecifyItem(@RequestBody StatementCashFlow statementCashFlow, @CurrentUser UserInfo user) {
        statementCashFlow.setBookId(user.getBookId());
        return statementCashFlowService.changeSpecifyItem(statementCashFlow);
    }
}
