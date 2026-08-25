package com.jinbooks.dto.config;

import com.jinbooks.common.PageQuery;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * @description:
 * @author: orangeBabu
 * @time: 2025/2/8 17:48
 */

@Data
@EqualsAndHashCode(callSuper=false)
public class ConfigSalaryFormulaPageDto extends PageQuery {
    /**
	 * 
	 */
	private static final long serialVersionUID = -4144200997026123655L;
	String ruleName;
}
