package com.financial.cloud.service.book;

import cn.hutool.core.bean.BeanUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.incrementer.IdentifierGenerator;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.financial.cloud.constants.system.ConstsSysConfig;
import com.financial.cloud.common.ExcelImport;
import com.financial.cloud.common.Message;
import com.financial.cloud.domain.book.BookInitBalance;
import com.financial.cloud.dto.book.BookInitBalanceChangeDto;
import com.financial.cloud.dto.book.BookInitBalanceImportResultVo;
import com.financial.cloud.dto.book.BookInitBalancePageDto;
import com.financial.cloud.dto.book.BookInitBalanceVo;
import com.financial.cloud.domain.book.BookSubject;
import com.financial.cloud.dto.book.SubjectPageDto;
import com.financial.cloud.domain.statement.StatementSubjectBalance;
import com.financial.cloud.enums.statement.StatementPeriodTypeEnum;
import com.financial.cloud.enums.book.SubjectDirectionEnum;
import com.financial.cloud.enums.common.YesNoEnum;
import com.financial.cloud.repository.book.BookInitBalanceMapper;
import com.baomidou.mybatisplus.extension.toolkit.Db;
import com.financial.cloud.repository.book.BookSubjectMapper;
import com.financial.cloud.repository.statement.StatementSubjectBalanceMapper;
import com.financial.cloud.service.config.ConfigSysService;
import com.financial.cloud.service.statement.StatementSubjectBalanceService;
import com.financial.cloud.util.ExcelUtils;
import com.financial.cloud.util.excel.ExcelExporter;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.apache.commons.lang3.StringUtils;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.IOException;
import java.math.BigDecimal;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.stream.Collectors;

@RequiredArgsConstructor
@Service
public class BookInitBalanceService extends ServiceImpl<BookInitBalanceMapper, BookInitBalance>{

    private final BookInitBalanceMapper bookInitBalanceMapper;
    private final IdentifierGenerator identifierGenerator;
    private final BookSubjectMapper bookSubjectMapper;
    private final StatementSubjectBalanceMapper statementSubjectBalanceMapper;
    private final StatementSubjectBalanceService statementSubjectBalanceService;
    private final ConfigSysService configSysService;

