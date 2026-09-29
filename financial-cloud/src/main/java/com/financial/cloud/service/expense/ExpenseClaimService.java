package com.financial.cloud.service.expense;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.financial.cloud.common.Message;
import com.financial.cloud.domain.book.Book;
import com.financial.cloud.domain.book.BookSubject;
import com.financial.cloud.domain.expense.ExpenseClaim;
import com.financial.cloud.dto.expense.ExpenseClaimSaveDto;
import com.financial.cloud.dto.voucher.VoucherChangeDto;
import com.financial.cloud.dto.voucher.VoucherItemChangeDto;
import com.financial.cloud.enums.voucher.VoucherStatusEnum;
import com.financial.cloud.exception.BusinessException;
import com.financial.cloud.repository.book.BookMapper;
import com.financial.cloud.repository.book.BookSubjectMapper;
import com.financial.cloud.repository.expense.ExpenseClaimMapper;
import com.financial.cloud.service.voucher.VoucherService;
import lombok.RequiredArgsConstructor;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.Date;
import java.util.List;

/**
 * 费用报销：报销单暂存 → 提交 → 审核/拒绝，已审核可一键生成报销凭证（暂存态，
 * 由凭证流程继续提交/审核/过账）。凭证借费用科目、贷付款科目（库存现金/银行存款）。
 */
@Service
@RequiredArgsConstructor
public class ExpenseClaimService {

    private static final String DEFAULT_WORD = "记";

    private final ExpenseClaimMapper expenseClaimMapper;
    private final BookSubjectMapper bookSubjectMapper;
    private final BookMapper bookMapper;
    private final VoucherService voucherService;

    public Page<ExpenseClaim> page(String bookId, String status, String keyword, long pageNum, long pageSize) {
        return expenseClaimMapper.selectPage(new Page<>(pageNum, pageSize),
                Wrappers.<ExpenseClaim>lambdaQuery()
                        .eq(ExpenseClaim::getBookId, bookId)
                        .eq(StringUtils.isNotBlank(status), ExpenseClaim::getClaimStatus, status)
                        .and(StringUtils.isNotBlank(keyword), w -> w
                                .like(ExpenseClaim::getClaimNo, keyword)
                                .or().like(ExpenseClaim::getClaimant, keyword)
                                .or().like(ExpenseClaim::getSummary, keyword))
                        .orderByDesc(ExpenseClaim::getClaimDate)
                        .orderByDesc(ExpenseClaim::getClaimNo));
    }

    @Transactional
    public Message<String> save(String bookId, ExpenseClaimSaveDto dto) {
        validate(dto);
        BookSubject expense = requireSubject(bookId, dto.getExpenseSubjectCode(), "费用科目");
        BookSubject fund = requireSubject(bookId, dto.getFundSubjectCode(), "付款科目");

        ExpenseClaim claim;
        if (StringUtils.isNotBlank(dto.getId())) {
            claim = expenseClaimMapper.selectById(dto.getId());
            if (claim == null || !bookId.equals(claim.getBookId())) {
                throw new BusinessException(404, "报销单不存在");
            }
            if (!ExpenseClaim.STATUS_DRAFT.equals(claim.getClaimStatus())
                    && !ExpenseClaim.STATUS_REJECTED.equals(claim.getClaimStatus())) {
                throw new BusinessException(400, "仅暂存或已拒绝的报销单可修改");
            }
        } else {
            claim = ExpenseClaim.builder()
                    .bookId(bookId)
                    .claimNo(nextClaimNo(bookId, dto.getClaimDate()))
                    .claimStatus(ExpenseClaim.STATUS_DRAFT)
                    .build();
        }
        claim.setClaimant(dto.getClaimant().trim());
        claim.setClaimDate(LocalDate.parse(dto.getClaimDate()));
        claim.setExpenseSubjectCode(expense.getCode());
        claim.setExpenseSubjectName(expense.getCode() + "-" + expense.getName());
        claim.setFundSubjectCode(fund.getCode());
        claim.setFundSubjectName(fund.getCode() + "-" + fund.getName());
        claim.setAmount(dto.getAmount().setScale(2, java.math.RoundingMode.HALF_UP));
        claim.setSummary(dto.getSummary());
        if (StringUtils.isNotBlank(dto.getId())) {
            expenseClaimMapper.updateById(claim);
        } else {
            expenseClaimMapper.insert(claim);
        }
        return Message.ok(claim.getId());
    }

