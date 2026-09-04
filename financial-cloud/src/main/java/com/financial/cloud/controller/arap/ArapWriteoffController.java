package com.financial.cloud.controller.arap;

import com.financial.cloud.authn.annotation.CurrentUser;
import com.financial.cloud.common.Message;
import com.financial.cloud.domain.idm.UserInfo;
import com.financial.cloud.dto.arap.ArapOpenItemVo;
import com.financial.cloud.dto.arap.ArapQueryDto;
import com.financial.cloud.dto.arap.ArapWriteoffConfirmDto;
import com.financial.cloud.dto.arap.ArapWriteoffVo;
import com.financial.cloud.service.arap.ArapWriteoffService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/arap/writeoff")
@RequiredArgsConstructor
public class ArapWriteoffController {

	private final ArapWriteoffService arapWriteoffService;

	@GetMapping("/open-items")
	public Message<List<ArapOpenItemVo>> openItems(ArapQueryDto dto, @CurrentUser UserInfo userInfo) {
		return arapWriteoffService.openItems(userInfo.getBookId(), dto);
	}

	@GetMapping("/suggest")
	public Message<List<ArapWriteoffConfirmDto.Leg>> suggest(ArapQueryDto dto, @CurrentUser UserInfo userInfo) {
		return arapWriteoffService.suggest(userInfo.getBookId(), dto);
	}

	@PostMapping("/confirm")
	public Message<String> confirm(@RequestBody ArapWriteoffConfirmDto dto, @CurrentUser UserInfo userInfo) {
		return arapWriteoffService.confirm(userInfo.getBookId(), userInfo.getId(), dto);
	}

	@PostMapping("/reverse/{id}")
	public Message<String> reverse(@PathVariable("id") String id, @CurrentUser UserInfo userInfo) {
		return arapWriteoffService.reverse(userInfo.getBookId(), id);
	}

	@GetMapping("/list")
	public Message<List<ArapWriteoffVo>> list(ArapQueryDto dto, @CurrentUser UserInfo userInfo) {
		return arapWriteoffService.list(userInfo.getBookId(), dto);
	}
}
