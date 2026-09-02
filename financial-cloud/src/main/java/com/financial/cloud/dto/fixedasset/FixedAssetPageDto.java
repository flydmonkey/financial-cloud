package com.financial.cloud.dto.fixedasset;

import com.financial.cloud.common.PageQuery;
import lombok.*;

@EqualsAndHashCode(callSuper = true)
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class FixedAssetPageDto extends PageQuery {
    private static final long serialVersionUID = 1L;
    private String bookId;
    private String code;
    private String name;
    private String categoryId;
    private String deptId;
    private String status;
    /** 是否包含已清理；默认 false */
    private Boolean includeDisposed;
    private String noId;
}
