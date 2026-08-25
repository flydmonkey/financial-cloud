package com.jinbooks.controller.standard;

import com.jinbooks.authn.annotation.CurrentUser;
import com.jinbooks.common.Message;
import com.jinbooks.domain.idm.UserInfo;
import com.jinbooks.dto.standard.StandardSubjectCashFlowDto;
import com.jinbooks.dto.standard.StandardSubjectCashFlowVo;
import com.jinbooks.service.standard.StandardSubjectCashFlowService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

/**
 * @description:
 * @author: orangeBabu
 * @time: 2025/4/21 10:43
 */

@RestController
@RequestMapping("/api/config/subject-cash-flow")
@Slf4j
@RequiredArgsConstructor
public class StandardSubjectCashFlowController {
    private final StandardSubjectCashFlowService standardSubjectCashFlowService;

    @PostMapping(value = {"/save"})
    public Message<String> save(@Validated @RequestBody StandardSubjectCashFlowDto dto,
                                                 @CurrentUser UserInfo userInfo) {
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
