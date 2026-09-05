package com.financial.cloud.controller.standard;

import com.financial.cloud.authn.annotation.CurrentUser;
import com.financial.cloud.common.Message;
import com.financial.cloud.constants.auth.ProductRoles;
import com.financial.cloud.domain.idm.UserInfo;
import com.financial.cloud.dto.standard.StandardSubjectCashFlowDto;
import com.financial.cloud.dto.standard.StandardSubjectCashFlowVo;
import com.financial.cloud.service.standard.StandardSubjectCashFlowService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/config/subject-cash-flow")
@Slf4j
@RequiredArgsConstructor
public class StandardSubjectCashFlowController {
    private final StandardSubjectCashFlowService standardSubjectCashFlowService;

    @PostMapping(value = {"/save"})
    public Message<String> save(@Validated @RequestBody StandardSubjectCashFlowDto dto,
                                                 @CurrentUser UserInfo userInfo) {
        ProductRoles.requireAdministrator();
        dto.setBookId(userInfo.getBookId());
        return standardSubjectCashFlowService.save(dto);
    }

    @GetMapping("/fetch-relationships")
    public Message<StandardSubjectCashFlowVo> fetchRelationships(StandardSubjectCashFlowDto dto,
                                                                 @CurrentUser UserInfo userInfo) {
        dto.setBookId(userInfo.getBookId());
        return standardSubjectCashFlowService.fetchRelationships(dto);
    }
}
