package com.financial.cloud.service.statement;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.financial.cloud.constants.system.ConstsSysConfig;
import com.financial.cloud.common.Message;
import com.financial.cloud.domain.statement.StatementBalanceSheetItem;
import com.financial.cloud.domain.statement.StatementRules;
import com.financial.cloud.domain.statement.StatementSubjectBalance;
import com.financial.cloud.dto.statement.StatementBalanceSheetItemListVo;

import com.financial.cloud.enums.statement.StatementSymbolEnum;
import com.financial.cloud.enums.statement.StatementTypeEnum;
import com.financial.cloud.repository.statement.StatementBalanceSheetItemMapper;
import com.baomidou.mybatisplus.extension.toolkit.Db;
import com.financial.cloud.repository.statement.StatementRulesMapper;
import com.financial.cloud.repository.statement.StatementSubjectBalanceMapper;
import com.financial.cloud.service.config.ConfigSysService;
import com.financial.cloud.service.statement.StatementBalanceSheetConfigService;
import com.financial.cloud.service.statement.StatementBalanceSheetService;

import com.financial.cloud.util.SubjectCodeCompat;
import lombok.RequiredArgsConstructor;

import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;


@RequiredArgsConstructor
@Service
public class StatementBalanceSheetConfigService{
    private final StatementBalanceSheetItemMapper statementBalanceSheetItemMapper;
    private final StatementRulesMapper rulesMapper;
    private final StatementSubjectBalanceMapper subjectBalanceMapper;
    private final ConfigSysService configSysService;
    private final StatementBalanceSheetService statementBalanceSheetService;

    /**
     * 获取资产负载配置明细
     *
     * @return 结果
     */
    public Message<StatementBalanceSheetItem> get(String bookId, String itemCode) {
        LambdaQueryWrapper<StatementBalanceSheetItem> itemLqw = Wrappers.lambdaQuery();
        itemLqw.eq(StatementBalanceSheetItem::getBookId, bookId);
        itemLqw.eq(StatementBalanceSheetItem::getBalanceSheetId, ConstsSysConfig.SYS_CONFIG_TEMPLATE_ID);
        itemLqw.eq(StatementBalanceSheetItem::getItemCode, itemCode);
        StatementBalanceSheetItem balanceSheetItem = statementBalanceSheetItemMapper.selectOne(itemLqw);

        LambdaQueryWrapper<StatementRules> lqw = Wrappers.lambdaQuery();
        lqw.eq(StatementRules::getBookId, bookId);
        lqw.eq(StatementRules::getItemCode, balanceSheetItem.getItemCode());
        lqw.eq(StatementRules::getType, StatementTypeEnum.balance_sheet.name());
        List<StatementRules> rules = rulesMapper.selectList(lqw);

        if (!rules.isEmpty()) {
            Map<String, StatementRules> rulesMap = new HashMap<>();
            for (StatementRules item : rules) {
                for (String candidate : SubjectCodeCompat.lookupCandidates(item.getSubjectCode())) {
                    rulesMap.putIfAbsent(candidate, item);
                }
            }
            Set<String> codes = SubjectCodeCompat.expandLookupCodes(
                    rules.stream().map(StatementRules::getSubjectCode).toList());
            LambdaQueryWrapper<StatementSubjectBalance> lqwSubject = Wrappers.lambdaQuery();
            lqwSubject.eq(StatementSubjectBalance::getBookId, bookId);
            lqwSubject.in(StatementSubjectBalance::getSubjectCode, codes);
            List<StatementSubjectBalance> subjectBalances = subjectBalanceMapper.selectList(lqwSubject);
            for (StatementSubjectBalance subjectBalance : subjectBalances) {
                StatementRules statementRules = SubjectCodeCompat.resolveFromMap(rulesMap, subjectBalance.getSubjectCode());
                statementBalanceSheetService.updateRuleBalance(subjectBalance, statementRules);
            }
        }
        balanceSheetItem.setRules(rules);
        return Message.ok(balanceSheetItem);
    }

    /**
     * 获取资产负载表配置
     *
     * @param bookId 账簿ID
     * @return 结果
     */
    public Message<StatementBalanceSheetItemListVo> list(String bookId) {
        //StatementBalanceSheet balanceSheet = getBalanceSheetCurrentPeriod(bookId);
        LambdaQueryWrapper<StatementBalanceSheetItem> lqw = Wrappers.lambdaQuery();
        lqw.eq(StatementBalanceSheetItem::getBookId, bookId);
        lqw.eq(StatementBalanceSheetItem::getBalanceSheetId, ConstsSysConfig.SYS_CONFIG_TEMPLATE_ID);
        List<StatementBalanceSheetItem> balanceSheets = statementBalanceSheetItemMapper.selectList(lqw);
        statementBalanceSheetService.refreshItemsBalance(balanceSheets, bookId, configSysService.getCurrentTerm(bookId), 3);
        StatementBalanceSheetItemListVo itemListVo = statementBalanceSheetService.insertSubtotals(balanceSheets);
        itemListVo.getAssets().sort(Comparator.comparing(StatementBalanceSheetItem::getItemCode));
        itemListVo.getLiability().sort(Comparator.comparing(StatementBalanceSheetItem::getItemCode));
        return Message.ok(itemListVo);
    }

