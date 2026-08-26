package com.financial.cloud.repository.config;

import com.financial.cloud.repository.config.ConfigPersonalTaxMapper;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.financial.cloud.domain.config.ConfigPersonalTax;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface ConfigPersonalTaxMapper extends BaseMapper<ConfigPersonalTax> {
}
