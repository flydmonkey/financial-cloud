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
