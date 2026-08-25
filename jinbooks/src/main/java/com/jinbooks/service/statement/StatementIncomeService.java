package com.jinbooks.service.statement;


import lombok.extern.slf4j.Slf4j;
import com.jinbooks.repository.standard.StandardStatementRulesMapper;
import com.jinbooks.repository.standard.StandardStatementIncomeMapper;
import com.jinbooks.repository.statement.StatementRulesMapper;
import com.jinbooks.repository.statement.StatementIncomeItemMapper;
import com.jinbooks.repository.statement.StatementIncomeMapper;
import com.baomidou.mybatisplus.extension.toolkit.Db;
import com.jinbooks.repository.book.BookMapper;
import com.jinbooks.repository.voucher.VoucherItemMapper;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.incrementer.IdentifierGenerator;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.jinbooks.constants.ConstsSysConfig;
import com.jinbooks.common.Message;
import com.jinbooks.domain.book.Book;
import com.jinbooks.domain.book.Settlement;
import com.jinbooks.dto.book.BookChangeDto;
import com.jinbooks.domain.standard.StandardStatementIncome;
import com.jinbooks.domain.standard.StandardStatementRules;
import com.jinbooks.domain.statement.*;
import com.jinbooks.dto.statement.StatementParamsDto;
import com.jinbooks.dto.statement.StatementIncomeExport;
import com.jinbooks.dto.voucher.VoucherItemVo;
import com.jinbooks.enums.StatementPeriodTypeEnum;
import com.jinbooks.enums.StatementSymbolEnum;
import com.jinbooks.enums.StatementTypeEnum;
import com.jinbooks.service.config.ConfigSysService;
import com.jinbooks.service.statement.StatementIncomeService;

import com.jinbooks.util.excel.ExcelDataModeEnum;
import com.jinbooks.util.excel.ExcelExporter;
import com.jinbooks.util.excel.ExcelParams;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;

import java.io.File;
import java.io.IOException;
import java.math.BigDecimal;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;

import org.springframework.stereotype.Service;
import org.springframework.util.ResourceUtils;


@Slf4j
@RequiredArgsConstructor
@Service
public class StatementIncomeService{

    class Rules {
        /**
         * 损益发生额
         */
        public static String PROFIT_AND_LOSS_AMOUNT = "PROFIT_AND_LOSS_AMOUNT";
        /**
         * 借方发生额
         */
        public static String DEBIT_AMOUNT = "DEBIT_AMOUNT";
        /**
         * 贷方发生额
         */
        public static String CREDIT_AMOUNT = "CREDIT_AMOUNT";

    }

    private final ConfigSysService configSysService;
    private final BookMapper bookMapper;
    private final VoucherItemMapper voucherItemMapper;
    private final StatementRulesMapper statementRulesMapper;
    private final StatementIncomeMapper statementIncomeMapper;
    private final StatementIncomeItemMapper statementIncomeItemMapper;
    private final IdentifierGenerator identifierGenerator;
    private final StandardStatementIncomeMapper standardStatementIncomeMapper;
    private final StandardStatementRulesMapper standardStatementRulesMapper;
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

        //读取科目余额表
        List<VoucherItemVo> voucherItemVos = voucherItemMapper.selectSubjectAmount(dto);

