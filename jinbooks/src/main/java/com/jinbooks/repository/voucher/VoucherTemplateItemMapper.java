package com.jinbooks.repository.voucher;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.jinbooks.domain.voucher.VoucherTemplateItem;
import com.jinbooks.dto.voucher.VoucherTemplateItemPageDto;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

/**
 * @description:
 * @author: orangeBabu
 * @time: 2024/12/31 11:12
 */

@Mapper
public interface VoucherTemplateItemMapper extends BaseMapper<VoucherTemplateItem> {
    Page<VoucherTemplateItem> pageList(Page page, @Param("dto") VoucherTemplateItemPageDto dto);
}
