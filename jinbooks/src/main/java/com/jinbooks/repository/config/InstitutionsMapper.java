package com.jinbooks.repository.config;

import com.jinbooks.repository.config.InstitutionsMapper;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Select;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.jinbooks.domain.config.Institutions;

@Mapper
public interface InstitutionsMapper extends BaseMapper<Institutions> {

	@Select("select * from  institutions where deleted = 'n' and id = #{value} or domain = #{value}  or console_domain = #{value}" )
	public Institutions getByInstIdOrDomain(String instIdOrDomain);
}
