package com.financial.cloud.controller.statement;

import com.financial.cloud.authn.annotation.CurrentUser;
import com.financial.cloud.common.Message;
import com.financial.cloud.domain.idm.UserInfo;
import com.financial.cloud.domain.statement.StatementSubjectBalance;
import com.financial.cloud.service.statement.StatementSubjectBalanceService;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/statement/subject-balance")
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
