package com.financial.cloud.repository.config;

import com.financial.cloud.repository.config.InstitutionsMapper;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Select;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.financial.cloud.domain.config.Institutions;

@Mapper
public interface InstitutionsMapper extends BaseMapper<Institutions> {

	@Select("select * from  institutions where deleted = 'n' and id = #{value} or domain = #{value}  or console_domain = #{value}" )
	public Institutions getByInstIdOrDomain(String instIdOrDomain);
}
