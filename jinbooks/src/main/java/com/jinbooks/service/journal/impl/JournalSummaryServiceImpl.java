/*
 * Copyright [2025] [JinBooks of copyright http://www.jinbooks.com]
 * 
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * 
 *     http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 */
 

package com.jinbooks.service.journal.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.jinbooks.common.Message;
import com.jinbooks.dto.common.ListIdsDto;
import com.jinbooks.domain.journal.JournalSummary;
import com.jinbooks.dto.journal.JournalSummaryDto;
import com.jinbooks.dto.journal.JournalSummaryPageDto;
import com.jinbooks.dto.journal.JournalSummaryVo;
import com.jinbooks.repository.journal.JournalSummaryMapper;
import com.jinbooks.service.journal.JournalSummaryService;

import lombok.extern.slf4j.Slf4j;

import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;

@Slf4j
@Service
public class JournalSummaryServiceImpl extends ServiceImpl<JournalSummaryMapper, JournalSummary> implements JournalSummaryService {


    @Override
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

    

    @Override
    @Transactional
    public Message<String> delete(ListIdsDto dto) {
        List<String> listIds = dto.getListIds();
        boolean result = false;
        for(String id : listIds) {

        }
        result = super.removeBatchByIds(listIds);

        return result ? new Message<>(Message.SUCCESS, "删除成功") : new Message<>(Message.FAIL, "删除失败");
    }



	@Override
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
