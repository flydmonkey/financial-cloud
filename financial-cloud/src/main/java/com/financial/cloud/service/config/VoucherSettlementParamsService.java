package com.financial.cloud.service.config;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.financial.cloud.common.Message;
import com.financial.cloud.constants.system.ConstsSysConfig;
import com.financial.cloud.domain.book.Book;
import com.financial.cloud.domain.config.ConfigSys;
import com.financial.cloud.domain.idm.UserInfo;
import com.financial.cloud.dto.config.VoucherSettlementParamsSaveDto;
import com.financial.cloud.dto.config.VoucherSettlementParamsVo;
import com.financial.cloud.exception.BusinessException;
import com.financial.cloud.repository.book.BookMapper;
import com.financial.cloud.service.book.BookSealGuard;
import com.financial.cloud.service.book.BookService;
import com.financial.cloud.service.book.SettlementService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * 凭证与结账参数中心：审核开关 + 往来软提示 / 逾期硬阻断。
 */
@Service
@RequiredArgsConstructor
public class VoucherSettlementParamsService {

    static final List<String> HARD_GATE_LABELS = List.of(
            "未过账凭证必须处理完毕",
            "凭证号须连续（断号须在凭证列表或月结向导中整理）",
            "本期凭证借贷合计须平衡",
            "必做损益结转须完成（无余额可跳过）",
            "应计提折旧须完成（无资产可跳过）"
    );

    private final BookMapper bookMapper;
    private final BookService bookService;
    private final BookSealGuard bookSealGuard;
    private final ConfigSysService configSysService;

    public VoucherSettlementParamsVo get(UserInfo user) {
        String bookId = requireBookId(user);
        configSysService.ensureBookConfigsComplete(bookId);
        Book book = bookMapper.selectById(bookId);
        if (book == null) {
            throw new BusinessException(400, "账套不存在");
        }
        boolean canEdit = bookService.isBookAdministrator(user, bookId);
        String arapRaw = configSysService.selectConfigByKey(bookId, ConstsSysConfig.SYS_SETTLEMENT_ARAP_VERIFY);
        String overdueHardRaw = configSysService.selectConfigByKey(bookId,
                ConstsSysConfig.SYS_SETTLEMENT_ARAP_OVERDUE_HARD);
        return VoucherSettlementParamsVo.builder()
                .voucherReviewed(book.getVoucherReviewed() == null ? 0 : book.getVoucherReviewed())
                .arapVerifyEnabled(SettlementService.arapVerifyEnabledFromConfig(arapRaw))
                .arapOverdueHard(SettlementService.arapOverdueHardFromConfig(overdueHardRaw))
                .canEdit(canEdit)
                .hardGateLabels(HARD_GATE_LABELS)
                .build();
    }

    @Transactional
    public Message<String> save(VoucherSettlementParamsSaveDto dto, UserInfo user) {
        String bookId = requireBookId(user);
        bookService.requireBookAdministrator(user, bookId);
        bookSealGuard.assertWritable(bookId);
        if (dto == null) {
            throw new BusinessException(400, "请求体不能为空");
        }
        if (dto.getVoucherReviewed() != null) {
            int reviewed = dto.getVoucherReviewed();
            if (reviewed != 0 && reviewed != 1) {
                throw new BusinessException(400, "凭证审核只能为关闭或开启");
            }
            Book patch = new Book();
            patch.setId(bookId);
            patch.setVoucherReviewed(reviewed);
            bookMapper.updateById(patch);
        }
        if (dto.getArapVerifyEnabled() != null || dto.getArapOverdueHard() != null) {
            configSysService.ensureBookConfigsComplete(bookId);
        }
        if (dto.getArapVerifyEnabled() != null) {
            ConfigSys cfg = new ConfigSys();
            cfg.setBookId(bookId);
            cfg.setConfigKey(ConstsSysConfig.SYS_SETTLEMENT_ARAP_VERIFY);
            cfg.setConfigValue(Boolean.TRUE.equals(dto.getArapVerifyEnabled()) ? "true" : "false");
            configSysService.update(cfg);
        }
        if (dto.getArapOverdueHard() != null) {
            ConfigSys cfg = new ConfigSys();
            cfg.setBookId(bookId);
            cfg.setConfigKey(ConstsSysConfig.SYS_SETTLEMENT_ARAP_OVERDUE_HARD);
            cfg.setConfigValue(Boolean.TRUE.equals(dto.getArapOverdueHard()) ? "true" : "false");
            configSysService.update(cfg);
        }
        return Message.ok("保存成功");
    }

    private static String requireBookId(UserInfo user) {
        if (user == null || user.getBookId() == null || user.getBookId().isBlank()) {
            throw new BusinessException(400, "请先选择账套");
        }
        return user.getBookId();
    }
}