    /**
     * 分页查询
     *
     * @param dto 分页参数
     * @return 查询结果
     */
    public Message<List<BookInitBalanceVo>> list(BookInitBalancePageDto dto) {
        LambdaQueryWrapper<BookInitBalance> lqw = Wrappers.lambdaQuery();
        lqw.eq(dto.getCategory() != null, BookInitBalance::getCategory, dto.getCategory());
        lqw.eq(BookInitBalance::getBookId, dto.getBookId());
        lqw.likeRight(StringUtils.isNotBlank(dto.getCode()), BookInitBalance::getCode, dto.getCode());
        lqw.likeRight(StringUtils.isNotBlank(dto.getName()), BookInitBalance::getName, dto.getName());
        List<BookInitBalanceVo> result = BeanUtil.copyToList(bookInitBalanceMapper.selectList(lqw), BookInitBalanceVo.class);
        Map<String, BookInitBalanceVo> mapRes = new HashMap<>();
        for (BookInitBalanceVo bookInitBalanceVo : result) {
            mapRes.put(bookInitBalanceVo.getCode(), bookInitBalanceVo);
        }

        // 取所有会计科目，合并到已有初始配置中
        SubjectPageDto subjectPageDto = new SubjectPageDto();
        subjectPageDto.setBookId(dto.getBookId());
        subjectPageDto.setCategory(dto.getCategory());
        Page<BookSubject> subjectPage = bookSubjectMapper.pageListByBook(new Page<>(1, 10000), subjectPageDto);
        for (BookSubject bookSubject : subjectPage.getRecords()) {
            BookInitBalanceVo bookInitBalanceVo = mapRes.get(bookSubject.getCode());
            if (bookInitBalanceVo == null) {
                bookInitBalanceVo = new BookInitBalanceVo();
                BeanUtil.copyProperties(bookSubject, bookInitBalanceVo);
                bookInitBalanceVo.setBalance(BigDecimal.ZERO);
                bookInitBalanceVo.setCreditAmount(BigDecimal.ZERO);
                bookInitBalanceVo.setDebitAmount(BigDecimal.ZERO);
                bookInitBalanceVo.setOpeningYearBalanceDebit(BigDecimal.ZERO);
                bookInitBalanceVo.setOpeningYearBalanceCredit(BigDecimal.ZERO);
                bookInitBalanceVo.setOriginId(bookSubject.getId());
                bookInitBalanceVo.setId(null);
                bookInitBalanceVo.setCreatedBy(null);
                bookInitBalanceVo.setCreatedDate(null);
                bookInitBalanceVo.setModifiedDate(null);
                bookInitBalanceVo.setModifiedBy(null);
                bookInitBalanceVo.setHasVoucher(false);
                result.add(bookInitBalanceVo);
                mapRes.put(bookSubject.getCode(), bookInitBalanceVo);
            }
        }

        for (BookInitBalanceVo bookInitBalanceVo : result) {
            if (StringUtils.isBlank(bookInitBalanceVo.getOriginId())) {
                bookInitBalanceVo.setOriginId(bookInitBalanceVo.getId());
            }
        }

        // 比对科目和凭证使用情况，对于已使用凭证的科目不能再次修改
        List<String> codes = result.stream().map(BookInitBalanceVo::getCode).toList();
        List<StatementSubjectBalance> statementSubjectBalances = statementSubjectBalanceService.selectIsVoucherByCode(dto.getBookId(), codes);
        for (StatementSubjectBalance statementSubjectBalance : statementSubjectBalances) {
            BookInitBalanceVo bookInitBalanceVo = mapRes.get(statementSubjectBalance.getSubjectCode());
            if (bookInitBalanceVo != null) {
                bookInitBalanceVo.setHasVoucher(true);
            }
        }

        result = result.stream()
                .sorted(Comparator.comparing(BookInitBalanceVo::getCode))
                .collect(Collectors.toList());

        return Message.ok(result);
    }

