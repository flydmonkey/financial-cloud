package com.financial.cloud.service.statement;


import lombok.extern.slf4j.Slf4j;
import com.financial.cloud.repository.standard.StandardStatementRulesMapper;
import com.financial.cloud.repository.standard.StandardStatementIncomeMapper;
import com.financial.cloud.repository.statement.StatementRulesMapper;
import com.financial.cloud.repository.statement.StatementIncomeItemMapper;
import com.financial.cloud.repository.statement.StatementIncomeMapper;
import com.baomidou.mybatisplus.extension.toolkit.Db;
import com.financial.cloud.repository.book.BookMapper;
import com.financial.cloud.repository.voucher.VoucherItemMapper;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.incrementer.IdentifierGenerator;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.financial.cloud.constants.system.ConstsSysConfig;
import com.financial.cloud.common.Message;
import com.financial.cloud.domain.book.Book;
import com.financial.cloud.domain.book.Settlement;
import com.financial.cloud.dto.book.BookChangeDto;
import com.financial.cloud.domain.standard.StandardStatementIncome;
import com.financial.cloud.domain.standard.StandardStatementRules;
import com.financial.cloud.domain.statement.*;
import com.financial.cloud.dto.statement.StatementParamsDto;
import com.financial.cloud.dto.statement.StatementIncomeExport;
import com.financial.cloud.dto.voucher.VoucherItemVo;
import com.financial.cloud.enums.statement.StatementPeriodTypeEnum;
import com.financial.cloud.enums.statement.StatementSymbolEnum;
import com.financial.cloud.enums.statement.StatementTypeEnum;
import com.financial.cloud.service.config.ConfigSysService;
import com.financial.cloud.service.statement.StatementIncomeService;

import com.financial.cloud.util.excel.ExcelDataModeEnum;
import com.financial.cloud.util.excel.ExcelExporter;
import com.financial.cloud.util.excel.ExcelParams;
import com.financial.cloud.util.excel.ExportTemplateFiles;
import com.financial.cloud.util.StatementIncomeRules;
import com.financial.cloud.util.SubjectCodeCompat;
import com.financial.cloud.exception.ServiceException;
import com.financial.cloud.enums.error.StatementErrorCode;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.transaction.annotation.Transactional;
import lombok.RequiredArgsConstructor;

import java.io.File;
import java.io.IOException;
import java.math.BigDecimal;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;


@Slf4j
@RequiredArgsConstructor
@Service
public class StatementIncomeService{

    private final ConfigSysService configSysService;
    private final BookMapper bookMapper;
    private final VoucherItemMapper voucherItemMapper;
    private final StatementRulesMapper statementRulesMapper;
    private final StatementIncomeMapper statementIncomeMapper;
    private final StatementIncomeItemMapper statementIncomeItemMapper;
    private final IdentifierGenerator identifierGenerator;
    private final StandardStatementIncomeMapper standardStatementIncomeMapper;
    private final StandardStatementRulesMapper standardStatementRulesMapper;

    @Value("${financial-cloud.statement.income.strict-formula-validation:false}")
    private boolean strictFormulaValidation;

    public Message<StatementIncome> getIncomeStatement(StatementParamsDto dto, boolean force) {
        dto.parse();
        String currentTerm = configSysService.getCurrentTerm(dto.getBookId());
        // 查出所有数据
        Message<StatementIncome> msgIncome = new Message<>();
        LambdaQueryWrapper<StatementIncome> lqw = Wrappers.lambdaQuery();
        lqw.eq(StatementIncome::getYearPeriod, dto.getReportDate());
        lqw.eq(StatementIncome::getPeriodType, dto.getPeriodType());
        lqw.eq(StatementIncome::getBookId, dto.getBookId());
        StatementIncome statementIncome = statementIncomeMapper.selectOne(lqw);
        List<String> allMonths = dto.getAllMonths();
        if (statementIncome == null || allMonths.contains(currentTerm)) {
            msgIncome = generateIncomeStatement(dto, false);
        } else {
            LambdaQueryWrapper<StatementIncomeItem> itemlqw = Wrappers.lambdaQuery();
            itemlqw.eq(StatementIncomeItem::getBookId, statementIncome.getBookId());
            itemlqw.eq(StatementIncomeItem::getIncomeId, statementIncome.getId());
            itemlqw.orderByAsc(StatementIncomeItem::getSortIndex);
            statementIncome.setItems(statementIncomeItemMapper.selectList(itemlqw));

            msgIncome = Message.ok(statementIncome);
            validateFormulaChain(msgIncome.getData().getItems());
        }
        if (msgIncome != null) {
            Map<String, StatementIncomeItem> itemMap = new HashMap<>();
            for (StatementIncomeItem item : msgIncome.getData().getItems()) {
                itemMap.put(item.getItemCode(), item);
            }
            msgIncome.getData().setItemMap(itemMap);
            return msgIncome;
        }
        return msgIncome;
    }

