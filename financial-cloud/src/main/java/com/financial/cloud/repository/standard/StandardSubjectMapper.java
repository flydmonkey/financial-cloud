package com.financial.cloud.repository.standard;

import com.financial.cloud.repository.standard.StandardSubjectMapper;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.financial.cloud.dto.book.SubjectPageDto;
import com.financial.cloud.domain.standard.StandardSubject;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

@Mapper
public interface StandardSubjectMapper extends BaseMapper<StandardSubject> {
    Page<StandardSubject> pageList(Page page, @Param("Dto") SubjectPageDto dto);

}