    /**
     * 插入数据,同步科目表、科目余额表数据
     *
     * @param dtos 插入对象
     * @return 插入结果
     */
    @Transactional
    public Message<String> save(List<BookInitBalanceChangeDto> dtos) {
        String currentTerm = configSysService.getCurrentTerm(dtos.get(0).getBookId());
        String initializeTask = configSysService.selectConfigByKey(dtos.get(0).getBookId(), ConstsSysConfig.SYS_INITIALIZE_TASK);
        if ("true".equals(initializeTask)) {
            return Message.failed("当前不允许操作，初始化已完成");
        }

        List<BookInitBalance> bookInitBalances = new ArrayList<>();
        // 所有孩子数据
        Map<String, List<BookInitBalance>> balanceMap = new HashMap<>();
        // 用于同步父级ID
        Map<String, String> originIdMap = new HashMap<>();
        dtos = dtos.stream().peek(dto -> {
            if (dto.getBalance() == null) {
                dto.setBalance(BigDecimal.ZERO);
            }
            if (dto.getOpeningYearBalanceDebit() == null) {
                dto.setOpeningYearBalanceDebit(BigDecimal.ZERO);
            }
            if (dto.getOpeningYearBalanceCredit() == null) {
                dto.setOpeningYearBalanceCredit(BigDecimal.ZERO);
            }
            if (dto.getDebitAmount() == null) {
                dto.setDebitAmount(BigDecimal.ZERO);
            }
            if (dto.getCreditAmount() == null) {
                dto.setCreditAmount(BigDecimal.ZERO);
            }
        }).toList();
        dtos.stream().filter(dto -> dto.getParentId() != null)
                .forEach(dto -> {
                    BookInitBalance bookInitBalance = BookInitBalance.builder().build();
                    BeanUtil.copyProperties(dto, bookInitBalance);

                    List<BookInitBalance> list = balanceMap.getOrDefault(bookInitBalance.getParentId(), new ArrayList<>());
                    list.add(bookInitBalance);
                    balanceMap.put(bookInitBalance.getParentId(), list);

                    bookInitBalances.add(bookInitBalance);
                    originIdMap.put(dto.getCode(), dto.getOriginId());
                });
        dtos.stream().filter(dto -> dto.getParentId() == null).forEach(dto -> {
            BookInitBalance bookInitBalance = BookInitBalance.builder().build();
            BeanUtil.copyProperties(dto, bookInitBalance);
            bookInitBalances.add(bookInitBalance);
            originIdMap.put(dto.getCode(), dto.getOriginId());
        });
        // 如果ID不存在，则生成新的ID，同步更新父级ID
        for (BookInitBalance bookInitBalance : bookInitBalances) {
            if (bookInitBalance.getId() == null) {
                String currentId = identifierGenerator.nextId(bookInitBalance).toString();
                bookInitBalance.setId(currentId);
                List<BookInitBalance> children = balanceMap.getOrDefault(originIdMap.get(bookInitBalance.getCode()), new ArrayList<>());
                for (BookInitBalance child : children) {
                    child.setParentId(currentId);
                }
            }
        }
        // 更新科目余额：必须带上 BookSubject.id，否则 source_id 为空会被 idPath 空段误匹配
        List<String> codes = bookInitBalances.stream().map(BookInitBalance::getCode).toList();
        String bookId = dtos.get(0).getBookId();
        Map<String, BookSubject> map = bookSubjectMapper.selectList(
                        Wrappers.<BookSubject>lambdaQuery()
                                .eq(BookSubject::getBookId, bookId)
                                .in(BookSubject::getCode, codes))
                .stream()
                .collect(Collectors.toMap(BookSubject::getCode, item -> item, (a, b) -> a));

        // 更新科目余额表
        LambdaQueryWrapper<StatementSubjectBalance> balanceLqw = Wrappers.lambdaQuery();
        balanceLqw.in(StatementSubjectBalance::getSubjectCode, codes);
        balanceLqw.eq(StatementSubjectBalance::getBookId, bookId);
        balanceLqw.eq(StatementSubjectBalance::getPeriodType, StatementPeriodTypeEnum.MONTH.getValue());
        balanceLqw.eq(StatementSubjectBalance::getYearPeriod, currentTerm);
        List<StatementSubjectBalance> balances = statementSubjectBalanceMapper.selectList(balanceLqw);
        Map<String, StatementSubjectBalance> mapBalance = balances.stream()
                .collect(Collectors.toMap(StatementSubjectBalance::getSubjectCode, bookSubject -> bookSubject));
        List<StatementSubjectBalance> updateBalances = new ArrayList<>();
        for (BookInitBalance bookInitBalance : bookInitBalances) {
            StatementSubjectBalance balance = mapBalance.get(bookInitBalance.getCode());
            BookSubject bookSubject = map.get(bookInitBalance.getCode());
            // 已经存在凭证，不再更新
            if (balance != null && YesNoEnum.y.name().equals(balance.getIsVoucher())) {
                continue;
            } else if (balance == null) {
                balance = StatementSubjectBalance.builder()
                        .bookId(bookInitBalance.getBookId())
                        .periodType(StatementPeriodTypeEnum.MONTH.getValue())
                        .yearPeriod(currentTerm)
                        .sourceId(bookSubject != null ? bookSubject.getId() : null)
                        .parentId(bookSubject != null ? bookSubject.getParentId() : null)
                        .subjectCode(bookInitBalance.getCode())
                        .subjectName(bookInitBalance.getName())
                        .direction(bookInitBalance.getDirection())
                        .isAuxiliary(YesNoEnum.n.name())
                        .isVoucher(YesNoEnum.n.name())
                        .build();
                String currentId = identifierGenerator.nextId(balance).toString();
                balance.setId(currentId);
            }
            if (bookSubject != null) {
                balance.setSourceId(bookSubject.getId());
                balance.setParentId(bookSubject.getParentId());
            }
            // 年初
            balance.setOpeningYearBalanceDebit(bookInitBalance.getOpeningYearBalanceDebit());
            balance.setOpeningYearBalanceCredit(bookInitBalance.getOpeningYearBalanceCredit());
            // 本年累计
            balance.setYearToDateDebit(bookInitBalance.getDebitAmount());
            balance.setYearToDateCredit(bookInitBalance.getCreditAmount());
            // 期初余额
            balance.setOpeningBalanceDebit(bookInitBalance.getOpeningYearBalanceDebit()
                    .add(bookInitBalance.getDebitAmount()));
            balance.setOpeningBalanceCredit(bookInitBalance.getOpeningYearBalanceCredit()
                    .add(bookInitBalance.getCreditAmount()));
            // 本期发生额
            balance.setCurrentPeriodDebit(BigDecimal.ZERO);
            balance.setCurrentPeriodCredit(BigDecimal.ZERO);

            // 最终余额,其余余额更新
            balance.setBalance(bookInitBalance.getBalance());
            balance.setClosingBalanceCredit(BigDecimal.ZERO);
            balance.setClosingBalanceDebit(BigDecimal.ZERO);
            if (SubjectDirectionEnum.DEBIT.getValue().equals(balance.getDirection())) {
                balance.setClosingBalanceDebit(balance.getOpeningBalanceDebit()
                        .subtract(balance.getOpeningBalanceCredit()));
            } else {
                balance.setClosingBalanceCredit(balance.getOpeningBalanceCredit()
                        .subtract(balance.getOpeningBalanceDebit()));
            }
            
            //上月期末余额
            balance.setPrevBalance(
                    balance.getPrevBalance() != null ? balance.getPrevBalance() : balance.getBalance());
            //上月期末借贷余额
            balance.setPrevClosingBalanceDebit(
                    balance.getPrevClosingBalanceDebit() != null
                            ? balance.getPrevClosingBalanceDebit()
                            : Optional.ofNullable(balance.getClosingBalanceDebit()).orElse(BigDecimal.ZERO));
            balance.setPrevClosingBalanceCredit(
                    balance.getPrevClosingBalanceCredit() != null
                            ? balance.getPrevClosingBalanceCredit()
                            : Optional.ofNullable(balance.getClosingBalanceCredit()).orElse(BigDecimal.ZERO));
            //上月期末年度累计
            balance.setPrevYearToDateDebit(
                    balance.getPrevYearToDateDebit() != null
                            ? balance.getPrevYearToDateDebit()
                            : Optional.ofNullable(balance.getYearToDateDebit()).orElse(BigDecimal.ZERO));
            balance.setPrevYearToDateCredit(
                    balance.getPrevYearToDateCredit() != null
                            ? balance.getPrevYearToDateCredit()
                            : Optional.ofNullable(balance.getYearToDateCredit()).orElse(BigDecimal.ZERO));
            
            updateBalances.add(balance);
        }
        Db.saveOrUpdateBatch(updateBalances);

        boolean save = Db.saveOrUpdateBatch(bookInitBalances);
        return save ? new Message<>(Message.SUCCESS, "保存成功") : new Message<>(Message.FAIL, "保存失败");
    }

