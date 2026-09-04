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

@EqualsAndHashCode(callSuper = true)
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@TableName("arap_writeoff_line")
public class ArapWriteoffLine extends BaseEntity implements Serializable {
	@Serial
	private static final long serialVersionUID = 1L;

	@TableId(type = IdType.ASSIGN_ID)
	private String id;
	private String writeoffId;
	private String bookId;
	private String voucherItemId;
	private String voucherId;
	private Integer voucherYear;
	private Integer voucherMonth;
	private BigDecimal amount;
}