    /** 提交：暂存/已拒绝 → 已提交 */
    @Transactional
    public Message<Void> submit(String id, String bookId) {
        ExpenseClaim claim = require(id, bookId);
        if (!ExpenseClaim.STATUS_DRAFT.equals(claim.getClaimStatus())
                && !ExpenseClaim.STATUS_REJECTED.equals(claim.getClaimStatus())) {
            throw new BusinessException(400, "仅暂存或已拒绝的报销单可提交");
        }
        claim.setClaimStatus(ExpenseClaim.STATUS_SUBMITTED);
        claim.setRejectReason(null);
        expenseClaimMapper.updateById(claim);
        return new Message<>(Message.SUCCESS, "ok", null);
    }

    /** 审核：已提交 → 已审核/已拒绝 */
    @Transactional
    public Message<Void> audit(String id, String bookId, boolean approve, String reason, String auditor) {
        ExpenseClaim claim = require(id, bookId);
        if (!ExpenseClaim.STATUS_SUBMITTED.equals(claim.getClaimStatus())) {
            throw new BusinessException(400, "仅已提交的报销单可审核");
        }
        if (!approve && StringUtils.isBlank(reason)) {
            throw new BusinessException(400, "拒绝时必须填写拒绝原因");
        }
        claim.setClaimStatus(approve ? ExpenseClaim.STATUS_APPROVED : ExpenseClaim.STATUS_REJECTED);
        claim.setRejectReason(approve ? null : reason.trim());
        claim.setAuditBy(auditor);
        claim.setAuditTime(LocalDateTime.now());
        expenseClaimMapper.updateById(claim);
        return new Message<>(Message.SUCCESS, "ok", null);
    }

    /**
     * 一键生成报销凭证（暂存态）：借费用科目、贷付款科目。
     * 幂等：已生成过凭证的单据直接返回原凭证ID。
     */
    @Transactional
    public Message<String> generateVoucher(String id, String bookId) {
        ExpenseClaim claim = require(id, bookId);
        if (!ExpenseClaim.STATUS_APPROVED.equals(claim.getClaimStatus())) {
            throw new BusinessException(400, "仅已审核的报销单可生成凭证");
        }
        if (StringUtils.isNotBlank(claim.getVoucherId())) {
            return Message.ok(claim.getVoucherId());
        }
        BookSubject expense = requireSubject(bookId, claim.getExpenseSubjectCode(), "费用科目");
        BookSubject fund = requireSubject(bookId, claim.getFundSubjectCode(), "付款科目");
        Book book = bookMapper.selectById(bookId);

        LocalDate claimDate = claim.getClaimDate() != null ? claim.getClaimDate() : LocalDate.now();
        int year = claimDate.getYear();
        int month = claimDate.getMonthValue();
        Integer wordNum = voucherService.getAbleWordNum(bookId, DEFAULT_WORD, year, month).getData();

        String summary = "费用报销 " + claim.getClaimNo() + " " + claim.getClaimant()
                + (StringUtils.isNotBlank(claim.getSummary()) ? " " + claim.getSummary() : "");
        BigDecimal amount = claim.getAmount();

        VoucherChangeDto voucherDto = new VoucherChangeDto();
        voucherDto.setWordHead(DEFAULT_WORD);
        voucherDto.setWordNum(wordNum);
        voucherDto.setBookId(bookId);
        voucherDto.setCompanyName(book != null ? book.getCompanyName() : "");
        voucherDto.setVoucherDate(Date.from(claimDate.atStartOfDay(ZoneId.systemDefault()).toInstant()));
        voucherDto.setVoucherYear(year);
        voucherDto.setVoucherMonth(month);
        voucherDto.setDebitAmount(amount);
        voucherDto.setCreditAmount(amount);
        voucherDto.setReceiptNum(0);
        voucherDto.setRemark(summary);
        voucherDto.setStatus(VoucherStatusEnum.DRAFT.getValue());
        voucherDto.setItems(List.of(
                createItem(expense, summary, amount, true),
                createItem(fund, summary, amount, false)));

        Message<String> voucherMsg = voucherService.save(voucherDto);
        if (voucherMsg.getCode() != Message.SUCCESS) {
            throw new BusinessException(500, StringUtils.defaultIfBlank(voucherMsg.getMessage(), "报销凭证生成失败"));
        }
        claim.setVoucherId(voucherMsg.getData());
        expenseClaimMapper.updateById(claim);
        return Message.ok(voucherMsg.getData());
    }

