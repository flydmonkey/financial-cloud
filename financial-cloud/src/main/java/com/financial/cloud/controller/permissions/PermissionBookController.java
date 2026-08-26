package com.financial.cloud.controller.permissions;



import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import java.util.List;

import com.baomidou.mybatisplus.core.incrementer.IdentifierGenerator;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.financial.cloud.authn.annotation.CurrentUser;
import com.financial.cloud.common.Message;
import com.financial.cloud.domain.book.Book;
import com.financial.cloud.domain.idm.UserInfo;
import com.financial.cloud.domain.permissions.PermissionBook;
import com.financial.cloud.domain.idm.RoleMember;
import com.financial.cloud.dto.permissions.PermissionBookDto;
import com.financial.cloud.dto.permissions.PermissionBookPageDto;
import com.financial.cloud.service.history.HistorySystemLogsService;
import com.financial.cloud.service.permissions.PermissionBookService;
import org.springframework.http.MediaType;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;


@RequiredArgsConstructor
@Slf4j
@RestController
@RequestMapping(value={"/api/permissions/permissionBook"})
public class PermissionBookController {

	private final PermissionBookService permissionBookService;

	private final HistorySystemLogsService historySystemLogsService;

	private final IdentifierGenerator identifierGenerator;

	@GetMapping(value = { "/userAccessBook" })
	public Message<Page<Book>> userAccessBook(PermissionBookPageDto dto,
													@CurrentUser UserInfo currentUser) {
		log.debug("userAccessBook : {}",dto);
		return Message.ok(permissionBookService.userAccessBook(dto.build(), dto));
	}

	@GetMapping(value = { "/userNotAccessBook" })
	public Message<Page<Book>> userNotAccessBook(PermissionBookPageDto dto,
													   @CurrentUser UserInfo currentUser) {
		log.debug("userNotAccessBook : {}",dto);
		return Message.ok(permissionBookService.userNotAccessBook(dto.build(), dto));
	}
	
	
	/**
	 * Members add to the Group
	 * @param currentUser
	 * @return
	 */
	@PostMapping(value = {"/add"})
	public Message<PermissionBook> add(@Validated @RequestBody PermissionBookDto dto,@CurrentUser UserInfo currentUser) {
		boolean result = true;
		for (int i = 0; i < dto.bookIds().size(); i++) {
			PermissionBook newPermission =
					new PermissionBook(
							dto.userId(),
							dto.bookIds().get(i));
			result = permissionBookService.save(newPermission);
		}
		if(result) {
			return new Message<>(Message.SUCCESS);
		}
		return new Message<>(Message.FAIL);
	}
	
	@DeleteMapping(value={"/delete"}, produces = {MediaType.APPLICATION_JSON_VALUE})
	public Message<RoleMember> delete(@RequestParam("ids") List<String> ids,@CurrentUser UserInfo currentUser) {
		log.debug("-delete ids : {}" , ids);
		if (permissionBookService.removeBatchByIds(ids)) {
			 return new Message<>(Message.SUCCESS);
		} else {
			return new Message<>(Message.FAIL);
		}
	}

}
