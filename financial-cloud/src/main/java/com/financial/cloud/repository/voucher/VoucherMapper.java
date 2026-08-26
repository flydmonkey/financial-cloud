package com.financial.cloud.repository.voucher;

import com.financial.cloud.domain.voucher.Voucher;
import com.financial.cloud.dto.voucher.VoucherVo;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface VoucherMapper extends BaseMapper<Voucher> {

}
