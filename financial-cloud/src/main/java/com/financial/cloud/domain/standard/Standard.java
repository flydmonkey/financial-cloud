package com.financial.cloud.domain.standard;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.financial.cloud.common.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.io.Serial;
import java.io.Serializable;

/**
 * @description:
 * @author: orangeBabu
 * @time: 2024/12/26 17:32
 */

@EqualsAndHashCode(callSuper = true)
@Data
@TableName("standard")
public class Standard extends BaseEntity implements Serializable {

    @Serial
    private static final long serialVersionUID = -8233475494294429842L;

    @TableId(type = IdType.ASSIGN_ID)
    String id;

    String name;

    Integer status;
}
