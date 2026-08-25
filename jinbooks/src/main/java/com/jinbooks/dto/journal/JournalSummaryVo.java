package com.jinbooks.dto.journal;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.jinbooks.domain.journal.JournalSummary;
import lombok.Data;

@Data
public class JournalSummaryVo {
	
	Page<JournalSummary> tableData;
	
	JournalSummary tableSummary;
	
}
