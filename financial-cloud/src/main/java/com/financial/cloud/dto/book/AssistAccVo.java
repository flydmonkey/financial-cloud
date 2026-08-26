package com.financial.cloud.dto.book;

import com.financial.cloud.domain.book.AssistAcc;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.io.Serial;

@EqualsAndHashCode(callSuper = true)
@Data
public class AssistAccVo extends AssistAcc {
    @Serial
    private static final long serialVersionUID = 1L;
}
