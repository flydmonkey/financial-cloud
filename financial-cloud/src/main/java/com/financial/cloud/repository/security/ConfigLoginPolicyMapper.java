package com.financial.cloud.repository.security;

import com.financial.cloud.repository.security.ConfigLoginPolicyMapper;
import org.apache.ibatis.annotations.Mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.financial.cloud.domain.security.ConfigLoginPolicy;

/**
 * @author Crystal.sea
 *
 */

@Mapper
public interface ConfigLoginPolicyMapper extends BaseMapper<ConfigLoginPolicy> {

}
