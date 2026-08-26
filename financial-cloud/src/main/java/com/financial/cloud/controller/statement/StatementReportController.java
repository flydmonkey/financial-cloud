package com.financial.cloud.controller.statement;

import com.financial.cloud.authn.annotation.CurrentUser;
import com.financial.cloud.common.Message;
import com.financial.cloud.domain.idm.UserInfo;
import com.financial.cloud.domain.statement.StatementCashFlow;
import com.financial.cloud.domain.statement.StatementSubjectBalance;
import com.financial.cloud.dto.statement.StatementParamsDto;
import com.financial.cloud.enums.error.StatementErrorCode;
import com.financial.cloud.exception.ServiceException;
import com.financial.cloud.service.statement.StatementReportService;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.web.bind.annotation.*;

import java.io.IOException;
import java.util.List;

/**
 * åç±»æ¥è¡¨æ¥å£
 */

@RestController
@RequestMapping("/api/statement")
@Slf4j
@RequiredArgsConstructor
public class StatementReportController {
    private final StatementReportService statementReportService;

    /**
     * æ¥è¡¨-ç°éæµéè¡?
     *
     * @param dto æ¥è¯¢åæ°
     * @return ç»æ
     */
    @GetMapping(value = {"/cash-flow"})
    public Message<List<StatementCashFlow>> cashFlow(StatementParamsDto dto,
                                                                   @CurrentUser UserInfo userInfo) {
        dto.setBookId(userInfo.getBookId());
        validParams(dto);
        return statementReportService.cashFlowStatement(dto);
    }

    /**
     * ç°éæµéè¡¨å¯¼åºåè?
     */
    @GetMapping("/cash-flow/export")
    public void cashFlowExport(HttpServletResponse response,
                       StatementParamsDto dto,
                       @CurrentUser UserInfo userInfo) throws IOException {
        dto.setBookId(userInfo.getBookId());
        validParams(dto);
        statementReportService.cashFlowExport(dto, response);
    }

    /**
     * æ¥è¡¨-ç§ç®ä½é¢è¡?
     *
     * @param dto æ¥è¯¢åæ°
     * @return ç»æ
     */
    @GetMapping(value = {"/subject-balance"})
    public Message<List<StatementSubjectBalance>> subjectBalance(StatementParamsDto dto,
                                                               @CurrentUser UserInfo userInfo) {
        dto.setBookId(userInfo.getBookId());
        validParams(dto);
        return statementReportService.subjectBalance(dto);
    }

    /**
     * ç§ç®ä½é¢è¡¨å¯¼åºåè?
     */
    @GetMapping("/subject-balance/export")
    public void subjectBalanceExport(HttpServletResponse response,
                       StatementParamsDto dto,
                       @CurrentUser UserInfo userInfo) throws IOException {
        dto.setBookId(userInfo.getBookId());
        validParams(dto);
        statementReportService.subjectBalanceExport(dto, response);
    }

    /**
     * æ¥è¡¨-å­è¯æ±æ»è¡¨
     *
     * @param dto æ¥è¯¢åæ°
     * @return ç»æ
     */
    @GetMapping(value = {"/voucher-summary"})
    public Message<List<StatementSubjectBalance>> voucherSummary(StatementParamsDto dto,
                                                            @CurrentUser UserInfo userInfo) {
        dto.setBookId(userInfo.getBookId());
        validParams(dto);
        return statementReportService.voucherSummary(dto);
    }

    /**
     * å­è¯æ±æ»è¡¨å¯¼åºåè½
     */
    @GetMapping("/voucher-summary/export")
    public void voucherSummaryExport(HttpServletResponse response,
                                     StatementParamsDto dto,
                                     @CurrentUser UserInfo userInfo) throws IOException {
        dto.setBookId(userInfo.getBookId());
        validParams(dto);
        statementReportService.voucherSummaryExport(dto, response);
    }

    private void validParams(StatementParamsDto dto) {
        if (StringUtils.isEmpty(dto.getPeriodType())) {
            throw new ServiceException(StatementErrorCode.PERIOD_TYPE_EMPTY);
        } else if (StringUtils.isEmpty(dto.getReportDate())) {
            throw new ServiceException(StatementErrorCode.REPORT_DATE_EMPTY);
        }
    }
}
