package com.financial.cloud.dto.config;

import com.financial.cloud.common.PageQuery;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(callSuper=false)
public class ConfigSalaryFormulaPageDto extends PageQuery {
    /**
	 * 
	 */
	private static final long serialVersionUID = -4144200997026123655L;
	String ruleName;
}
