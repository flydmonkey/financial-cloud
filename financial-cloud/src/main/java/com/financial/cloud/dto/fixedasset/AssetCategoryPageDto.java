package com.financial.cloud.dto.fixedasset;

import com.financial.cloud.common.PageQuery;
import lombok.*;

@EqualsAndHashCode(callSuper = true)
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AssetCategoryPageDto extends PageQuery {
    private static final long serialVersionUID = 1L;
    private String bookId;
    private String code;
    private String name;
    private String noId;
}
