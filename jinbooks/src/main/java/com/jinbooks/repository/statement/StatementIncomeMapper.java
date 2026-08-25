package com.jinbooks.repository.statement;

import com.jinbooks.domain.statement.StatementIncome;
import com.jinbooks.domain.statement.StatementIncomeItem;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface StatementIncomeMapper extends BaseMapper<StatementIncome> {

}
