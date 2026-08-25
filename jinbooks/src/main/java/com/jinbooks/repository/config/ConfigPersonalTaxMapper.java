package com.jinbooks.repository.config;

import com.jinbooks.repository.config.ConfigPersonalTaxMapper;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.jinbooks.domain.config.ConfigPersonalTax;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface ConfigPersonalTaxMapper extends BaseMapper<ConfigPersonalTax> {
}