    /**
     * 生成利润报表
     *
     * @param dto  查询参数
     * @param save 是否保存
     * @return 结果
     */
    public Message<StatementIncome> incomeStatement(StatementParamsDto dto, boolean save) {
        dto.parse();
        Message<StatementIncome> res = Message.failed("生成失败！");
        if (save) {
            deleteIncomeStatement(dto);
            res = generateIncomeStatement(dto, true);
        }
        return res;
    }

    /**
     * 生成报表
     */
    @Transactional
    public Message<StatementIncome> generateIncomeStatement(StatementParamsDto dto, boolean save) {
        //参数解析
        dto.parse();
        // 查出所有数据
        LambdaQueryWrapper<StatementIncome> lqw = Wrappers.lambdaQuery();
        lqw.eq(StatementIncome::getYearPeriod, dto.getReportDate());
        lqw.eq(StatementIncome::getPeriodType, dto.getPeriodType());
        lqw.eq(StatementIncome::getBookId, dto.getBookId());
        StatementIncome statementIncome = statementIncomeMapper.selectOne(lqw);
        boolean isExist = statementIncome != null;

        //新建报表
        statementIncome = new StatementIncome();
        statementIncome.setId(identifierGenerator.nextId(statementIncome).toString());
        statementIncome.setBookId(dto.getBookId());
        statementIncome.setYearPeriod(dto.getReportDate());
        statementIncome.setPeriodType(dto.getPeriodType());

        //取出账套模板配置
        LambdaQueryWrapper<StatementIncomeItem> itemlqw = Wrappers.lambdaQuery();
        itemlqw.eq(StatementIncomeItem::getBookId, statementIncome.getBookId());
        itemlqw.eq(StatementIncomeItem::getIncomeId, ConstsSysConfig.SYS_CONFIG_TEMPLATE_ID);
        itemlqw.orderByAsc(StatementIncomeItem::getSortIndex);

        List<StatementIncomeItem> statementIncomeItems = statementIncomeItemMapper.selectList(itemlqw);

        //获取规则
        LambdaQueryWrapper<StatementRules> ruleslqw = Wrappers.lambdaQuery();
        ruleslqw.eq(StatementRules::getBookId, statementIncome.getBookId());
        ruleslqw.eq(StatementRules::getType, StatementTypeEnum.income.name());
        List<StatementRules> rulesList = statementRulesMapper.selectList(ruleslqw);

        dto.setPostedOnly(true);
        //读取科目发生额（仅已过账凭证）
        List<VoucherItemVo> voucherItemVos = voucherItemMapper.selectSubjectAmount(dto);

        for (StatementIncomeItem item : statementIncomeItems) {
            item.setId(null);
            item.setIncomeId(statementIncome.getId());
            BigDecimal currentAmount = accumulateLineAmount(item.getItemCode(), rulesList, voucherItemVos);
            log.debug("ItemCode {} , ItemName {} , Amount {}", item.getItemCode(), item.getItemName(), currentAmount);
            item.setCurrentBalance(currentAmount);

        }
        generateIncomeStatementYear(dto, statementIncomeItems, rulesList);

        if (!statementIncomeItems.isEmpty()) {
            StatementIncomeRules.calculateDerivedLines(statementIncomeItems);
            validateFormulaChain(statementIncomeItems);
        }

        statementIncome.setItems(statementIncomeItems);

        if (save) {
            if (!isExist) {
                statementIncomeMapper.insert(statementIncome);
                Db.saveBatch(statementIncomeItems);
            } else {
                return Message.failed("本期报表数据已生成。");
            }
        }
        return Message.ok(statementIncome);
    }