    /** 删除：仅暂存/已拒绝可删 */
    @Transactional
    public Message<Void> delete(String id, String bookId) {
        ExpenseClaim claim = require(id, bookId);
        if (!ExpenseClaim.STATUS_DRAFT.equals(claim.getClaimStatus())
                && !ExpenseClaim.STATUS_REJECTED.equals(claim.getClaimStatus())) {
            throw new BusinessException(400, "仅暂存或已拒绝的报销单可删除");
        }
        expenseClaimMapper.deleteById(id);
        return new Message<>(Message.SUCCESS, "ok", null);
    }

    private ExpenseClaim require(String id, String bookId) {
        ExpenseClaim claim = expenseClaimMapper.selectById(id);
        if (claim == null || !bookId.equals(claim.getBookId())) {
            throw new BusinessException(404, "报销单不存在");
        }
        return claim;
    }

    private void validate(ExpenseClaimSaveDto dto) {
        if (StringUtils.isBlank(dto.getClaimant())) {
            throw new BusinessException(400, "请填写报销人");
        }
        if (StringUtils.isBlank(dto.getClaimDate())) {
            throw new BusinessException(400, "请选择报销日期");
        }
        try {
            LocalDate.parse(dto.getClaimDate());
        } catch (Exception e) {
            throw new BusinessException(400, "报销日期格式应为 yyyy-MM-dd");
        }
        if (StringUtils.isBlank(dto.getExpenseSubjectCode())) {
            throw new BusinessException(400, "请选择费用科目");
        }
        if (StringUtils.isBlank(dto.getFundSubjectCode())) {
            throw new BusinessException(400, "请选择付款科目");
        }
        if (dto.getAmount() == null || dto.getAmount().signum() <= 0) {
            throw new BusinessException(400, "报销金额须大于 0");
        }
    }

    private BookSubject requireSubject(String bookId, String code, String label) {
        BookSubject subject = bookSubjectMapper.selectOne(Wrappers.<BookSubject>lambdaQuery()
                .eq(BookSubject::getBookId, bookId)
                .eq(BookSubject::getCode, code)
                .last("limit 1"));
        if (subject == null) {
            throw new BusinessException(400, label + "不存在：" + code);
        }
        return subject;
    }

    /** 单号：BXyyyyMM-4位序号（按账簿 + 前缀计数） */
    private String nextClaimNo(String bookId, String claimDate) {
        String prefix = "BX" + claimDate.substring(0, 7).replace("-", "") + "-";
        Long count = expenseClaimMapper.selectCount(Wrappers.<ExpenseClaim>lambdaQuery()
                .eq(ExpenseClaim::getBookId, bookId)
                .likeRight(ExpenseClaim::getClaimNo, prefix));
        return prefix + String.format("%04d", count + 1);
    }

    private VoucherItemChangeDto createItem(BookSubject subject, String summary, BigDecimal amount, boolean debit) {
        VoucherItemChangeDto item = new VoucherItemChangeDto();
        item.setSummary(summary);
        item.setSubjectId(subject.getId());
        item.setSubjectCode(subject.getCode());
        item.setSubjectName(subject.getCode() + "-" + subject.getName());
        item.setSubjectBalance(subject.getBalance());
        item.setAuxiliary(List.of());
        item.setDetailedAccounts("");
        if (debit) {
            item.setDebitAmount(amount);
            item.setCreditAmount(BigDecimal.ZERO);
        } else {
            item.setDebitAmount(BigDecimal.ZERO);
            item.setCreditAmount(amount);
        }
        return item;
    }
}
