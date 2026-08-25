package com.jinbooks.controller.statement;

import com.jinbooks.authn.annotation.CurrentUser;
import com.jinbooks.common.Message;
import com.jinbooks.domain.idm.UserInfo;
import com.jinbooks.domain.statement.StatementIncomeItem;
import com.jinbooks.service.statement.StatementIncomeConfigService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 报表利润表配置接口
 */

@RestController
@RequestMapping("/api/statement/config/income")
@Slf4j
@RequiredArgsConstructor
public class StatementIncomeConfigController {

    private final StatementIncomeConfigService statementIncomeConfigService;

    /**
     * 获取利润表配信
     *
     * @param itemCode 主键
     */
    @GetMapping(value = {"/{itemCode}"})
    public Message<StatementIncomeItem> get(@PathVariable("itemCode") String itemCode,@CurrentUser UserInfo userInfo) {
        return statementIncomeConfigService.get(userInfo.getBookId(), itemCode);
    }

    /**
     * 获取利润表配置
     */
    @GetMapping(value = {"/fetch"})
    public Message<List<StatementIncomeItem>> fetch(@CurrentUser UserInfo userInfo) {
        return statementIncomeConfigService.list(userInfo.getBookId());
    }

    /**
     * 利润表配置
     *
     * @param dto 查询参数
     * @return 结果
     */
    @PostMapping
    public Message<StatementIncomeItem> save(@Validated @RequestBody StatementIncomeItem dto,
                                                           @CurrentUser UserInfo userInfo) {
        dto.setBookId(userInfo.getBookId());
        return statementIncomeConfigService.save(dto);
    }

    /**
     * 利润表配置
     *
     * @param id 被删除项ID
     * @return 结果
     */
    @DeleteMapping(value = {"/{id}"})
    public Message<Boolean> delete(@PathVariable("id") String id) {
        return statementIncomeConfigService.delete(id);
    }
}
