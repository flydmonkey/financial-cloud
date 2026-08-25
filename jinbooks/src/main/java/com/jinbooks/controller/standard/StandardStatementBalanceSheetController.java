package com.jinbooks.controller.standard;

import com.jinbooks.service.statement.StatementBalanceSheetService;
import com.jinbooks.common.Message;
import com.jinbooks.domain.standard.StandardStatementBalanceSheet;
import com.jinbooks.dto.standard.StandardStatementBalanceSheetListVo;
import com.jinbooks.service.standard.StandardStatementBalanceSheetService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

/**
 * 报表资产负债表配置接口
 */

@RestController
@RequestMapping("/api/standard/balance-sheet")
@Slf4j
@RequiredArgsConstructor
public class StandardStatementBalanceSheetController {
    private final StandardStatementBalanceSheetService statementBalanceSheetService;

    /**
     * 获取资产负债表配信
     */
    @GetMapping(value = {"/get"})
    public Message<StandardStatementBalanceSheet> get(StandardStatementBalanceSheet dto) {
        return statementBalanceSheetService.get(dto);
    }

    /**
     * 获取资产负债表配置
     */
    @GetMapping(value = {"/fetch"})
    public Message<StandardStatementBalanceSheetListVo> listBalanceSheet(StandardStatementBalanceSheet dto) {
        return statementBalanceSheetService.list(dto);
    }

    /**
     * 资产负债表配置
     *
     * @param dto 查询参数
     * @return 结果
     */
    @PostMapping(value = {"/save"})
    public Message<StandardStatementBalanceSheet> save(@Validated @RequestBody StandardStatementBalanceSheet dto) {
        return statementBalanceSheetService.save(dto);
    }

    /**
     * 资产负债表配置
     *
     * @param id 被删除项ID
     * @return 结果
     */
    @DeleteMapping(value = {"/delete/{id}"})
    public Message<Boolean> delete(@PathVariable("id") String id) {
        return statementBalanceSheetService.delete(id);
    }

}
