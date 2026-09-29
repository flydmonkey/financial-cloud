package com.financial.cloud.dto.workspace;

import lombok.Builder;
import lombok.Data;

import java.util.List;

@Data
@Builder
public class BooksBoardVo {
    private String focusPeriod;
    private List<BooksBoardRowVo> rows;
    private int totalGranted;
    private boolean truncated;
}
