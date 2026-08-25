package com.jinbooks.dto.common;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import lombok.Data;

import java.util.List;

/**
 * @description:
 * @author: orangeBabu
 * @time: 2024/12/23 11:04
 */

@Data
public class ListIdsDto {

    private String bookId;

    @Valid
    @NotEmpty(message = "所选ID不能为空")
    List<String> listIds;
}
