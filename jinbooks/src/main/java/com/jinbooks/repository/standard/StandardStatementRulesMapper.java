package com.jinbooks.repository.standard;

import com.jinbooks.repository.standard.StandardStatementRulesMapper;
import com.jinbooks.domain.standard.StandardStatementRules;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

@Mapper
public interface StandardStatementRulesMapper extends BaseMapper<StandardStatementRules> {

    /**
     * 增加 code 的方法
     *
     * @param standardId 准则ID
     * @param itemCode   code编码
     * @param type       报表类型
     */
    int incrementItemCode(@Param("standardId") String standardId,
                          @Param("itemCode") String itemCode,
                          @Param("type") String type);

    /**
     * 减少 code 的方法
     *
     * @param standardId 准则ID
     * @param itemCode   code编码
     * @param type       报表类型
     */
    int decrementItemCode(@Param("standardId") String standardId,
                          @Param("itemCode") String itemCode,
                          @Param("type") String type);
}
