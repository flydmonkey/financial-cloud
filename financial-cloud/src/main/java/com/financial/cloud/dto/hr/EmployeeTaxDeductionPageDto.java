package com.financial.cloud.dto.hr;

import com.financial.cloud.common.PageQuery;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(callSuper=false)
public class EmployeeTaxDeductionPageDto extends PageQuery {
	/**
	 * 
	 */
	private static final long serialVersionUID = -4852707257925404150L;
	
	String bookId;
	/**
	 * 工号
	 */
	String employeeNo;
	/**
	 * 姓名
	 */
	String employeeName;
	
	/**
	 * 证件号
	 */
	String idCardNo;
}
