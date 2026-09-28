package com.financial.cloud.controller.statement;

import com.financial.cloud.authn.annotation.CurrentUser;
import com.financial.cloud.common.Message;
import com.financial.cloud.domain.idm.UserInfo;
import com.financial.cloud.dto.statement.TaxEstimateVo;
import com.financial.cloud.service.statement.TaxEstimateService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.math.BigDecimal;

/**
 * 税费测算：增值税 / 附加税 / 企业所得税测算与税负预警（申报前参考，非申报表）。
 */
@RestController
@RequestMapping("/api/tax-estimate")
@RequiredArgsConstructor
public class TaxEstimateController {

    private final TaxEstimateService taxEstimateService;

    @GetMapping
    public Message<TaxEstimateVo> estimate(@RequestParam String yearMonth,
                                           @RequestParam(defaultValue = "0.07") BigDecimal urbanRate,
                                           @RequestParam(defaultValue = "0.03") BigDecimal eduRate,
                                           @RequestParam(defaultValue = "0.02") BigDecimal localEduRate,
                                           @RequestParam(defaultValue = "0.25") BigDecimal incomeTaxRate,
                                           @RequestParam(defaultValue = "0.01") BigDecimal burdenThreshold,
                                           @CurrentUser UserInfo userInfo) {
        return Message.ok(taxEstimateService.estimate(userInfo.getBookId(), yearMonth,
                urbanRate, eduRate, localEduRate, incomeTaxRate, burdenThreshold));
    }
}
