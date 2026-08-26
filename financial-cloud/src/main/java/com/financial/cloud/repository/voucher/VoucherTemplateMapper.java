package com.financial.cloud.repository.voucher;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.financial.cloud.domain.voucher.VoucherTemplate;
import com.financial.cloud.dto.voucher.VoucherTemplatePageDto;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

/**
 * @description:
 * @author: orangeBabu
 * @time: 2024/12/31 11:12
 */

@Mapper
public interface VoucherTemplateMapper extends BaseMapper<VoucherTemplate> {
    Page<VoucherTemplate> pageList(Page page, @Param("dto") VoucherTemplatePageDto dto);

}
