package com.financial.cloud.dto.book;

import com.financial.cloud.common.PageQuery;
import lombok.Data;
import lombok.EqualsAndHashCode;

@EqualsAndHashCode(callSuper = true)
@Data
public class BookInitBalancePageDto extends PageQuery {
    /**
	 * 
	 */
	private static final long serialVersionUID = 6252697330250960515L;

	/**
     * 所属账套
     */
    private String bookId;

    /**
     * 类别
     */
    private Integer category;

    /**
     * 初始余额编码
     */
    private String assistType;

    /**
     * 初始余额名称
     */
    private String name;
    
    /**
     * 初始余额名称
     */
    private String code;
}
