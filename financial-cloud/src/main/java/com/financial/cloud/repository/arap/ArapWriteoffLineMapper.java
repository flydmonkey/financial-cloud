package com.financial.cloud.repository.arap;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.financial.cloud.domain.arap.ArapWriteoffLine;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

@Mapper
public interface ArapWriteoffLineMapper extends BaseMapper<ArapWriteoffLine> {

	@Select("""
			SELECT l.voucher_item_id AS voucherItemId, SUM(l.amount) AS writtenOff
			FROM arap_writeoff_line l
			INNER JOIN arap_writeoff w ON w.id = l.writeoff_id AND w.status = 'ACTIVE'
			WHERE l.book_id = #{bookId}
			GROUP BY l.voucher_item_id
			""")
	List<Map<String, Object>> sumActiveByItem(@Param("bookId") String bookId);

	@Select("""
			SELECT COUNT(1) FROM arap_writeoff
			WHERE book_id = #{bookId} AND status = 'ACTIVE'
			AND (#{counterpartId} IS NULL OR #{counterpartId} = '' OR counterpart_id = #{counterpartId})
			AND (#{side} IS NULL OR #{side} = '' OR side = #{side})
			""")
	int countActive(@Param("bookId") String bookId,
			@Param("counterpartId") String counterpartId,
			@Param("side") String side);
}
