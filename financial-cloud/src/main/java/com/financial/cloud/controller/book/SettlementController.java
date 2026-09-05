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

import java.util.List;

import org.springframework.web.bind.annotation.*;

@RequiredArgsConstructor
@Slf4j
@RestController
@RequestMapping("/api/settlement")
public class SettlementController {

    private final SettlementService settlementService;

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
    	return settlementService.checkout(dto);
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
        return settlementService.uncheckout(userInfo.getBookId(), period, userInfo.getId());
    }
    
    @GetMapping(value = { "/verify" })
    public Message<List<SettlementVerifyVo>> verify(@CurrentUser UserInfo userInfo) {
    	return settlementService.verify(userInfo.getBookId());
    }
}