    void validateFormulaChain(List<StatementIncomeItem> items) {
        StatementIncomeRules.FormulaChainDiff diff = StatementIncomeRules.computeFormulaChainDiff(items);
        if (diff == null || diff.withinTolerance()) {
            return;
        }
        if (strictFormulaValidation) {
            throw new ServiceException(
                    StatementErrorCode.INCOME_STATEMENT_FORMULA_FAILED,
                    diff.maxAbsDiff());
        }
        log.warn("Income statement formula chain off by {}", diff.maxAbsDiff());
    }

    private BigDecimal accumulateLineAmount(
            String itemCode,
            List<StatementRules> rulesList,
            List<VoucherItemVo> voucherItemVos) {
        BigDecimal total = BigDecimal.ZERO;
        // selectSubjectAmount 按科目聚合无分录 id；多条企业子科目规则（660201…）
        // 映射到同一小企业科目（5602）时，每个凭证科目只计一次。
        Set<String> appliedSubjects = new HashSet<>();
        for (StatementRules rule : rulesList) {
            if (!itemCode.equalsIgnoreCase(rule.getItemCode())) {
                continue;
            }
            for (VoucherItemVo voucherItemVo : voucherItemVos) {
                if (!SubjectCodeCompat.incomeRuleMatchesVoucherSubject(
                        rule.getSubjectCode(), voucherItemVo.getSubjectCode())) {
                    continue;
                }
                String subjectKey = voucherItemVo.getSubjectCode();
                if (!appliedSubjects.add(subjectKey)) {
                    continue;
                }
                String effectiveRule = StatementIncomeRules.effectiveAmountRule(
                        rule.getRule(),
                        voucherItemVo.getDebitAmount(),
                        voucherItemVo.getCreditAmount());
                log.debug("\tSubjectCode {} , DEBIT_AMOUNT {} , CREDIT_AMOUNT {}",
                        voucherItemVo.getSubjectCode(), voucherItemVo.getDebitAmount(), voucherItemVo.getCreditAmount());
                log.debug("\tSymbol {} , Rule {} (effective {})", rule.getSymbol(), rule.getRule(), effectiveRule);
                total = total.add(StatementIncomeRules.applyRuleContribution(
                        voucherItemVo.getDebitAmount(),
                        voucherItemVo.getCreditAmount(),
                        effectiveRule,
                        rule.getSymbol()));
            }
        }
        return total;
    }

