package com.financial.cloud.controller.journal;



import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.financial.cloud.authn.annotation.CurrentUser;
import com.financial.cloud.authn.support.AuthorizationUtils;
import com.financial.cloud.common.Message;
import com.financial.cloud.constants.auth.ProductRoles;
import com.financial.cloud.dto.common.ListIdsDto;
import com.financial.cloud.domain.idm.UserInfo;
import com.financial.cloud.domain.journal.JournalEntry;
import com.financial.cloud.dto.journal.JournalEntryDto;
import com.financial.cloud.dto.journal.JournalEntryPageDto;
import com.financial.cloud.dto.voucher.GenerateVoucherDto;
import com.financial.cloud.service.journal.JournalEntryService;
import com.financial.cloud.validation.AddGroup;
import com.financial.cloud.validation.EditGroup;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;



@RequiredArgsConstructor
@Slf4j
@RestController
@RequestMapping("/api/journal/entry")
public class JournalEntryController {

    private final JournalEntryService journalEntryService;

    @GetMapping(value = {"/fetch"})
    public Message<Page<JournalEntry>> fetch(JournalEntryPageDto dto) {
    	dto.setBookId(AuthorizationUtils.getUserInfo().getBookId());
        log.debug("fetch {}", dto);

        return journalEntryService.pageList(dto);
    }


    @GetMapping("/get/{id}")
    public Message<JournalEntry> getById(@PathVariable(name = "id") String id) {
        return new Message<>(Message.SUCCESS, journalEntryService.getById(id));
    }

    @PostMapping("/add")
    public Message<String> add(@Validated(value = AddGroup.class) @RequestBody JournalEntryDto dto) {
    	ProductRoles.requireWriteBusiness();
    	dto.setBookId(AuthorizationUtils.getUserInfo().getBookId());
        return journalEntryService.save(dto);
    }

    @PutMapping("/update")
    public Message<String> update(@Validated(value = EditGroup.class) @RequestBody JournalEntryDto dto) {
    	ProductRoles.requireWriteBusiness();
    	dto.setBookId(AuthorizationUtils.getUserInfo().getBookId());
        return journalEntryService.update(dto);
    }

    @DeleteMapping("/delete")
    public Message<String> delete(@Validated @RequestBody ListIdsDto dto) {
    	ProductRoles.requireWriteBusiness();
        return journalEntryService.delete(dto);
    }

    @PostMapping("/generate-voucher")
    public Message<String> generateVoucher(@Validated @RequestBody GenerateVoucherDto dto,
                                          @CurrentUser UserInfo currentUser) {
    	ProductRoles.requireWriteBusiness();
        dto.setBookId(currentUser.getBookId());
        return journalEntryService.generateVoucher(dto);
    }
}
