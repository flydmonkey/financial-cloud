package com.financial.cloud.controller.report;

import com.financial.cloud.authn.annotation.CurrentUser;
import com.financial.cloud.common.Message;
import com.financial.cloud.dto.report.*;
import com.financial.cloud.domain.idm.UserInfo;
import com.financial.cloud.dto.statement.StatementParamsDto;
import com.financial.cloud.service.report.FundDashboardService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;


@RequiredArgsConstructor
@RestController
@RequestMapping("/api/statistics")
@Slf4j
public class FundDashboardController {
    private final FundDashboardService fundDashboardService;

    private static void bindBookContext(StatementParamsDto params, UserInfo currentUser) {
        params.setBookId(currentUser.getBookId());
        params.parse();
    }

    @GetMapping(value = {"/fund-balance"})
    public Message<FundBalanceVo> statisticsFundBalance(@CurrentUser UserInfo currentUser,
                                                        StatementParamsDto params) {
        bindBookContext(params, currentUser);
        FundBalanceVo data = fundDashboardService.statisticsFundBalance(params);
        return Message.ok(data);
    }

    @GetMapping(value = {"/accounts-receivable"})
    public Message<AccountsReceivePaymentVo> statisticsAccountsReceivable(@CurrentUser UserInfo currentUser,
                                                                          StatementParamsDto params) {
        bindBookContext(params, currentUser);
        AccountsReceivePaymentVo data = fundDashboardService.statisticsAccountsReceivable(params);
        return Message.ok(data);
    }

    @GetMapping(value = {"/accounts-payable"})
    public Message<AccountsReceivePaymentVo> statisticsAccountsPayable(@CurrentUser UserInfo currentUser,
                                                                       StatementParamsDto params) {
        bindBookContext(params, currentUser);
        AccountsReceivePaymentVo data = fundDashboardService.statisticsAccountsPayable(params);
        return Message.ok(data);
    }

    @GetMapping(value = {"/able-cash"})
    public Message<ExpectedAvailableFunds> statisticsAbleCash(@CurrentUser UserInfo currentUser,
                                                              StatementParamsDto params) {
        bindBookContext(params, currentUser);
        ExpectedAvailableFunds data = fundDashboardService.statisticsAbleCash(params);
        return Message.ok(data);
    }

    @GetMapping(value = {"/other-subjects"})
    public Message<List<OtherSubjectsVo>> statisticsOtherSubjects(@CurrentUser UserInfo currentUser,
                                                              StatementParamsDto params) {
        bindBookContext(params, currentUser);
        List<OtherSubjectsVo> data = fundDashboardService.statisticsOtherSubjects(params);
        return Message.ok(data);
    }

    @GetMapping(value = {"/net-profit"})
    public Message<NetProfitVo> statisticsNetProfit(@CurrentUser UserInfo currentUser,
                                                              StatementParamsDto params) {
        bindBookContext(params, currentUser);
        NetProfitVo data = fundDashboardService.statisticsNetProfit(params);
        return Message.ok(data);
    }

    @GetMapping(value = {"/revenue-cost"})
    public Message<RevenueCostVo> statisticsRevenueCost(@CurrentUser UserInfo currentUser,
                                                        StatementParamsDto params) {
        bindBookContext(params, currentUser);
        RevenueCostVo data = fundDashboardService.statisticsRevenueCost(params);
        return Message.ok(data);
    }

    @GetMapping(value = {"/expense"})
    public Message<ExpenseVo> statisticsExpense(@CurrentUser UserInfo currentUser,
                                                        StatementParamsDto params) {
        bindBookContext(params, currentUser);
        ExpenseVo data = fundDashboardService.statisticsExpense(params);
        return Message.ok(data);
    }

    @GetMapping(value = {"/added-tax"})
    public Message<AddTaxVo> statisticsAddedTax(@CurrentUser UserInfo currentUser,
                                                        StatementParamsDto params) {
        bindBookContext(params, currentUser);
        AddTaxVo data = fundDashboardService.statisticsAddedTax(params);
        return Message.ok(data);
    }


}
