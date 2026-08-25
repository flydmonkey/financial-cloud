package com.jinbooks.repository.idm;

import com.jinbooks.repository.idm.OrganizationsMapper;
import java.util.List;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.jinbooks.dto.idm.OrgPageDto;
import org.apache.ibatis.annotations.Param;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.jinbooks.domain.idm.Organizations;

public interface OrganizationsMapper extends BaseMapper<Organizations> {

	List<Organizations> queryOrgs(Organizations organization);


	Page<Organizations> pageList(Page page, @Param("Dto") OrgPageDto dto);
}
