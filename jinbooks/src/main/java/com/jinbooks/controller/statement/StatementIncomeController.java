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

import com.jinbooks.authn.annotation.CurrentUser;
import com.jinbooks.common.Message;
import com.jinbooks.domain.idm.UserInfo;
import com.jinbooks.domain.statement.StatementIncome;
import com.jinbooks.dto.statement.StatementParamsDto;
import com.jinbooks.exception.ServiceException;
import com.jinbooks.service.statement.StatementIncomeService;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.web.bind.annotation.*;

import java.io.IOException;

/**
 * å©æ¶¦è¡¨æ¥å?
 */

@RestController
@RequestMapping("/statement")
@Slf4j
@RequiredArgsConstructor
public class StatementIncomeController {

    //å©æ¶¦è¡?
    private final StatementIncomeService statementIncomeService;

    /**
     * æ¥è¡¨-æ¥è¯¢å©æ¶¦è¡?
     *
     * @param dto æ¥è¯¢åæ°
     * @return ç»æ
     */
    @GetMapping(value = {"/income"})
    public Message<StatementIncome> income(StatementParamsDto dto,
                                           @CurrentUser UserInfo userInfo) {
        dto.setBookId(userInfo.getBookId());
        validParams(dto);
        log.debug("StatementParamsDto {} ", dto);
        return statementIncomeService.getIncomeStatement(dto, false);
    }

    /**
     * å¯¼åºåè½
     */
    @GetMapping("/income/export")
    public void export(HttpServletResponse response,
                       StatementParamsDto dto,
                       @CurrentUser UserInfo userInfo) throws IOException {
        dto.setBookId(userInfo.getBookId());
        validParams(dto);
        statementIncomeService.export(dto, response);
    }

    private void validParams(StatementParamsDto dto) {
        if (StringUtils.isEmpty(dto.getPeriodType())) {
            throw new ServiceException("ç»è®¡ç±»ååæ°ä¸ºç©º");
        } else if (StringUtils.isEmpty(dto.getReportDate())) {
            throw new ServiceException("ç»è®¡æ¥æåæ°ä¸ºç©º");
        }
    }
}
