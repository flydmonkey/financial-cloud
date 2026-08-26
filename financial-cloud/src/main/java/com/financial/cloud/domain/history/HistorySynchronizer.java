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

/**
 * @author 24096
 */
@EqualsAndHashCode(callSuper = true)
@Data
@NoArgsConstructor
@TableName("history_synchronizer")
public class HistorySynchronizer  extends BaseEntity  implements Serializable{
    @Serial
	private static final long serialVersionUID = -1184644499009162756L;

	@TableId(type = IdType.ASSIGN_ID)
    String id;

    String syncId;

    String sessionId;

    String syncName;

    String objectId;

    String objectType;

    String objectName;

    Date syncTime;

    String result;

	private String bookId;

	private String instName;
	Date startDate;
	Date endDate;

    public HistorySynchronizer(String id, String syncId,  String syncName, String objectId,
			String objectType, String objectName, Date syncTime, String result,String bookId) {
		super();
		this.id = id;
		this.syncId = syncId;
		this.syncName = syncName;
		this.objectId = objectId;
		this.objectType = objectType;
		this.objectName = objectName;
		this.syncTime = syncTime;
		this.result = result;
		this.bookId = bookId;
	}

	public HistorySynchronizer(String id, String syncId, String sessionId, String syncName, String objectId,
			String objectType, String objectName, Date syncTime, String result, String bookId) {
		super();
		this.id = id;
		this.syncId = syncId;
		this.sessionId = sessionId;
		this.syncName = syncName;
		this.objectId = objectId;
		this.objectType = objectType;
		this.objectName = objectName;
		this.syncTime = syncTime;
		this.result = result;
		this.bookId = bookId;
	}

}
