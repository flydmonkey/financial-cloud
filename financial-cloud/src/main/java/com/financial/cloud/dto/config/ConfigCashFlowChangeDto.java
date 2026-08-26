package com.financial.cloud.dto.config;

import com.financial.cloud.domain.config.ConfigCashFlowBalance;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import lombok.Data;

import java.util.List;

/**
 * @description:
 * @author: orangeBabu
 * @time: 2025/3/24 16:04
 */

@Data
public class ConfigCashFlowChangeDto {

    @Valid
    @NotEmpty(message = "入参集合不能为空")
    private List<ConfigCashFlowBalance> cashFlowItemDtos;
}
