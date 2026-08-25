package com.jinbooks.service.statement;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.toolkit.ObjectUtils;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.jinbooks.common.Message;
import com.jinbooks.domain.statement.StatementCashFlow;
import com.jinbooks.repository.statement.StatementCashFlowMapper;
import com.jinbooks.service.statement.StatementCashFlowService;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

/**
 * @description:
 * @author: orangeBabu
 * @time: 2025/4/29 11:40
 */

@Service
public class StatementCashFlowService extends ServiceImpl<StatementCashFlowMapper, StatementCashFlow>{
    public Message<String> changeSpecifyItem(StatementCashFlow statementCashFlow) {
        //期间
        String yearPeriod = statementCashFlow.getYearPeriod();
        String periodType = statementCashFlow.getPeriodType();
        LocalDate reportDate = null;
        if (StringUtils.hasText(yearPeriod)) {
            // 正常传了值，补齐成 yyyy-MM-01
            reportDate = LocalDate.parse(yearPeriod + "-01");
        } else {
            // 没传值，默认当前月第一天
            reportDate = LocalDate.now().withDayOfMonth(1);
        }
        statementCashFlow.setReportDate(reportDate);
        BigDecimal monthlyAmount = statementCashFlow.getMonthlyAmount();
        String bookId = statementCashFlow.getBookId();
        String itemCode = statementCashFlow.getItemCode();

        LambdaQueryWrapper<StatementCashFlow> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(StatementCashFlow::getBookId, bookId);
        wrapper.eq(StatementCashFlow::getReportDate, reportDate);
        wrapper.eq(StatementCashFlow::getPeriodType, periodType);
        wrapper.eq(StatementCashFlow::getItemCode, itemCode);

        List<StatementCashFlow> list = super.list(wrapper);

        boolean result = false;

        if (ObjectUtils.isNotEmpty(list)) {
            //修改
            StatementCashFlow statementCashFlowExist = list.get(0);
            statementCashFlowExist.setMonthlyAmount(monthlyAmount);
            result = super.updateById(statementCashFlowExist);
        } else {
            result = super.save(statementCashFlow);
        }

        return result ? Message.ok("保存成功") : Message.failed("保存失败");
    }
}
