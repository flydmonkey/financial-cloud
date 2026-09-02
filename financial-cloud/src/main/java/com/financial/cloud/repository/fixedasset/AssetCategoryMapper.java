package com.financial.cloud.repository.fixedasset;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.financial.cloud.domain.fixedasset.AssetCategory;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface AssetCategoryMapper extends BaseMapper<AssetCategory> {
}
