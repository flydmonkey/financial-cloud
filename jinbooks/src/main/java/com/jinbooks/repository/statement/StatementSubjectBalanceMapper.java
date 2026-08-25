package com.jinbooks.repository.statement;

import com.jinbooks.domain.statement.StatementSubjectBalance;
import com.jinbooks.dto.statement.StatementParamsDto;
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
