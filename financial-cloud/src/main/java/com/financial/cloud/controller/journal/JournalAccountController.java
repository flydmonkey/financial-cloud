package com.financial.cloud.controller.journal;



import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.financial.cloud.authn.support.AuthorizationUtils;
import com.financial.cloud.common.Message;
import com.financial.cloud.dto.common.ListIdsDto;
import com.financial.cloud.domain.journal.JournalAccount;
import com.financial.cloud.dto.journal.JournalAccountDto;
import com.financial.cloud.dto.journal.JournalAccountPageDto;
import com.financial.cloud.service.journal.JournalAccountService;
import com.financial.cloud.validation.AddGroup;
import com.financial.cloud.validation.EditGroup;

import java.math.BigDecimal;
import java.util.List;

import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;



@RequiredArgsConstructor
@Slf4j
@RestController
@RequestMapping("/api/journal/account")
public class JournalAccountController {

    private final JournalAccountService journalAccountService;

    @GetMapping(value = {"/fetch"})
    public Message<Page<JournalAccount>> fetch(JournalAccountPageDto dto) {
    	dto.setBookId(AuthorizationUtils.getUserInfo().getBookId());
        log.debug("fetch {}", dto);

        return journalAccountService.pageList(dto);
    }
    
    @GetMapping(value = {"/findAll"})
    public Message<List<JournalAccount>> findAll() {
        return journalAccountService.findAll(AuthorizationUtils.getUserInfo().getBookId());
    }
    
    @GetMapping(value = {"/allBalance"})
    public Message<BigDecimal> allBalance() {
    	Message<List<JournalAccount>> accountListMsg = journalAccountService.findAll(AuthorizationUtils.getUserInfo().getBookId());
    	BigDecimal balance = BigDecimal.ZERO; 
    	if(accountListMsg.getCode()== 0 && accountListMsg.getData()!= null) {
    		for(JournalAccount account : accountListMsg.getData()) {
    			balance = balance.add(account.getBalance());
    		}
    	}
    	log.debug("Account balance ",balance);
        return Message.ok(balance);
    }

    @GetMapping("/get/{id}")
    public Message<JournalAccount> getById(@PathVariable(name = "id") String id) {
        return new Message<>(Message.SUCCESS, journalAccountService.getById(id));
    }

    @PostMapping("/add")
    public Message<String> add(@Validated(value = AddGroup.class) @RequestBody JournalAccountDto dto) {
    	dto.setBookId(AuthorizationUtils.getUserInfo().getBookId());
        return journalAccountService.save(dto);
    }

    @PutMapping("/update")
    public Message<String> update(@Validated(value = EditGroup.class) @RequestBody JournalAccountDto dto) {
    	dto.setBookId(AuthorizationUtils.getUserInfo().getBookId());
    	return journalAccountService.update(dto);
    }

    @DeleteMapping("/delete")
    public Message<String> delete(@Validated ListIdsDto dto) {
        return journalAccountService.delete(dto);
    }
}
