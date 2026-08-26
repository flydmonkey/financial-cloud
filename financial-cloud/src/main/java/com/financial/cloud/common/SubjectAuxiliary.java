package com.financial.cloud.common;

import lombok.*;

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
