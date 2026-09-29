package com.financial.cloud.service.report;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.financial.cloud.common.Message;
import com.financial.cloud.domain.book.Book;
import com.financial.cloud.domain.voucher.Voucher;
import com.financial.cloud.dto.arap.ArapMonthEndSummaryVo;
import com.financial.cloud.dto.fixedasset.FixedAssetDepreciationStatusVo;
import com.financial.cloud.dto.report.DashboardTodoVo;
import com.financial.cloud.enums.voucher.VoucherStatusEnum;
import com.financial.cloud.repository.book.BookMapper;
import com.financial.cloud.repository.voucher.VoucherMapper;
import com.financial.cloud.service.arap.ArapService;
import com.financial.cloud.service.config.ConfigSysService;
import com.financial.cloud.service.fixedasset.FixedAssetDepreciationService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;

/**
 * 首页待办聚合：待审凭证、待过账凭证、本期折旧、逾期往来。
 * 对应 docs/product/20-gap-analysis.md §9.2。
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class DashboardTodoService {

    private final BookMapper bookMapper;
    private final VoucherMapper voucherMapper;
    private final ConfigSysService configSysService;
    private final FixedAssetDepreciationService fixedAssetDepreciationService;
    private final ArapService arapService;

    public DashboardTodoVo todo(String bookId) {
        DashboardTodoVo.DashboardTodoVoBuilder builder = DashboardTodoVo.builder()
                .overdueReceivable(BigDecimal.ZERO)
                .overduePayable(BigDecimal.ZERO);
        if (StringUtils.isBlank(bookId)) {
            return builder.build();
        }
        Book book = bookMapper.selectById(bookId);
        if (book == null) {
            return builder.build();
        }
        boolean reviewed = Integer.valueOf(1).equals(book.getVoucherReviewed());
        builder.voucherReviewed(reviewed);

        String currentTerm = configSysService.getCurrentTerm(bookId);
        builder.currentTerm(currentTerm);

        if (reviewed) {
            builder.pendingAuditCount(voucherMapper.selectCount(Wrappers.<Voucher>lambdaQuery()
                    .eq(Voucher::getBookId, bookId)
                    .eq(Voucher::getStatus, VoucherStatusEnum.UNDER_REVIEW.getValue())));
        }
        builder.pendingPostCount(voucherMapper.selectCount(postableQuery(bookId)));

        try {
            Message<FixedAssetDepreciationStatusVo> status =
                    fixedAssetDepreciationService.status(bookId, currentTerm);
            FixedAssetDepreciationStatusVo data = status == null ? null : status.getData();
            builder.depreciationPending(data != null && data.isNeeded() && !data.isAccrued());
        } catch (Exception e) {
            log.warn("折旧待办状态获取失败 bookId={}: {}", bookId, e.getMessage());
        }

        try {
            ArapMonthEndSummaryVo summary = arapService.monthEndSummary(bookId, currentTerm);
            if (summary != null) {
                builder.overdueReceivable(summary.getOverdueReceivable());
                builder.overduePayable(summary.getOverduePayable());
            }
        } catch (Exception e) {
            log.warn("往来逾期待办获取失败 bookId={}: {}", bookId, e.getMessage());
        }
        return builder.build();
    }

    /** 待过账：已审核（或无需审核直接完成）且未过账、未作废 */
    private LambdaQueryWrapper<Voucher> postableQuery(String bookId) {
        return Wrappers.<Voucher>lambdaQuery()
                .eq(Voucher::getBookId, bookId)
                .eq(Voucher::getStatus, VoucherStatusEnum.COMPLETED.getValue())
                .and(w -> w.isNull(Voucher::getSenderId).or().eq(Voucher::getSenderId, ""));
    }
}
