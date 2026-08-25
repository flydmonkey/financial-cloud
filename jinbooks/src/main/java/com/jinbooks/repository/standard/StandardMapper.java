package com.jinbooks.repository.standard;

import com.jinbooks.repository.standard.StandardMapper;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.jinbooks.domain.standard.Standard;

import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface StandardMapper extends BaseMapper<Standard> {
    Standard selectOneStandard(String subjectId);
}
