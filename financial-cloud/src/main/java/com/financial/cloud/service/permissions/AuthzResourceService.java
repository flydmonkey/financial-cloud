package com.financial.cloud.service.permissions;



import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import com.financial.cloud.service.auth.FileStorageService;
import com.financial.cloud.service.permissions.AuthzService;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.CompletableFuture;

import org.apache.commons.collections4.CollectionUtils;
import org.springframework.stereotype.Repository;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.financial.cloud.dto.auth.QueryAppResourceDto;
import com.financial.cloud.domain.idm.UserInfo;
import com.financial.cloud.domain.idm.Roles;
import com.financial.cloud.domain.permissions.Resources;
import com.financial.cloud.repository.permissions.AuthzResourceMapper;
import com.financial.cloud.service.permissions.AuthzResourceService;

@RequiredArgsConstructor
@Slf4j
@Repository
public class AuthzResourceService   extends ServiceImpl<AuthzResourceMapper,UserInfo>{

	private final AuthzService authzService;

	private final AuthzResourceMapper authzResourceMapper;

	private final FileStorageService fileStorageService;

	public AuthzResourceMapper getMapper() {
		return authzResourceMapper;
	}

	/**
	 * 根据主体获取用户对应得应用资源清单
	 * @param user
	 * @param app
	 * @return 资源清单列表
	 */
    public Set<Resources> getResourcesBySubject(UserInfo user){
    	log.debug("user {} , app {}",user);

    	// Fail-closed: menus require an active book; blank bookId must not merge multi-book packs.
    	if (user == null || user.getBookId() == null || user.getBookId().isBlank()) {
    		return Set.of();
    	}

    	List<CompletableFuture<List<Resources>>> futures = new ArrayList<>();
    	//根据用户读取应用资源
    	QueryAppResourceDto dto = new QueryAppResourceDto(user.getId());

    	//根据用户组获取应用资源
    	List<Roles> listRole = authzService.queryRolesByMembers(user);
    	for(Roles r : listRole) {
    		dto.getRoleIds().add(r.getId());
    	}
    	if (CollectionUtils.isNotEmpty(dto.getRoleIds())) {
	    	CompletableFuture<List<Resources>> subjectRoleResourcesFuture = CompletableFuture.supplyAsync(() -> {
	    		return queryResourcesByRoleId(dto);
	    	});
	    	futures.add(subjectRoleResourcesFuture);
    	}

        if (futures.isEmpty()) {
        	return Set.of();
        }

        @SuppressWarnings("unchecked")
		CompletableFuture<List<Resources>>[] completableFutures = futures.toArray(new CompletableFuture[futures.size()]);

        //合并数据并去重
        CompletableFuture<Set<Resources>> completableFuture =
        		CompletableFuture.allOf(completableFutures).thenApply(result -> {
        			Set<Resources> resourcesList = new HashSet<>();
                	for (CompletableFuture<List<Resources>> future : completableFutures) {
                		resourcesList.addAll(future.join());
                	}
                	return  resourcesList;
                });

    	return completableFuture.join();
    }


	/**
	 * 根据组列表获取资源清单
	 * @param dto
	 * @return
	 */
	public List<Resources> queryResourcesByRoleId(QueryAppResourceDto dto) {
		return getMapper().queryResourcesByRoleId(dto);
	}

}
