package com.financial.cloud.dto.journal;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.financial.cloud.domain.journal.JournalSummary;
import lombok.Data;

@Data
public class JournalSummaryVo {
	
	Page<JournalSummary> tableData;
	
	JournalSummary tableSummary;
	
}
