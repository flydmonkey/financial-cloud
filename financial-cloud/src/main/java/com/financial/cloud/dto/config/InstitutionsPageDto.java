package com.financial.cloud.dto.config;

import com.financial.cloud.common.PageQuery;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * @description:
 * @author: orangeBabu
 * @time: 2024/11/26 17:16
 */

@Data
@EqualsAndHashCode(callSuper=false)
public class InstitutionsPageDto extends PageQuery {
	/**
	 * 
	 */
	private static final long serialVersionUID = -5337421047169976733L;
}
