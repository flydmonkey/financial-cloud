package com.financial.cloud.repository.journal;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.financial.cloud.domain.journal.JournalSummary;
import com.financial.cloud.dto.journal.JournalSummaryDto;
import com.financial.cloud.dto.journal.JournalSummaryPageDto;

import java.util.List;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;


@Mapper
public interface JournalSummaryMapper extends BaseMapper<JournalSummary> {
    Page<JournalSummary> pageList(Page page, @Param("dto") JournalSummaryPageDto dto);

    public JournalSummary summarySum(@Param("dto") JournalSummaryPageDto dto);
    
    public List<JournalSummary> summaryAccount(JournalSummaryDto dto);
    
}
