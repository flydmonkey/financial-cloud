package com.financial.cloud.service.statement;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.financial.cloud.domain.book.Book;
import com.financial.cloud.dto.statement.StatementParamsDto;
import com.financial.cloud.dto.voucher.VoucherItemPageDto;
import com.financial.cloud.dto.voucher.VoucherItemVo;
import com.financial.cloud.dto.voucher.VoucherPageDto;
import com.financial.cloud.exception.BusinessException;
import com.financial.cloud.repository.book.BookMapper;
import com.financial.cloud.service.voucher.VoucherService;
import com.financial.cloud.util.HttpExportCapture;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.poi.ss.usermodel.CellStyle;
import org.apache.poi.ss.usermodel.CreationHelper;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.stereotype.Service;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.math.BigDecimal;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.time.YearMonth;
import java.time.format.DateTimeParseException;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;
import java.util.zip.ZipOutputStream;

@Service
@RequiredArgsConstructor
@Slf4j
public class MonthlyBooksPackService {

    public static final String CONTENT_TYPE = "application/zip";
    private static final int EXPORT_PAGE_SIZE = 100_000;
    private static final String[] DETAIL_HEADERS = {
            "日期", "凭证字号", "摘要", "科目编码", "科目名称", "借方金额", "贷方金额", "方向", "余额"
    };

    private final StatementReportService statementReportService;
    private final StatementBalanceSheetService balanceSheetService;
    private final StatementIncomeService incomeService;
    private final VoucherService voucherService;
    private final BookMapper bookMapper;

    public BooksPack build(String bookId, String yearPeriod, boolean includeVoucherList, String userId) {
        YearMonth period = parsePeriod(yearPeriod);
        Book book = bookMapper.selectById(bookId);
        if (book == null) {
            throw new BusinessException(400, "当前账套不存在或不可访问");
        }
        String stage = "start";
        try {
            StatementParamsDto params = statementParams(bookId, yearPeriod);
            Map<String, byte[]> members = new LinkedHashMap<>();
            stage = "subject-balance";
            members.put(fileName("科目余额表", yearPeriod), HttpExportCapture.capture(
                    response -> statementReportService.subjectBalanceExport(copy(params), response)));
            stage = "detail-ledger";
            members.put(fileName("科目明细账", yearPeriod), detailLedger(bookId, yearPeriod));
            stage = "balance-sheet";
            members.put(fileName("资产负债表", yearPeriod), HttpExportCapture.capture(
                    response -> balanceSheetService.export(copy(params), response)));
            stage = "income";
            members.put(fileName("利润表", yearPeriod), HttpExportCapture.capture(
                    response -> incomeService.export(copy(params), response)));
            stage = "cash-flow";
            members.put(fileName("现金流量表", yearPeriod), HttpExportCapture.capture(
                    response -> statementReportService.cashFlowExport(copy(params), response)));
            if (includeVoucherList) {
                stage = "voucher-list";
                VoucherPageDto voucherParams = new VoucherPageDto();
                voucherParams.setBookId(bookId);
                voucherParams.setVoucherYear(period.getYear());
                voucherParams.setVoucherMonth(period.getMonthValue());
                members.put(fileName("凭证清单", yearPeriod), HttpExportCapture.capture(
                        response -> voucherService.export(voucherParams, response)));
            }
            stage = "zip";
            byte[] zip = zip(members);
            return new BooksPack(zipName(book.getName(), yearPeriod), zip, members.size());
        } catch (IOException | RuntimeException ex) {
            log.error("books pack generation failed bookId={} period={} userId={} stage={}",
                    bookId, yearPeriod, userId, stage, ex);
            if (ex instanceof BusinessException businessException) throw businessException;
            throw new BusinessException(500, "本月账本包生成失败：" + stage);
        }
    }

    /**
     * 多账套账本包总 ZIP：成功套放入 {@code 账套名/本月账本包_period.zip}；失败写入 errors.txt。
     */
    public BooksPack buildBatch(List<String> bookIds, String yearPeriod, boolean includeVoucherList,
                                String userId, java.util.function.Predicate<String> granted) {
        parsePeriod(yearPeriod);
        if (bookIds == null || bookIds.isEmpty()) {
            throw new BusinessException(400, "请选择至少一个账套");
        }
        Map<String, byte[]> members = new LinkedHashMap<>();
        StringBuilder errors = new StringBuilder();
        for (String bookId : bookIds) {
            if (bookId == null || bookId.isBlank()) {
                continue;
            }
            if (granted == null || !granted.test(bookId)) {
                errors.append(bookId).append(": 无权限或未授权\n");
                continue;
            }
            try {
                BooksPack pack = build(bookId, yearPeriod, includeVoucherList, userId);
                Book book = bookMapper.selectById(bookId);
                String folder = safeName(book == null ? bookId : book.getName());
                members.put(folder + "/" + pack.fileName(), pack.content());
            } catch (RuntimeException ex) {
                String msg = ex instanceof BusinessException be ? be.getMessage() : ex.getMessage();
                errors.append(bookId).append(": ").append(msg == null ? "导出失败" : msg).append('\n');
                log.warn("books pack batch member failed bookId={} period={} userId={}: {}",
                        bookId, yearPeriod, userId, msg);
            }
        }
        if (!errors.isEmpty()) {
            members.put("errors.txt", errors.toString().getBytes(StandardCharsets.UTF_8));
        }
        if (members.isEmpty()) {
            throw new BusinessException(400, "没有可导出的账本包");
        }
        try {
            byte[] zip = zip(members);
            return new BooksPack("批量账本包_" + yearPeriod + ".zip", zip, members.size());
        } catch (IOException ex) {
            throw new BusinessException(500, "批量账本包生成失败");
        }
    }

