package com.financial.cloud.controller.statement;

import com.financial.cloud.authn.annotation.CurrentUser;
import com.financial.cloud.common.Message;
import com.financial.cloud.domain.idm.UserInfo;
import com.financial.cloud.domain.statement.StatementCashFlow;
import com.financial.cloud.dto.voucher.VoucherItemCashFlowDto;
import com.financial.cloud.dto.voucher.VoucherItemPageDto;
import com.financial.cloud.dto.voucher.VoucherItemVo;
import com.financial.cloud.enums.StatementErrorCode;
import com.financial.cloud.exception.BusinessException;
import com.financial.cloud.service.config.ConfigSysService;
import com.financial.cloud.service.statement.StatementCashFlowService;
import com.financial.cloud.service.voucher.VoucherItemCashFlowService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * @description:
 * @author: orangeBabu
 * @time: 2025/3/25 15:00
 */


@RestController
@RequestMapping("/api/statement/cash-flow")
@Slf4j
@RequiredArgsConstructor
public class StatementCashFlowController {
    private final VoucherItemCashFlowService voucherItemCashFlowService;

    private final StatementCashFlowService statementCashFlowService;

    private final ConfigSysService configSysService;


    @PostMapping("/specify")
    public Message<String> specifyCashFlowItems(@Validated @RequestBody VoucherItemCashFlowDto dto, @CurrentUser UserInfo user) {
        dto.setBookId(user.getBookId());
        String voucherDate = dto.getVoucherDate();
        String currentTerm = configSysService.getCurrentTerm(dto.getBookId());
        if (!currentTerm.equals(voucherDate)) {
            throw new BusinessException(StatementErrorCode.CASH_FLOW_MODIFY_FORBIDDEN);
        }
        return voucherItemCashFlowService.specifyCashFlowItems(dto);
    }

    @GetMapping("/get")
    public Message<List<VoucherItemVo>> getCashFlowItems(VoucherItemPageDto paramsDto, @CurrentUser UserInfo user) {
        paramsDto.setBookId(user.getBookId());
        return voucherItemCashFlowService.getCashFlowItems(paramsDto);
    }

    @PostMapping("/save")
    public Message<String> changeSpecifyItem(@RequestBody StatementCashFlow statementCashFlow, @CurrentUser UserInfo user) {
        statementCashFlow.setBookId(user.getBookId());
        return statementCashFlowService.changeSpecifyItem(statementCashFlow);
    }
}
