package com.financial.cloud.service.voucher;

import com.baomidou.mybatisplus.core.incrementer.IdentifierGenerator;
import com.financial.cloud.common.Message;
import com.financial.cloud.domain.book.Book;
import com.financial.cloud.domain.idm.UserInfo;
import com.financial.cloud.domain.voucher.Voucher;
import com.financial.cloud.dto.voucher.VoucherChangeDto;
import com.financial.cloud.dto.voucher.VoucherItemChangeDto;
import com.financial.cloud.enums.voucher.VoucherReviewedOnOffEnum;
import com.financial.cloud.enums.voucher.VoucherStatusEnum;
import com.financial.cloud.repository.book.BookMapper;
import com.financial.cloud.repository.hr.EmployeeSalarySummaryMapper;
import com.financial.cloud.repository.standard.StandardSubjectCashFlowMapper;
import com.financial.cloud.repository.idm.UserInfoMapper;
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
import java.util.Calendar;
import java.util.Date;
import java.util.GregorianCalendar;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class VoucherServiceTest {

    private static final String BOOK_ID = "book-test-1";
    private static final String TERM = "2025-01";

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
    @Mock
    private com.financial.cloud.service.book.BookSealGuard bookSealGuard;
    @Mock
    private org.springframework.beans.factory.ObjectProvider<com.financial.cloud.service.journal.JournalEntryService> journalEntryServiceProvider;

    @Spy
    @InjectMocks
    private VoucherService voucherService;

    @BeforeEach
    void wireBaseMapper() {
        ReflectionTestUtils.setField(voucherService, "baseMapper", voucherMapper);
    }

    static VoucherItemChangeDto debitLine(String subjectId, String summary, String amount) {
        return VoucherItemChangeDto.builder()
                .subjectId(subjectId)
                .subjectName("科目-" + subjectId)
                .summary(summary)
                .debitAmount(new BigDecimal(amount))
                .creditAmount(BigDecimal.ZERO)
                .build();
    }

    static VoucherItemChangeDto creditLine(String subjectId, String summary, String amount) {
        return VoucherItemChangeDto.builder()
                .subjectId(subjectId)
                .subjectName("科目-" + subjectId)
                .summary(summary)
                .debitAmount(BigDecimal.ZERO)
                .creditAmount(new BigDecimal(amount))
                .build();
    }

    static List<VoucherItemChangeDto> balancedItems() {
        return List.of(
                debitLine("subject-cash", "测试摘要", "100.00"),
                creditLine("subject-bank", "测试摘要", "100.00")
        );
    }

    static VoucherChangeDto validDraftDto() {
        Date voucherDate = new GregorianCalendar(2025, Calendar.JANUARY, 15).getTime();
        return VoucherChangeDto.builder()
                .bookId(BOOK_ID)
                .wordHead("记")
                .wordNum(1)
                .companyName("测试公司")
                .receiptNum(0)
                .voucherDate(voucherDate)
                .voucherYear(2025)
                .voucherMonth(1)
                .items(balancedItems())
                .build();
    }

    @Test
    void saveRejectsEmptyItems() {
        VoucherChangeDto dto = validDraftDto();
        dto.setItems(List.of());

        Message<String> result = voucherService.save(dto);

        assertNotEquals(Message.SUCCESS, result.getCode());
        assertEquals("凭证明细不能为空", result.getMessage());
    }

    @Test
    void saveRejectsSingleItem() {
        VoucherChangeDto dto = validDraftDto();
        dto.setItems(List.of(debitLine("subject-cash", "测试", "100")));

        Message<String> result = voucherService.save(dto);

        assertEquals("至少需要两条分录", result.getMessage());
    }

    @Test
    void saveRejectsMissingSummary() {
        VoucherChangeDto dto = validDraftDto();
        dto.setItems(List.of(
                debitLine("subject-cash", "摘要", "100"),
                creditLine("subject-bank", "", "100")
        ));

        Message<String> result = voucherService.save(dto);

        assertEquals("请至少输入一项摘要", result.getMessage());
    }

    @Test
    void saveRejectsPlaceholderSummaryOnly() {
        VoucherChangeDto dto = validDraftDto();
        dto.setItems(List.of(
                debitLine("subject-cash", "摘要", "100"),
                creditLine("subject-bank", "摘要", "100")
        ));

        Message<String> result = voucherService.save(dto);

        assertEquals("请至少输入一项摘要", result.getMessage());
    }

    @Test
    void saveRejectsUnbalancedAmounts() {
        VoucherChangeDto dto = validDraftDto();
        dto.setItems(List.of(
                debitLine("subject-cash", "工资", "100"),
                creditLine("subject-bank", "工资", "90")
        ));

        Message<String> result = voucherService.save(dto);

        assertEquals("借贷不平衡", result.getMessage());
    }

    @Test
    void saveRejectsMissingSubject() {
        VoucherChangeDto dto = validDraftDto();
        dto.setItems(List.of(
                debitLine("", "工资", "100"),
                creditLine("subject-bank", "工资", "100")
        ));

        Message<String> result = voucherService.save(dto);

        assertEquals("存在未选择科目的分录", result.getMessage());
    }

    @Test
    void submitWithoutReviewSetsCompleted() {
        VoucherChangeDto dto = validDraftDto();
        when(configSysService.getCurrentTerm(BOOK_ID)).thenReturn(TERM);
        Book book = new Book();
        book.setVoucherReviewed(VoucherReviewedOnOffEnum.OFF.getCode());
        when(bookMapper.selectById(BOOK_ID)).thenReturn(book);
        doReturn(new Message<>(Message.SUCCESS, "ok", "voucher-1"))
                .when(voucherService).update(any(VoucherChangeDto.class));

        Message<String> result = voucherService.submit(dto, false);

        assertEquals(Message.SUCCESS, result.getCode());
        assertEquals(VoucherStatusEnum.COMPLETED.getValue(), dto.getStatus());
    }

    @Test
    void submitWithReviewSetsReviewing() {
        VoucherChangeDto dto = validDraftDto();
        when(configSysService.getCurrentTerm(BOOK_ID)).thenReturn(TERM);
        Book book = new Book();
        book.setVoucherReviewed(VoucherReviewedOnOffEnum.ON.getCode());
        when(bookMapper.selectById(BOOK_ID)).thenReturn(book);
        doReturn(new Message<>(Message.SUCCESS, "ok", "voucher-1"))
                .when(voucherService).update(any(VoucherChangeDto.class));

        Message<String> result = voucherService.submit(dto, false);

        assertEquals(Message.SUCCESS, result.getCode());
        assertEquals(VoucherStatusEnum.UNDER_REVIEW.getValue(), dto.getStatus());
    }

    @Test
    void submitRejectsNonCurrentTerm() {
        VoucherChangeDto dto = validDraftDto();
        when(configSysService.getCurrentTerm(BOOK_ID)).thenReturn("2025-02");

        Message<String> result = voucherService.submit(dto, false);

        assertNotEquals(Message.SUCCESS, result.getCode());
        assertTrue(result.getMessage().contains("非当前期"));
    }

    @Test
    void saveRejectsClosedPeriod() {
        VoucherChangeDto dto = validDraftDto();
        when(configSysService.getCurrentTerm(BOOK_ID)).thenReturn("2025-03");

        Message<String> result = voucherService.save(dto);

        assertNotEquals(Message.SUCCESS, result.getCode());
        assertTrue(result.getMessage().contains("已结账期间"));
    }

    @Test
    void submitRejectsAlreadySubmittedVoucher() {
        VoucherChangeDto dto = validDraftDto();
        dto.setId("existing-voucher");
        Voucher existing = Voucher.builder()
                .id("existing-voucher")
                .status(VoucherStatusEnum.COMPLETED.getValue())
                .build();
        when(voucherMapper.selectById("existing-voucher")).thenReturn(existing);

        Message<String> result = voucherService.submit(dto, false);

        assertEquals("凭证已提交，不允许修改", result.getMessage());
    }

    @Test
    void auditRejectsSelfCreatedVoucher() {
        Voucher selfCreated = Voucher.builder()
                .id("v-self")
                .status(VoucherStatusEnum.UNDER_REVIEW.getValue())
                .build();
        selfCreated.setCreatedBy("user-1");
        when(voucherMapper.selectByIds(List.of("v-self"))).thenReturn(List.of(selfCreated));
        UserInfo auditor = new UserInfo();
        auditor.setId("user-1");
        auditor.setDisplayName("张三");

        Message<Void> result = voucherService.audit(List.of("v-self"), auditor);

        assertEquals(Message.SUCCESS, result.getCode());
        assertTrue(result.getMessage().contains("成功：0"));
        assertTrue(result.getMessage().contains("制单人与审核人相同被拒：1"));
        // 自制凭证不允许进入审核通过流程
        org.mockito.Mockito.verify(voucherMapper, org.mockito.Mockito.never())
                .updateById(org.mockito.ArgumentMatchers.any(Voucher.class));
    }

    @Test
    void auditAllowsDifferentAuditor() {
        Voucher others = Voucher.builder()
                .id("v-other")
                .status(VoucherStatusEnum.UNDER_REVIEW.getValue())
                .build();
        others.setCreatedBy("user-2");
        when(voucherMapper.selectByIds(List.of("v-other"))).thenReturn(List.of(others));
        UserInfo auditor = new UserInfo();
        auditor.setId("user-1");
        auditor.setDisplayName("张三");

        Message<Void> result = voucherService.audit(List.of("v-other"), auditor);

        assertEquals(Message.SUCCESS, result.getCode());
        assertTrue(result.getMessage().contains("制单人与审核人相同被拒") == false);
    }

    private Voucher voidableVoucher(String status) {
        return Voucher.builder()
                .id("v-void")
                .bookId(BOOK_ID)
                .status(status)
                .voucherDate(new GregorianCalendar(2025, Calendar.JANUARY, 15).getTime())
                .build();
    }

    @Test
    void voidById_marksDraftCancelled() {
        when(voucherMapper.selectById("v-void")).thenReturn(voidableVoucher(VoucherStatusEnum.DRAFT.getValue()));
        when(configSysService.getCurrentTerm(BOOK_ID)).thenReturn(TERM);

        Message<String> result = voucherService.voidById("v-void", BOOK_ID);

        assertEquals(Message.SUCCESS, result.getCode());
    }

    @Test
    void voidById_rejectsPosted() {
        Voucher posted = voidableVoucher(VoucherStatusEnum.COMPLETED.getValue());
        posted.setSenderId("user-9");
        when(voucherMapper.selectById("v-void")).thenReturn(posted);
        when(configSysService.getCurrentTerm(BOOK_ID)).thenReturn(TERM);

        Message<String> result = voucherService.voidById("v-void", BOOK_ID);

        assertNotEquals(Message.SUCCESS, result.getCode());
        assertTrue(result.getMessage().contains("已过账"));
    }

    @Test
    void voidById_rejectsReviewing() {
        when(voucherMapper.selectById("v-void")).thenReturn(voidableVoucher(VoucherStatusEnum.UNDER_REVIEW.getValue()));
        when(configSysService.getCurrentTerm(BOOK_ID)).thenReturn(TERM);

        Message<String> result = voucherService.voidById("v-void", BOOK_ID);

        assertNotEquals(Message.SUCCESS, result.getCode());
        assertTrue(result.getMessage().contains("暂存或被拒绝"));
    }

    @Test
    void voidById_rejectsClosedPeriod() {
        when(voucherMapper.selectById("v-void")).thenReturn(voidableVoucher(VoucherStatusEnum.DRAFT.getValue()));
        when(configSysService.getCurrentTerm(BOOK_ID)).thenReturn("2025-03");

        Message<String> result = voucherService.voidById("v-void", BOOK_ID);

        assertNotEquals(Message.SUCCESS, result.getCode());
        assertTrue(result.getMessage().contains("已结账期间"));
    }

    @Test
    void voidById_rejectsForeignBook() {
        Voucher other = voidableVoucher(VoucherStatusEnum.DRAFT.getValue());
        other.setBookId("book-other");
        when(voucherMapper.selectById("v-void")).thenReturn(other);

        Message<String> result = voucherService.voidById("v-void", BOOK_ID);

        assertNotEquals(Message.SUCCESS, result.getCode());
    }

    @Test
    void unvoidById_restoresDraft() {
        when(voucherMapper.selectById("v-void")).thenReturn(voidableVoucher(VoucherStatusEnum.CANCELLED.getValue()));
        when(configSysService.getCurrentTerm(BOOK_ID)).thenReturn(TERM);

        Message<String> result = voucherService.unvoidById("v-void", BOOK_ID);

        assertEquals(Message.SUCCESS, result.getCode());
    }

    @Test
    void unvoidById_rejectsNonCancelled() {
        when(voucherMapper.selectById("v-void")).thenReturn(voidableVoucher(VoucherStatusEnum.DRAFT.getValue()));
        when(configSysService.getCurrentTerm(BOOK_ID)).thenReturn(TERM);

        Message<String> result = voucherService.unvoidById("v-void", BOOK_ID);

        assertNotEquals(Message.SUCCESS, result.getCode());
        assertTrue(result.getMessage().contains("已作废"));
    }

    private Voucher postedVoucher() {
        Voucher voucher = voidableVoucher(VoucherStatusEnum.COMPLETED.getValue());
        voucher.setId("v-posted");
        voucher.setWordHead("记");
        voucher.setSenderId("user-9");
        return voucher;
    }

    @Test
    void reverseById_rejectsUnposted() {
        Voucher unposted = postedVoucher();
        unposted.setSenderId(null);
        when(voucherMapper.selectById("v-posted")).thenReturn(unposted);

        Message<String> result = voucherService.reverseById("v-posted", BOOK_ID);

        assertNotEquals(Message.SUCCESS, result.getCode());
        assertTrue(result.getMessage().contains("已过账"));
    }

    @Test
    void reverseById_rejectsForeignBook() {
        Voucher other = postedVoucher();
        other.setBookId("book-other");
        when(voucherMapper.selectById("v-posted")).thenReturn(other);

        Message<String> result = voucherService.reverseById("v-posted", BOOK_ID);

        assertNotEquals(Message.SUCCESS, result.getCode());
    }

    @Test
    void reverseById_rejectsDoubleReverse() {
        when(voucherMapper.selectById("v-posted")).thenReturn(postedVoucher());
        when(voucherMapper.selectCount(any())).thenReturn(1L);

        Message<String> result = voucherService.reverseById("v-posted", BOOK_ID);

        assertNotEquals(Message.SUCCESS, result.getCode());
        assertTrue(result.getMessage().contains("重复冲销"));
    }

    @Test
    void reverseById_generatesNegativeDraft() {
        when(voucherMapper.selectById("v-posted")).thenReturn(postedVoucher());
        when(voucherMapper.selectCount(any())).thenReturn(0L);
        when(configSysService.getCurrentTerm(BOOK_ID)).thenReturn(TERM);

        com.financial.cloud.dto.voucher.VoucherVo vo = new com.financial.cloud.dto.voucher.VoucherVo();
        vo.setId("v-posted");
        vo.setWord("记-3");
        vo.setWordHead("记");
        vo.setItems(List.of(
                new com.financial.cloud.dto.voucher.VoucherItemVo(),
                new com.financial.cloud.dto.voucher.VoucherItemVo()));
        vo.getItems().get(0).setDebitAmount(new BigDecimal("100"));
        vo.getItems().get(0).setCreditAmount(BigDecimal.ZERO);
        vo.getItems().get(0).setSummary("购货");
        vo.getItems().get(1).setDebitAmount(BigDecimal.ZERO);
        vo.getItems().get(1).setCreditAmount(new BigDecimal("100"));
        vo.getItems().get(1).setSummary("购货");
        doReturn(new Message<>(Message.SUCCESS, vo)).when(voucherService).queryById("v-posted");
        when(voucherWordMapper.selectPage(any(), any()))
                .thenReturn(new com.baomidou.mybatisplus.extension.plugins.pagination.Page<>());
        doReturn(new Message<>(Message.SUCCESS, "暂存成功", "v-reversal"))
                .when(voucherService).save(org.mockito.ArgumentMatchers.<VoucherChangeDto>any());

        Message<String> result = voucherService.reverseById("v-posted", BOOK_ID);

        assertEquals(Message.SUCCESS, result.getCode());
        assertEquals("v-reversal", result.getData());
        org.mockito.Mockito.verify(voucherService).save(org.mockito.ArgumentMatchers.<VoucherChangeDto>argThat(dto ->
                dto.getItems().stream().allMatch(i ->
                        (i.getDebitAmount() == null || i.getDebitAmount().signum() <= 0)
                                && (i.getCreditAmount() == null || i.getCreditAmount().signum() <= 0))
                        && dto.getItems().stream().allMatch(i -> i.getSummary().startsWith("冲销："))));
    }

    @Test
    void modifyBlockedReason_postedCancelledAndClosed() {
        Voucher posted = Voucher.builder().id("v1").senderId("u1").status(VoucherStatusEnum.COMPLETED.getValue()).build();
        assertTrue(VoucherService.modifyBlockedReason(posted, true).contains("反过账"));

        Voucher cancelled = Voucher.builder().id("v2").status(VoucherStatusEnum.CANCELLED.getValue()).build();
        assertEquals("已作废凭证不能修改", VoucherService.modifyBlockedReason(cancelled, true));

        Voucher audited = Voucher.builder().id("v2b").status(VoucherStatusEnum.COMPLETED.getValue()).build();
        assertTrue(VoucherService.modifyBlockedReason(audited, true).contains("反审核"));

        Voucher reviewing = Voucher.builder().id("v2c").status(VoucherStatusEnum.UNDER_REVIEW.getValue()).build();
        assertTrue(VoucherService.modifyBlockedReason(reviewing, true).contains("撤回"));

        Voucher closed = Voucher.builder().id("v3").status(VoucherStatusEnum.DRAFT.getValue()).build();
        assertEquals("已结账期间的凭证不能修改", VoucherService.modifyBlockedReason(closed, false));

        Voucher ok = Voucher.builder().id("v4").status(VoucherStatusEnum.DRAFT.getValue()).build();
        assertEquals(null, VoucherService.modifyBlockedReason(ok, true));
    }
}
