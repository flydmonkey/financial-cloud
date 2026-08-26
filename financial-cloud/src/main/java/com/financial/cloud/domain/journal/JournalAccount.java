package com.financial.cloud.domain.journal;

import java.io.Serializable;
import java.math.BigDecimal;

import com.baomidou.mybatisplus.annotation.FieldFill;
import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableLogic;
import com.baomidou.mybatisplus.annotation.TableName;
import com.financial.cloud.common.BaseEntity;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;

@EqualsAndHashCode(callSuper = true)
@Data
@TableName("journal_account")
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class JournalAccount  extends BaseEntity implements Serializable {

	private static final long serialVersionUID = 3265176233566931867L;

	@TableId(type = IdType.ASSIGN_ID)
    String id;
	
	String bookId;
	/**
	 * 账户类型：现金cash  银行deposit
	 */
	String category;

	String accCode;
	
	String accName;
	
	String subjectId;
	
	String currency;
	
	String bankNo;
	
	String bank;
	
	Integer sortIndex;
	/**
	 * 期初余额
	 */
	BigDecimal openingBalance;
	/**
	 * 可用余额
	 */
	BigDecimal balance;
	
	String description;
	
	/**
     * 删除标记
     */
    @TableField(fill = FieldFill.INSERT)
    @TableLogic(value = "n", delval = "y")
    private String deleted;

}
