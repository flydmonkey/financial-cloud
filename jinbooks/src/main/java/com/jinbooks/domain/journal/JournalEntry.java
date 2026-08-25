package com.jinbooks.domain.journal;

import java.io.Serializable;
import java.math.BigDecimal;
import java.util.Date;

import com.baomidou.mybatisplus.annotation.FieldFill;
import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableLogic;
import com.baomidou.mybatisplus.annotation.TableName;
import com.fasterxml.jackson.annotation.JsonFormat;
import com.jinbooks.common.BaseEntity;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;

/**
 * æ¥è®°è´?
 */
@EqualsAndHashCode(callSuper = true)
@Data
@TableName("journal_entry")
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class JournalEntry  extends BaseEntity implements Serializable {

	private static final long serialVersionUID = 7917671531089586327L;

	@TableId(type = IdType.ASSIGN_ID)
    String id;
	
	String bookId;
	/**
	 * è´¦æ·ç±»åï¼ç°écash  é¶è¡deposit
	 */
	String category;
	
	String remark;
	
	String accId;
	
	String accCode;
	
	String accName;
	
	String subjectId;
	
	String voucherId;
	/**
	 * æ¹åæ¶å¥ãæ¯åºåæåï¼? Iï¼EæOï¼Income and Expenditureï¼oï¼opening
	 */
	String direction;
	
	BigDecimal income;
	
	BigDecimal expenditure;
	
	BigDecimal balance;
	
	String description;
	
	@TableField(fill = FieldFill.INSERT)
    @JsonFormat(pattern="yyyy-MM-dd HH:mm:ss",timezone="GMT+8")
    Date tradeDate;
	
	/**
     * å é¤æ è®°
     */
    @TableField(fill = FieldFill.INSERT)
    @TableLogic(value = "n", delval = "y")
    private String deleted;
}
