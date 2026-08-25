package com.jinbooks.controller.history;



import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.jinbooks.authn.annotation.CurrentUser;
import com.jinbooks.common.Message;
import com.jinbooks.domain.history.HistoryConnector;
import com.jinbooks.dto.history.HistoryConnectorPageDto;
import com.jinbooks.domain.idm.UserInfo;
import com.jinbooks.service.history.HistoryConnectorService;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 连接器日志查询
 *
 * @author Crystal.sea
 *
 */

@RequiredArgsConstructor
@Slf4j
@RestController
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
