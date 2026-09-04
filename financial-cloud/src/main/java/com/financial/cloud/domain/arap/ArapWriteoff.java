package com.financial.cloud.domain.arap;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.financial.cloud.common.BaseEntity;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;

import java.io.Serial;
import java.io.Serializable;
import java.math.BigDecimal;
import java.util.Date;

@EqualsAndHashCode(callSuper = true)
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@TableName("arap_writeoff")
public class ArapWriteoff extends BaseEntity implements Serializable {
	@Serial
	private static final long serialVersionUID = 1L;

	public static final String STATUS_ACTIVE = "ACTIVE";
	public static final String STATUS_REVERSED = "REVERSED";

	@TableId(type = IdType.ASSIGN_ID)
	private String id;
	private String bookId;
	private String side;
	private String counterpartId;
	private String counterpartName;
	private BigDecimal amount;
	private String status;
	private Date writeoffDate;
}
