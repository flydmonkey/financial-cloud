package com.financial.cloud.domain.book;

import com.baomidou.mybatisplus.annotation.*;
import com.financial.cloud.common.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.io.Serializable;
import java.math.BigDecimal;

/**
 * @description:
 * @author: orangeBabu
 * @time: 2024/12/31 9:54
 */

@EqualsAndHashCode(callSuper = true)
@Data
@TableName("settlement")
public class Settlement extends BaseEntity implements Serializable {

    /**
	 *
	 */
	private static final long serialVersionUID = 5892695992406543326L;

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

    /**
     * 期末余额
     */
    BigDecimal endingBalance;

    /**
     * 状态
     */
    Integer status;

    @TableField(fill = FieldFill.INSERT)
    @TableLogic(value="n",delval="y")
    String deleted;

    @TableField(exist = false)
    String period;
    
    /**
     * 当前期
     */
    @TableField(exist = false)
    String currentTerm;
    
    /**
     * 下前期
     */
    @TableField(exist = false)
    String nextTerm;

}
