package com.financial.cloud.service.statement;

import com.financial.cloud.repository.config.ConfigCashFlowBalanceMapper;
import com.financial.cloud.service.config.ConfigSysService;
import com.financial.cloud.repository.statement.StatementSubjectBalanceMapper;
import com.financial.cloud.repository.statement.StatementIncomeItemMapper;
import com.financial.cloud.repository.statement.StatementIncomeMapper;
import com.financial.cloud.repository.statement.StatementCashFlowMapper;
import com.financial.cloud.service.statement.StatementSubjectBalanceService;
import com.financial.cloud.service.statement.StatementReportService;
import com.financial.cloud.repository.book.BookInitBalanceMapper;
import com.financial.cloud.repository.book.SettlementMapper;
import com.financial.cloud.repository.book.BookSubjectMapper;
import com.financial.cloud.repository.book.BookMapper;
import com.financial.cloud.repository.voucher.VoucherMapper;
import com.financial.cloud.repository.voucher.VoucherItemMapper;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.financial.cloud.constants.system.ConstsSysConfig;
import com.financial.cloud.common.Message;
import com.financial.cloud.domain.book.BookInitBalance;
import com.financial.cloud.domain.book.Book;
import com.financial.cloud.domain.book.BookSubject;
import com.financial.cloud.domain.book.Settlement;
import com.financial.cloud.domain.config.ConfigCashFlowBalance;
import com.financial.cloud.domain.statement.*;
import com.financial.cloud.dto.statement.StatementParamsDto;
import com.financial.cloud.dto.statement.StatementIncomeExport;
import com.financial.cloud.domain.voucher.Voucher;
import com.financial.cloud.dto.voucher.VoucherItemPageDto;
import com.financial.cloud.dto.voucher.VoucherItemVo;
import com.financial.cloud.enums.statement.CashFlowItemEnum;
import com.financial.cloud.enums.voucher.VoucherStatusEnum;
import com.financial.cloud.enums.common.YesNoEnum;
import com.financial.cloud.enums.error.StatementErrorCode;
import com.financial.cloud.exception.BusinessException;
import com.financial.cloud.exception.ServiceException;
import com.financial.cloud.util.StatementCashFlowIndirectRules;
import com.financial.cloud.util.StatementCashFlowRules;
import com.financial.cloud.util.SubjectCodeCompat;
import com.financial.cloud.util.excel.ExcelDataModeEnum;
import com.financial.cloud.util.excel.ExcelExporter;
import com.financial.cloud.util.excel.ExcelParams;
import com.financial.cloud.util.excel.ExportTemplateFiles;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.io.File;
import java.io.IOException;
import java.math.BigDecimal;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.YearMonth;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.stream.Collectors;

@RequiredArgsConstructor
@Service
public class StatementReportService{

    private final BookSubjectMapper bookSubjectMapper;
    private final StatementSubjectBalanceMapper subjectBalanceMapper;
    private final StatementSubjectBalanceService subjectBalanceService;
    private final ConfigSysService configSysService;
    private final ConfigCashFlowBalanceMapper configCashFlowBalanceMapper;
    private final StatementIncomeMapper statementIncomeMapper;
    private final StatementIncomeItemMapper statementIncomeItemMapper;
    private final StatementCashFlowMapper statementCashFlowMapper;
    private final SettlementMapper settlementMapper;
    private final BookInitBalanceMapper bookInitBalanceMapper;
    private final BookMapper bookMapper;
    private final VoucherMapper voucherMapper;
    private final VoucherItemMapper voucherItemMapper;
    private final StatementBalanceSheetService balanceSheetService;
    private final StatementIncomeService statementIncomeService;

