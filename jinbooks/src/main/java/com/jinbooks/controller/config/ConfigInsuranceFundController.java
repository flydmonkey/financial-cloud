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
 

package com.jinbooks.controller.config;

import com.jinbooks.authn.annotation.CurrentUser;
import com.jinbooks.common.Message;
import com.jinbooks.domain.config.ConfigInsuranceFund;
import com.jinbooks.domain.idm.UserInfo;
import com.jinbooks.service.config.ConfigInsuranceFundService;
import lombok.extern.slf4j.Slf4j;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

/**
 * @description:
 * @author: orangeBabu
 * @time: 2025/2/12 15:11
 */

@RestController
@RequestMapping("/config/insurance_fund")
@Slf4j
public class ConfigInsuranceFundController {
    static final Logger logger = LoggerFactory.getLogger(ConfigInsuranceFundController.class);

    @Autowired
    private ConfigInsuranceFundService configInsuranceFundService;

    @GetMapping("/getCurrent")
    public Message<ConfigInsuranceFund> getCurrent(@CurrentUser UserInfo currentUser){
        String bookId = currentUser.getBookId();
        return configInsuranceFundService.getCurrent(bookId);
    }

    @PutMapping("/updateCurrent")
    public Message<String> updateCurrent(@RequestBody ConfigInsuranceFund configInsuranceFund,
                                         @CurrentUser UserInfo currentUser) {
        configInsuranceFund.setBookId(currentUser.getBookId());
        logger.debug("update {} ",configInsuranceFund);
        if(configInsuranceFundService.saveOrUpdate(configInsuranceFund)) {
            return new Message<>(Message.SUCCESS);
        } else {
            return new Message<>(Message.FAIL);
        }
    }
}
