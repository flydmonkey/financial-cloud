package com.jinbooks.dto.config;

import com.jinbooks.validation.AddGroup;
import com.jinbooks.validation.EditGroup;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

/**
 * @description:
 * @author: orangeBabu
 * @time: 2025/2/7 10:02
 */

@Data
public class ConfigPersonalTaxChangeDto {

    @NotNull(message = "编辑对象不能为空", groups = {EditGroup.class})
    private String id;

    @NotNull(message = "等级不能为空", groups = {AddGroup.class, EditGroup.class})
    Integer level;

    Integer minNum;

    Integer maxNum;

    @NotNull(message = "税率不能为空", groups = {AddGroup.class, EditGroup.class})
    Integer taxRate;

    String bookId;

    Integer type;

    // 获取真实税率（将整数转换为小数）
    public double getRealRate() {
        return taxRate / 100.0;
    }
}
