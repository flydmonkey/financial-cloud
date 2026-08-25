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
 

package com.jinbooks.service.report;

import com.jinbooks.service.report.FundDashboardService;
import com.jinbooks.dto.report.*;
import com.jinbooks.dto.statement.StatementParamsDto;

import java.util.List;

public interface FundDashboardService {

    FundBalanceVo statisticsFundBalance(StatementParamsDto params);

    AccountsReceivePaymentVo statisticsAccountsReceivable(StatementParamsDto params);

    AccountsReceivePaymentVo statisticsAccountsPayable(StatementParamsDto params);

    ExpectedAvailableFunds statisticsAbleCash(StatementParamsDto params);

    List<OtherSubjectsVo> statisticsOtherSubjects(StatementParamsDto params);

    NetProfitVo statisticsNetProfit(StatementParamsDto params);

    RevenueCostVo statisticsRevenueCost(StatementParamsDto params);

    ExpenseVo statisticsExpense(StatementParamsDto params);

    AddTaxVo statisticsAddedTax(StatementParamsDto params);
}
