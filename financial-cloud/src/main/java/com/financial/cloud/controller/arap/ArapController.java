package com.financial.cloud.controller.arap;

import com.financial.cloud.authn.annotation.CurrentUser;
import com.financial.cloud.common.Message;
import com.financial.cloud.domain.idm.UserInfo;
import com.financial.cloud.dto.arap.ArapAgingVo;
import com.financial.cloud.dto.arap.ArapBalanceVo;
import com.financial.cloud.dto.arap.ArapDetailLineVo;
import com.financial.cloud.dto.arap.ArapQueryDto;
import com.financial.cloud.service.arap.ArapService;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.io.IOException;
import java.util.List;

@Slf4j
@RestController
@RequestMapping("/api/arap")
@RequiredArgsConstructor
public class ArapController {

	private final ArapService arapService;

	@GetMapping("/balance")
	public Message<List<ArapBalanceVo>> balance(ArapQueryDto dto, @CurrentUser UserInfo userInfo) {
		return arapService.balances(userInfo.getBookId(), dto);
	}

	@GetMapping("/detail")
	public Message<List<ArapDetailLineVo>> detail(ArapQueryDto dto, @CurrentUser UserInfo userInfo) {
		return arapService.detail(userInfo.getBookId(), dto);
	}

	@GetMapping("/aging")
	public Message<List<ArapAgingVo>> aging(ArapQueryDto dto, @CurrentUser UserInfo userInfo) {
		return arapService.aging(userInfo.getBookId(), dto);
	}

	@GetMapping("/statement/export")
	public void exportStatement(ArapQueryDto dto,
			@CurrentUser UserInfo userInfo,
			HttpServletResponse response) throws IOException {
		try {
			arapService.exportStatement(userInfo.getBookId(), dto, response);
		} catch (IllegalArgumentException ex) {
			response.reset();
			response.setStatus(400);
			response.setContentType("application/json;charset=UTF-8");
			response.getWriter().write("{\"code\":1,\"message\":\"" + ex.getMessage() + "\"}");
		}
	}
}