    /**
     * 本年累计金额
     *
     * @param dto
     * @param statementIncomeItems
     * @param rulesList
     */
    private void generateIncomeStatementYear(StatementParamsDto dto, List<StatementIncomeItem> statementIncomeItems, List<StatementRules> rulesList) {
        StatementParamsDto yearStatementParamsDto = new StatementParamsDto();
        yearStatementParamsDto.setYear(dto.getYear());
        yearStatementParamsDto.setBookId(dto.getBookId());
        yearStatementParamsDto.setCountType("sum");
        yearStatementParamsDto.setDateRangeEnd(dto.getDateRangeEnd());
        yearStatementParamsDto.setPostedOnly(true);

        //读取科目发生额（仅已过账凭证）
        List<VoucherItemVo> voucherItemVos = voucherItemMapper.selectSubjectAmount(yearStatementParamsDto);

        for (StatementIncomeItem item : statementIncomeItems) {
            item.setCumulativeBalance(accumulateLineAmount(item.getItemCode(), rulesList, voucherItemVos));
        }
    }
    public Message<StatementIncome> deleteIncomeStatement(StatementParamsDto dto) {
        LambdaQueryWrapper<StatementIncome> lqw = Wrappers.lambdaQuery();
        lqw.eq(StatementIncome::getYearPeriod, dto.getReportDate());
        lqw.eq(StatementIncome::getPeriodType, dto.getPeriodType());
        lqw.eq(StatementIncome::getBookId, dto.getBookId());
        StatementIncome statementIncome = statementIncomeMapper.selectOne(lqw);
        statementIncomeMapper.deleteById(statementIncome);
        if (statementIncome != null) {
            LambdaQueryWrapper<StatementIncomeItem> itemlqw = Wrappers.lambdaQuery();
            itemlqw.eq(StatementIncomeItem::getBookId, statementIncome.getBookId());
            itemlqw.eq(StatementIncomeItem::getIncomeId, statementIncome.getId());
            statementIncomeItemMapper.delete(itemlqw);
        }
        return Message.failed("报表数据删除成功！");
    }
    public void initIncomeStatement(BookChangeDto dto) {
        LambdaQueryWrapper<StandardStatementIncome> sItemlqw = Wrappers.lambdaQuery();
        sItemlqw.eq(StandardStatementIncome::getStandardId, dto.getStandardId());
        List<StandardStatementIncome> items = standardStatementIncomeMapper.selectList(sItemlqw);
        List<StatementIncomeItem> inComeItems = new ArrayList<>();
        for (StandardStatementIncome item : items) {
            StatementIncomeItem inComeitem = new StatementIncomeItem();
            inComeitem.setBookId(dto.getId());
            inComeitem.setIncomeId(ConstsSysConfig.SYS_CONFIG_TEMPLATE_ID);
            inComeitem.setItemCode(item.getItemCode());
            inComeitem.setItemName(item.getItemName());
            inComeitem.setSortIndex(item.getSortIndex());
            inComeitem.setLevel(item.getLevel());
            inComeitem.setSymbol(item.getSymbol());
            inComeitem.setCurrentBalance(BigDecimal.ZERO);
            inComeitem.setCumulativeBalance(BigDecimal.ZERO);
            inComeItems.add(inComeitem);
        }

        LambdaQueryWrapper<StandardStatementRules> sRulelqw = Wrappers.lambdaQuery();
        sRulelqw.eq(StandardStatementRules::getStandardId, dto.getStandardId());
        sRulelqw.eq(StandardStatementRules::getType, "income");
        List<StandardStatementRules> sRuleitems = standardStatementRulesMapper.selectList(sRulelqw);

        List<StatementRules> itemRuls = new ArrayList<>();
        Set<String> seenRuleKeys = new HashSet<>();
        for (StandardStatementRules itemRule : sRuleitems) {
            String mappedSubject = SubjectCodeCompat.mapIncomeRuleSubject(itemRule.getSubjectCode());
            String ruleType = StatementIncomeRules.normalizeIncomeRuleType(
                    itemRule.getRule(), mappedSubject);
            String dedupeKey = itemRule.getItemCode() + "|" + mappedSubject + "|" + ruleType + "|" + itemRule.getSymbol();
            if (!seenRuleKeys.add(dedupeKey)) {
                continue;
            }
            StatementRules rule = new StatementRules();
            rule.setBookId(dto.getId());
            rule.setType("income");
            rule.setItemCode(itemRule.getItemCode());
            rule.setSubjectCode(mappedSubject);
            rule.setRule(ruleType);
            rule.setSymbol(itemRule.getSymbol());
            itemRuls.add(rule);
        }

        Db.saveBatch(inComeItems);
        Db.saveBatch(itemRuls);
    }

