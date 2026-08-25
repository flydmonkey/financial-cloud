package com.jinbooks.dto.config;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.util.List;

/**
 * @description:
 * @author: orangeBabu
 * @time: 2025/2/11 10:09
 */

@Data
@NoArgsConstructor
@AllArgsConstructor
public class ConfigSalaryJson {
    List<ConfigSalaryItem> formulaItems;
}
