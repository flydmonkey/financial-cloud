package com.jinbooks.controller.history;



import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.jinbooks.authn.annotation.CurrentUser;
import com.jinbooks.common.Message;
import com.jinbooks.domain.history.HistorySystemLogs;
import com.jinbooks.dto.history.HistorySystemLogsPageDto;
import com.jinbooks.domain.idm.UserInfo;
import com.jinbooks.service.history.HistorySystemLogsService;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.bind.annotation.RestController;

/**
 * 系统操作日志查询
 *
 * @author Crystal.sea
 *
 */

@RequiredArgsConstructor
@Slf4j
@RestController
@RequestMapping(value={"/api/historys"})
public class SystemLogsController {

	private final HistorySystemLogsService historySystemLogsService;

	/**
	 * 查询操作日志
	 * @param dto
	 * @return
	 */
	@GetMapping(value={"/systemLogs/fetch"})
	@ResponseBody
	public Message<Page<HistorySystemLogs>> fetch(HistorySystemLogsPageDto dto,
												  @CurrentUser UserInfo currentUser){
		log.debug("historys/historyLog/fetch {} ",dto);

		LambdaQueryWrapper<HistorySystemLogs> wrapper = new LambdaQueryWrapper<>();
		wrapper.eq(HistorySystemLogs::getBookId, currentUser.getBookId());

		return new Message<>(historySystemLogsService.page(dto.build(), wrapper));
	}

}
