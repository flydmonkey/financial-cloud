package com.financial.cloud.service.journal;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.financial.cloud.common.Message;
import com.financial.cloud.dto.common.ListIdsDto;
import com.financial.cloud.domain.journal.JournalSummary;
import com.financial.cloud.dto.journal.JournalSummaryDto;
import com.financial.cloud.dto.journal.JournalSummaryPageDto;
import com.financial.cloud.dto.journal.JournalSummaryVo;
import com.financial.cloud.repository.journal.JournalSummaryMapper;
import com.financial.cloud.service.journal.JournalSummaryService;

import lombok.extern.slf4j.Slf4j;

import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;

@Slf4j
@Service
public class JournalSummaryService extends ServiceImpl<JournalSummaryMapper, JournalSummary>{
    public Message<JournalSummaryVo> pageList(JournalSummaryPageDto dto) {
    	JournalSummaryVo vo = new JournalSummaryVo();
        //LambdaQueryWrapper<JournalSummary> wrapper = new LambdaQueryWrapper<>();
    	if(StringUtils.isNotBlank( dto.getYearPeriodPicker())) {
	        dto.setYears(Integer.valueOf(dto.getYearPeriodPicker().split("-")[0]));
	        dto.setPeriods(Integer.valueOf(dto.getYearPeriodPicker().split("-")[1]));
    	}
    	//summary.setYearPeriod(Integer.valueOf(dto.getYearPeriodPicker().replace("-", "")));
        vo.setTableData(this.getBaseMapper().pageList(dto.build(), dto));
        vo.setTableSummary(this.getBaseMapper().summarySum(dto));
        return new Message<>(Message.SUCCESS, vo);
    }
    @Transactional
    public Message<String> delete(ListIdsDto dto) {
        List<String> listIds = dto.getListIds();
        boolean result = false;
        for(String id : listIds) {

        }
        result = super.removeBatchByIds(listIds);

        return result ? new Message<>(Message.SUCCESS, "删除成功") : new Message<>(Message.FAIL, "删除失败");
    }
	public Message<String> summaryAccount(JournalSummaryDto dto) {
		boolean saveResult =false;
		dto.setYearPeriodStart(dto.getYearPeriodPicker()+"-01");
		dto.setYears(Integer.valueOf(dto.getYearPeriodPicker().split("-")[0]));
		dto.setPeriods(Integer.valueOf(dto.getYearPeriodPicker().split("-")[1]));
		dto.setYearPeriod(Integer.valueOf(dto.getYearPeriodPicker().replace("-", "")));
		LambdaQueryWrapper<JournalSummary> wrapper = new LambdaQueryWrapper<>();
		wrapper.eq(JournalSummary::getBookId,dto.getBookId());
		wrapper.eq(JournalSummary::getYearPeriod, dto.getYearPeriod());
		wrapper.eq(JournalSummary::getDeleted, 'n');
		if(this.getBaseMapper().selectCount(wrapper) <= 0) {
	        List<JournalSummary> summaryList = this.getBaseMapper().summaryAccount(dto);
	        for (JournalSummary summary :summaryList) {
	        	summary.setYears(dto.getYears());
	        	summary.setPeriods(dto.getPeriods());
	        	summary.setYearPeriod(dto.getYearPeriod());
	        }
	        saveResult = this.saveBatch(summaryList);
	        return saveResult ? new Message<>(Message.SUCCESS, "新增成功") : new Message<>(Message.FAIL, "新增失败");
		}else {
			return new Message<>(Message.FAIL, "本期已存在！");
		}
	}
}
