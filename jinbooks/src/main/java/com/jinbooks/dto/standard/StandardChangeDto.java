package com.jinbooks.dto.standard;

import com.jinbooks.validation.AddGroup;
import com.jinbooks.validation.EditGroup;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;

/**
 * @description:
 * @author: orangeBabu
 * @time: 2024/12/27 17:11
 */

@Data
public class StandardChangeDto {

    @NotEmpty(message = "编辑对象不能为空", groups = {EditGroup.class})
    String id;

    @NotEmpty(message = "会计准则不能为空", groups = {AddGroup.class, EditGroup.class})
    @Size(max = 21, message = "会计准则的长度不能超过21位", groups = {AddGroup.class, EditGroup.class})
    String name;

    @NotNull(message = "状态不能为null", groups = {AddGroup.class, EditGroup.class})
    Integer status;
}
