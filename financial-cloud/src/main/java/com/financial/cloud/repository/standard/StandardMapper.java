package com.financial.cloud.repository.standard;

import com.financial.cloud.repository.standard.StandardMapper;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.financial.cloud.domain.standard.Standard;

import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface StandardMapper extends BaseMapper<Standard> {
    Standard selectOneStandard(String subjectId);
}
