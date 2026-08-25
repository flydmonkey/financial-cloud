/*
 * Copyright [2025] [JinBooks of copyright http://www.jinbooks.com]
 * 
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * 
 *     http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 */
 

package com.jinbooks.repository.standard;

import com.jinbooks.repository.standard.StandardStatementBalanceSheetMapper;
import com.jinbooks.domain.standard.StandardStatementBalanceSheet;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

@Mapper
public interface StandardStatementBalanceSheetMapper extends BaseMapper<StandardStatementBalanceSheet> {

    /**
     * 增加 sort_index 的方法
     *
     * @param standardId 准则ID
     * @param sortIndex  行号
     * @param id         ID
     */
    int incrementSortIndex(@Param("standardId") String standardId,
                           @Param("sortIndex") int sortIndex,
                           @Param("id") String id);

    /**
     * 减少 sort_index 的方法
     *
     * @param standardId 准则ID
     * @param sortIndex  行号
     * @param id         ID
     */
    int decrementSortIndex(@Param("standardId") String standardId,
                           @Param("sortIndex") int sortIndex,
                           @Param("id") String id);

    /**
     * 增加 code 的方法
     *
     * @param standardId 准则ID
     * @param itemCode   code编码
     * @param id         ID
     */
    int incrementItemCode(@Param("standardId") String standardId,
                          @Param("itemCode") String itemCode,
                          @Param("id") String id);

    /**
     * 减少 code 的方法
     *
     * @param standardId 准则ID
     * @param itemCode   code编码
     * @param id         ID
     */
    int decrementItemCode(@Param("standardId") String standardId,
                          @Param("itemCode") String itemCode,
                          @Param("id") String id);
}