    static String safeName(String bookName) {
        String safe = text(bookName).replaceAll("[\\\\/:*?\"<>|]", "_").trim();
        return safe.isBlank() ? "账套" : safe;
    }

    byte[] detailLedger(String bookId, String yearPeriod) throws IOException {
        VoucherItemPageDto query = new VoucherItemPageDto();
        query.setBookId(bookId);
        query.setPeriodType("month");
        query.setReportDate(yearPeriod);
        query.setPageNumber(1);
        query.setPageSize(EXPORT_PAGE_SIZE);
        query.setOrderByColumn("subjectCode,voucherDate,id");
        query.setIsAsc("asc,asc,asc");
        Page<VoucherItemVo> page = voucherService.subLedger(query).getData();
        List<VoucherItemVo> rows = page == null ? List.of() : page.getRecords();
        try (Workbook workbook = new XSSFWorkbook(); ByteArrayOutputStream output = new ByteArrayOutputStream()) {
            Sheet sheet = workbook.createSheet("科目明细账");
            Row header = sheet.createRow(0);
            for (int i = 0; i < DETAIL_HEADERS.length; i++) header.createCell(i).setCellValue(DETAIL_HEADERS[i]);
            sheet.createFreezePane(0, 1);
            CellStyle amountStyle = workbook.createCellStyle();
            amountStyle.setDataFormat(workbook.createDataFormat().getFormat("#,##0.00;[Red]-#,##0.00"));
            CellStyle dateStyle = workbook.createCellStyle();
            CreationHelper helper = workbook.getCreationHelper();
            dateStyle.setDataFormat(helper.createDataFormat().getFormat("yyyy-mm-dd"));
            Map<String, BigDecimal> balances = new LinkedHashMap<>();
            int index = 1;
            for (VoucherItemVo item : rows) {
                String subject = item.getSubjectCode() == null ? "" : item.getSubjectCode();
                BigDecimal debit = zero(item.getDebitAmount());
                BigDecimal credit = zero(item.getCreditAmount());
                BigDecimal balance = balances.getOrDefault(subject, BigDecimal.ZERO).add(debit).subtract(credit);
                balances.put(subject, balance);
                Row row = sheet.createRow(index++);
                if (item.getVoucherDate() != null) {
                    row.createCell(0).setCellValue(item.getVoucherDate());
                    row.getCell(0).setCellStyle(dateStyle);
                }
                row.createCell(1).setCellValue(text(item.getWord()));
                row.createCell(2).setCellValue(text(item.getSummary()));
                row.createCell(3).setCellValue(subject);
                row.createCell(4).setCellValue(text(item.getSubjectName()));
                row.createCell(5).setCellValue(debit.doubleValue());
                row.createCell(6).setCellValue(credit.doubleValue());
                row.createCell(7).setCellValue(balance.signum() < 0 ? "贷" : "借");
                row.createCell(8).setCellValue(balance.abs().doubleValue());
                row.getCell(5).setCellStyle(amountStyle);
                row.getCell(6).setCellStyle(amountStyle);
                row.getCell(8).setCellStyle(amountStyle);
            }
            int[] widths = {12, 12, 28, 14, 28, 14, 14, 8, 14};
            for (int i = 0; i < widths.length; i++) sheet.setColumnWidth(i, widths[i] * 256);
            workbook.write(output);
            return output.toByteArray();
        }
    }

    private static byte[] zip(Map<String, byte[]> members) throws IOException {
        try (ByteArrayOutputStream output = new ByteArrayOutputStream();
             ZipOutputStream zip = new ZipOutputStream(output, StandardCharsets.UTF_8)) {
            for (Map.Entry<String, byte[]> member : members.entrySet()) {
                zip.putNextEntry(new ZipEntry(member.getKey()));
                zip.write(member.getValue());
                zip.closeEntry();
            }
            zip.finish();
            return output.toByteArray();
        }
    }

    static List<String> entryNames(byte[] zipBytes) throws IOException {
        java.util.ArrayList<String> names = new java.util.ArrayList<>();
        try (ZipInputStream input = new ZipInputStream(new ByteArrayInputStream(zipBytes), StandardCharsets.UTF_8)) {
            ZipEntry entry;
            while ((entry = input.getNextEntry()) != null) names.add(entry.getName());
        }
        return names;
    }

    private static StatementParamsDto statementParams(String bookId, String yearPeriod) {
        return StatementParamsDto.builder().bookId(bookId).periodType("month").reportDate(yearPeriod).build();
    }

    private static StatementParamsDto copy(StatementParamsDto source) {
        return statementParams(source.getBookId(), source.getReportDate());
    }

    private static YearMonth parsePeriod(String value) {
        try {
            if (value == null || !value.matches("\\d{4}-(0[1-9]|1[0-2])")) throw new DateTimeParseException("", "", 0);
            return YearMonth.parse(value);
        } catch (DateTimeParseException ex) {
            throw new BusinessException(400, "账期格式无效，应为 YYYY-MM");
        }
    }

    private static String fileName(String label, String period) {
        return label + "_" + period + ".xlsx";
    }

    private static String zipName(String bookName, String period) {
        String safe = text(bookName).replaceAll("[\\\\/:*?\"<>|]", "_");
        return (safe.isBlank() ? "账套" : safe) + "_本月账本包_" + period + ".zip";
    }

    private static BigDecimal zero(BigDecimal value) {
        return value == null ? BigDecimal.ZERO : value;
    }

    private static String text(String value) {
        return value == null ? "" : value;
    }

    public record BooksPack(String fileName, byte[] content, int entryCount) {
        public String encodedFileName() {
            return URLEncoder.encode(fileName, StandardCharsets.UTF_8).replace("+", "%20");
        }
    }
}
