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
import com.financial.cloud.service.book.SettlementService;
import com.financial.cloud.service.config.ConfigSysService;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.YearMonth;
import java.util.*;

@Slf4j
@Service
@RequiredArgsConstructor
public class JournalSummaryService extends ServiceImpl<JournalSummaryMapper, JournalSummary> {

	private final SettlementService settlementService;
	private final ConfigSysService configSysService;

	public Message<JournalSummaryVo> pageList(JournalSummaryPageDto dto) {
		if (StringUtils.isNotBlank(dto.getYearPeriodPicker())) {
			Message<String> periodCheck = ensurePeriodNotAfterCurrent(dto.getBookId(), dto.getYearPeriodPicker());
			if (periodCheck.getCode() != 0) {
				return new Message<>(Message.FAIL, periodCheck.getMessage());
			}
			dto.setYears(Integer.valueOf(dto.getYearPeriodPicker().split("-")[0]));
			dto.setPeriods(Integer.valueOf(dto.getYearPeriodPicker().split("-")[1]));
		}
		JournalSummaryVo vo = new JournalSummaryVo();
		vo.setTableData(this.getBaseMapper().pageList(dto.build(), dto));
		vo.setTableSummary(this.getBaseMapper().summarySum(dto));
		return new Message<>(Message.SUCCESS, vo);
	}

	@Transactional
	public Message<String> delete(ListIdsDto dto) {
		List<String> listIds = dto.getListIds();
		boolean result = super.removeBatchByIds(listIds);
		return result ? new Message<>(Message.SUCCESS, "删除成功") : new Message<>(Message.FAIL, "删除失败");
	}

	public Message<String> summaryAccount(JournalSummaryDto dto) {
		if (StringUtils.isBlank(dto.getYearPeriodPicker())) {
			return Message.failed("请选择期间");
		}
		Message<String> periodCheck = ensurePeriodNotAfterCurrent(dto.getBookId(), dto.getYearPeriodPicker());
		if (periodCheck.getCode() != 0) {
			return periodCheck;
		}
		periodCheck = settlementService.check(dto.getBookId(), dto.getYearPeriodPicker());
		if (periodCheck.getCode() != 0) {
			return periodCheck;
		}
		dto.setYearPeriodStart(dto.getYearPeriodPicker() + "-01");
		dto.setYears(Integer.valueOf(dto.getYearPeriodPicker().split("-")[0]));
		dto.setPeriods(Integer.valueOf(dto.getYearPeriodPicker().split("-")[1]));
		dto.setYearPeriod(Integer.valueOf(dto.getYearPeriodPicker().replace("-", "")));
		LambdaQueryWrapper<JournalSummary> wrapper = new LambdaQueryWrapper<>();
		wrapper.eq(JournalSummary::getBookId, dto.getBookId());
		wrapper.eq(JournalSummary::getYearPeriod, dto.getYearPeriod());
		wrapper.eq(JournalSummary::getDeleted, 'n');
		if (this.getBaseMapper().selectCount(wrapper) <= 0) {
			List<JournalSummary> summaryList = this.getBaseMapper().summaryAccount(dto);
			for (JournalSummary summary : summaryList) {
				summary.setYears(dto.getYears());
				summary.setPeriods(dto.getPeriods());
				summary.setYearPeriod(dto.getYearPeriod());
			}
			boolean saveResult = this.saveBatch(summaryList);
			return saveResult ? new Message<>(Message.SUCCESS, "新增成功") : new Message<>(Message.FAIL, "新增失败");
		}
		return new Message<>(Message.FAIL, "本期已存在！");
	}

	private Message<String> ensurePeriodNotAfterCurrent(String bookId, String yearPeriodPicker) {
		String currentTerm = configSysService.getCurrentTerm(bookId);
		if (YearMonth.parse(yearPeriodPicker).isAfter(YearMonth.parse(currentTerm))) {
			return Message.failed("期间[" + yearPeriodPicker + "]不能超过当前账期[" + currentTerm + "]");
		}
		return Message.ok("ok");
	}
}
