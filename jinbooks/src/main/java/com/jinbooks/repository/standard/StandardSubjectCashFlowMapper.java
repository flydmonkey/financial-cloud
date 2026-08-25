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

import com.jinbooks.repository.standard.StandardSubjectCashFlowMapper;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.jinbooks.domain.standard.StandardSubjectCashFlow;
import com.jinbooks.dto.standard.StandardSubjectCashFlowDto;
import com.jinbooks.dto.standard.StandardSubjectCashFlowVo;
import com.jinbooks.domain.voucher.VoucherItemCashFlow;
import com.jinbooks.dto.voucher.VoucherChangeDto;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

@Mapper
public interface StandardSubjectCashFlowMapper extends BaseMapper<StandardSubjectCashFlow> {
    StandardSubjectCashFlowVo fetchRelationships(@Param("dto") StandardSubjectCashFlowDto dto);

    List<VoucherItemCashFlow> getSubjectCashFlow(@Param("dto") VoucherChangeDto dto);
}
