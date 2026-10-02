package com.financial.cloud.controller.journal;

import com.financial.cloud.service.book.BookOwnershipGuard;
import com.financial.cloud.authn.annotation.CurrentUser;
import com.financial.cloud.common.Message;
import com.financial.cloud.constants.auth.ProductRoles;
import com.financial.cloud.domain.idm.UserInfo;
import com.financial.cloud.domain.journal.JournalReconciliation;
import com.financial.cloud.dto.journal.JournalReconciliationDtos;
import com.financial.cloud.service.journal.JournalReconciliationService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

/**
 * 银行对账 / 余额调节表。
 */
@RestController
@RequestMapping("/api/journal/reconciliation")
@RequiredArgsConstructor
public class JournalReconciliationController {
    private final BookOwnershipGuard bookOwnershipGuard;

    private final JournalReconciliationService reconciliationService;

    @GetMapping
    public Message<JournalReconciliationDtos.ReconciliationVo> get(@RequestParam String accId,
                                                                   @RequestParam String yearPeriod,
                                                                   @CurrentUser UserInfo userInfo) {
        bookOwnershipGuard.checkReference("journal_reconciliation", "accId", accId);
        return Message.ok(reconciliationService.get(userInfo.getBookId(), accId, yearPeriod));
    }

    @PutMapping("/statement")
    public Message<JournalReconciliation> saveStatement(@RequestBody JournalReconciliationDtos.StatementDto dto,
                                                        @CurrentUser UserInfo userInfo) {
        bookOwnershipGuard.checkRequest("journal_reconciliation", dto);
        ProductRoles.requireWriteBusiness();
        return Message.ok(reconciliationService.saveStatement(userInfo.getBookId(), dto));
    }

    @PutMapping("/mark")
    public Message<Integer> mark(@RequestBody JournalReconciliationDtos.MarkDto dto,
                                 @CurrentUser UserInfo userInfo) {
        bookOwnershipGuard.checkRequest("journal_reconciliation", dto);
        ProductRoles.requireWriteBusiness();
        return Message.ok(reconciliationService.mark(userInfo.getBookId(), dto));
    }
}
