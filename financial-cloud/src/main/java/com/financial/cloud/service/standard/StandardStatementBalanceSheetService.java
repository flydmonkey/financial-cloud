package com.financial.cloud.service.standard;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.incrementer.IdentifierGenerator;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.financial.cloud.common.Message;
import com.financial.cloud.domain.standard.StandardStatementBalanceSheet;
import com.financial.cloud.domain.standard.StandardStatementRules;
import com.financial.cloud.dto.standard.StandardStatementBalanceSheetListVo;
import com.financial.cloud.enums.statement.AssetOrLiabilityEnum;
import com.financial.cloud.enums.statement.StatementSymbolEnum;
import com.financial.cloud.enums.statement.StatementTypeEnum;
import com.financial.cloud.repository.standard.StandardStatementBalanceSheetMapper;
import com.financial.cloud.repository.standard.StandardStatementRulesMapper;
import com.baomidou.mybatisplus.extension.toolkit.Db;
import com.financial.cloud.service.standard.StandardStatementBalanceSheetService;
import lombok.RequiredArgsConstructor;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Comparator;
import java.util.List;

@RequiredArgsConstructor
@Service
public class StandardStatementBalanceSheetService{

    private final StandardStatementBalanceSheetMapper balanceSheetMapper;
    private final StandardStatementRulesMapper rulesMapper;
    private final IdentifierGenerator identifierGenerator;

    /**
     * 获取资产负载配置明细
     */
    public Message<StandardStatementBalanceSheet> get(StandardStatementBalanceSheet dto) {
        LambdaQueryWrapper<StandardStatementBalanceSheet> sqw = Wrappers.lambdaQuery();
        sqw.eq(StandardStatementBalanceSheet::getStandardId, dto.getStandardId());
        sqw.eq(StandardStatementBalanceSheet::getItemCode, dto.getItemCode());
        StandardStatementBalanceSheet balanceSheet = balanceSheetMapper.selectOne(sqw);

        LambdaQueryWrapper<StandardStatementRules> lqw = Wrappers.lambdaQuery();
        lqw.eq(StandardStatementRules::getStandardId, dto.getStandardId());
        lqw.eq(StandardStatementRules::getItemCode, dto.getItemCode());
        lqw.eq(StandardStatementRules::getType, StatementTypeEnum.balance_sheet.name());
        List<StandardStatementRules> rules = rulesMapper.selectList(lqw);

        balanceSheet.setRules(rules);
        return Message.ok(balanceSheet);
    }

    /**
     * 获取资产负载表配置
     */
    public Message<StandardStatementBalanceSheetListVo> list(StandardStatementBalanceSheet dto) {
        LambdaQueryWrapper<StandardStatementBalanceSheet> lqw = Wrappers.lambdaQuery();
        lqw.eq(StandardStatementBalanceSheet::getStandardId, dto.getStandardId());
        lqw.orderByAsc(StandardStatementBalanceSheet::getItemCode, StandardStatementBalanceSheet::getSortIndex);
        List<StandardStatementBalanceSheet> balanceSheets = balanceSheetMapper.selectList(lqw);

        StandardStatementBalanceSheetListVo data = StandardStatementBalanceSheetListVo.builder()
                .assets(balanceSheets.stream()
                        .filter(item -> AssetOrLiabilityEnum.asset.name().equals(item.getAssetOrLiability()))
                        .sorted(Comparator.comparing(StandardStatementBalanceSheet::getItemCode))
                        .toList())
                .liability(balanceSheets.stream()
                        .filter(item -> AssetOrLiabilityEnum.liability.name().equals(item.getAssetOrLiability()))
                        .sorted(Comparator.comparing(StandardStatementBalanceSheet::getItemCode))
                        .toList())
                .build();

        return Message.ok(data);
    }