    /**
     * 导出
     */
    public void export(StatementParamsDto dto, HttpServletResponse response) throws IOException {
        StatementIncome incomeStatement = getIncomeStatement(dto, true).getData();
        Book book = bookMapper.selectById(dto.getBookId());
        File templateSource = ExportTemplateFiles.copyToTemp("static/export-template/template-income.xlsx", "template-income_src_");
        Path tempFilePath = Files.createTempFile("template-income_", ".xlsx");
        File tempFile = tempFilePath.toFile();

        List<StatementIncomeExport.Item> items = new ArrayList<>();
        incomeStatement.getItems().forEach(item -> items.add(StatementIncomeExport.Item.builder()
                .itemName(item.getItemName())
                .rowNum(item.getSortIndex())
                .currentBalance(item.getCurrentBalance())
                .yearBalance(item.getCumulativeBalance())
                .build()));
        StatementIncomeExport data = StatementIncomeExport.builder()
                .companyName(book.getCompanyName())
                .date(incomeStatement.getYearPeriod())
                .items(items)
                .build();

        // 单项数据渲染
        ExcelParams<StatementIncomeExport> paramsObj = ExcelParams.<StatementIncomeExport>builder()
                .httpResponse(null)
                .mode(ExcelDataModeEnum.base_attribute)
                .dataModel(data)
                .outputDirectory(tempFile.getParent()) // 临时目录路径
                .outputFileName(tempFile.getName())    // 临时文件名
                .enableMergeCells(false)
                .autoSizeColumns(false)
                .recalculateFormulas(true)
                .templateFilePath(templateSource.getAbsolutePath())
                .build();
        ExcelExporter.export(paramsObj);
        // 列表数据渲染
        ExcelParams<List<StatementIncomeExport.Item>> paramsList = ExcelParams.<List<StatementIncomeExport.Item>>builder()
                .httpResponse(response)
                .mode(ExcelDataModeEnum.include_list)
                .dataModel(items)
                .enableMergeCells(false)
                .autoSizeColumns(false)
                .recalculateFormulas(true)
                .templateFilePath(tempFile.getPath())
                .build();

        ExcelExporter.export(paramsList);
        // 最后删除临时文件
        if (tempFile.exists()) tempFile.delete();
        if (templateSource.exists()) templateSource.delete();
    }
    public boolean deleteByBookIds(List<String> bookIds) {
        LambdaQueryWrapper<StatementIncome> slqw = Wrappers.lambdaQuery();
        slqw.in(StatementIncome::getBookId, bookIds);
        statementIncomeMapper.delete(slqw);

        LambdaQueryWrapper<StatementIncomeItem> sItemlqw = Wrappers.lambdaQuery();
        sItemlqw.in(StatementIncomeItem::getBookId, bookIds);

        statementIncomeItemMapper.delete(sItemlqw);

        LambdaQueryWrapper<StatementRules> sRulelqw = Wrappers.lambdaQuery();
        sRulelqw.in(StatementRules::getBookId, bookIds);
        sRulelqw.eq(StatementRules::getType, "income");
        statementRulesMapper.delete(sRulelqw);
        return true;
    }

    /**
     * 删除结账写入的利润表快照（反结账后允许重新生成）。
     */
    public void deletePeriodSnapshot(String bookId, String yearPeriod, String periodType) {
        LambdaQueryWrapper<StatementIncome> lqw = Wrappers.lambdaQuery();
        lqw.eq(StatementIncome::getBookId, bookId);
        lqw.eq(StatementIncome::getYearPeriod, yearPeriod);
        lqw.eq(StatementIncome::getPeriodType, periodType);
        StatementIncome statementIncome = statementIncomeMapper.selectOne(lqw);
        if (statementIncome == null) {
            return;
        }
        LambdaQueryWrapper<StatementIncomeItem> itemlqw = Wrappers.lambdaQuery();
        itemlqw.eq(StatementIncomeItem::getBookId, bookId);
        itemlqw.eq(StatementIncomeItem::getIncomeId, statementIncome.getId());
        statementIncomeItemMapper.delete(itemlqw);
        statementIncomeMapper.deleteById(statementIncome.getId());
    }

    /**
     * 结账检查入库
     *
     * @param dto 结账参数
     * @return 结果
     */
    @Transactional
    public boolean checkout(Settlement dto) {
        //月报
        StatementParamsDto monthParamsDto = new StatementParamsDto();
        monthParamsDto.setBookId(dto.getBookId());
        monthParamsDto.setPeriodType(StatementPeriodTypeEnum.MONTH.getValue());
        monthParamsDto.setReportDate(dto.getCurrentTerm());
        generateIncomeStatement(monthParamsDto, true);

        //季报
        StatementParamsDto quarterParamsDto = new StatementParamsDto();
        quarterParamsDto.setBookId(dto.getBookId());
        quarterParamsDto.setPeriodType(StatementPeriodTypeEnum.QUARTER.getValue());
        quarterParamsDto.setReportDate(dto.getCurrentTerm());
        if (monthParamsDto.isQuarterReportMonth()) {
            generateIncomeStatement(quarterParamsDto, true);
        }

        //年报
        StatementParamsDto yearParamsDto = new StatementParamsDto();
        yearParamsDto.setBookId(dto.getBookId());
        yearParamsDto.setPeriodType(StatementPeriodTypeEnum.YEAR.getValue());
        yearParamsDto.setReportDate(dto.getCurrentTerm());
        if (monthParamsDto.isYearReportMonth()) {
            generateIncomeStatement(yearParamsDto, true);
        }

        return true;
    }
}
