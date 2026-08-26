package com.financial.cloud.dto.book;

import com.financial.cloud.domain.book.AssistAcc;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.io.Serial;

/**
 * 辅助核算项目对象视图对象
 *
 * @author Wuyan
 * {@code @date} 2025-02-18
 */

@EqualsAndHashCode(callSuper = true)
@Data
public class AssistAccVo extends AssistAcc {
    @Serial
    private static final long serialVersionUID = 1L;
}
