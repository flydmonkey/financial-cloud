package com.jinbooks.repository.standard;

import com.jinbooks.repository.standard.StandardSubjectMapper;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.jinbooks.dto.book.SubjectPageDto;
import com.jinbooks.domain.standard.StandardSubject;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

@Mapper
public interface StandardSubjectMapper extends BaseMapper<StandardSubject> {
    Page<StandardSubject> pageList(Page page, @Param("Dto") SubjectPageDto dto);

}
