package com.jinbooks.service.hr;


import lombok.RequiredArgsConstructor;
import com.jinbooks.repository.hr.EmployeeSalarySummaryMapper;
import com.jinbooks.repository.hr.EmployeeSalaryMapper;
import com.jinbooks.repository.book.BookSubjectMapper;
import com.jinbooks.repository.book.BookMapper;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.toolkit.ObjectUtils;
import com.baomidou.mybatisplus.core.toolkit.StringUtils;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.jinbooks.common.Message;
import com.jinbooks.domain.hr.EmployeeSalarySummary;
import com.jinbooks.dto.hr.SalaryDetailPageDto;
import com.jinbooks.dto.hr.SalarySummaryChangeDto;
import com.jinbooks.service.config.ConfigSysService;
import com.jinbooks.service.hr.EmployeeSalarySummaryService;
import com.jinbooks.service.voucher.VoucherService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.YearMonth;

/**
 * @description:
 * @author: orangeBabu
 * @time: 2025/2/27 17:45
 */

@Service

@RequiredArgsConstructor
public class EmployeeSalarySummaryService extends ServiceImpl<EmployeeSalarySummaryMapper, EmployeeSalarySummary>{

    private final EmployeeSalaryMapper employeeSalaryMapper;

    private final VoucherService voucherService;

    private final BookMapper bookMapper;

    private final BookSubjectMapper bookSubjectMapper;

    private final ConfigSysService configSysService;
    @Transactional
    public Message<String> save(SalarySummaryChangeDto dto) {
        YearMonth lastMonth = YearMonth.parse(configSysService.getCurrentTerm(dto.getBookId()));
        dto.setBelongDate(lastMonth);
        int count = employeeSalaryMapper.countEmployeeSalaries(dto);
        if (count > 0) {
            super.remove(Wrappers.<EmployeeSalarySummary>lambdaQuery()
                    .eq(EmployeeSalarySummary::getBookId, dto.getBookId())
                    .eq(EmployeeSalarySummary::getBelongDate, lastMonth));

            //员工费用
            boolean result =false;
            EmployeeSalarySummary employeeSalarySummary = employeeSalaryMapper.selectSalarySummary(dto);
            if(employeeSalarySummary != null) {
            	result = super.save(employeeSalarySummary);
            }

            //兼职费用
            employeeSalarySummary = employeeSalaryMapper.selectSalarySummaryLabor(dto);
            if(employeeSalarySummary != null) {
            	result = super.save(employeeSalarySummary);
            }
            return result ? Message.ok("成功") : Message.failed("失败");
        }

        return Message.failed("暂无数据，请先计算当月工资然后推送工资明细");
    }
    public Message<Page<EmployeeSalarySummary>> pageList(SalaryDetailPageDto dto) {
        LambdaQueryWrapper<EmployeeSalarySummary> wrapper = new LambdaQueryWrapper<>();
        if (StringUtils.isNotEmpty(dto.getLabel())) {
            wrapper.like(EmployeeSalarySummary::getLabel, dto.getLabel());
        }
        wrapper.eq(EmployeeSalarySummary::getBookId, dto.getBookId());
        if (ObjectUtils.isNotEmpty(dto.getBelongDateRange()) && dto.getBelongDateRange().length >= 2) {
            String startDate = dto.getBelongDateRange()[0];
            String endDate = dto.getBelongDateRange()[1];

            wrapper.ge(EmployeeSalarySummary::getBelongDate, startDate)
                    .le(EmployeeSalarySummary::getBelongDate, endDate);
        }
        wrapper.orderByDesc(EmployeeSalarySummary::getBelongDate);
        Page<EmployeeSalarySummary> page = super.page(dto.build(), wrapper);
        return Message.ok(page);
    }
    public EmployeeSalarySummary selectSalarySummary(SalarySummaryChangeDto dto) {
        return this.baseMapper.selectSalarySummary(dto);
    }

}
