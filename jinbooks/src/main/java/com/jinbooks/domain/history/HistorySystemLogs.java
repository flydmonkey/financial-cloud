package com.jinbooks.domain.history;

import java.io.Serial;
import java.io.Serializable;
import java.util.Date;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;

import com.baomidou.mybatisplus.annotation.TableName;

import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * .
 * @author Crystal.Sea
 *
 */

@Data
@NoArgsConstructor
@TableName("history_system_logs")
public class HistorySystemLogs implements Serializable {

    @Serial
    private static final long serialVersionUID = 6560201093784960493L;

    @TableId(type = IdType.ASSIGN_ID)
    String id;

    String topic;

    String message;

    String messageAction;

    String messageResult;

    String targetId;

    String targetName;

    String cipherText;

    String userId;

    String username;

    String displayName;

    Date executeTime;

	private String bookId;

    @TableField(exist = false)
	String jsonCotent;

    @TableField(exist = false)
	private String instName;

    @TableField(exist = false)
	Date startDate;

    @TableField(exist = false)
	Date endDate;

}
