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
import com.jinbooks.domain.statement.StatementSubjectBalance;
import com.jinbooks.service.statement.StatementSubjectBalanceService;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

/**
 * 科目余额表接口
 */

@RestController
@RequestMapping("/statement/subject-balance")
@Slf4j
@RequiredArgsConstructor
public class StatementSubjectBalanceController {
    private final StatementSubjectBalanceService statementSubjectBalanceService;

    /**
     * 获取单个
     */
    @GetMapping(value = {"/get"})
    public Message<StatementSubjectBalance> getSubjectBalance(StatementSubjectBalance params,
                                                            @CurrentUser UserInfo userInfo) {
        params.setBookId(userInfo.getBookId());
        return statementSubjectBalanceService.getSubjectBalance(params);
    }

}
