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
 

package com.jinbooks.repository.voucher;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.jinbooks.domain.statement.StatementSubjectBalance;
import com.jinbooks.dto.statement.StatementParamsDto;
import com.jinbooks.domain.voucher.VoucherItem;
import com.jinbooks.dto.voucher.VoucherItemPageDto;
import com.jinbooks.dto.voucher.VoucherItemVo;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

@Mapper
public interface VoucherItemMapper extends BaseMapper<VoucherItem> {
    /**
     * 统计各科目的借方和贷方金额
     *
     * @param params 查询参数
     * @return 结果
     */
    List<VoucherItemVo> selectSubjectAmount(@Param("params") StatementParamsDto params);

    /**
     * 凭证明细分页查询
     *
     * @param page 分页参数
     * @param params  查询参数
     */
    Page<VoucherItemVo> subLedgerPage(Page<VoucherItem> page,@Param("params")  VoucherItemPageDto params);

    Page<VoucherItemVo> fetchByCashFlow(Page<VoucherItem> page,@Param("params")  VoucherItemPageDto params);
    
    List<StatementSubjectBalance> voucherSubjectBalanceSummary(StatementParamsDto dto);
}
