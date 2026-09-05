package com.financial.cloud.controller.permissions;



import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import java.util.List;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.financial.cloud.authn.annotation.CurrentUser;
import com.financial.cloud.common.Message;
import com.financial.cloud.constants.auth.ProductRoles;
import com.financial.cloud.context.WebContext;
import com.financial.cloud.domain.book.Book;
import com.financial.cloud.domain.idm.UserInfo;
import com.financial.cloud.domain.permissions.PermissionBook;
import com.financial.cloud.domain.idm.RoleMember;
import com.financial.cloud.dto.permissions.PermissionBookDto;
import com.financial.cloud.dto.permissions.PermissionBookPageDto;
import com.financial.cloud.enums.error.UsersBusinessCode;
import com.financial.cloud.exception.BusinessException;
import com.financial.cloud.service.idm.RoleMemberService;
import com.financial.cloud.service.permissions.PermissionBookService;
import org.apache.commons.lang3.StringUtils;
import org.springframework.http.MediaType;
import org.springframework.transaction.annotation.Transactional;
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

	private final RoleMemberService roleMemberService;

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
	 * 授权账套并绑定该账套下的产品角色
	 */
	@PostMapping(value = {"/add"})
	@Transactional
	public Message<PermissionBook> add(@Validated @RequestBody PermissionBookDto dto,@CurrentUser UserInfo currentUser) {
		if (!ProductRoles.isProductRoleId(dto.roleId())) {
			throw new BusinessException(UsersBusinessCode.ROLE_REQUIRED);
		}
		boolean result = true;
		for (String bookId : dto.bookIds()) {
			if (StringUtils.isBlank(bookId)) {
				continue;
			}
			PermissionBook newPermission = new PermissionBook(dto.userId(), bookId);
			result = permissionBookService.save(newPermission) && result;

			roleMemberService.remove(new LambdaQueryWrapper<RoleMember>()
					.eq(RoleMember::getMemberId, dto.userId())
					.eq(RoleMember::getBookId, bookId));
			RoleMember member = new RoleMember(dto.roleId(), dto.userId(), "USER", bookId);
			member.setId(WebContext.genId());
			result = roleMemberService.save(member) && result;
		}
		if(result) {
			return new Message<>(Message.SUCCESS);
		}
		return new Message<>(Message.FAIL);
	}
	
	@DeleteMapping(value={"/delete"}, produces = {MediaType.APPLICATION_JSON_VALUE})
	@Transactional
	public Message<RoleMember> delete(@RequestParam("ids") List<String> ids,@CurrentUser UserInfo currentUser) {
		log.debug("-delete ids : {}" , ids);
		List<PermissionBook> books = permissionBookService.listByIds(ids);
		if (permissionBookService.removeBatchByIds(ids)) {
			for (PermissionBook book : books) {
				roleMemberService.remove(new LambdaQueryWrapper<RoleMember>()
						.eq(RoleMember::getMemberId, book.getUserId())
						.eq(RoleMember::getBookId, book.getBookId()));
			}
			 return new Message<>(Message.SUCCESS);
		} else {
			return new Message<>(Message.FAIL);
		}
	}

}
