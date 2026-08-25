package com.jinbooks.controller.config;


import lombok.RequiredArgsConstructor;
import com.jinbooks.authn.annotation.CurrentUser;
import com.jinbooks.common.Message;
import com.jinbooks.domain.config.ConfigInsuranceFund;
import com.jinbooks.domain.idm.UserInfo;
import com.jinbooks.service.config.ConfigInsuranceFundService;
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