        for (StatementIncomeItem item : statementIncomeItems) {
            item.setId(null);
            item.setIncomeId(statementIncome.getId());
            BigDecimal currentAmount = BigDecimal.ZERO;
            log.debug("ItemCode {} , ItemName {}", item.getItemCode(), item.getItemName());
            for (StatementRules rule : rulesList) {
                if (item.getItemCode().equalsIgnoreCase(rule.getItemCode())) {
                    for (VoucherItemVo voucherItemVo : voucherItemVos) {
                        if (voucherItemVo.getDebitAmount() == null) {
                            voucherItemVo.setDebitAmount(BigDecimal.ZERO);
                        }
                        if (voucherItemVo.getCreditAmount() == null) {
                            voucherItemVo.setCreditAmount(BigDecimal.ZERO);
                        }
                        if (rule.getSubjectCode().equalsIgnoreCase(voucherItemVo.getSubjectCode())) {
                            log.debug("\tSubjectCode {} , DEBIT_AMOUNT {} , CREDIT_AMOUNT {}", voucherItemVo.getSubjectCode(), voucherItemVo.getDebitAmount(), voucherItemVo.getCreditAmount());
                            log.debug("\tSymbol {} , Rule {}", rule.getSymbol(), rule.getRule());
                            if (rule.getSymbol().equals(StatementSymbolEnum.PLUS.getValue())) {
                                if (rule.getRule().equalsIgnoreCase(Rules.PROFIT_AND_LOSS_AMOUNT)) {
                                    //+ (借方-贷方)
                                    currentAmount = currentAmount.add(
                                            voucherItemVo.getDebitAmount().abs()
                                                    .subtract(voucherItemVo.getCreditAmount().abs()));
                                } else if (rule.getRule().equalsIgnoreCase(Rules.DEBIT_AMOUNT)) {
                                    //+ 借方
                                    currentAmount = currentAmount.add(voucherItemVo.getDebitAmount());
                                } else if (rule.getRule().equalsIgnoreCase(Rules.CREDIT_AMOUNT)) {
                                    //+ 贷方
                                    currentAmount = currentAmount.add(voucherItemVo.getCreditAmount());
                                }
                            } else if (rule.getSymbol().equals(StatementSymbolEnum.MINUS.getValue())) {
                                if (rule.getRule().equalsIgnoreCase(Rules.PROFIT_AND_LOSS_AMOUNT)) {
                                    //- (借方-贷方)
                                    currentAmount = currentAmount.subtract(
                                            voucherItemVo.getDebitAmount().abs()
                                                    .subtract(voucherItemVo.getCreditAmount().abs()));
                                } else if (rule.getRule().equalsIgnoreCase(Rules.DEBIT_AMOUNT)) {
                                    //- 借方
                                    currentAmount = currentAmount.subtract(voucherItemVo.getDebitAmount());
                                } else if (rule.getRule().equalsIgnoreCase(Rules.CREDIT_AMOUNT)) {
                                    //- 贷方
                                    currentAmount = currentAmount.subtract(voucherItemVo.getCreditAmount());
                                }
                            }
                        }
                    }

                }
            }
            log.debug("ItemCode {} , ItemName {} , Amount {}", item.getItemCode(), item.getItemName(), currentAmount);
            item.setCurrentBalance(currentAmount);

        }
        generateIncomeStatementYear(dto, statementIncomeItems, rulesList);

