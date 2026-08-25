package com.jinbooks.domain.standard;

import com.baomidou.mybatisplus.annotation.*;
import com.jinbooks.common.BaseSubject;

import lombok.Data;
import lombok.EqualsAndHashCode;

import java.io.Serial;

/**
 * @description:
 * @author: orangeBabu
 * @time: 2024/12/19 15:45
 */

@EqualsAndHashCode(callSuper = true)
@Data
@TableName(value = "standard_subject", autoResultMap = true)
public class StandardSubject extends BaseSubject{
    @Serial
    private static final long serialVersionUID = -4940236669574217392L;

    String standardId;
}
