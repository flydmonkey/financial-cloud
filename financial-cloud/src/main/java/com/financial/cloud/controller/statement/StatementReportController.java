package com.financial.cloud.controller.statement;

import com.financial.cloud.authn.annotation.CurrentUser;
import com.financial.cloud.common.Message;
import com.financial.cloud.domain.idm.UserInfo;
import com.financial.cloud.domain.statement.StatementCashFlow;
import com.financial.cloud.domain.statement.StatementSubjectBalance;
import com.financial.cloud.dto.statement.StatementExpenseDetailReport;
import com.financial.cloud.dto.statement.StatementGeneralLedgerReport;
import com.financial.cloud.dto.statement.StatementParamsDto;
import com.financial.cloud.enums.error.StatementErrorCode;
import com.financial.cloud.enums.statement.StatementPeriodTypeEnum;
import com.financial.cloud.exception.ServiceException;
import com.financial.cloud.service.statement.StatementExpenseDetailService;
import com.financial.cloud.service.statement.StatementGeneralLedgerService;
import com.financial.cloud.service.statement.StatementReportService;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.web.bind.annotation.*;

import java.io.IOException;
import java.util.List;


@RestController
@RequestMapping("/api/statement")
@Slf4j
@RequiredArgsConstructor
public class StatementReportController {
    private final StatementReportService statementReportService;
    private final StatementExpenseDetailService statementExpenseDetailService;
    private final StatementGeneralLedgerService statementGeneralLedgerService;

    @GetMapping(value = {"/cash-flow"})
    public Message<List<StatementCashFlow>> cashFlow(StatementParamsDto dto,
                                                                   @CurrentUser UserInfo userInfo) {
        dto.setBookId(userInfo.getBookId());
        validParams(dto);
        return statementReportService.cashFlowStatement(dto);
    }

    @GetMapping("/cash-flow/export")
    public void cashFlowExport(HttpServletResponse response,
                       StatementParamsDto dto,
                       @CurrentUser UserInfo userInfo) throws IOException {
        dto.setBookId(userInfo.getBookId());
        validParams(dto);
        statementReportService.cashFlowExport(dto, response);
    }

    @GetMapping(value = {"/subject-balance"})
    public Message<List<StatementSubjectBalance>> subjectBalance(StatementParamsDto dto,
                                                               @CurrentUser UserInfo userInfo) {
        dto.setBookId(userInfo.getBookId());
        validParams(dto);
        return statementReportService.subjectBalance(dto);
    }

    @GetMapping("/subject-balance/export")
    public void subjectBalanceExport(HttpServletResponse response,
                       StatementParamsDto dto,
                       @CurrentUser UserInfo userInfo) throws IOException {
        dto.setBookId(userInfo.getBookId());
        validParams(dto);
        statementReportService.subjectBalanceExport(dto, response);
    }

    @GetMapping(value = {"/voucher-summary"})
    public Message<List<StatementSubjectBalance>> voucherSummary(StatementParamsDto dto,
                                                            @CurrentUser UserInfo userInfo) {
        dto.setBookId(userInfo.getBookId());
        validParams(dto);
        return statementReportService.voucherSummary(dto);
    }

    @GetMapping("/voucher-summary/export")
    public void voucherSummaryExport(HttpServletResponse response,
                                     StatementParamsDto dto,
                                     @CurrentUser UserInfo userInfo) throws IOException {
        dto.setBookId(userInfo.getBookId());
        validParams(dto);
        statementReportService.voucherSummaryExport(dto, response);
    }

    @GetMapping("/expense-detail")
    public Message<StatementExpenseDetailReport> expenseDetail(
            StatementParamsDto dto, @CurrentUser UserInfo userInfo) {
        dto.setBookId(userInfo.getBookId());
        if (StringUtils.isBlank(dto.getPeriodType())) {
            dto.setPeriodType("between");
        }
        validParams(dto);
        return statementExpenseDetailService.query(dto);
    }

    @GetMapping("/expense-detail/export")
    public void expenseDetailExport(HttpServletResponse response,
            StatementParamsDto dto, @CurrentUser UserInfo userInfo) throws IOException {
        dto.setBookId(userInfo.getBookId());
        if (StringUtils.isBlank(dto.getPeriodType())) {
            dto.setPeriodType("between");
        }
        validParams(dto);
        statementExpenseDetailService.export(dto, response);
    }

    @GetMapping("/general-ledger")
    public Message<StatementGeneralLedgerReport> generalLedger(
            StatementParamsDto dto, @CurrentUser UserInfo userInfo) {
        dto.setBookId(userInfo.getBookId());
        if (StringUtils.isBlank(dto.getPeriodType())) {
            dto.setPeriodType("between");
        }
        validParams(dto);
        return statementGeneralLedgerService.query(dto);
    }

    @GetMapping("/general-ledger/export")
    public void generalLedgerExport(HttpServletResponse response,
            StatementParamsDto dto, @CurrentUser UserInfo userInfo) throws IOException {
        dto.setBookId(userInfo.getBookId());
        if (StringUtils.isBlank(dto.getPeriodType())) {
            dto.setPeriodType("between");
        }
        validParams(dto);
        statementGeneralLedgerService.export(dto, response);
    }

    private void validParams(StatementParamsDto dto) {
        if (StringUtils.isEmpty(dto.getPeriodType())) {
            throw new ServiceException(StatementErrorCode.PERIOD_TYPE_EMPTY);
        }
        if (StatementPeriodTypeEnum.BETWEEN_MONTH.getValue().equals(dto.getPeriodType())) {
            String[] dateRange = dto.getDateRange();
            if (dateRange == null || dateRange.length != 2
                    || StringUtils.isBlank(dateRange[0]) || StringUtils.isBlank(dateRange[1])) {
                throw new ServiceException(StatementErrorCode.DATE_RANGE_SIZE);
            }
        } else if (StringUtils.isEmpty(dto.getReportDate())) {
            throw new ServiceException(StatementErrorCode.REPORT_DATE_EMPTY);
        }
    }
}
