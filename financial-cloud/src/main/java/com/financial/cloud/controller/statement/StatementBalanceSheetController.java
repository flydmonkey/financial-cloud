package com.financial.cloud.controller.statement;

import com.financial.cloud.authn.annotation.CurrentUser;
import com.financial.cloud.common.Message;
import com.financial.cloud.domain.idm.UserInfo;
import com.financial.cloud.domain.statement.StatementBalanceSheet;
import com.financial.cloud.dto.statement.StatementParamsDto;
import com.financial.cloud.enums.error.StatementErrorCode;
import com.financial.cloud.exception.ServiceException;
import com.financial.cloud.service.statement.StatementBalanceSheetService;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.web.bind.annotation.*;

import java.io.IOException;

/**
 * 资产负债表接口
 */

@RestController
@RequestMapping("/api/statement")
@Slf4j
@RequiredArgsConstructor
public class StatementBalanceSheetController {
    //资产负债表
    private final StatementBalanceSheetService statementBalanceSheetService;

    /**
     * 报表-资产负债表
     *
     * @param dto 查询参数
     * @return 结果
     */
    @GetMapping(value = {"/balance-sheet"})
    public Message<StatementBalanceSheet> balanceSheet(StatementParamsDto dto,
                                                  @CurrentUser UserInfo userInfo) {
        dto.setBookId(userInfo.getBookId());
        validParams(dto);
        return statementBalanceSheetService.queryBalanceSheet(dto, false);
    }

    /**
     * 导出功能
     */
    @GetMapping("/balance-sheet/export")
    public void export(HttpServletResponse response,
                       StatementParamsDto dto,
                       @CurrentUser UserInfo userInfo) throws IOException {
        dto.setBookId(userInfo.getBookId());
        validParams(dto);
        statementBalanceSheetService.export(dto, response);
    }

    private void validParams(StatementParamsDto dto) {
        if (StringUtils.isEmpty(dto.getPeriodType())) {
            throw new ServiceException(StatementErrorCode.PERIOD_TYPE_EMPTY);
        } else if (StringUtils.isEmpty(dto.getReportDate())) {
            throw new ServiceException(StatementErrorCode.REPORT_DATE_EMPTY);
        }
    }
}
