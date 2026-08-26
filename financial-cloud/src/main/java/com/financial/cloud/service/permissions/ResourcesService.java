package com.financial.cloud.service.permissions;


import lombok.RequiredArgsConstructor;
import java.util.List;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.financial.cloud.common.Message;
import com.financial.cloud.dto.permissions.ResourcesPageDto;
import org.springframework.stereotype.Repository;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.financial.cloud.domain.permissions.Resources;
import com.financial.cloud.repository.permissions.ResourcesMapper;
import com.financial.cloud.service.permissions.ResourcesService;

@RequiredArgsConstructor
@Repository
public class ResourcesService  extends ServiceImpl<ResourcesMapper,Resources>{

	private final ResourcesMapper resourcesMapper;

	public List<Resources> queryResourcesTree(Resources resource){
	   return  resourcesMapper.queryResourcesTree(resource);
	}
	public Message<Page<Resources>> pageList(ResourcesPageDto dto) {
		return new Message<>(Message.SUCCESS, resourcesMapper.pageList(dto.build(), dto));
	}
}
