/*
 * Copyright [2025] [JinBooks of copyright http://www.jinbooks.com]
 * 
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * 
 *     http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 */
 

package com.jinbooks.controller.journal;

import com.jinbooks.authn.support.AuthorizationUtils;
import com.jinbooks.common.Message;
import com.jinbooks.dto.common.ListIdsDto;
import com.jinbooks.dto.journal.JournalSummaryDto;
import com.jinbooks.dto.journal.JournalSummaryPageDto;
import com.jinbooks.dto.journal.JournalSummaryVo;
import com.jinbooks.service.journal.JournalSummaryService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;



@RestController
@RequestMapping("/journal/summary")
public class JournalSummaryController {
    private static final Logger logger = LoggerFactory.getLogger(JournalSummaryController.class);

    @Autowired
    JournalSummaryService journalSummaryService;

    @GetMapping(value = {"/fetch"})
    public Message<JournalSummaryVo> fetch(JournalSummaryPageDto dto) {
    	dto.setBookId(AuthorizationUtils.getUserInfo().getBookId());
        logger.debug("fetch {}", dto);

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
