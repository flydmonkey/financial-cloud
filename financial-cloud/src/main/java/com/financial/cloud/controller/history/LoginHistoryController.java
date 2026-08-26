package com.financial.cloud.controller.history;



import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.financial.cloud.authn.annotation.CurrentUser;
import com.financial.cloud.common.Message;
import com.financial.cloud.domain.history.HistoryLogin;
import com.financial.cloud.dto.history.HistoryLoginPageDto;
import com.financial.cloud.domain.idm.UserInfo;
import com.financial.cloud.service.history.HistoryLoginService;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 登录日志查询
 *
 * @author Crystal.sea
 *
 */

@RequiredArgsConstructor
@Slf4j
@RestController
@RequestMapping(value={"/api/historys"})
public class LoginHistoryController {

	private final HistoryLoginService historyLoginService;

	/**
	 * @param dto
	 * @return
	 */
	@GetMapping(value={"/loginHistory/fetch"})
	public Message<Page<HistoryLogin>> fetch(
				@ModelAttribute("historyLogin") HistoryLoginPageDto dto,
				@CurrentUser UserInfo currentUser
			){
		log.debug("historys/loginHistory/fetch/ {}",dto);

		LambdaQueryWrapper<HistoryLogin> wrapper = new LambdaQueryWrapper<>();
		wrapper.eq(HistoryLogin::getBookId, currentUser.getBookId());

		return new Message<>(historyLoginService.page(dto.build(), wrapper));
	}

}
