/**
 *
 */
package com.jinbooks.repository.permissions;

import com.jinbooks.repository.permissions.PermissionBookMapper;
import org.apache.ibatis.annotations.Param;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.jinbooks.domain.book.Book;
import com.jinbooks.domain.permissions.PermissionBook;
import com.jinbooks.dto.permissions.PermissionBookPageDto;

/**
 * @author Crystal.sea
 *
 */

public  interface PermissionBookMapper extends BaseMapper<PermissionBook> {

    public Page<Book> userAccessBook(Page page, @Param("dto") PermissionBookPageDto permission);

    public Page<Book> userNotAccessBook(Page page, @Param("dto") PermissionBookPageDto permission);

}
