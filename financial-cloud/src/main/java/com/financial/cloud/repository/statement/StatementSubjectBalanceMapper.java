package com.financial.cloud.repository.statement;

import com.financial.cloud.domain.statement.StatementSubjectBalance;
import com.financial.cloud.dto.statement.StatementParamsDto;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;

@Mapper
public interface StatementSubjectBalanceMapper extends BaseMapper<StatementSubjectBalance> {
    List<StatementSubjectBalance> groupCodeSubjectBalance(@Param("dto") StatementParamsDto dto,
                                                          @Param("allMonths") List<String> allMonths,
                                                          @Param("minPeriod") String minPeriod,
                                                          @Param("maxPeriod") String maxPeriod);
}