    @Value("${financial-cloud.statement.cash-flow.strict-reconciliation:false}")
    private boolean strictCashFlowReconciliation;
    public BigDecimal getEndingBalance(StatementParamsDto dto) {
        dto.parse();
        List<StatementCashFlow> data = cashFlowStatement(dto).getData();

        return data.stream()
                .filter(item -> "38-xj-qmye".equals(item.getItemCode()))
                .map(StatementCashFlow::getMonthlyAmount)
                .findFirst()
                .orElse(null);
    }
    public void cashFlowExport(StatementParamsDto dto, HttpServletResponse response) throws IOException {
        dto.parse();
        List<StatementCashFlow> cashFlows = cashFlowStatement(dto).getData();
        Book book = bookMapper.selectById(dto.getBookId());
        File templateSource = ExportTemplateFiles.copyToTemp("static/export-template/template-cash-flow.xlsx", "template-cash-flow_src_");
        Path tempFilePath = Files.createTempFile("template-cash-flow_", ".xlsx");
        File tempFile = tempFilePath.toFile();

        List<StatementIncomeExport.Item> items = new ArrayList<>();
        cashFlows.forEach(item -> items.add(StatementIncomeExport.Item.builder()
                .itemName(item.getItemName())
                .rowNum(item.getSortIndex())
                .currentBalance(item.getMonthlyAmount())
                .yearBalance(item.getCurrentAmount())
                .build()));
        StatementIncomeExport data = StatementIncomeExport.builder()
                .companyName(book.getCompanyName())
                .date(dto.getReportDate())
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

    /**
     * 科目余额表导出
     */
    public void subjectBalanceExport(StatementParamsDto dto, HttpServletResponse response) throws IOException {
        dto.parse();
        List<StatementSubjectBalance> subjectBalances = subjectBalance(dto).getData();
        Book book = bookMapper.selectById(dto.getBookId());
        File templateSource = ExportTemplateFiles.copyToTemp("static/export-template/template-subject-balance.xlsx", "template-subject-balance_src_");
        Path tempFilePath = Files.createTempFile("template-subject-balance_", ".xlsx");
        File tempFile = tempFilePath.toFile();

        Map<String, Object> data = new HashMap<>();
        data.put("date", dto.getReportDate());
        data.put("bookName", book.getName());

        // 单项数据渲染
        ExcelParams<Map<String, Object>> paramsObj = ExcelParams.<Map<String, Object>>builder()
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
        ExcelParams<List<StatementSubjectBalance>> paramsList = ExcelParams.<List<StatementSubjectBalance>>builder()
                .httpResponse(response)
                .mode(ExcelDataModeEnum.include_list)
                .dataModel(subjectBalances)
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

    /**
     * 凭证汇总表导出
     */
    public void voucherSummaryExport(StatementParamsDto dto, HttpServletResponse response) throws IOException {
        dto.parse();
        List<StatementSubjectBalance> subjectBalances = voucherSummary(dto).getData();
        Book book = bookMapper.selectById(dto.getBookId());
        QueryWrapper<Voucher> queryWrapper = new QueryWrapper<>();
        queryWrapper
                .select("SUM(receipt_num) AS files", "COUNT(*) AS total")
                .ge("voucher_date", dto.getDateRangeStart())
                .le("voucher_date", dto.getDateRangeEnd())
                .eq("book_id", dto.getBookId())
                .eq("status", VoucherStatusEnum.COMPLETED.getValue());
        Map<String, Object> result = voucherMapper.selectMaps(queryWrapper).get(0);
        String voucherNumber = result.get("total").toString();
        String voucherFileNum = result.get("files").toString();

        File templateSource = ExportTemplateFiles.copyToTemp("static/export-template/template-voucher-summary.xlsx", "template-voucher-summary_src_");
        Path tempFilePath = Files.createTempFile("template-voucher-summary_", ".xlsx");
        File tempFile = tempFilePath.toFile();

        Map<String, Object> data = new HashMap<>();
        data.put("voucherInfo", "凭证字: (所有) #凭证总张数: " + voucherNumber + "张 附件总张数: " + voucherFileNum + "张");
        data.put("date", dto.getDateRangeStart() + "至" + dto.getDateRangeEnd());
        data.put("bookName", book.getName());

        // 单项数据渲染
        ExcelParams<Map<String, Object>> paramsObj = ExcelParams.<Map<String, Object>>builder()
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
        ExcelParams<List<StatementSubjectBalance>> paramsList = ExcelParams.<List<StatementSubjectBalance>>builder()
                .httpResponse(response)
                .mode(ExcelDataModeEnum.include_list)
                .dataModel(subjectBalances)
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

    private Map<String, Boolean> checkTermIfSameYear(StatementParamsDto dto) {
        //是否为当前年份
        int currentYear = Integer.parseInt(dto.getDateRangeStart().substring(0, 4));
        String startDate = configSysService.selectConfigByKey(dto.getBookId(), ConstsSysConfig.SYS_PAYMENT_TERM_START);
        int startYear = Integer.parseInt(startDate.substring(0, 4));
        Map<String, Boolean> map = new HashMap<>();

        map.put("isSameYear", currentYear == startYear);
        map.put("isSameMonth", dto.getDateRangeStart().substring(0, 7).equals(startDate));


        return map;
    }
    public Message<List<StatementCashFlow>> cashFlowStatement(StatementParamsDto dto) {
        dto.parse();

        //判断是否是起始账套年份
        Map<String, Boolean> stringBooleanMap = checkTermIfSameYear(dto);
        Boolean isSameYear = stringBooleanMap.get("isSameYear");
        Boolean isSameMonth = stringBooleanMap.get("isSameMonth");

        LambdaQueryWrapper<ConfigCashFlowBalance> configCashFlowBalanceLambdaQueryWrapper = new LambdaQueryWrapper<>();
        configCashFlowBalanceLambdaQueryWrapper.eq(ConfigCashFlowBalance::getBookId, dto.getBookId());
        configCashFlowBalanceLambdaQueryWrapper.orderByAsc(ConfigCashFlowBalance::getSortIndex);
        //获取项目名称
        List<ConfigCashFlowBalance> configCashFlowBalances = configCashFlowBalanceMapper.selectList(configCashFlowBalanceLambdaQueryWrapper);
        if (configCashFlowBalances.isEmpty()) {
            throw new BusinessException(StatementErrorCode.CASH_FLOW_INIT_REQUIRED);
        }

        //根据全年获取指定项目金额
        VoucherItemPageDto voucherItemPageDtoYear = new VoucherItemPageDto();
        voucherItemPageDtoYear.setYear(Integer.parseInt(dto.getReportDate().substring(0, 4)));
        voucherItemPageDtoYear.setBookId(dto.getBookId());
        if ("month".equals(dto.getPeriodType())) {
            voucherItemPageDtoYear.setEndMonth(dto.getMonth());
        } else if ("quarter".equals(dto.getPeriodType())) {
            voucherItemPageDtoYear.setEndQuarter(dto.getQuarter());
        }
        List<VoucherItemVo> voucherItemVoYears = statementCashFlowMapper.fetchByCashFlowAccumulated(voucherItemPageDtoYear);
        //获取特定科目
        List<StatementCashFlow> statementCashFlowsSpecifyYears = statementCashFlowMapper.fetchSpecifyCashFlow(voucherItemPageDtoYear);
        Map<String, BigDecimal> stringBigDecimalMapSpecifyYears = generateSpecifyCashFlowBalance(statementCashFlowsSpecifyYears);
        Map<String, BigDecimal> stringBigDecimalMapYear = generateCashFlowBalance(voucherItemVoYears);
        stringBigDecimalMapYear.putAll(stringBigDecimalMapSpecifyYears);


        //根据时间范围获取指定项目金额
        VoucherItemPageDto voucherItemPageDto = new VoucherItemPageDto();
        voucherItemPageDto.setBookId(dto.getBookId());
        voucherItemPageDto.setYear(dto.getYear());
        voucherItemPageDto.setMonth(dto.getMonth());
        voucherItemPageDto.setQuarter(dto.getQuarter());
        List<VoucherItemVo> voucherItemVos = statementCashFlowMapper.fetchByCashFlow(voucherItemPageDto);
        //获取特定科目
        List<StatementCashFlow> statementCashFlowsSpecify = statementCashFlowMapper.fetchSpecifyCashFlow(voucherItemPageDto);

        Map<String, BigDecimal> stringBigDecimalMapSpecify = generateSpecifyCashFlowBalance(statementCashFlowsSpecify);
        Map<String, BigDecimal> stringBigDecimalMap = generateCashFlowBalance(voucherItemVos);

        stringBigDecimalMap.putAll(stringBigDecimalMapSpecify);

        //获取利润表净利润
        // 查出所有数据
        LambdaQueryWrapper<StatementIncome> lqw = Wrappers.lambdaQuery();
        lqw.eq(StatementIncome::getYearPeriod, dto.getReportDate());
        lqw.eq(StatementIncome::getPeriodType, dto.getPeriodType());
        lqw.eq(StatementIncome::getBookId, dto.getBookId());
        StatementIncome statementIncome = statementIncomeMapper.selectOne(lqw);

        if (statementIncome != null) {
            LambdaQueryWrapper<StatementIncomeItem> itemlqw = Wrappers.lambdaQuery();
            itemlqw.eq(StatementIncomeItem::getBookId, statementIncome.getBookId());
            itemlqw.eq(StatementIncomeItem::getIncomeId, statementIncome.getId());
            //净利润编码
            itemlqw.eq(StatementIncomeItem::getItemCode, 4);
            StatementIncomeItem statementIncomeItem = statementIncomeItemMapper.selectList(itemlqw).get(0);
            if (Objects.nonNull(statementIncomeItem)) {
                stringBigDecimalMapYear.put("41-xj-jlr", statementIncomeItem.getCumulativeBalance());
                stringBigDecimalMap.put("41-xj-jlr", statementIncomeItem.getCurrentBalance());
            }
        }

        applyIndirectCashFlowAdjustments(
                stringBigDecimalMap,
                stringBigDecimalMapYear,
                dto,
                isSameMonth,
                dto.getDateRangeStart().substring(0, 7));

        //生成报表
        List<StatementCashFlow> statementCashFlows = getStatementCashFlows(configCashFlowBalances, stringBigDecimalMapYear,
                stringBigDecimalMap, isSameYear, isSameMonth, dto.getBookId(), dto.getDateRangeStart().substring(0, 7));

        return Message.ok(statementCashFlows);
    }

    /**
     * 科目余额报表
     *
     * @param dto 查询参数
     * @return 结果
     */
    public Message<List<StatementSubjectBalance>> subjectBalance(StatementParamsDto dto) {
        dto.parse();
        String currentTerm = configSysService.selectConfigByKey(dto.getBookId(), ConstsSysConfig.SYS_PAYMENT_TERM_CURRENT);
        List<String> allMonths = dto.getAllMonths(currentTerm);

        List<StatementSubjectBalance> res = null;
        if (allMonths.size() > 1) {
            res = subjectBalanceMapper.groupCodeSubjectBalance(dto, allMonths,allMonths.get(0), allMonths.get(allMonths.size() - 1));
        } else {
            LambdaQueryWrapper<StatementSubjectBalance> lqw = Wrappers.lambdaQuery();
            lqw.in(StatementSubjectBalance::getYearPeriod, allMonths);
            lqw.eq(StatementSubjectBalance::getBookId, dto.getBookId());
            lqw.eq(Boolean.FALSE.equals(dto.getShowAll()), StatementSubjectBalance::getIsVoucher, YesNoEnum.y.name());
            res = subjectBalanceMapper.selectList(lqw);
        }

//        // 拉取父级数据
//        List<String> subjectIds = new ArrayList<>(res.stream().map(StatementSubjectBalance::getSourceId).toList());
//        List<String> parentIds = res.stream().filter(item -> item.getIsAuxiliary().equals(YesNoEnum.y.name()))
//                .map(StatementSubjectBalance::getParentId).toList();
//        subjectIds.addAll(parentIds);
//        if (!subjectIds.isEmpty()) {
//            LambdaQueryWrapper<BookSubject> lqwSubject = Wrappers.lambdaQuery();
//            lqwSubject.eq(BookSubject::getBookId, dto.getBookId());
//            List<BookSubject> bookSubjects = bookSubjectMapper.selectList(lqwSubject);
//            // 找出所有父级
//            Set<String> subjectPaths = new HashSet<>();
//            bookSubjects.forEach(bookSubject -> {
//                for (String subjectId : subjectIds) {
//                    if (bookSubject.getIdPath().contains(subjectId)) {
//                        subjectPaths.addAll(List.of(bookSubject.getIdPath().split("/")));
//                    }
//                }
//            });
//            // 创建父级
//            List<StatementSubjectBalance> balanceList = bookSubjects.stream()
//                    .filter(bookSubject -> subjectPaths.contains(bookSubject.getId()))
//                    .map(subject -> subjectBalanceService.create(subject, dto.getReportDate()))
//                    .toList();
//            // 合并
//            Set<String> existingSourceIds = res.stream().map(StatementSubjectBalance::getSourceId).collect(Collectors.toSet());
//            for (StatementSubjectBalance item : balanceList) {
//                if (!existingSourceIds.contains(item.getSourceId())) {
//                    res.add(item);
//                }
//            }
//        }
//        counterBalance(res);

        // 按照科目编号升序排列
        res.sort(Comparator.comparing(StatementSubjectBalance::getSubjectCode));

//        if (!dto.getShowAux()) {
//            res = res.stream().filter(item -> item.getIsAuxiliary().equals(YesNoEnum.n.name())).toList();
//        }
        return new Message<>(res);
    }

    /**
     * 报表-凭证汇总
     *
     * @param dto 查询参数
     * @return 结果
     */
    public Message<List<StatementSubjectBalance>> voucherSummary(StatementParamsDto dto) {
        dto.parse();
        List<StatementSubjectBalance> res = voucherItemMapper.voucherSubjectBalanceSummary(dto);

        return Message.ok(res);
    }

    /**
     * @Description: 生成报表数据
     * @Param: [configCashFlowBalances]
     * @return: java.util.List<com.financial.cloud.domain.statement.StatementCashFlow>
     * @Author: xZen
     * @Date: 2025/4/3 17:28
     */
    private List<StatementCashFlow> getStatementCashFlows(List<ConfigCashFlowBalance> configCashFlowBalances, Map<String, BigDecimal> stringBigDecimalMapYear,
                                                          Map<String, BigDecimal> stringBigDecimalMap,
                                                          Boolean sameYear, Boolean isSameMonth, String bookId, String reportDate) {
        List<StatementCashFlow> statementCashFlows = new ArrayList<>();
        for (ConfigCashFlowBalance configCashFlowBalance : configCashFlowBalances) {
            StatementCashFlow statementCashFlow = new StatementCashFlow();
            statementCashFlow.setItemName(configCashFlowBalance.getItemName());
            statementCashFlow.setSortIndex(configCashFlowBalance.getSortIndex());
            statementCashFlow.setMonthlyAmount(stringBigDecimalMap.get(configCashFlowBalance.getItemCode()));
            if (sameYear) {
                statementCashFlow.setCurrentAmount(
                        Optional.ofNullable(stringBigDecimalMapYear.get(configCashFlowBalance.getItemCode()))
                                .orElse(BigDecimal.ZERO)
                                .add(configCashFlowBalance.getBalance())
                );
            } else {
                statementCashFlow.setCurrentAmount(stringBigDecimalMapYear.get(configCashFlowBalance.getItemCode()));
            }
            statementCashFlow.setItemCode(configCashFlowBalance.getItemCode());
            statementCashFlow.setIsTitle(configCashFlowBalance.getIsTitle());
            statementCashFlow.setIsAdditional(configCashFlowBalance.getIsAdditional());
            statementCashFlow.setIsMain(configCashFlowBalance.getIsMain());
            statementCashFlow.setIsResult(configCashFlowBalance.getIsResult());
            statementCashFlows.add(statementCashFlow);
        }

        //设置期初余额
        //获取科目初始余额
        LambdaQueryWrapper<BookInitBalance> wrapperBookInit = new LambdaQueryWrapper<>();
        wrapperBookInit.eq(BookInitBalance::getBookId, bookId);
        wrapperBookInit.eq(BookInitBalance::getLevel, 1);
        wrapperBookInit.eq(BookInitBalance::getIsCash, 1);
        List<BookInitBalance> bookInitBalances = bookInitBalanceMapper.selectList(wrapperBookInit);
        BigDecimal yearBalanceBeginning = bookInitBalances.stream()
                .map(BookInitBalance::getBalance)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        //期末余额
        BigDecimal endingBalance = BigDecimal.ZERO;
        if (Boolean.FALSE.equals(isSameMonth)) {
            endingBalance = Optional.ofNullable(
                            settlementMapper.selectOne(Wrappers.<Settlement>lambdaQuery()
                                    .eq(Settlement::getBookId, bookId)
                                    .eq(Settlement::getYearPeriod, getPreviousMonth(reportDate)))
                    ).map(Settlement::getEndingBalance)
                    .orElse(BigDecimal.ZERO);

        }

        BigDecimal amountTerm = yearBalanceBeginning;
        BigDecimal amountYear = yearBalanceBeginning;
        //如果为第一期，期初余额和年初余额从科目初始余额中获取

        if (Boolean.FALSE.equals(isSameMonth)) {
            //如果不是第一期但是第一年，期初余额取上一期的期末余额，年初余额保存不变
            amountTerm = endingBalance;
            if (Boolean.FALSE.equals(sameYear)) {
                //既不是第一期也不是第一年,期初余额是上一月的期末金额，年初余额上一年的最后一期的期末金额
                amountYear = Optional.ofNullable(
                                settlementMapper.selectOne(Wrappers.<Settlement>lambdaQuery()
                                        .eq(Settlement::getBookId, bookId)
                                        .eq(Settlement::getYearPeriod, getLastMonthOfPreviousYear(reportDate)))
                        ).map(Settlement::getEndingBalance)
                        .orElse(BigDecimal.ZERO);
            }
        }

        // 第二步：计算小计和净额
        Map<String, BigDecimal> monthlyResults = StatementCashFlowRules.calculateSubtotalsAndNetAmounts(
                statementCashFlows, true, amountTerm);
        Map<String, BigDecimal> yearlyResults = StatementCashFlowRules.calculateSubtotalsAndNetAmounts(
                statementCashFlows, false, amountYear);

        // 第三步：更新小计和净额值
        for (StatementCashFlow flow : statementCashFlows) {
            String code = flow.getItemCode();
            // 检查项目是否为小计或净额
            if (flow.getIsResult() == 1 || "56-xj-qita".equals(flow.getItemCode())) {
                flow.setMonthlyAmount(monthlyResults.get(code));
                flow.setCurrentAmount(yearlyResults.get(code));
            }
        }

        validateCashFlowReconciliation(monthlyResults);

        return statementCashFlows;
    }

    private void applyIndirectCashFlowAdjustments(
            Map<String, BigDecimal> periodMap,
            Map<String, BigDecimal> yearMap,
            StatementParamsDto dto,
            Boolean firstBookPeriod,
            String reportYearPeriod) {
        String priorPeriod = getPreviousMonth(reportYearPeriod);
        Map<String, StatementCashFlowIndirectRules.ReportLineBalance> reportLines =
                balanceSheetService.computeReportLineBalances(dto.getBookId(), reportYearPeriod);
        Map<String, StatementCashFlowIndirectRules.ReportLineBalance> priorLines = Boolean.TRUE.equals(firstBookPeriod)
                ? reportLines
                : balanceSheetService.computeReportLineBalances(dto.getBookId(), priorPeriod);

        StatementCashFlowIndirectRules.WorkingCapitalChanges workingCapital =
                StatementCashFlowIndirectRules.computeWorkingCapitalChanges(
                        reportLines, priorLines, Boolean.TRUE.equals(firstBookPeriod));

        periodMap.put(CashFlowItemEnum.REDUCE_INVENTORY.getDbCode(), workingCapital.inventoryChangePeriod());
        periodMap.put(CashFlowItemEnum.DECREASE_OPERATING_RECEIVABLES.getDbCode(), workingCapital.receivableChangePeriod());
        periodMap.put(CashFlowItemEnum.INCREASE_OPERATING_PAYABLE.getDbCode(), workingCapital.payableChangePeriod());
        yearMap.put(CashFlowItemEnum.REDUCE_INVENTORY.getDbCode(), workingCapital.inventoryChangeYear());
        yearMap.put(CashFlowItemEnum.DECREASE_OPERATING_RECEIVABLES.getDbCode(), workingCapital.receivableChangeYear());
        yearMap.put(CashFlowItemEnum.INCREASE_OPERATING_PAYABLE.getDbCode(), workingCapital.payableChangeYear());

        List<StatementSubjectBalance> subjectBalances = loadIndirectSubjectBalances(dto);
        BigDecimal financialExpensePeriod = resolveIncomeItemAmount(dto, ConstsSysConfig.SYS_DEFAULT_FINANCIAL_EXPENSES, false);
        BigDecimal financialExpenseYear = resolveIncomeItemAmount(dto, ConstsSysConfig.SYS_DEFAULT_FINANCIAL_EXPENSES, true);
        BigDecimal investmentIncomePeriod = resolveIncomeItemAmount(
                dto, StatementCashFlowIndirectRules.INVESTMENT_INCOME_INCOME_ITEM_CODE, false);
        BigDecimal investmentIncomeYear = resolveIncomeItemAmount(
                dto, StatementCashFlowIndirectRules.INVESTMENT_INCOME_INCOME_ITEM_CODE, true);

        StatementCashFlowIndirectRules.SupplementaryAdjustments supplementary =
                StatementCashFlowIndirectRules.computeSupplementaryAdjustments(
                        subjectBalances,
                        reportLines,
                        priorLines,
                        Boolean.TRUE.equals(firstBookPeriod),
                        financialExpensePeriod,
                        financialExpenseYear,
                        investmentIncomePeriod,
                        investmentIncomeYear);

        periodMap.put(CashFlowItemEnum.PROVISION_ASSET_IMPAIRMENT.getDbCode(), supplementary.assetImpairmentPeriod());
        periodMap.put(CashFlowItemEnum.AMORTIZATION_INTANGIBLE_ASSETS.getDbCode(), supplementary.amortizationIntangiblePeriod());
        periodMap.put(CashFlowItemEnum.AMORTIZATION_LONGTERM_DEFERRED_EXPENSES.getDbCode(),
                supplementary.amortizationDeferredExpensePeriod());
        periodMap.put(CashFlowItemEnum.FINANCIAL_EXPENSES.getDbCode(), supplementary.financialExpensePeriod());
        periodMap.put(CashFlowItemEnum.INVESTMENT_LOSSES.getDbCode(), supplementary.investmentLossPeriod());
        periodMap.put(CashFlowItemEnum.DECREASE_DEFERRED_TAX_ASSETS.getDbCode(), supplementary.deferredTaxAssetDecreasePeriod());
        periodMap.put(CashFlowItemEnum.INCREASE_DEFERRED_TAX_LIABILITIES.getDbCode(),
                supplementary.deferredTaxLiabilityIncreasePeriod());
        periodMap.put(CashFlowItemEnum.EXCHANGE_RATE_EFFECT.getDbCode(), supplementary.exchangeRateEffectPeriod());
        periodMap.put(CashFlowItemEnum.DEPRECIATION_FIXED_ASSETS.getDbCode(),
                StatementCashFlowIndirectRules.sumDepreciationCredit(subjectBalances, false));

        yearMap.put(CashFlowItemEnum.PROVISION_ASSET_IMPAIRMENT.getDbCode(), supplementary.assetImpairmentYear());
        yearMap.put(CashFlowItemEnum.AMORTIZATION_INTANGIBLE_ASSETS.getDbCode(), supplementary.amortizationIntangibleYear());
        yearMap.put(CashFlowItemEnum.AMORTIZATION_LONGTERM_DEFERRED_EXPENSES.getDbCode(),
                supplementary.amortizationDeferredExpenseYear());
        yearMap.put(CashFlowItemEnum.FINANCIAL_EXPENSES.getDbCode(), supplementary.financialExpenseYear());
        yearMap.put(CashFlowItemEnum.INVESTMENT_LOSSES.getDbCode(), supplementary.investmentLossYear());
        yearMap.put(CashFlowItemEnum.DECREASE_DEFERRED_TAX_ASSETS.getDbCode(), supplementary.deferredTaxAssetDecreaseYear());
        yearMap.put(CashFlowItemEnum.INCREASE_DEFERRED_TAX_LIABILITIES.getDbCode(),
                supplementary.deferredTaxLiabilityIncreaseYear());
        yearMap.put(CashFlowItemEnum.EXCHANGE_RATE_EFFECT.getDbCode(), supplementary.exchangeRateEffectYear());
        yearMap.put(CashFlowItemEnum.DEPRECIATION_FIXED_ASSETS.getDbCode(),
                StatementCashFlowIndirectRules.sumDepreciationCredit(subjectBalances, true));
    }

    private BigDecimal resolveIncomeItemAmount(StatementParamsDto dto, String configOrItemCode, boolean cumulative) {
        String itemCode = configOrItemCode;
        if (itemCode.startsWith("sys.")) {
            itemCode = configSysService.selectConfigByKey(dto.getBookId(), configOrItemCode);
        }
        if (itemCode == null || itemCode.isBlank()) {
            return BigDecimal.ZERO;
        }
        StatementIncome income = statementIncomeService.getIncomeStatement(dto, true).getData();
        if (income == null || income.getItems() == null) {
            return BigDecimal.ZERO;
        }
        for (StatementIncomeItem item : income.getItems()) {
            if (itemCode.equals(item.getItemCode())) {
                return cumulative
                        ? defaultZero(item.getCumulativeBalance())
                        : defaultZero(item.getCurrentBalance());
            }
        }
        return BigDecimal.ZERO;
    }

    private List<StatementSubjectBalance> loadIndirectSubjectBalances(StatementParamsDto dto) {
        String currentTerm = configSysService.selectConfigByKey(dto.getBookId(), ConstsSysConfig.SYS_PAYMENT_TERM_CURRENT);
        List<String> allMonths = dto.getAllMonths(currentTerm);
        Set<String> codes = SubjectCodeCompat.expandLookupCodes(StatementCashFlowIndirectRules.INDIRECT_SUBJECT_ROOTS);
        if (allMonths.size() > 1) {
            return subjectBalanceMapper.groupCodeSubjectBalance(
                    dto, allMonths, allMonths.get(0), allMonths.get(allMonths.size() - 1)).stream()
                    .filter(row -> codes.contains(row.getSubjectCode()))
                    .toList();
        }
        LambdaQueryWrapper<StatementSubjectBalance> lqw = Wrappers.lambdaQuery();
        lqw.in(StatementSubjectBalance::getYearPeriod, allMonths);
        lqw.eq(StatementSubjectBalance::getBookId, dto.getBookId());
        lqw.in(StatementSubjectBalance::getSubjectCode, codes);
        return subjectBalanceMapper.selectList(lqw);
    }

    private static BigDecimal defaultZero(BigDecimal value) {
        return value != null ? value : BigDecimal.ZERO;
    }

    private void validateCashFlowReconciliation(Map<String, BigDecimal> monthlyResults) {
        if (!strictCashFlowReconciliation) {
            return;
        }
        BigDecimal directNet = monthlyResults.get(CashFlowItemEnum.OPERATING_CASH_NET.getDbCode());
        BigDecimal indirectNet = monthlyResults.get(CashFlowItemEnum.OPERATING_CASH_NET_SECOND.getDbCode());
        if (StatementCashFlowRules.isWithinReconciliationTolerance(directNet, indirectNet)) {
            return;
        }
        throw new ServiceException(
                StatementErrorCode.CASH_FLOW_RECONCILIATION_FAILED,
                StatementCashFlowRules.reconciliationDiff(directNet, indirectNet).abs(),
                directNet,
                indirectNet);
    }

    private Map<String, BigDecimal> generateSpecifyCashFlowBalance(List<StatementCashFlow> statementCashFlows) {
        Map<String, BigDecimal> resultMap = new HashMap<>();

        for (StatementCashFlow scf : statementCashFlows) {
            String key = scf.getItemCode();
            BigDecimal value = scf.getMonthlyAmount();

            resultMap.merge(key, value, BigDecimal::add);
        }

        return resultMap;
    }

    private Map<String, BigDecimal> generateCashFlowBalance(List<VoucherItemVo> voucherItems) {
        Map<String, BigDecimal> cashFlowMap = new HashMap<>();

        for (VoucherItemVo item : voucherItems) {
            String code = item.getCashFlowItemCode();
            BigDecimal balance = item.getCashFlowBalance();

            cashFlowMap.compute(code, (k, v) -> (v == null) ? balance : v.add(balance));
        }

        return cashFlowMap;
    }

    private String getPreviousMonth(String reportDate) {
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM");
        YearMonth current = YearMonth.parse(reportDate, formatter);
        YearMonth previous = current.minusMonths(1);
        return previous.format(formatter);
    }

    public String getLastMonthOfPreviousYear(String reportDate) {
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM");
        YearMonth current = YearMonth.parse(reportDate, formatter);
        YearMonth lastMonthOfPrevYear = YearMonth.of(current.getYear() - 1, 12);
        return lastMonthOfPrevYear.format(formatter);
    }
}
