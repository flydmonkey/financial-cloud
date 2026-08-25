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
 

package com.jinbooks.service.statement;

import com.jinbooks.common.Message;
import com.jinbooks.domain.statement.StatementCashFlow;
import com.jinbooks.domain.statement.StatementSubjectBalance;
import com.jinbooks.dto.statement.StatementParamsDto;
import jakarta.servlet.http.HttpServletResponse;

import java.io.IOException;
import java.math.BigDecimal;
import java.util.List;

/**
 * 财务报表业务接口
 */
public interface StatementReportService {

    Message<List<StatementCashFlow>> cashFlowStatement(StatementParamsDto dto);

    Message<List<StatementSubjectBalance>> subjectBalance(StatementParamsDto dto);

    Message<List<StatementSubjectBalance>> voucherSummary(StatementParamsDto dto);

    /**
     * @Description: 获取当前期的期末余额
     * @Param: [dto]
     * @return: java.math.BigDecimal
     * @Author: xZen
     * @Date: 2025/5/14 17:36
     */
    BigDecimal getEndingBalance(StatementParamsDto dto);

    void cashFlowExport(StatementParamsDto dto, HttpServletResponse response) throws IOException;

    void subjectBalanceExport(StatementParamsDto dto, HttpServletResponse response) throws IOException;

    void voucherSummaryExport(StatementParamsDto dto, HttpServletResponse response) throws IOException;
}
