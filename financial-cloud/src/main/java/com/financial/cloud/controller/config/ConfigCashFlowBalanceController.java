package com.financial.cloud.controller.config;

import java.util.List;

import com.financial.cloud.dto.report.CashFlowSubjectBalanceVo;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.financial.cloud.authn.annotation.CurrentUser;
import com.financial.cloud.common.Message;
import com.financial.cloud.domain.config.ConfigCashFlowBalance;
import com.financial.cloud.dto.config.ConfigCashFlowChangeDto;
import com.financial.cloud.dto.config.ConfigCashFlowPageDto;
import com.financial.cloud.domain.idm.UserInfo;
import com.financial.cloud.service.config.ConfigCashFlowBalanceService;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@RestController
@RequestMapping("/api/config/cash-flow-balance")
@Slf4j
@RequiredArgsConstructor
public class ConfigCashFlowBalanceController {

    private final ConfigCashFlowBalanceService configCashFlowService;

    @GetMapping(value = {"/fetch"})
    public Message<CashFlowSubjectBalanceVo> fetch(ConfigCashFlowPageDto dto,
                                                   @CurrentUser UserInfo currentUser) {
        log.debug("fetch {}", dto);
        dto.setBookId(currentUser.getBookId());
        return configCashFlowService.pageList(dto);
    }

    @PostMapping("/save")
    public Message<String> save(@RequestBody ConfigCashFlowChangeDto dto,
                                                      @CurrentUser UserInfo currentUser) {
        log.debug("save {}", dto);
        return configCashFlowService.save(dto);
    }


    @GetMapping(value = {"/get-select-item"})
    public Message<List<ConfigCashFlowBalance>> getSelectItem(@RequestParam(name="cashFlowItemType") Integer cashFlowItemType) {
        return configCashFlowService.getSelectItem(cashFlowItemType);
    }
}
