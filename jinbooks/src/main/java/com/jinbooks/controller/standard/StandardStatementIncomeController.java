/*
 * Copyright [2025] [JinBooks of copyright http://www.jinbooks.com]
 * 
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * 
 *     http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 */
 

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
@RequestMapping("/standardstatementincome/")
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
