package com.financial.cloud.repository.fixedasset;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.financial.cloud.domain.fixedasset.FixedAssetAccrual;
import org.apache.ibatis.annotations.Delete;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

@Mapper
public interface FixedAssetAccrualMapper extends BaseMapper<FixedAssetAccrual> {

    @Delete("DELETE FROM fixed_asset_accrual WHERE id = #{id}")
    int physicalDeleteById(@Param("id") String id);
}
