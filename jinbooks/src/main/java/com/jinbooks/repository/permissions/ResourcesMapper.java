/**
 *
 */
package com.jinbooks.repository.permissions;

import com.jinbooks.repository.permissions.ResourcesMapper;
import java.util.List;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.jinbooks.domain.permissions.Resources;
import com.jinbooks.dto.permissions.ResourcesPageDto;
import org.apache.ibatis.annotations.Param;

/**
 * @author Crystal.sea
 *
 */

public  interface ResourcesMapper extends BaseMapper<Resources> {

	public List<Resources> queryResourcesTree(Resources resource);

	Page<Resources> pageList(Page page, @Param("Dto") ResourcesPageDto dto);
}
