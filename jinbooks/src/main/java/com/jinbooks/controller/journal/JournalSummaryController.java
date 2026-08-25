package com.jinbooks.controller.journal;



import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import com.jinbooks.authn.support.AuthorizationUtils;
import com.jinbooks.common.Message;
import com.jinbooks.dto.common.ListIdsDto;
import com.jinbooks.dto.journal.JournalSummaryDto;
import com.jinbooks.dto.journal.JournalSummaryPageDto;
import com.jinbooks.dto.journal.JournalSummaryVo;
import com.jinbooks.service.journal.JournalSummaryService;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;



@RequiredArgsConstructor
@Slf4j
@RestController
@RequestMapping("/api/journal/summary")
public class JournalSummaryController {

    private final JournalSummaryService journalSummaryService;

    @GetMapping(value = {"/fetch"})
    public Message<JournalSummaryVo> fetch(JournalSummaryPageDto dto) {
    	dto.setBookId(AuthorizationUtils.getUserInfo().getBookId());
        log.debug("fetch {}", dto);

        return journalSummaryService.pageList(dto);
    }

    @DeleteMapping("/delete")
    public Message<String> delete(@Validated ListIdsDto dto) {
        return journalSummaryService.delete(dto);
    }
    
    @GetMapping(value = {"/summaryAccount"})
    public Message<String> summaryAccount(JournalSummaryDto dto) {
    	dto.setBookId(AuthorizationUtils.getUserInfo().getBookId());
    	return journalSummaryService.summaryAccount(dto);
    }
}
