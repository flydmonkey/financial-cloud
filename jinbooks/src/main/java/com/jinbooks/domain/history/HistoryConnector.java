package com.jinbooks.domain.history;

import java.io.Serial;
import java.io.Serializable;
import java.util.Date;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import lombok.EqualsAndHashCode;

import com.baomidou.mybatisplus.annotation.TableName;
import com.jinbooks.common.BaseEntity;

import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * @author 24096
 */
@EqualsAndHashCode(callSuper = true)
@Data
@NoArgsConstructor
@TableName("JBX_HISTORY_CONNECTOR")
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
