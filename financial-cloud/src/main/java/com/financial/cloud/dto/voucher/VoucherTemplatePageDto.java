package com.financial.cloud.dto.voucher;

import com.financial.cloud.common.PageQuery;
import lombok.Data;
import lombok.EqualsAndHashCode;

@EqualsAndHashCode(callSuper = true)
@Data
public class VoucherTemplatePageDto extends PageQuery {
	/**
	 * 
	 */
	private static final long serialVersionUID = -61352733699177717L;

	String id;

    /**
     * 关联编码
     */
    String relatedId;

    String code;
    /**
     * 名称
     */
    String name;
    
    int category;
    
    String yearPeriod;
    
    String bookId;

}
