package com.financial.cloud.dto.common;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import lombok.Data;
import com.financial.cloud.constants.common.MessageKeys;

import java.util.List;

@Data
public class ListIdsDto {

    private String bookId;

    @Valid
    @NotEmpty(message = MessageKeys.Validation.COMMON_SELECTED_ID_REQUIRED)
    List<String> listIds;
}
