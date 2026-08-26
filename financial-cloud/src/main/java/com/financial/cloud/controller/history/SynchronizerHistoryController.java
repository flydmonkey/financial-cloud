package com.financial.cloud.controller.history;



import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.financial.cloud.authn.annotation.CurrentUser;
import com.financial.cloud.common.Message;
import com.financial.cloud.domain.history.HistorySynchronizer;
import com.financial.cloud.domain.idm.UserInfo;
import com.financial.cloud.service.history.HistorySynchronizerService;
import com.financial.cloud.dto.history.HistorySynchronizerPageDto;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;


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
