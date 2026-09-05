package com.financial.cloud.controller.standard;

import com.financial.cloud.service.statement.StatementBalanceSheetService;
import com.financial.cloud.common.Message;
import com.financial.cloud.constants.auth.ProductRoles;
import com.financial.cloud.domain.standard.StandardStatementBalanceSheet;
import com.financial.cloud.dto.standard.StandardStatementBalanceSheetListVo;
import com.financial.cloud.service.standard.StandardStatementBalanceSheetService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

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
        ProductRoles.requireAdministrator();
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
        ProductRoles.requireAdministrator();
        return statementBalanceSheetService.delete(id);
    }

}
