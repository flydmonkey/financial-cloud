package com.financial.cloud.repository.arap;

import com.financial.cloud.dto.arap.ArapMovementRow;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

@Mapper
public interface ArapMapper {

	List<ArapMovementRow> selectMovements(
			@Param("bookId") String bookId,
			@Param("assistType") String assistType,
			@Param("counterpartId") String counterpartId,
			@Param("prefixes") List<String> prefixes);
}
