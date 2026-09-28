package com.financial.cloud.dto.fixedasset;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.financial.cloud.common.PageQuery;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.io.Serial;
import java.util.Date;
import java.util.List;

/**
 * 资产盘点请求/查询 DTO 集合。
 */
public final class FixedAssetCheckDtos {

    private FixedAssetCheckDtos() {
    }

    /** 盘点单分页查询 */
    @EqualsAndHashCode(callSuper = true)
    @Data
    public static class PageDto extends PageQuery {
        @Serial
        private static final long serialVersionUID = 1L;
        private String bookId;
        private String status;
    }

    /** 新建盘点单 */
    @Data
    public static class CreateDto {
        private String bookId;
        private String title;
        @JsonFormat(pattern = "yyyy-MM-dd", timezone = "GMT+8")
        private Date checkDate;
        private String remark;
    }

    /** 盘点明细实盘录入 */
    @Data
    public static class ItemDto {
        private String id;
        private String bookId;
        private Integer actualQuantity;
        private String actualLocation;
        private String remark;
    }

    /** 盘点单详情（头 + 明细） */
    @Data
    public static class DetailVo {
        private com.financial.cloud.domain.fixedasset.FixedAssetCheck check;
        private List<com.financial.cloud.domain.fixedasset.FixedAssetCheckItem> items;
    }

    /** 盘亏下账结果 */
    @Data
    public static class DeficitDisposeVo {
        /** 成功下账（生成清理凭证）的资产数 */
        private int processedCount;
        /** 跳过的明细及原因（部分盘亏、下账失败等） */
        private List<SkipReason> skipped = new java.util.ArrayList<>();
        /** 盘盈条数（仅提示，不自动入账） */
        private int surplusCount;
    }

    /** 跳过原因 */
    @Data
    @lombok.AllArgsConstructor
    public static class SkipReason {
        private String assetCode;
        private String assetName;
        private String reason;
    }
}
