package com.financial.cloud.dto.config;

import com.financial.cloud.domain.config.ConfigSalaryFormula;
import com.financial.cloud.dto.config.ConfigSalaryItem;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.io.Serial;
import java.util.List;

@Data
@EqualsAndHashCode(callSuper=false)
public class ConfigSalaryFormulaVo extends ConfigSalaryFormula {
    @Serial
    private static final long serialVersionUID = 6645770640418865289L;

    List<ConfigSalaryItem> formulaItems;
}
