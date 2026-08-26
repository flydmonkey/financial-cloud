package com.financial.cloud.domain.book;

import com.baomidou.mybatisplus.annotation.*;
import com.financial.cloud.common.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.io.Serializable;

/**
 * @description:
 * @author: orangeBabu
 * @time: 2024/12/31 9:54
 */

@EqualsAndHashCode(callSuper = true)
@Data
@TableName("settlement_carryforward")
public class SettlementCarryforward extends BaseEntity implements Serializable {

	/**
	 * 
	 */
	private static final long serialVersionUID = -201265021469562154L;

	@TableId(type = IdType.ASSIGN_ID)
    String id;
    
    /**
     * 账套
     */
    String bookId;

    /**
     * 所属年份
     */
    int year;
    /**
     * 账期
     */
    String yearPeriod;
    
    String voucherId;
    
    String voucherTemplateId;
    
    @TableField(fill = FieldFill.INSERT)
    @TableLogic(value="n",delval="y")
    String deleted;
    
    @TableField(exist = false)
    String period;

}
