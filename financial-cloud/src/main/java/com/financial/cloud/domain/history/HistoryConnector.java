package com.financial.cloud.domain.history;

import java.io.Serial;
import java.io.Serializable;
import java.util.Date;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import lombok.EqualsAndHashCode;

import com.baomidou.mybatisplus.annotation.TableName;
import com.financial.cloud.common.BaseEntity;

import lombok.Data;
import lombok.NoArgsConstructor;

@EqualsAndHashCode(callSuper = true)
@Data
@NoArgsConstructor
@TableName("history_connector")
public class HistoryConnector  extends BaseEntity  implements Serializable{
    @Serial
    private static final long serialVersionUID = 3465459057253994386L;

    @TableId(type = IdType.ASSIGN_ID)
    String id;

    String conName;

    String conType;

    String conAction;

    String sourceId;

    String sourceName;

    String objectId;

    String objectName;

    String description;

    Date syncTime;

    String result;

    Date startDate;

    Date endDate;

	private String instId;

	private String instName;

}
