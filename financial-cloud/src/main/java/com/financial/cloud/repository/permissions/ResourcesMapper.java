package com.financial.cloud.repository.permissions;

import com.financial.cloud.repository.permissions.ResourcesMapper;
import java.util.List;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.financial.cloud.domain.permissions.Resources;
import com.financial.cloud.dto.permissions.ResourcesPageDto;
import org.apache.ibatis.annotations.Param;

public  interface ResourcesMapper extends BaseMapper<Resources> {

	public List<Resources> queryResourcesTree(Resources resource);

	Page<Resources> pageList(Page page, @Param("Dto") ResourcesPageDto dto);
}
