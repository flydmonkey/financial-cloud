package com.jinbooks.service.permissions;


import lombok.RequiredArgsConstructor;
import java.util.List;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.jinbooks.common.Message;
import com.jinbooks.dto.permissions.ResourcesPageDto;
import org.springframework.stereotype.Repository;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.jinbooks.domain.permissions.Resources;
import com.jinbooks.repository.permissions.ResourcesMapper;
import com.jinbooks.service.permissions.ResourcesService;

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