    private static final String[] EXPORT_HEADERS = {
            "科目编码", "科目名称", "方向", "年初余额借方", "年初余额贷方", "本年累计借方", "本年累计贷方", "余额"
    };

    public void export(BookInitBalancePageDto dto, HttpServletResponse response) throws IOException {
        Message<List<BookInitBalanceVo>> listed = list(dto);
        List<BookInitBalanceVo> rows = listed.getData() == null ? List.of() : listed.getData();
        try (Workbook workbook = new XSSFWorkbook()) {
            Sheet sheet = workbook.createSheet("期初余额");
            Row header = sheet.createRow(0);
            for (int i = 0; i < EXPORT_HEADERS.length; i++) {
                header.createCell(i).setCellValue(EXPORT_HEADERS[i]);
            }
            int rowIndex = 1;
            for (BookInitBalanceVo vo : rows) {
                Row row = sheet.createRow(rowIndex++);
                int col = 0;
                row.createCell(col++).setCellValue(StringUtils.defaultString(vo.getCode()));
                row.createCell(col++).setCellValue(StringUtils.defaultString(vo.getName()));
                row.createCell(col++).setCellValue(StringUtils.defaultString(vo.getDirection()));
                setAmountCell(row, col++, vo.getOpeningYearBalanceDebit());
                setAmountCell(row, col++, vo.getOpeningYearBalanceCredit());
                setAmountCell(row, col++, vo.getDebitAmount());
                setAmountCell(row, col++, vo.getCreditAmount());
                setAmountCell(row, col, vo.getBalance());
            }
            for (int i = 0; i < EXPORT_HEADERS.length; i++) {
                sheet.autoSizeColumn(i);
            }
            response.setContentType(ExcelExporter.APPLICATION_MS_EXCEL);
            response.setHeader("Content-Disposition", "attachment; filename="
                    + URLEncoder.encode("期初余额.xlsx", StandardCharsets.UTF_8));
            workbook.write(response.getOutputStream());
            response.getOutputStream().flush();
        }
    }