        if (!statementIncomeItems.isEmpty()) {
            calculate(statementIncomeItems);
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

    private void calculate(List<StatementIncomeItem> items) {
        Map<String, StatementIncomeItem> itemsMap = new HashMap<>();
        for (StatementIncomeItem item : items) {
            itemsMap.put(item.getItemCode(), item);
        }

        BigDecimal currentAmount = BigDecimal.ZERO;
        BigDecimal yearAmount = BigDecimal.ZERO;
        for (StatementIncomeItem item : items) {
            //[一、] 相加减
            if (item.getItemCode().length() == 3
                    && item.getItemCode().startsWith("1")) {
                if (StatementSymbolEnum.PLUS.getValue().equalsIgnoreCase(item.getSymbol())) {
                    currentAmount = currentAmount.add(item.getCurrentBalance());
                    yearAmount = yearAmount.add(item.getCumulativeBalance());
                } else if (StatementSymbolEnum.MINUS.getValue().equalsIgnoreCase(item.getSymbol())) {
                    currentAmount = currentAmount.subtract(item.getCurrentBalance());
                    yearAmount = yearAmount.subtract(item.getCumulativeBalance());
                }
            }
        }
        //计算营业利润
        StatementIncomeItem yysrItem = itemsMap.get("1");
        StatementIncomeItem yylrItem = itemsMap.get("2");
        yylrItem.setCurrentBalance(yysrItem.getCurrentBalance().subtract(currentAmount));
        yylrItem.setCumulativeBalance(yysrItem.getCumulativeBalance().subtract(yearAmount));

        //营业利润减除项
        currentAmount = BigDecimal.ZERO;
        yearAmount = BigDecimal.ZERO;
        for (StatementIncomeItem item : items) {
            //[二、] 相加减
            if (item.getItemCode().length() == 3 && item.getItemCode().startsWith("2")) {
                if (StatementSymbolEnum.PLUS.getValue().equalsIgnoreCase(item.getSymbol())) {
                    currentAmount = currentAmount.add(item.getCurrentBalance());
                    yearAmount = yearAmount.add(item.getCumulativeBalance());
                } else if (StatementSymbolEnum.MINUS.getValue().equalsIgnoreCase(item.getSymbol())) {
                    currentAmount = currentAmount.subtract(item.getCurrentBalance());
                    yearAmount = yearAmount.subtract(item.getCumulativeBalance());
                }
            }
        }

        //利润总额 = 营业利润 -营业利润减除项
        StatementIncomeItem lrzeItem = itemsMap.get("3");
        lrzeItem.setCurrentBalance(yylrItem.getCurrentBalance().add(currentAmount));
        lrzeItem.setCumulativeBalance(yylrItem.getCumulativeBalance().add(yearAmount));

        //利润总额减除项
        currentAmount = BigDecimal.ZERO;
        yearAmount = BigDecimal.ZERO;
        for (StatementIncomeItem item : items) {
            //[三、] 相加减
            if (item.getItemCode().startsWith("3") && !item.getItemCode().equalsIgnoreCase("3")) {
                if (StatementSymbolEnum.PLUS.getValue().equalsIgnoreCase(item.getSymbol())) {
                    currentAmount = currentAmount.add(item.getCurrentBalance());
                    yearAmount = yearAmount.add(item.getCumulativeBalance());
                } else if (StatementSymbolEnum.MINUS.getValue().equalsIgnoreCase(item.getSymbol())) {
                    currentAmount = currentAmount.subtract(item.getCurrentBalance());
                    yearAmount = yearAmount.subtract(item.getCumulativeBalance());
                }
            }
        }

        //净利润 = 利润总额 - 利润总额减除项
        StatementIncomeItem jlrItem = itemsMap.get("4");
        jlrItem.setCurrentBalance(lrzeItem.getCurrentBalance().subtract(currentAmount));
        jlrItem.setCumulativeBalance(lrzeItem.getCumulativeBalance().subtract(yearAmount));
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

        //读取科目余额表
        List<VoucherItemVo> voucherItemVos = voucherItemMapper.selectSubjectAmount(yearStatementParamsDto);

        for (StatementIncomeItem item : statementIncomeItems) {
            BigDecimal yearAmount = BigDecimal.ZERO;
            for (StatementRules rule : rulesList) {
                if (item.getItemCode().equalsIgnoreCase(rule.getItemCode())) {

                    for (VoucherItemVo voucherItemVo : voucherItemVos) {
                        if (voucherItemVo.getDebitAmount() == null) {
                            voucherItemVo.setDebitAmount(BigDecimal.ZERO);
                        }
                        if (voucherItemVo.getCreditAmount() == null) {
                            voucherItemVo.setCreditAmount(BigDecimal.ZERO);
                        }
                        if (rule.getSubjectCode().equalsIgnoreCase(voucherItemVo.getSubjectCode())) {
                            if (rule.getSymbol().equals(StatementSymbolEnum.PLUS.getValue())) {
                                if (rule.getRule().equalsIgnoreCase(Rules.PROFIT_AND_LOSS_AMOUNT)) {
                                    //+ (借方-贷方)
                                    yearAmount = yearAmount.add(
                                            voucherItemVo.getDebitAmount().abs()
                                                    .subtract(voucherItemVo.getCreditAmount().abs()));
                                } else if (rule.getRule().equalsIgnoreCase(Rules.DEBIT_AMOUNT)) {
                                    //+ 借方
                                    yearAmount = yearAmount.add(voucherItemVo.getDebitAmount());
                                } else if (rule.getRule().equalsIgnoreCase(Rules.CREDIT_AMOUNT)) {
                                    //+ 贷方
                                    yearAmount = yearAmount.add(voucherItemVo.getCreditAmount());
                                }
                            } else if (rule.getSymbol().equals(StatementSymbolEnum.MINUS.getValue())) {
                                if (rule.getRule().equalsIgnoreCase(Rules.PROFIT_AND_LOSS_AMOUNT)) {
                                    //- (借方-贷方)
                                    yearAmount = yearAmount.subtract(
                                            voucherItemVo.getDebitAmount().abs()
                                                    .subtract(voucherItemVo.getCreditAmount().abs()));
                                } else if (rule.getRule().equalsIgnoreCase(Rules.DEBIT_AMOUNT)) {
                                    //- 借方
                                    yearAmount = yearAmount.subtract(voucherItemVo.getDebitAmount());
                                } else if (rule.getRule().equalsIgnoreCase(Rules.CREDIT_AMOUNT)) {
                                    //- 贷方
                                    yearAmount = yearAmount.subtract(voucherItemVo.getCreditAmount());
                                }
                            }
                        }
                    }

                }
            }
            item.setCumulativeBalance(yearAmount);
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
        for (StandardStatementRules itemRule : sRuleitems) {
            StatementRules rule = new StatementRules();
            rule.setBookId(dto.getId());
            rule.setType(itemRule.getType());
            rule.setType("income");
            rule.setItemCode(itemRule.getItemCode());
            rule.setSubjectCode(itemRule.getSubjectCode());
            rule.setRule(itemRule.getRule());
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
        String templatePath = ResourceUtils
                .getURL("classpath:static/export-template/template-income.xlsx")
                .getPath();
        // 注意：Windows下getPath()前会带'/'，可做处理
        if (templatePath.startsWith("/")) {
            templatePath = templatePath.substring(1);
        }
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
                .templateFilePath(templatePath)
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
