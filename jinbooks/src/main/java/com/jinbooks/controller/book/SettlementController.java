package com.jinbooks.controller.book;



import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.jinbooks.authn.annotation.CurrentUser;
import com.jinbooks.common.Message;
import com.jinbooks.domain.book.Settlement;
import com.jinbooks.dto.book.SettlementPageDto;
import com.jinbooks.dto.book.SettlementVerifyVo;
import com.jinbooks.domain.idm.UserInfo;
import com.jinbooks.service.book.SettlementService;

import java.util.List;

import org.springframework.web.bind.annotation.*;

/**
 * @description:
 * @author: orangeBabu
 * @time: 2024/12/31 11:18
 */

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
