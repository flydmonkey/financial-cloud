package com.financial.cloud.repository.config;

import com.financial.cloud.repository.config.ConfigSalaryFormulaMapper;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.financial.cloud.domain.config.ConfigSalaryFormula;
import org.apache.ibatis.annotations.Mapper;

/**
 * @description:
 * @author: orangeBabu
 * @time: 2025/2/8 17:54
 */

@Mapper
public interface ConfigSalaryFormulaMapper extends BaseMapper<ConfigSalaryFormula> {
}
