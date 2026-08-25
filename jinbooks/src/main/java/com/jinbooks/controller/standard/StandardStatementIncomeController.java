package com.jinbooks.controller.standard;

import com.jinbooks.authn.annotation.CurrentUser;
import com.jinbooks.common.Message;
import com.jinbooks.domain.idm.UserInfo;
import com.jinbooks.domain.standard.StandardStatementIncome;
import com.jinbooks.service.standard.StandardStatementIncomeService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * ååæ¥è¡¨å©æ¶¦è¡¨éç½®æ¥å?
 */

@RestController
@RequestMapping("/api/standardstatementincome/")
@Slf4j
@RequiredArgsConstructor
public class StandardStatementIncomeController {
	
    private final StandardStatementIncomeService standardStatementIncomeService;

    /**
     * è·åå©æ¶¦è¡¨éä¿?
     *
     * @param id ä¸»é®
     */
    @GetMapping(value = {"/get"})
    public Message<StandardStatementIncome> get(StandardStatementIncome dto) {
        return standardStatementIncomeService.get(dto);
    }

    /**
     * è·åå©æ¶¦è¡¨éç½?
     *
     * @param
     */
    @GetMapping(value = {"/fetch"})
    public Message<List<StandardStatementIncome>> fetch(StandardStatementIncome dto,@CurrentUser UserInfo userInfo) {
        return standardStatementIncomeService.list(dto);
    }

    /**
     * å©æ¶¦è¡¨éç½?
     *
     * @param dto æ¥è¯¢åæ°
     * @return ç»æ
     */
    @PostMapping(value = {"/save"})
    public Message<StandardStatementIncome> save(@Validated @RequestBody StandardStatementIncome dto,
                                                           @CurrentUser UserInfo userInfo) {
        return standardStatementIncomeService.save(dto);
    }

    /**
     * å©æ¶¦è¡¨éç½?
     *
     * @param id è¢«å é¤é¡¹ID
     * @return ç»æ
     */
    @DeleteMapping(value = {"/delete/{id}"})
    public Message<Boolean> delete(@PathVariable("id") String id) {
        return standardStatementIncomeService.delete(id);
    }

}
