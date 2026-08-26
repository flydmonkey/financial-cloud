package com.financial.cloud.dto.book;

import com.financial.cloud.common.PageQuery;
import lombok.*;

/**
 * 辅助核算项目查询对象
 */

@EqualsAndHashCode(callSuper = true)
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AssistAccPageDto extends PageQuery {
    /**
	 * 
	 */
	private static final long serialVersionUID = -979219411529190936L;

	/**
     * 所属账套
     */
    private String bookId;

    /**
     * 辅助核算编码
     */
    private String assistType;
    
    /**
     * 辅助核算编码
     */
    private String assistCode;

    /**
     * 辅助核算名称
     */
    private String assistName;

    private String status;

    /**
     * no ID
     */
    private String noId;
}
