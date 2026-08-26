package com.financial.cloud.dto.config;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class ConfigSalaryJson {
    List<ConfigSalaryItem> formulaItems;
}
