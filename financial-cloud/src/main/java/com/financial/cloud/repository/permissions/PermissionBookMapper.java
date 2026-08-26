/**
 *
 */
package com.financial.cloud.repository.permissions;

import com.financial.cloud.repository.permissions.PermissionBookMapper;
import org.apache.ibatis.annotations.Param;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.financial.cloud.domain.book.Book;
import com.financial.cloud.domain.permissions.PermissionBook;
import com.financial.cloud.dto.permissions.PermissionBookPageDto;

/**
 * @author Crystal.sea
 *
 */

public  interface PermissionBookMapper extends BaseMapper<PermissionBook> {

    public Page<Book> userAccessBook(Page page, @Param("dto") PermissionBookPageDto permission);

    public Page<Book> userNotAccessBook(Page page, @Param("dto") PermissionBookPageDto permission);

}
