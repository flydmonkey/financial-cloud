package com.financial.cloud.controller.book;



import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.financial.cloud.authn.annotation.CurrentUser;
import com.financial.cloud.common.Message;
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
    	dto.setBookId(userInfo.getBookId());
    	return settlementService.checkout(dto);
    }
    
    @GetMapping(value = { "/verify" })
    public Message<List<SettlementVerifyVo>> verify(@CurrentUser UserInfo userInfo) {
    	return settlementService.verify(userInfo.getBookId());
    }
}
