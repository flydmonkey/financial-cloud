package com.financial.cloud.controller.config;


import lombok.RequiredArgsConstructor;
import com.financial.cloud.authn.annotation.CurrentUser;
import com.financial.cloud.common.Message;
import com.financial.cloud.domain.config.ConfigInsuranceFund;
import com.financial.cloud.domain.idm.UserInfo;
import com.financial.cloud.service.config.ConfigInsuranceFundService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

/**
 * @description:
 * @author: orangeBabu
 * @time: 2025/2/12 15:11
 */

@RequiredArgsConstructor
@RestController
@RequestMapping("/api/config/insurance_fund")
@Slf4j
public class ConfigInsuranceFundController {

    private final ConfigInsuranceFundService configInsuranceFundService;

    @GetMapping("/getCurrent")
    public Message<ConfigInsuranceFund> getCurrent(@CurrentUser UserInfo currentUser){
        String bookId = currentUser.getBookId();
        return configInsuranceFundService.getCurrent(bookId);
    }

    @PutMapping("/updateCurrent")
    public Message<String> updateCurrent(@RequestBody ConfigInsuranceFund configInsuranceFund,
                                         @CurrentUser UserInfo currentUser) {
        configInsuranceFund.setBookId(currentUser.getBookId());
        log.debug("update {} ",configInsuranceFund);
        if(configInsuranceFundService.saveOrUpdate(configInsuranceFund)) {
            return new Message<>(Message.SUCCESS);
        } else {
            return new Message<>(Message.FAIL);
        }
    }
}
