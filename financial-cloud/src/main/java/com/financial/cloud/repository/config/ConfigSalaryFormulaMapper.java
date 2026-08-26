package com.financial.cloud.repository.config;

import com.financial.cloud.repository.config.ConfigSalaryFormulaMapper;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.financial.cloud.domain.config.ConfigSalaryFormula;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface ConfigSalaryFormulaMapper extends BaseMapper<ConfigSalaryFormula> {
}
