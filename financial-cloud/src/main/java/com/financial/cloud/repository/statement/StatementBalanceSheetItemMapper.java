package com.financial.cloud.repository.statement;

import com.financial.cloud.domain.statement.StatementBalanceSheet;
import com.financial.cloud.domain.statement.StatementBalanceSheetItem;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface StatementBalanceSheetItemMapper extends BaseMapper<StatementBalanceSheetItem> {

}
