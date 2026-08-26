package com.financial.cloud.dto.voucher;

import com.financial.cloud.common.PageQuery;
import lombok.Data;
import lombok.EqualsAndHashCode;

@EqualsAndHashCode(callSuper = true)
@Data
public class VoucherTemplateItemPageDto extends PageQuery {
	/**
	 * 
	 */
	private static final long serialVersionUID = -61352733699177717L;

	String id;

    /**
     * 关联编码
     */
    String relatedId;

    /**
     * 名称
     */
    String name;

}
