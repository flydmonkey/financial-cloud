package com.financial.cloud.dto.config;

import com.financial.cloud.domain.config.ConfigCashFlowBalance;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import lombok.Data;
import com.financial.cloud.constants.common.MessageKeys;

import java.util.List;

@Data
public class ConfigCashFlowChangeDto {

    @Valid
    @NotEmpty(message = MessageKeys.Validation.COMMON_INPUT_PARAM_LIST_REQUIRED)
    private List<ConfigCashFlowBalance> cashFlowItemDtos;
}
