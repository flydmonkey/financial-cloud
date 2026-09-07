package com.financial.cloud.service.voucher;

import com.baomidou.mybatisplus.core.incrementer.IdentifierGenerator;
import com.financial.cloud.common.Message;
import com.financial.cloud.domain.book.Book;
import com.financial.cloud.domain.book.BookSubject;
import com.financial.cloud.domain.voucher.Voucher;
import com.financial.cloud.dto.voucher.VoucherChangeDto;
import com.financial.cloud.dto.voucher.VoucherPageDto;
import com.financial.cloud.repository.book.BookMapper;
import com.financial.cloud.repository.hr.EmployeeSalarySummaryMapper;
import com.financial.cloud.repository.idm.UserInfoMapper;
import com.financial.cloud.repository.standard.StandardSubjectCashFlowMapper;
import com.financial.cloud.repository.voucher.VoucherItemAuxiliaryMapper;
import com.financial.cloud.repository.voucher.VoucherItemCashFlowMapper;
import com.financial.cloud.repository.voucher.VoucherItemMapper;
import com.financial.cloud.repository.voucher.VoucherMapper;
import com.financial.cloud.repository.voucher.VoucherWordMapper;
import com.financial.cloud.service.book.BookSubjectService;
import com.financial.cloud.service.config.ConfigSysService;
import com.financial.cloud.service.statement.StatementSubjectBalanceService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.GregorianCalendar;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class VoucherImportExcelTest {

    private static final String BOOK_ID = "book-import-1";

    @Mock
    private IdentifierGenerator identifierGenerator;
    @Mock
    private VoucherItemMapper voucherItemMapper;
    @Mock
    private VoucherWordMapper voucherWordMapper;
    @Mock
    private VoucherItemAuxiliaryMapper voucherItemAuxiliaryMapper;
    @Mock
    private UserInfoMapper userInfoMapper;
    @Mock
    private BookSubjectService bookSubjectService;
    @Mock
    private StatementSubjectBalanceService subjectBalanceService;
    @Mock
    private BookMapper bookMapper;
    @Mock
    private ConfigSysService configSysService;
    @Mock
    private StandardSubjectCashFlowMapper standardSubjectCashFlowMapper;
    @Mock
    private VoucherItemCashFlowMapper voucherItemCashFlowMapper;
    @Mock
    private EmployeeSalarySummaryMapper employeeSalarySummaryMapper;
    @Mock
    private VoucherMapper voucherMapper;

    @Spy
    @InjectMocks
    private VoucherService voucherService;

    @BeforeEach
    void wireBaseMapper() {
        ReflectionTestUtils.setField(voucherService, "baseMapper", voucherMapper);
    }

    @Test
    void groupImportLines_mergesContinuationRows() {
        List<VoucherService.ImportLine> lines = new ArrayList<>();
        VoucherService.ImportLine head = new VoucherService.ImportLine();
        head.excelRow = 2;
        head.dateRaw = "2026-01-15";
        head.wordHead = "记";
        head.wordNumRaw = "1";
        head.subjectCode = "1001";
        head.debitAmount = new BigDecimal("100");
        head.creditAmount = BigDecimal.ZERO;
        lines.add(head);

        VoucherService.ImportLine cont = new VoucherService.ImportLine();
        cont.excelRow = 3;
        cont.subjectCode = "1002";
        cont.debitAmount = BigDecimal.ZERO;
        cont.creditAmount = new BigDecimal("100");
        lines.add(cont);

        List<VoucherService.ImportGroup> groups = VoucherService.groupImportLines(lines);
        assertEquals(1, groups.size());
        assertEquals(2, groups.get(0).lines.size());
        assertEquals("记", groups.get(0).wordHead);
        assertEquals(1, groups.get(0).wordNum);
    }

    @Test
    void ioHeaders_matchRoundTripContract() {
        assertEquals(9, VoucherService.VOUCHER_IO_HEADERS.length);
        assertEquals("凭证日期", VoucherService.VOUCHER_IO_HEADERS[0]);
        assertEquals("科目编码", VoucherService.VOUCHER_IO_HEADERS[6]);
        assertEquals("贷方金额", VoucherService.VOUCHER_IO_HEADERS[8]);
    }

    @Test
    void workbookSheets_useInstructionAndVoucherNames() {
        assertEquals("填写说明", VoucherService.SHEET_INSTRUCTIONS);
        assertEquals("凭证", VoucherService.SHEET_DATA);
    }

    @Test
    void classifyConflict_unpostedVsPosted() {
        Voucher unposted = new Voucher();
        unposted.setSenderId(null);
        Voucher posted = new Voucher();
        posted.setSenderId("sender-1");

        assertFalse(VoucherService.isPosted(null));
        assertFalse(VoucherService.isPosted(unposted));
        assertTrue(VoucherService.isPosted(posted));
    }

    @Test
    void isLegacyVoucherExportSheet_detectsOldHeaders() {
        try (org.apache.poi.xssf.usermodel.XSSFWorkbook wb = new org.apache.poi.xssf.usermodel.XSSFWorkbook()) {
            org.apache.poi.ss.usermodel.Sheet sheet = wb.createSheet();
            org.apache.poi.ss.usermodel.Row row = sheet.createRow(1);
            row.createCell(0).setCellValue("凭证字");
            row.createCell(1).setCellValue("账套ID");
            assertTrue(VoucherService.isLegacyVoucherExportSheet(sheet));
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    @Test
    void saveImportGroup_rejectsUnknownSubject() {
        Book book = new Book();
        book.setId(BOOK_ID);
        book.setCompanyName("测试公司");
        when(bookSubjectService.selectSubject(eq(BOOK_ID), eq("9999"))).thenReturn(null);
        when(configSysService.getCurrentTerm(BOOK_ID)).thenReturn("2026-01");

        VoucherService.ImportGroup group = openPeriodGroup("9999", "100", "0");
        Message<String> msg = ReflectionTestUtils.invokeMethod(
                voucherService, "saveImportGroup", book, new java.util.HashMap<>(), group);
        assertEquals(Message.FAIL, msg.getCode());
        assertTrue(msg.getMessage().contains("科目编码不存在"));
        verify(voucherService, never()).save(any(VoucherChangeDto.class));
    }

    @Test
    void saveImportGroup_rejectsUnbalanced() {
        Book book = new Book();
        book.setId(BOOK_ID);
        book.setCompanyName("测试公司");
        BookSubject cash = subject("s1", "1001", "库存现金");
        BookSubject bank = subject("s2", "1002", "银行存款");
        when(bookSubjectService.selectSubject(eq(BOOK_ID), eq("1001"))).thenReturn(cash);
        when(bookSubjectService.selectSubject(eq(BOOK_ID), eq("1002"))).thenReturn(bank);
        when(configSysService.getCurrentTerm(BOOK_ID)).thenReturn("2026-01");

        VoucherService.ImportGroup group = new VoucherService.ImportGroup();
        group.firstExcelRow = 2;
        group.voucherDate = new GregorianCalendar(2026, 0, 15).getTime();
        group.wordHead = "记";
        group.wordNum = 1;
        group.receiptNum = 0;
        VoucherService.ImportLine d = new VoucherService.ImportLine();
        d.subjectCode = "1001";
        d.summary = "借";
        d.debitAmount = new BigDecimal("100");
        d.creditAmount = BigDecimal.ZERO;
        VoucherService.ImportLine c = new VoucherService.ImportLine();
        c.subjectCode = "1002";
        c.summary = "贷";
        c.debitAmount = BigDecimal.ZERO;
        c.creditAmount = new BigDecimal("50");
        group.lines.add(d);
        group.lines.add(c);

        Message<String> msg = ReflectionTestUtils.invokeMethod(
                voucherService, "saveImportGroup", book, new java.util.HashMap<>(), group);
        assertEquals(Message.FAIL, msg.getCode());
        assertTrue(msg.getMessage().contains("借贷不平衡"));
    }

    @Test
    void saveImportGroup_rejectsClosedPeriod() {
        Book book = new Book();
        book.setId(BOOK_ID);
        book.setCompanyName("测试公司");
        when(configSysService.getCurrentTerm(BOOK_ID)).thenReturn("2026-03");

        VoucherService.ImportGroup group = openPeriodGroup("1001", "100", "0");
        group.voucherDate = new GregorianCalendar(2026, 0, 15).getTime();

        Message<String> msg = ReflectionTestUtils.invokeMethod(
                voucherService, "saveImportGroup", book, new java.util.HashMap<>(), group);
        assertEquals(Message.FAIL, msg.getCode());
        assertTrue(msg.getMessage().contains("已结账期间"));
    }

    @Test
    void listAllMatching_ignoresPageSize() {
        VoucherPageDto dto = new VoucherPageDto();
        dto.setBookId(BOOK_ID);
        dto.setPageNumber(1);
        dto.setPageSize(1);
        when(voucherMapper.selectList(any())).thenReturn(List.of());
        List<?> result = voucherService.listAllMatching(dto);
        assertTrue(result.isEmpty());
        verify(voucherMapper).selectList(any());
    }

    private static BookSubject subject(String id, String code, String name) {
        BookSubject s = new BookSubject();
        s.setId(id);
        s.setCode(code);
        s.setName(name);
        return s;
    }

    private static VoucherService.ImportGroup openPeriodGroup(String code, String debit, String credit) {
        VoucherService.ImportGroup group = new VoucherService.ImportGroup();
        group.firstExcelRow = 2;
        group.voucherDate = new GregorianCalendar(2026, 0, 15).getTime();
        group.wordHead = "记";
        group.wordNum = 1;
        group.receiptNum = 0;
        VoucherService.ImportLine line = new VoucherService.ImportLine();
        line.subjectCode = code;
        line.summary = "摘要";
        line.debitAmount = new BigDecimal(debit);
        line.creditAmount = new BigDecimal(credit);
        group.lines.add(line);
        VoucherService.ImportLine line2 = new VoucherService.ImportLine();
        line2.subjectCode = code;
        line2.summary = "摘要2";
        line2.debitAmount = new BigDecimal(credit);
        line2.creditAmount = new BigDecimal(debit);
        group.lines.add(line2);
        return group;
    }
}
