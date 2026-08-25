/*
 * Copyright [2025] [JinBooks of copyright http://www.jinbooks.com]
 * 
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * 
 *     http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 */
 





package com.jinbooks.service.permissions.impl;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Repository;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.jinbooks.domain.book.Book;
import com.jinbooks.domain.permissions.PermissionBook;
import com.jinbooks.dto.permissions.PermissionBookPageDto;
import com.jinbooks.repository.permissions.PermissionBookMapper;
import com.jinbooks.service.permissions.PermissionBookService;

@Repository
public class PermissionBookServiceImpl  extends ServiceImpl<PermissionBookMapper,PermissionBook> implements PermissionBookService {
	static final  Logger logger = LoggerFactory.getLogger(PermissionBookServiceImpl.class);

	@Autowired
	PermissionBookMapper permissionBookMapper;

	@Override
	public Page<Book> userAccessBook(Page page, PermissionBookPageDto dto) {
		return permissionBookMapper.userAccessBook(page, dto);
	}

	
	@Override
	public Page<Book> userNotAccessBook(Page page, PermissionBookPageDto dto) {
		return permissionBookMapper.userNotAccessBook(page, dto);
	}


}
