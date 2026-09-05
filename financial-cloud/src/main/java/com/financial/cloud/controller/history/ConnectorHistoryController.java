package com.financial.cloud.controller.history;



import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.financial.cloud.authn.annotation.CurrentUser;
import com.financial.cloud.common.Message;
import com.financial.cloud.domain.history.HistoryConnector;
import com.financial.cloud.dto.history.HistoryConnectorPageDto;
import com.financial.cloud.domain.idm.UserInfo;
import com.financial.cloud.service.history.HistoryConnectorService;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RequiredArgsConstructor
@Slf4j
// DISABLED open-register-book-auth: menu hidden, code retained
//@RestController
@RequestMapping(value={"/api/historys"})
public class ConnectorHistoryController {

	private final HistoryConnectorService historyConnectorService;

	/**
	 * @Description:
	 * @Param: [dto, currentUser]
	 * @Date: 2024/11/26 16:15
	 */
	@GetMapping(value={"/connectorHistory/fetch"})
    public Message<Page<HistoryConnector>> fetch(
    		@ModelAttribute("historyConnector") HistoryConnectorPageDto dto,
			@CurrentUser UserInfo currentUser){
        log.debug("historys/historyConnector/fetch/ {}",dto);
		LambdaQueryWrapper<HistoryConnector> wrapper = new LambdaQueryWrapper<>();
		wrapper.eq(HistoryConnector::getInstId, currentUser.getBookId());
		return new Message<>(historyConnectorService.page(dto.build(), wrapper));
    }

}
