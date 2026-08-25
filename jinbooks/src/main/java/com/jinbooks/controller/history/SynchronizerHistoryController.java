package com.jinbooks.controller.history;



import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.jinbooks.authn.annotation.CurrentUser;
import com.jinbooks.common.Message;
import com.jinbooks.domain.history.HistorySynchronizer;
import com.jinbooks.domain.idm.UserInfo;
import com.jinbooks.service.history.HistorySynchronizerService;
import com.jinbooks.dto.history.HistorySynchronizerPageDto;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * åæ­¥å¨æ¥å¿æ¥è¯?
 *
 * @author Crystal.sea
 *
 */

@RequiredArgsConstructor
@Slf4j
@RestController
@RequestMapping(value={"/api/historys"})
public class SynchronizerHistoryController {

	private final HistorySynchronizerService historySynchronizerService;

	/**
     * @param dto
     * @return
     */
	@GetMapping(value={"/synchronizerHistory/fetch"})
    public Message<Page<HistorySynchronizer>> fetch(
    			HistorySynchronizerPageDto dto,
    			@CurrentUser UserInfo currentUser){
        log.debug("historys/synchronizerHistory/fetch/ {}",dto);
		LambdaQueryWrapper<HistorySynchronizer> wrapper = new LambdaQueryWrapper<>();
		wrapper.eq(HistorySynchronizer::getBookId, currentUser.getBookId());
        return new Message<>(historySynchronizerService.page(dto.build(), wrapper));
    }

}
