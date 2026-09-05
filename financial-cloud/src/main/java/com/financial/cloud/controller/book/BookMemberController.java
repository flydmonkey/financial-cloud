package com.financial.cloud.controller.book;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

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

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.financial.cloud.authn.annotation.CurrentUser;
import com.financial.cloud.common.Message;
import com.financial.cloud.constants.auth.ProductRoles;
import com.financial.cloud.context.WebContext;
import com.financial.cloud.domain.idm.RoleMember;
import com.financial.cloud.domain.idm.Roles;
import com.financial.cloud.domain.idm.UserInfo;
import com.financial.cloud.domain.permissions.PermissionBook;
import com.financial.cloud.dto.book.BookMemberGrantDto;
import com.financial.cloud.enums.error.UsersBusinessCode;
import com.financial.cloud.exception.BusinessException;
import com.financial.cloud.service.book.BookService;
import com.financial.cloud.service.idm.RoleMemberService;
import com.financial.cloud.service.idm.RolesService;
import com.financial.cloud.service.idm.UserInfoService;
import com.financial.cloud.service.permissions.PermissionBookService;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@RequiredArgsConstructor
@Slf4j
@RestController
@RequestMapping("/api/book/members")
public class BookMemberController {

	private final PermissionBookService permissionBookService;

	private final RoleMemberService roleMemberService;

	private final UserInfoService userInfoService;

	private final RolesService rolesService;

	private final BookService bookService;

	@GetMapping(value = "/list", produces = MediaType.APPLICATION_JSON_VALUE)
	public Message<List<Map<String, Object>>> list(@RequestParam("bookId") String bookId,
			@CurrentUser UserInfo currentUser) {
		requireBookAdministrator(currentUser, bookId);
		List<PermissionBook> grants = permissionBookService.list(new LambdaQueryWrapper<PermissionBook>()
				.eq(PermissionBook::getBookId, bookId));
		List<Map<String, Object>> rows = new ArrayList<>();
		for (PermissionBook grant : grants) {
			UserInfo user = userInfoService.getById(grant.getUserId());
			if (user == null) {
				continue;
			}
			RoleMember rm = roleMemberService.getOne(new LambdaQueryWrapper<RoleMember>()
					.eq(RoleMember::getMemberId, grant.getUserId())
					.eq(RoleMember::getBookId, bookId)
					.last("limit 1"));
			String roleId = rm == null ? null : rm.getRoleId();
			String roleName = null;
			if (roleId != null) {
				Roles role = rolesService.getById(roleId);
				roleName = role == null ? roleId : role.getRoleName();
			}
			Map<String, Object> row = new HashMap<>();
			row.put("id", grant.getId());
			row.put("bookId", bookId);
			row.put("userId", user.getId());
			row.put("username", user.getUsername());
			row.put("displayName", user.getDisplayName());
			row.put("roleId", roleId);
			row.put("roleName", roleName);
			rows.add(row);
		}
		return Message.ok(rows);
	}

	@GetMapping(value = "/search", produces = MediaType.APPLICATION_JSON_VALUE)
	public Message<List<Map<String, Object>>> search(@RequestParam("bookId") String bookId,
			@RequestParam("q") String q,
			@CurrentUser UserInfo currentUser) {
		requireBookAdministrator(currentUser, bookId);
		String keyword = StringUtils.trimToEmpty(q);
		if (keyword.length() < 1) {
			return Message.ok(List.of());
		}
		List<UserInfo> users = userInfoService.list(new LambdaQueryWrapper<UserInfo>()
				.and(w -> w.like(UserInfo::getUsername, keyword).or().like(UserInfo::getDisplayName, keyword))
				.eq(UserInfo::getStatus, 1)
				.last("limit 20"));
		List<Map<String, Object>> rows = new ArrayList<>();
		for (UserInfo user : users) {
			Map<String, Object> row = new HashMap<>();
			row.put("userId", user.getId());
			row.put("username", user.getUsername());
			row.put("displayName", user.getDisplayName());
			rows.add(row);
		}
		return Message.ok(rows);
	}

	@PostMapping(value = "/grant", produces = MediaType.APPLICATION_JSON_VALUE)
	@Transactional
	public Message<Void> grant(@Validated @RequestBody BookMemberGrantDto dto,
			@CurrentUser UserInfo currentUser) {
		requireBookAdministrator(currentUser, dto.bookId());
		if (!ProductRoles.isProductRoleId(dto.roleId())) {
			throw new BusinessException(UsersBusinessCode.ROLE_REQUIRED);
		}
		UserInfo target = userInfoService.getById(dto.userId());
		if (target == null) {
			throw new BusinessException(UsersBusinessCode.USER_FORBIDDEN);
		}
		String bookId = dto.bookId();
		long exists = permissionBookService.count(new LambdaQueryWrapper<PermissionBook>()
				.eq(PermissionBook::getUserId, dto.userId())
				.eq(PermissionBook::getBookId, bookId));
		if (exists == 0) {
			permissionBookService.save(new PermissionBook(dto.userId(), bookId));
		}
		roleMemberService.remove(new LambdaQueryWrapper<RoleMember>()
				.eq(RoleMember::getMemberId, dto.userId())
				.eq(RoleMember::getBookId, bookId));
		RoleMember member = new RoleMember(dto.roleId(), dto.userId(), "USER", bookId);
		member.setId(WebContext.genId());
		roleMemberService.save(member);
		return new Message<>(Message.SUCCESS);
	}

	@DeleteMapping(value = "/revoke", produces = MediaType.APPLICATION_JSON_VALUE)
	@Transactional
	public Message<Void> revoke(@RequestParam("bookId") String bookId,
			@RequestParam("userId") String userId,
			@CurrentUser UserInfo currentUser) {
		requireBookAdministrator(currentUser, bookId);
		if (StringUtils.equals(userId, currentUser.getId())) {
			throw new BusinessException(UsersBusinessCode.PERMISSION_DENIED);
		}
		List<PermissionBook> grants = permissionBookService.list(new LambdaQueryWrapper<PermissionBook>()
				.eq(PermissionBook::getUserId, userId)
				.eq(PermissionBook::getBookId, bookId));
		if (!grants.isEmpty()) {
			permissionBookService.removeBatchByIds(grants.stream().map(PermissionBook::getId).toList());
		}
		roleMemberService.remove(new LambdaQueryWrapper<RoleMember>()
				.eq(RoleMember::getMemberId, userId)
				.eq(RoleMember::getBookId, bookId));
		return new Message<>(Message.SUCCESS);
	}

	/**
	 * Caller must hold ROLE_ADMINISTRATORS for the target book (not merely the active navbar book).
	 */
	private void requireBookAdministrator(UserInfo currentUser, String bookId) {
		bookService.requireBookAdministrator(currentUser, bookId);
	}
}
