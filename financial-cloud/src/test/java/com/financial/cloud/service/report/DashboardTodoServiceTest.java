package com.financial.cloud.service.report;

import com.financial.cloud.common.Message;
import com.financial.cloud.domain.book.Book;
import com.financial.cloud.domain.voucher.Voucher;
import com.financial.cloud.dto.arap.ArapMonthEndSummaryVo;
import com.financial.cloud.dto.fixedasset.FixedAssetDepreciationStatusVo;
import com.financial.cloud.dto.report.DashboardTodoVo;
import com.financial.cloud.repository.book.BookMapper;
import com.financial.cloud.repository.voucher.VoucherMapper;
import com.financial.cloud.service.arap.ArapService;
import com.financial.cloud.service.config.ConfigSysService;
import com.financial.cloud.service.fixedasset.FixedAssetDepreciationService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class DashboardTodoServiceTest {

    @Mock private BookMapper bookMapper;
    @Mock private VoucherMapper voucherMapper;
    @Mock private ConfigSysService configSysService;
    @Mock private FixedAssetDepreciationService depreciationService;
    @Mock private ArapService arapService;

    private DashboardTodoService service;

    @BeforeEach
    void setUp() {
        service = new DashboardTodoService(bookMapper, voucherMapper, configSysService,
                depreciationService, arapService);
        Book book = new Book();
        book.setId("book-1");
        book.setVoucherReviewed(1);
        when(bookMapper.selectById("book-1")).thenReturn(book);
        when(configSysService.getCurrentTerm("book-1")).thenReturn("2026-09");
        when(voucherMapper.selectCount(any())).thenReturn(3L);
        FixedAssetDepreciationStatusVo deprStatus = new FixedAssetDepreciationStatusVo();
        deprStatus.setNeeded(true);
        deprStatus.setAccrued(false);
        when(depreciationService.status(eq("book-1"), eq("2026-09")))
                .thenReturn(new Message<>(Message.SUCCESS, deprStatus));
        when(arapService.monthEndSummary("book-1", "2026-09"))
                .thenReturn(ArapMonthEndSummaryVo.builder()
                        .receivableTotal(BigDecimal.TEN)
                        .payableTotal(BigDecimal.ONE)
                        .overdueReceivable(new BigDecimal("5200.00"))
                        .overduePayable(BigDecimal.ZERO)
                        .hasOverdue(true)
                        .build());
    }

    @Test
    void aggregatesAllTodoSignals() {
        DashboardTodoVo todo = service.todo("book-1");

        assertThat(todo.getCurrentTerm()).isEqualTo("2026-09");
        assertThat(todo.isVoucherReviewed()).isTrue();
        assertThat(todo.getPendingAuditCount()).isEqualTo(3L);
        assertThat(todo.getPendingPostCount()).isEqualTo(3L);
        assertThat(todo.isDepreciationPending()).isTrue();
        assertThat(todo.getOverdueReceivable()).isEqualByComparingTo("5200.00");
        assertThat(todo.getOverduePayable()).isEqualByComparingTo("0");
    }

    @Test
    void blankBookIdReturnsEmptyTodo() {
        DashboardTodoVo todo = service.todo(" ");

        assertThat(todo.getCurrentTerm()).isNull();
        assertThat(todo.getPendingAuditCount()).isZero();
    }

    @Test
    void accruedDepreciationIsNotPending() {
        FixedAssetDepreciationStatusVo done = new FixedAssetDepreciationStatusVo();
        done.setNeeded(true);
        done.setAccrued(true);
        when(depreciationService.status(eq("book-1"), eq("2026-09")))
                .thenReturn(new Message<>(Message.SUCCESS, done));

        assertThat(service.todo("book-1").isDepreciationPending()).isFalse();
    }

    @Test
    void reviewDisabledBookSkipsAuditCount() {
        Book book = new Book();
        book.setId("book-1");
        book.setVoucherReviewed(0);
        when(bookMapper.selectById("book-1")).thenReturn(book);

        DashboardTodoVo todo = service.todo("book-1");

        assertThat(todo.isVoucherReviewed()).isFalse();
        assertThat(todo.getPendingAuditCount()).isZero();
    }
}
