package com.financial.cloud.controller.expense;

import com.financial.cloud.service.book.BookOwnershipGuard;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.financial.cloud.authn.annotation.CurrentUser;
import com.financial.cloud.common.Message;
import com.financial.cloud.domain.expense.ExpenseClaim;
import com.financial.cloud.domain.idm.UserInfo;
import com.financial.cloud.dto.expense.ExpenseClaimSaveDto;
import com.financial.cloud.service.expense.ExpenseClaimService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * 费用报销：报销单暂存 → 提交 → 审核/拒绝 → 一键生成报销凭证（暂存态）。
 */
@RestController
@RequestMapping("/api/expense/claim")
@RequiredArgsConstructor
public class ExpenseClaimController {
    private final BookOwnershipGuard bookOwnershipGuard;

    private final ExpenseClaimService expenseClaimService;

    @GetMapping
    public Message<Page<ExpenseClaim>> page(@RequestParam(defaultValue = "") String status,
                                            @RequestParam(defaultValue = "") String keyword,
                                            @RequestParam(defaultValue = "1") long pageNumber,
                                            @RequestParam(defaultValue = "20") long pageSize,
                                            @CurrentUser UserInfo userInfo) {
        return Message.ok(expenseClaimService.page(userInfo.getBookId(), status, keyword, pageNumber, pageSize));
    }

    @PostMapping
    public Message<String> save(@RequestBody ExpenseClaimSaveDto dto,
                                @CurrentUser UserInfo userInfo) {
        bookOwnershipGuard.checkRequest("expense_claim", dto);
        return expenseClaimService.save(userInfo.getBookId(), dto);
    }

    /** 单头 + 明细行 */
    @GetMapping("/{id}")
    public Message<ExpenseClaim> detail(@PathVariable String id,
                                        @CurrentUser UserInfo userInfo) {
        bookOwnershipGuard.checkReference("expense_claim", "id", id);
        return Message.ok(expenseClaimService.detail(id, userInfo.getBookId()));
    }

    /** 提交审核：暂存/已拒绝 → 已提交 */
    @PutMapping("/submit/{id}")
    public Message<Void> submit(@PathVariable String id,
                                @CurrentUser UserInfo userInfo) {
        bookOwnershipGuard.checkReference("expense_claim", "id", id);
        return expenseClaimService.submit(id, userInfo.getBookId());
    }

    /** 审核：approve=true 通过，false 拒绝（需 reason） */
    @PutMapping("/audit/{id}")
    public Message<Void> audit(@PathVariable String id,
                               @RequestParam boolean approve,
                               @RequestParam(defaultValue = "") String reason,
                               @CurrentUser UserInfo userInfo) {
        bookOwnershipGuard.checkReference("expense_claim", "id", id);
        return expenseClaimService.audit(id, userInfo.getBookId(), approve, reason, userInfo.getDisplayName());
    }

    /** 已审核单据一键生成报销凭证（暂存态，幂等） */
    @PostMapping("/voucher/{id}")
    public Message<String> generateVoucher(@PathVariable String id,
                                           @CurrentUser UserInfo userInfo) {
        bookOwnershipGuard.checkReference("expense_claim", "id", id);
        return expenseClaimService.generateVoucher(id, userInfo.getBookId());
    }

    @DeleteMapping("/{id}")
    public Message<Void> delete(@PathVariable String id,
                                @CurrentUser UserInfo userInfo) {
        bookOwnershipGuard.checkReference("expense_claim", "id", id);
        return expenseClaimService.delete(id, userInfo.getBookId());
    }
}
