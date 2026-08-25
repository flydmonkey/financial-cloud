package com.jinbooks.repository.voucher;

import com.jinbooks.domain.voucher.Voucher;
import com.jinbooks.dto.voucher.VoucherVo;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface VoucherMapper extends BaseMapper<Voucher> {

}