    /**
     * 资产负载表配置
     *
     * @param dto 配置参数
     * @return 结果
     */
    @Transactional
    public Message<StatementBalanceSheetItem> save(StatementBalanceSheetItem dto) {
        StatementBalanceSheetItem balanceSheetItem = statementBalanceSheetItemMapper.selectById(dto.getId());
        if (balanceSheetItem == null) {
            return Message.failed("操作不被允许（配置项不存在）");
        }
//        updateSortIndex(dto, StatementSymbolEnum.PLUS);
        statementBalanceSheetItemMapper.updateById(dto);
        // 规则更新
        if (dto.getRules() != null) {
            for (StatementRules rule : dto.getRules()) {
                rule.setItemCode(dto.getItemCode());
                rule.setBookId(dto.getBookId());
                rule.setType(StatementTypeEnum.balance_sheet.name());
            }
            saveRules(dto.getRules(), dto.getBookId(), dto.getItemCode());
        }

        return Message.ok(dto);
    }

    /**
     * 报表规则配置
     *
     * @param dto 配置项
     * @return 结果
     */
    public Message<List<StatementRules>> saveRules(List<StatementRules> dto, String bookId, String code) {
        LambdaQueryWrapper<StatementRules> queryWrapper = Wrappers.lambdaQuery();
        queryWrapper.eq(StatementRules::getItemCode, code);
        queryWrapper.eq(StatementRules::getBookId, bookId);
        queryWrapper.eq(StatementRules::getType, StatementTypeEnum.balance_sheet.name());
        rulesMapper.delete(queryWrapper);
        if (!dto.isEmpty()) {
            Db.saveOrUpdateBatch(dto);
        }
        return Message.ok(dto);
    }
    public Message<Boolean> delete(String id) {
        StatementBalanceSheetItem balanceSheet = statementBalanceSheetItemMapper.selectById(id);
        updateSortIndex(balanceSheet, StatementSymbolEnum.MINUS);
        return Message.ok(statementBalanceSheetItemMapper.deleteById(id) > 0);
    }

    /**
     * 获取配置
     *
     * @return 结果
     */
    public Message<List<StatementRules>> getRules(String itemCode) {
        LambdaQueryWrapper<StatementRules> lqw = Wrappers.lambdaQuery();
        lqw.eq(StatementRules::getItemCode, itemCode);
        lqw.eq(StatementRules::getType, StatementTypeEnum.balance_sheet.name());
        lqw.orderByAsc(StatementRules::getSubjectCode);
        List<StatementRules> rules = rulesMapper.selectList(lqw);
        return Message.ok(rules);
    }


 

    /**
     * 更新行号，保证插入的行号，不影响到原有布局。
     *
     * @param dto    更新参数
     * @param symbol 操作数
     */
    private void updateSortIndex(StatementBalanceSheetItem dto, StatementSymbolEnum symbol) {
        // 未发生变动的序号，不重复更新，减少压力
        if (!StringUtils.isBlank(dto.getId())) {
            StatementBalanceSheetItem balanceSheet = statementBalanceSheetItemMapper.selectById(dto.getId());
            if (balanceSheet != null && balanceSheet.getSortIndex().equals(dto.getSortIndex())) {
                return;
            }
        }

        LambdaQueryWrapper<StatementBalanceSheetItem> queryWrapper = Wrappers.lambdaQuery();
        queryWrapper.eq(StatementBalanceSheetItem::getBookId, dto.getBookId())
                .ge(StatementBalanceSheetItem::getSortIndex, dto.getSortIndex())
                .eq(StatementBalanceSheetItem::getAssetOrLiability, dto.getAssetOrLiability())
                .ne(StringUtils.isNotBlank(dto.getId()), StatementBalanceSheetItem::getId, dto.getId())
                .orderByAsc(StatementBalanceSheetItem::getSortIndex);
        List<StatementBalanceSheetItem> balanceSheets = statementBalanceSheetItemMapper.selectList(queryWrapper);
        int sortIndex = dto.getSortIndex();
        for (StatementBalanceSheetItem balanceSheet : balanceSheets) {
            if (symbol.equals(StatementSymbolEnum.PLUS)) {
                balanceSheet.setSortIndex(++sortIndex);
            } else {
                balanceSheet.setSortIndex(--sortIndex);
            }
        }
        if (!balanceSheets.isEmpty()) {
            Db.updateBatchById(balanceSheets);
        }
    }

}
