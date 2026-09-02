package com.financial.cloud.repository.fixedasset;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.financial.cloud.domain.fixedasset.FixedAssetDepr;
import org.apache.ibatis.annotations.Delete;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

@Mapper
public interface FixedAssetDeprMapper extends BaseMapper<FixedAssetDepr> {

    /** 物理删除：避免 uk(book,asset,period,deleted) 在重提软删时冲突 */
    @Delete("DELETE FROM fixed_asset_depr WHERE book_id = #{bookId} AND year_period = #{yearPeriod}")
    int physicalDeleteByBookPeriod(@Param("bookId") String bookId, @Param("yearPeriod") String yearPeriod);
}
