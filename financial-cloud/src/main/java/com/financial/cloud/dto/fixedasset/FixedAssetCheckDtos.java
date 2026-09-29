package com.financial.cloud.dto.fixedasset;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.financial.cloud.common.PageQuery;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.io.Serial;
import java.math.BigDecimal;
import java.util.ArrayList;
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

    /** 盘盈预览明细行 */
    @Data
    public static class SurplusPreviewRow {
        private String itemId;
        private String assetId;
        private String assetCode;
        private String assetName;
        private int bookQuantity;
        private int actualQuantity;
        private int surplusQuantity;
        private BigDecimal defaultAmount;
        /** split_card | bump_qty */
        private String strategy;
        /** 原卡已计提折旧且将走 bump_qty 时为 true */
        private boolean hasDepreciation;
        /** 前端提示文案；无则 null */
        private String warning;
    }

    /** 盘盈预览 */
    @Data
    public static class SurplusPreviewVo {
        private List<SurplusPreviewRow> rows = new ArrayList<>();
    }

    /** 盘盈入账单行金额 */
    @Data
    public static class SurplusBookItemDto {
        private String itemId;
        private BigDecimal amount;
    }

    /** 盘盈入账结果 */
    @Data
    public static class SurplusBookVo {
        private int processedCount;
        private List<SkipReason> skipped = new ArrayList<>();
    }
}
