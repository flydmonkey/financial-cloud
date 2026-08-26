package com.financial.cloud.repository.statement;

import com.financial.cloud.domain.statement.StatementIncome;
import com.financial.cloud.domain.statement.StatementIncomeItem;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface StatementIncomeItemMapper extends BaseMapper<StatementIncomeItem> {

}