    /**
     * 资产负载表配置
     *
     * @param dto 配置参数
     * @return 结果
     */
    @Transactional
    public Message<StandardStatementBalanceSheet> save(StandardStatementBalanceSheet dto) {
        // 顶级节点不参与行次
        if (dto.getLevel() == 1 && dto.getItemCode().endsWith("00")) {
            dto.setSortIndex(null);
        }
        updateSortIndex(dto, StatementSymbolEnum.PLUS);
        if (StringUtils.isBlank(dto.getId())) {
            String id = identifierGenerator.nextId(dto).toString();
            dto.setId(id);
            balanceSheetMapper.insert(dto);
        } else {
            balanceSheetMapper.updateById(dto);
        }
        // 规则更新
        if (dto.getRules() != null && !dto.getRules().isEmpty()) {
            for (StandardStatementRules rule : dto.getRules()) {
                rule.setStandardId(dto.getStandardId());
                rule.setItemCode(dto.getItemCode());
                rule.setType(StatementTypeEnum.balance_sheet.name());
            }
            saveRules(dto.getRules(), dto.getStandardId(), dto.getItemCode());
        }

        return Message.ok(dto);
    }

    /**
     * 报表规则配置
     *
     * @param dto 配置项
     * @return 结果
     */
    public Message<List<StandardStatementRules>> saveRules(List<StandardStatementRules> dto, String standardId, String itemCode) {
        LambdaQueryWrapper<StandardStatementRules> queryWrapper = Wrappers.lambdaQuery();
        queryWrapper.eq(StandardStatementRules::getStandardId, standardId);
        queryWrapper.eq(StandardStatementRules::getItemCode, itemCode);
        rulesMapper.delete(queryWrapper);
        if (!dto.isEmpty()) {
            Db.saveOrUpdateBatch(dto);
        }
        return Message.ok(dto);
    }
    public Message<Boolean> delete(String id) {
        StandardStatementBalanceSheet balanceSheet = balanceSheetMapper.selectById(id);
        updateSortIndex(balanceSheet, StatementSymbolEnum.MINUS);
        return Message.ok(balanceSheetMapper.deleteById(id) > 0);
    }


    /**
     * 获取配置
     */
    public Message<List<StandardStatementRules>> getRules(String standardId, String itemCode) {
        LambdaQueryWrapper<StandardStatementRules> lqw = Wrappers.lambdaQuery();
        lqw.eq(StandardStatementRules::getStandardId, standardId);
        lqw.eq(StandardStatementRules::getItemCode, itemCode);
        lqw.eq(StandardStatementRules::getType, StatementTypeEnum.balance_sheet.name());
        lqw.orderByAsc(StandardStatementRules::getSubjectCode);
        List<StandardStatementRules> rules = rulesMapper.selectList(lqw);
        return Message.ok(rules);
    }

    /**
     * 更新行号，保证插入的行号，不影响到原有布局。
     *
     * @param dto    更新参数
     * @param symbol 操作数
     */
    private void updateSortIndex(StandardStatementBalanceSheet dto, StatementSymbolEnum symbol) {
        // 一级节点，没有行号
        if (dto.getLevel() == 1 && dto.getItemCode().endsWith("00")) {
            return;
        }

        StandardStatementBalanceSheet balanceSheet = null;
        // 未发生变动的序号，不重复更新，减少压力
        if (StringUtils.isNotBlank(dto.getId())) {
            balanceSheet = balanceSheetMapper.selectById(dto.getId());
            if (balanceSheet != null
                    && balanceSheet.getSortIndex().equals(dto.getSortIndex())
                    && symbol.equals(StatementSymbolEnum.PLUS)) {
                return;
            }
        }

        // 更新行号
        if (symbol.equals(StatementSymbolEnum.PLUS)) {
            balanceSheetMapper.incrementSortIndex(dto.getStandardId(), dto.getSortIndex(), dto.getId());
        } else {
            balanceSheetMapper.decrementSortIndex(dto.getStandardId(), dto.getSortIndex(), dto.getId());
        }

        // 更新code
        if (balanceSheet != null
                && balanceSheet.getItemCode().equals(dto.getItemCode())
                && symbol.equals(StatementSymbolEnum.PLUS)) {
            return;
        }
        if (symbol.equals(StatementSymbolEnum.PLUS)) {
            balanceSheetMapper.incrementItemCode(dto.getStandardId(), dto.getItemCode(), dto.getId());
            rulesMapper.incrementItemCode(dto.getStandardId(), dto.getItemCode(), StatementTypeEnum.balance_sheet.name());
        } else {
            balanceSheetMapper.decrementItemCode(dto.getStandardId(), dto.getItemCode(), dto.getId());
            rulesMapper.decrementItemCode(dto.getStandardId(), dto.getItemCode(), StatementTypeEnum.balance_sheet.name());
        }

    }
}
