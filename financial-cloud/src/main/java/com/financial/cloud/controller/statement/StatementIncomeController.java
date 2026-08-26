package com.financial.cloud.controller.statement;

import com.financial.cloud.authn.annotation.CurrentUser;
import com.financial.cloud.common.Message;
import com.financial.cloud.domain.idm.UserInfo;
import com.financial.cloud.domain.statement.StatementIncome;
import com.financial.cloud.dto.statement.StatementParamsDto;
import com.financial.cloud.enums.error.StatementErrorCode;
import com.financial.cloud.exception.ServiceException;
import com.financial.cloud.service.statement.StatementIncomeService;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.web.bind.annotation.*;

import java.io.IOException;


@RestController
@RequestMapping("/api/statement")
@Slf4j
@RequiredArgsConstructor
public class StatementIncomeController {

    private final StatementIncomeService statementIncomeService;

    @GetMapping(value = {"/income"})
    public Message<StatementIncome> income(StatementParamsDto dto,
                                           @CurrentUser UserInfo userInfo) {
        dto.setBookId(userInfo.getBookId());
        validParams(dto);
        log.debug("StatementParamsDto {} ", dto);
        return statementIncomeService.getIncomeStatement(dto, false);
    }

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
            throw new ServiceException(StatementErrorCode.PERIOD_TYPE_EMPTY);
        } else if (StringUtils.isEmpty(dto.getReportDate())) {
            throw new ServiceException(StatementErrorCode.REPORT_DATE_EMPTY);
        }
    }
}
