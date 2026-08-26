package com.financial.cloud.repository.config;

import com.financial.cloud.repository.config.ConfigInsuranceFundMapper;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.financial.cloud.domain.config.ConfigInsuranceFund;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface ConfigInsuranceFundMapper extends BaseMapper<ConfigInsuranceFund> {
}
