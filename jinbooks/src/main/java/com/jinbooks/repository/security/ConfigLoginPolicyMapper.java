package com.jinbooks.repository.security;

import com.jinbooks.repository.security.ConfigLoginPolicyMapper;
import org.apache.ibatis.annotations.Mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.jinbooks.domain.security.ConfigLoginPolicy;

/**
 * @author Crystal.sea
 *
 */

@Mapper
public interface ConfigLoginPolicyMapper extends BaseMapper<ConfigLoginPolicy> {

}
