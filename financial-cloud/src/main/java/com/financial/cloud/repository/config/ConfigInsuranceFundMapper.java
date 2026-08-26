package com.financial.cloud.repository.config;

import com.financial.cloud.repository.config.ConfigInsuranceFundMapper;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.financial.cloud.domain.config.ConfigInsuranceFund;
import org.apache.ibatis.annotations.Mapper;

/**
 * @description:
 * @author: orangeBabu
 * @time: 2025/2/12 15:15
 */

@Mapper
public interface ConfigInsuranceFundMapper extends BaseMapper<ConfigInsuranceFund> {
}
