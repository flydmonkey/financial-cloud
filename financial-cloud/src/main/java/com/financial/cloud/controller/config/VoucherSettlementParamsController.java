package com.financial.cloud.controller.config;

import com.financial.cloud.authn.annotation.CurrentUser;
import com.financial.cloud.common.Message;
import com.financial.cloud.domain.idm.UserInfo;
import com.financial.cloud.dto.config.VoucherSettlementParamsSaveDto;
import com.financial.cloud.dto.config.VoucherSettlementParamsVo;
import com.financial.cloud.service.config.VoucherSettlementParamsService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RequiredArgsConstructor
@RestController
@RequestMapping("/api/config/voucher-settlement")
public class VoucherSettlementParamsController {

    private final VoucherSettlementParamsService voucherSettlementParamsService;

    @GetMapping
    public Message<VoucherSettlementParamsVo> get(@CurrentUser UserInfo userInfo) {
        return Message.ok(voucherSettlementParamsService.get(userInfo));
    }

    @PutMapping
    public Message<String> save(@RequestBody VoucherSettlementParamsSaveDto dto,
                                @CurrentUser UserInfo userInfo) {
        return voucherSettlementParamsService.save(dto, userInfo);
    }
}
