package com.financial.cloud.dto.statement;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serial;
import java.io.Serializable;
import java.math.BigDecimal;
import java.util.List;

/**
 * 增值税申报表（简版主表）：按官方主表行次组织的申报底稿。
 * 数据来自税费测算（已过账凭证分录口径），账面无法拆分的行次留空由人工填报。
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TaxDeclarationVo implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    /** 申报期间 yyyy-MM */
    private String yearMonth;

    /** 申报表行（按 section 分组展示） */
    private List<Line> lines;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class Line implements Serializable {

        @Serial
        private static final long serialVersionUID = 1L;

        /** 所属部分，如「一、销售额」 */
        private String section;
        /** 行次（官方主表编号；附加税/所得税参考行为空） */
        private String rowNo;
        /** 项目名称 */
        private String item;
        /** 金额（账面无法取数时为 null，展示为空由人工填报） */
        private BigDecimal amount;
    }
}
