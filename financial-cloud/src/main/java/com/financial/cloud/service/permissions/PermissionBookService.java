package com.financial.cloud.service.permissions;



import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Repository;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.financial.cloud.domain.book.Book;
import com.financial.cloud.domain.permissions.PermissionBook;
import com.financial.cloud.dto.permissions.PermissionBookPageDto;
import com.financial.cloud.repository.permissions.PermissionBookMapper;

@RequiredArgsConstructor
@Slf4j
@Repository
public class PermissionBookService  extends ServiceImpl<PermissionBookMapper,PermissionBook>{

	private final PermissionBookMapper permissionBookMapper;
	public Page<Book> userAccessBook(Page page, PermissionBookPageDto dto) {
		return permissionBookMapper.userAccessBook(page, dto);
	}
	public Page<Book> userNotAccessBook(Page page, PermissionBookPageDto dto) {
		return permissionBookMapper.userNotAccessBook(page, dto);
	}

	/** True when {@code userId} has an explicit permission_book grant for {@code bookId}. */
	public boolean userHasBook(String userId, String bookId) {
		if (StringUtils.isAnyBlank(userId, bookId)) {
			return false;
		}
		return count(new LambdaQueryWrapper<PermissionBook>()
				.eq(PermissionBook::getUserId, userId)
				.eq(PermissionBook::getBookId, bookId)) > 0;
	}


}
