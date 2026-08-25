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
 

package com.jinbooks.controller.report;

import com.jinbooks.authn.annotation.CurrentUser;
import com.jinbooks.common.Message;
import com.jinbooks.dto.report.*;
import com.jinbooks.domain.idm.UserInfo;
import com.jinbooks.dto.statement.StatementParamsDto;
import com.jinbooks.service.report.FundDashboardService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * èµéä»ªè¡¨ç?
 */

@RequiredArgsConstructor
@RestController
@RequestMapping("/statistics")
@Slf4j
public class FundDashboardController {
    private final FundDashboardService fundDashboardService;

    /**
     * èµéä½é¢
     *
     * @param currentUser å½åè´¦æ·
     * @param params      æ¥è¯¢åæ°
     * @return ç»æ
     */
    @GetMapping(value = {"/fund-balance"})
    public Message<FundBalanceVo> statisticsFundBalance(@CurrentUser UserInfo currentUser,
                                                        StatementParamsDto params) {
        params.parse();
        params.setBookId(currentUser.getBookId());
        FundBalanceVo data = fundDashboardService.statisticsFundBalance(params);
        return Message.ok(data);
    }

    /**
     * åºæ¶è´¦æ¬¾
     *
     * @param currentUser å½åè´¦æ·
     * @param params      æ¥è¯¢åæ°
     * @return ç»æ
     */
    @GetMapping(value = {"/accounts-receivable"})
    public Message<AccountsReceivePaymentVo> statisticsAccountsReceivable(@CurrentUser UserInfo currentUser,
                                                                          StatementParamsDto params) {
        params.parse();
        params.setBookId(currentUser.getBookId());
        AccountsReceivePaymentVo data = fundDashboardService.statisticsAccountsReceivable(params);
        return Message.ok(data);
    }

    /**
     * åºä»è´¦æ¬¾
     *
     * @param currentUser å½åè´¦æ·
     * @param params      æ¥è¯¢åæ°
     * @return ç»æ
     */
    @GetMapping(value = {"/accounts-payable"})
    public Message<AccountsReceivePaymentVo> statisticsAccountsPayable(@CurrentUser UserInfo currentUser,
                                                                       StatementParamsDto params) {
        params.parse();
        params.setBookId(currentUser.getBookId());
        AccountsReceivePaymentVo data = fundDashboardService.statisticsAccountsPayable(params);
        return Message.ok(data);
    }

    /**
     * é¢è®¡å¯ç¨ç°é
     *
     * @param currentUser å½åè´¦æ·
     * @param params      æ¥è¯¢åæ°
     * @return ç»æ
     */
    @GetMapping(value = {"/able-cash"})
    public Message<ExpectedAvailableFunds> statisticsAbleCash(@CurrentUser UserInfo currentUser,
                                                              StatementParamsDto params) {
        params.parse();
        params.setBookId(currentUser.getBookId());
        ExpectedAvailableFunds data = fundDashboardService.statisticsAbleCash(params);
        return Message.ok(data);
    }

    /**
     * å¶ä»ç§ç®ææ 
     *
     * @param currentUser å½åè´¦æ·
     * @param params      æ¥è¯¢åæ°
     * @return ç»æ
     */
    @GetMapping(value = {"/other-subjects"})
    public Message<List<OtherSubjectsVo>> statisticsOtherSubjects(@CurrentUser UserInfo currentUser,
                                                              StatementParamsDto params) {
        params.parse();
        params.setBookId(currentUser.getBookId());
        List<OtherSubjectsVo> data = fundDashboardService.statisticsOtherSubjects(params);
        return Message.ok(data);
    }

    /**
     * åå©æ¶¦
     *
     * @param currentUser å½åè´¦æ·
     * @param params      æ¥è¯¢åæ°
     * @return ç»æ
     */
    @GetMapping(value = {"/net-profit"})
    public Message<NetProfitVo> statisticsNetProfit(@CurrentUser UserInfo currentUser,
                                                              StatementParamsDto params) {
        params.parse();
        params.setBookId(currentUser.getBookId());
        NetProfitVo data = fundDashboardService.statisticsNetProfit(params);
        return Message.ok(data);
    }

    /**
     * æ¶å¥ææ¬
     *
     * @param currentUser å½åè´¦æ·
     * @param params      æ¥è¯¢åæ°
     * @return ç»æ
     */
    @GetMapping(value = {"/revenue-cost"})
    public Message<RevenueCostVo> statisticsRevenueCost(@CurrentUser UserInfo currentUser,
                                                        StatementParamsDto params) {
        params.parse();
        params.setBookId(currentUser.getBookId());
        RevenueCostVo data = fundDashboardService.statisticsRevenueCost(params);
        return Message.ok(data);
    }

    /**
     * è´¹ç¨
     *
     * @param currentUser å½åè´¦æ·
     * @param params      æ¥è¯¢åæ°
     * @return ç»æ
     */
    @GetMapping(value = {"/expense"})
    public Message<ExpenseVo> statisticsExpense(@CurrentUser UserInfo currentUser,
                                                        StatementParamsDto params) {
        params.parse();
        params.setBookId(currentUser.getBookId());
        ExpenseVo data = fundDashboardService.statisticsExpense(params);
        return Message.ok(data);
    }

    /**
     * è´¹ç¨
     *
     * @param currentUser å½åè´¦æ·
     * @param params      æ¥è¯¢åæ°
     * @return ç»æ
     */
    @GetMapping(value = {"/added-tax"})
    public Message<AddTaxVo> statisticsAddedTax(@CurrentUser UserInfo currentUser,
                                                        StatementParamsDto params) {
        params.parse();
        params.setBookId(currentUser.getBookId());
        AddTaxVo data = fundDashboardService.statisticsAddedTax(params);
        return Message.ok(data);
    }


}
