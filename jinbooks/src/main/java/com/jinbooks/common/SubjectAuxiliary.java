package com.jinbooks.common;

import lombok.*;

/**
 * 简介说明: 科目辅助项目对象
 *
 * @author wuyan
 * {@code @date} 2025/03/21 15:20:26
 * {@code @version} 1.0
 */

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class SubjectAuxiliary {
    /**
     * ID
     */
    private String id;

    /**
     * 名称
     */
    private String label;

    /**
     * 辅助项目类型
     */
    private String value;

    /**
     * 是否必填
     */
    private Boolean must;
}
