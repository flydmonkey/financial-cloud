package com.jinbooks.repository.statement;

import com.jinbooks.domain.statement.StatementBalanceSheet;
import com.jinbooks.domain.statement.StatementBalanceSheetItem;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface StatementBalanceSheetItemMapper extends BaseMapper<StatementBalanceSheetItem> {

}
