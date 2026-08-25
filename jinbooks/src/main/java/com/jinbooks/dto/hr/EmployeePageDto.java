package com.jinbooks.dto.hr;

import com.jinbooks.common.PageQuery;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * 员工信息分页查询对象
 *
 * @author wuyan
 * {@code @date} 2025-01-22
 */

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
