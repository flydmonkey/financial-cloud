package com.financial.cloud.repository.journal;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.financial.cloud.domain.journal.JournalEntry;
import com.financial.cloud.dto.journal.JournalEntryPageDto;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;


@Mapper
public interface JournalEntryMapper extends BaseMapper<JournalEntry> {
    Page<JournalEntry> pageList(Page page, @Param("dto") JournalEntryPageDto dto);
  
}
