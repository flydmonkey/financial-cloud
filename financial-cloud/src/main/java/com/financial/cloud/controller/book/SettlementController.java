package com.financial.cloud.controller.book;



import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.financial.cloud.authn.annotation.CurrentUser;
import com.financial.cloud.common.Message;
import com.financial.cloud.constants.auth.ProductRoles;
import com.financial.cloud.domain.book.Settlement;
import com.financial.cloud.dto.book.SettlementPageDto;
import com.financial.cloud.dto.book.SettlementVerifyVo;
import com.financial.cloud.domain.idm.UserInfo;
import com.financial.cloud.service.book.SettlementService;
import com.financial.cloud.service.history.HistorySystemLogsService;

import java.util.List;

import org.springframework.web.bind.annotation.*;

@RequiredArgsConstructor
@Slf4j
@RestController
@RequestMapping("/api/settlement")
public class SettlementController {

    private final SettlementService settlementService;
    private final HistorySystemLogsService historySystemLogsService;

    /** 结账/反结账审计（IP 由日志服务统一提取） */
    private void auditLog(String action, Message<?> result, UserInfo operator) {
        try {
            boolean success = result != null && result.getCode() == Message.SUCCESS;
            historySystemLogsService.log("月末结账", operator.getBookId(), "账期操作", null,
                    result == null ? "" : result.getMessage(),
                    action, success ? "success" : "fail", operator, null);
        } catch (Exception e) {
            log.warn("结账审计日志写入失败：{}", e.getMessage());
        }
    }

    @GetMapping(value = { "/fetch" })
    public Message<Page<Settlement>> fetch(SettlementPageDto dto,@CurrentUser UserInfo userInfo) {
    	dto.setBookId(userInfo.getBookId());
        log.debug("fetch {}",dto);

        return settlementService.pageList(dto);
    }
    
    @GetMapping(value = { "/checkout" })
    public Message<Settlement> checkout(Settlement dto,@CurrentUser UserInfo userInfo) {
        ProductRoles.requireClosePeriod();
    	dto.setBookId(userInfo.getBookId());
    	Message<Settlement> result = settlementService.checkout(dto);
    	auditLog("结账", result, userInfo);
    	return result;
    }

    /**
     * 反结账：仅最近已结月；body/query 可选 yearPeriod（须等于 currentTerm 上一月）。
     */
    @PostMapping(value = { "/uncheckout" })
    public Message<String> uncheckout(@RequestBody(required = false) Settlement dto,
                                      @RequestParam(value = "yearPeriod", required = false) String yearPeriod,
                                      @CurrentUser UserInfo userInfo) {
        ProductRoles.requireClosePeriod();
        String period = yearPeriod;
        if (dto != null && org.apache.commons.lang3.StringUtils.isNotBlank(dto.getYearPeriod())) {
            period = dto.getYearPeriod();
        }
        Message<String> result = settlementService.uncheckout(userInfo.getBookId(), period, userInfo.getId());
        auditLog("反结账", result, userInfo);
        return result;
    }
    
    @GetMapping(value = { "/verify" })
    public Message<List<SettlementVerifyVo>> verify(@CurrentUser UserInfo userInfo) {
    	return settlementService.verify(userInfo.getBookId());
    }
}
