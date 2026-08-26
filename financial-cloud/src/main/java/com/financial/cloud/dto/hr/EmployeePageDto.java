package com.financial.cloud.dto.hr;

import com.financial.cloud.common.PageQuery;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(callSuper=false)
public class EmployeePageDto extends PageQuery {
    /**
	 * 
	 */
	private static final long serialVersionUID = -90886914726064826L;

	/**
     * 姓名
     */
    private String displayName;

    /**
     * 工号
     */
    private String employeeNumber;

    /**
     * 部门ID
     */
    private String departmentId;

    /**
     * 当前账套ID
     */
    private String bookId;
}