    public void downloadImportTemplate(HttpServletResponse response) throws IOException {
        try (Workbook workbook = new XSSFWorkbook()) {
            Sheet sheet = workbook.createSheet("期初余额导入");
            Row header = sheet.createRow(0);
            for (int i = 0; i < EXPORT_HEADERS.length; i++) {
                header.createCell(i).setCellValue(EXPORT_HEADERS[i]);
            }
            Row sample = sheet.createRow(1);
            sample.createCell(0).setCellValue("1001");
            sample.createCell(1).setCellValue("库存现金");
            sample.createCell(2).setCellValue("1");
            sample.createCell(3).setCellValue(10000);
            sample.createCell(4).setCellValue(0);
            sample.createCell(5).setCellValue(0);
            sample.createCell(6).setCellValue(0);
            sample.createCell(7).setCellValue(10000);
            for (int i = 0; i < EXPORT_HEADERS.length; i++) {
                sheet.autoSizeColumn(i);
            }
            response.setContentType(ExcelExporter.APPLICATION_MS_EXCEL);
            response.setHeader("Content-Disposition", "attachment; filename="
                    + URLEncoder.encode("期初余额导入模板.xlsx", StandardCharsets.UTF_8));
            workbook.write(response.getOutputStream());
            response.getOutputStream().flush();
        }
    }

