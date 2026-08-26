package com.financial.cloud.controller.statement;

import com.financial.cloud.authn.annotation.CurrentUser;
import com.financial.cloud.common.Message;
import com.financial.cloud.domain.idm.UserInfo;
import com.financial.cloud.domain.statement.StatementBalanceSheetItem;
import com.financial.cloud.dto.statement.StatementBalanceSheetItemListVo;
import com.financial.cloud.service.statement.StatementBalanceSheetConfigService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;


/**
 * 报表资产负债表配置接口
 */

@RestController
@RequestMapping("/api/statement/config/balance-sheet")
@Slf4j
@RequiredArgsConstructor
public class StatementBalanceSheetConfigController {
    private final StatementBalanceSheetConfigService configService;

    /**
     * 获取资产负债表配置
     */
    @GetMapping(value = {"/{itemCode}"})
    public Message<StatementBalanceSheetItem> get(@PathVariable("itemCode") String itemCode,
                                                  @CurrentUser UserInfo userInfo) {
        return configService.get(userInfo.getBookId(), itemCode);
    }

    /**
     * 获取资产负债表配置
     */
    @GetMapping(value = {"/fetch"})
    public Message<StatementBalanceSheetItemListVo> list(@CurrentUser UserInfo userInfo) {
        return configService.list(userInfo.getBookId());
    }

    /**
     * 资产负债表配置
     *
     * @param dto 查询参数
     * @return 结果
     */
    @PostMapping
    public Message<StatementBalanceSheetItem> save(@Validated @RequestBody StatementBalanceSheetItem dto,
                                                   @CurrentUser UserInfo userInfo) {
        dto.setBookId(userInfo.getBookId());
        return configService.save(dto);
    }

    /**
     * 资产负债表配置
     *
     * @param id 被删除项ID
     * @return 结果
     */
    @DeleteMapping(value = {"/{id}"})
    public Message<Boolean> delete(@PathVariable("id") String id) {
        return configService.delete(id);
    }

}
