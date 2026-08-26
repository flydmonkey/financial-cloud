package com.financial.cloud.controller.statement;

import com.financial.cloud.authn.annotation.CurrentUser;
import com.financial.cloud.common.Message;
import com.financial.cloud.domain.idm.UserInfo;
import com.financial.cloud.domain.statement.StatementRules;
import com.financial.cloud.service.statement.StatementBalanceSheetConfigService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/statement/config/rules")
@Slf4j
@RequiredArgsConstructor
public class StatementRuleConfigController {
    private final StatementBalanceSheetConfigService configService;

    /**
     * 获取报表项统计规则配置
     */
    @GetMapping
    public Message<List<StatementRules>> getRules(@RequestParam("itemCode") String itemCode) {
        return configService.getRules(itemCode);
    }

    /**
     * 报表项统计规则配置
     *
     * @param dto 配置项
     * @return 结果
     */
    @PostMapping("/{itemCode}")
    public Message<List<StatementRules>> saveRules(@Validated @RequestBody List<StatementRules> dto,
                                                   @PathVariable("itemCode") String itemCode,
                                                   @CurrentUser UserInfo userInfo) {
        return configService.saveRules(dto, userInfo.getBookId(), itemCode);
    }
}
