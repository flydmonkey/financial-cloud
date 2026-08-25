package com.jinbooks.service.permissions;



import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Repository;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.jinbooks.domain.book.Book;
import com.jinbooks.domain.permissions.PermissionBook;
import com.jinbooks.dto.permissions.PermissionBookPageDto;
import com.jinbooks.repository.permissions.PermissionBookMapper;
import com.jinbooks.service.permissions.PermissionBookService;

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


}
