package com.financial.cloud.service.permissions;



import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import com.financial.cloud.service.auth.FileStorageService;
import java.util.ArrayList;
import java.util.List;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.stereotype.Repository;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.financial.cloud.constants.ConstsRoles;
import com.financial.cloud.dto.auth.QueryGroupMembersDto;
import com.financial.cloud.domain.idm.UserInfo;
import com.financial.cloud.domain.idm.Roles;
import com.financial.cloud.repository.permissions.AuthzMapper;
import com.financial.cloud.service.permissions.AuthzService;

@RequiredArgsConstructor
@Slf4j
@Repository
public class AuthzService   extends ServiceImpl<AuthzMapper,UserInfo>{

	private final AuthzMapper authzMapper;

	private final FileStorageService fileStorageService;
	public List<Roles> queryRoles(UserInfo userInfo){
		// query groups for user
        QueryGroupMembersDto groupMembersDto = new QueryGroupMembersDto();
        groupMembersDto.add(userInfo.getId());
        List<Roles> listGroup = authzMapper.queryRolesByMembers(groupMembersDto);
        log.debug("listGroup : {}" , listGroup);
        return listGroup;
	}
	public List<Roles> queryRolesByMembers(UserInfo userInfo){
		// query groups for user
        QueryGroupMembersDto groupMembersDto = new QueryGroupMembersDto();
        groupMembersDto.add(userInfo.getId());
        List<Roles> listGroup = authzMapper.queryRolesByMembers(groupMembersDto);
        log.debug("listGroup : {}" , listGroup);
        return listGroup;
	}

	/**
     * grant Authority by userinfo
     *
     * @param userInfo
     * @return ArrayList<GrantedAuthority>
     */
    public List<GrantedAuthority> grantAuthority(UserInfo userInfo) {
    	List<Roles> listGroup = queryRoles(userInfo);
        //set default groups
        ArrayList<GrantedAuthority> grantedAuthority = new ArrayList<>();
        grantedAuthority.add(ConstsRoles.ROLE_USER);
        grantedAuthority.add(ConstsRoles.ROLE_ALL_USER);
        grantedAuthority.add(ConstsRoles.ROLE_GENERAL_USER);
        for (Roles group : listGroup) {
            grantedAuthority.add(new SimpleGrantedAuthority(group.getId()));
            //Group Code和id不同的情况
            if(!grantedAuthority.contains(new SimpleGrantedAuthority(group.getRoleCode()))) {
            	grantedAuthority.add(new SimpleGrantedAuthority(group.getRoleCode()));
            }
            //判断角色类型
        	if(group.getCategory().equals(ConstsRoles.Category.SUPERVISOR)) {
        		grantedAuthority.add(ConstsRoles.ROLE_SUPERVISOR);
        	}else if(group.getCategory().equals(ConstsRoles.Category.ADMINISTRATOR)) {
        		grantedAuthority.add(ConstsRoles.ROLE_ADMINISTRATOR);
        	}else if(group.getCategory().equals(ConstsRoles.Category.MANAGER)) {
        		grantedAuthority.add(ConstsRoles.ROLE_MANAGER);
        	}
        }
        log.debug("Authority : {}" , grantedAuthority);
        return grantedAuthority;
    }

    public UserInfo findUserById(String userId) {
		return authzMapper.findUserById(userId);
	}

	public List<Roles> queryGroupsByMembers(QueryGroupMembersDto dto) {
		return authzMapper.queryRolesByMembers(dto);
	}

}