    @Transactional
    public Message<BookInitBalanceImportResultVo> importFromExcel(String bookId, ExcelImport excelImportFile) {
        BookInitBalanceImportResultVo result = new BookInitBalanceImportResultVo();
        if (StringUtils.isBlank(bookId)) {
            return Message.failed("所属账套ID不能为空");
        }
        String initializeTask = configSysService.selectConfigByKey(bookId, ConstsSysConfig.SYS_INITIALIZE_TASK);
        if ("true".equals(initializeTask)) {
            return Message.failed("当前不允许操作，初始化已完成");
        }
        if (excelImportFile == null || !excelImportFile.isExcelNotEmpty()) {
            result.setFailed(1);
            BookInitBalanceImportResultVo.RowError err = new BookInitBalanceImportResultVo.RowError();
            err.setRow(0);
            err.setMessage("请上传 Excel 文件");
            result.getErrors().add(err);
            return new Message<>(Message.FAIL, "导入失败", result);
        }

        BookInitBalancePageDto pageDto = new BookInitBalancePageDto();
        pageDto.setBookId(bookId);
        List<BookInitBalanceVo> current = Optional.ofNullable(list(pageDto).getData()).orElse(List.of());
        Map<String, BookInitBalanceVo> byCode = current.stream()
                .filter(v -> StringUtils.isNotBlank(v.getCode()))
                .collect(Collectors.toMap(BookInitBalanceVo::getCode, v -> v, (a, b) -> a));
        Set<String> parentOriginIds = current.stream()
                .map(BookInitBalanceVo::getParentId)
                .filter(StringUtils::isNotBlank)
                .collect(Collectors.toSet());

        List<BookInitBalanceChangeDto> toSave = new ArrayList<>();
        try {
            Workbook workbook = excelImportFile.biuldWorkbook();
            Sheet sheet = workbook.getSheetAt(0);
            int last = sheet.getLastRowNum();
            for (int r = 1; r <= last; r++) {
                Row row = sheet.getRow(r);
                if (row == null || isBlankRow(row)) {
                    continue;
                }
                int excelRow = r + 1;
                String code = StringUtils.trimToEmpty(ExcelUtils.getValue(row, 0));
                if (StringUtils.isBlank(code)) {
                    addImportError(result, excelRow, code, "科目编码不能为空");
                    continue;
                }
                BookInitBalanceVo existing = byCode.get(code);
                if (existing == null) {
                    addImportError(result, excelRow, code, "科目编码不存在于当前账套");
                    continue;
                }
                if (existing.isHasVoucher()) {
                    addImportError(result, excelRow, code, "科目已有凭证，不允许改期初");
                    continue;
                }
                String originId = StringUtils.defaultIfBlank(existing.getOriginId(), existing.getId());
                if (parentOriginIds.contains(originId)) {
                    addImportError(result, excelRow, code, "非末级科目，请只导入末级科目金额");
                    continue;
                }
                BookInitBalanceChangeDto dto = new BookInitBalanceChangeDto();
                BeanUtil.copyProperties(existing, dto);
                dto.setBookId(bookId);
                dto.setOpeningYearBalanceDebit(parseAmount(ExcelUtils.getValue(row, 3)));
                dto.setOpeningYearBalanceCredit(parseAmount(ExcelUtils.getValue(row, 4)));
                dto.setDebitAmount(parseAmount(ExcelUtils.getValue(row, 5)));
                dto.setCreditAmount(parseAmount(ExcelUtils.getValue(row, 6)));
                BigDecimal balance = parseAmount(ExcelUtils.getValue(row, 7));
                if (balance.compareTo(BigDecimal.ZERO) == 0) {
                    // 余额列空时按方向推算：借方科目=年初借+累计借-年初贷-累计贷
                    if (SubjectDirectionEnum.DEBIT.getValue().equals(String.valueOf(existing.getDirection()))) {
                        balance = dto.getOpeningYearBalanceDebit().add(dto.getDebitAmount())
                                .subtract(dto.getOpeningYearBalanceCredit()).subtract(dto.getCreditAmount());
                    } else {
                        balance = dto.getOpeningYearBalanceCredit().add(dto.getCreditAmount())
                                .subtract(dto.getOpeningYearBalanceDebit()).subtract(dto.getDebitAmount());
                    }
                }
                dto.setBalance(balance);
                toSave.add(dto);
                result.setSuccess(result.getSuccess() + 1);
            }
            excelImportFile.closeWorkbook();
        } catch (IOException e) {
            throw new IllegalStateException("读取 Excel 失败", e);
        }

        if (!toSave.isEmpty()) {
            Message<String> saved = save(toSave);
            if (saved.getCode() != Message.SUCCESS) {
                return new Message<>(Message.FAIL, saved.getMessage(), result);
            }
        }
        String msg = "导入完成：成功 " + result.getSuccess() + " 条，失败 " + result.getFailed() + " 条";
        return new Message<>(Message.SUCCESS, msg, result);
    }

    private void addImportError(BookInitBalanceImportResultVo result, int row, String code, String message) {
        result.setFailed(result.getFailed() + 1);
        BookInitBalanceImportResultVo.RowError err = new BookInitBalanceImportResultVo.RowError();
        err.setRow(row);
        err.setCode(code);
        err.setMessage(message);
        result.getErrors().add(err);
    }

    private boolean isBlankRow(Row row) {
        for (int i = 0; i < EXPORT_HEADERS.length; i++) {
            if (StringUtils.isNotBlank(ExcelUtils.getValue(row, i))) {
                return false;
            }
        }
        return true;
    }

    private void setAmountCell(Row row, int col, BigDecimal value) {
        if (value == null) {
            row.createCell(col).setCellValue("");
        } else {
            row.createCell(col).setCellValue(value.doubleValue());
        }
    }

    private BigDecimal parseAmount(String raw) {
        String text = StringUtils.trimToEmpty(raw).replace(",", "");
        if (StringUtils.isBlank(text)) {
            return BigDecimal.ZERO;
        }
        try {
            return new BigDecimal(text);
        } catch (Exception e) {
            return BigDecimal.ZERO;
        }
    }
}
