package com.jinbooks.controller.journal;



import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.jinbooks.authn.annotation.CurrentUser;
import com.jinbooks.authn.support.AuthorizationUtils;
import com.jinbooks.common.Message;
import com.jinbooks.dto.common.ListIdsDto;
import com.jinbooks.domain.idm.UserInfo;
import com.jinbooks.domain.journal.JournalEntry;
import com.jinbooks.dto.journal.JournalEntryDto;
import com.jinbooks.dto.journal.JournalEntryPageDto;
import com.jinbooks.dto.voucher.GenerateVoucherDto;
import com.jinbooks.service.journal.JournalEntryService;
import com.jinbooks.validation.AddGroup;
import com.jinbooks.validation.EditGroup;
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
    	dto.setBookId(AuthorizationUtils.getUserInfo().getBookId());
        return journalEntryService.save(dto);
    }

    @PutMapping("/update")
    public Message<String> update(@Validated(value = EditGroup.class) @RequestBody JournalEntryDto dto) {
    	dto.setBookId(AuthorizationUtils.getUserInfo().getBookId());
        return journalEntryService.update(dto);
    }

    @DeleteMapping("/delete")
    public Message<String> delete(@Validated ListIdsDto dto) {
        return journalEntryService.delete(dto);
    }
    
    @PostMapping("/generate-voucher")
    public Message<String> generateVoucher(@Validated @RequestBody GenerateVoucherDto dto, @CurrentUser UserInfo currentUser) {
        dto.setBookId(currentUser.getBookId());
        return journalEntryService.generateVoucher(dto);
    }
}
