package com.jinbooks.domain.permissions;

import java.io.Serializable;
import com.baomidou.mybatisplus.annotation.FieldFill;
import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableLogic;

import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;

import com.baomidou.mybatisplus.annotation.TableName;
import com.jinbooks.common.BaseEntity;

/**
 * @author 24096
 */
@EqualsAndHashCode(callSuper = true)
@Data
@NoArgsConstructor
@TableName("permission_book")
public class PermissionBook  extends BaseEntity implements Serializable {

    /**
	 * 
	 */
	private static final long serialVersionUID = 1406417974520137150L;

	@TableId(type = IdType.ASSIGN_ID)
    String id;

    String userId;

    String bookId;

    @TableField(fill = FieldFill.INSERT)
    @TableLogic(value="n",delval="y")
	String deleted;

    public PermissionBook(String userId, String bookId) {
        this.userId = userId;
        this.bookId = bookId;
    }

}
