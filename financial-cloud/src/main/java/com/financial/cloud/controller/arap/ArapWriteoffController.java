package com.financial.cloud.controller.arap;

import com.financial.cloud.service.book.BookOwnershipGuard;
import com.financial.cloud.authn.annotation.CurrentUser;
import com.financial.cloud.common.Message;
import com.financial.cloud.constants.auth.ProductRoles;
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
    private final BookOwnershipGuard bookOwnershipGuard;

	private final ArapWriteoffService arapWriteoffService;

	@GetMapping("/open-items")
	public Message<List<ArapOpenItemVo>> openItems(ArapQueryDto dto, @CurrentUser UserInfo userInfo) {
        bookOwnershipGuard.checkRequest("arap_writeoff", dto);
		return arapWriteoffService.openItems(userInfo.getBookId(), dto);
	}

	@GetMapping("/suggest")
	public Message<List<ArapWriteoffConfirmDto.Leg>> suggest(ArapQueryDto dto, @CurrentUser UserInfo userInfo) {
        bookOwnershipGuard.checkRequest("arap_writeoff", dto);
		return arapWriteoffService.suggest(userInfo.getBookId(), dto);
	}

	@PostMapping("/confirm")
	public Message<String> confirm(@RequestBody ArapWriteoffConfirmDto dto, @CurrentUser UserInfo userInfo) {
        bookOwnershipGuard.checkRequest("arap_writeoff", dto);
		ProductRoles.requireWriteBusiness();
		return arapWriteoffService.confirm(userInfo.getBookId(), userInfo.getId(), dto);
	}

	@PostMapping("/reverse/{id}")
	public Message<String> reverse(@PathVariable("id") String id, @CurrentUser UserInfo userInfo) {
        bookOwnershipGuard.checkReference("arap_writeoff", "id", id);
		ProductRoles.requireWriteBusiness();
		return arapWriteoffService.reverse(userInfo.getBookId(), id);
	}

	@GetMapping("/list")
	public Message<List<ArapWriteoffVo>> list(ArapQueryDto dto, @CurrentUser UserInfo userInfo) {
        bookOwnershipGuard.checkRequest("arap_writeoff", dto);
		return arapWriteoffService.list(userInfo.getBookId(), dto);
	}
}
