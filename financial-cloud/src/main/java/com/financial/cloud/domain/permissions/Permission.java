package com.financial.cloud.domain.permissions;

import java.io.Serial;
import java.io.Serializable;
import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;

import com.baomidou.mybatisplus.annotation.TableName;
import com.financial.cloud.constants.common.ConstsStatus;
import com.financial.cloud.common.BaseEntity;
import com.financial.cloud.context.WebContext;

@EqualsAndHashCode(callSuper = true)
@Data
@NoArgsConstructor
@TableName("permission")
public class Permission  extends BaseEntity implements Serializable {
    @Serial
    private static final long serialVersionUID = -8783585691243853899L;

    @TableId(type = IdType.ASSIGN_ID)
    String id;

    String roleId;

    String resourceId;

    int status = ConstsStatus.ACTIVE;

	private String bookId;

	private String instName;

    public Permission(String roleId, String bookId) {
        this.roleId = roleId;
        this.bookId = bookId;
    }

    /**
     * .
     * @param appId String
     * @param groupId String
     * @param resourceId String
     */
    public Permission(String roleId, String resourceId ,String createdBy, String bookId) {
        this.id = WebContext.genId();
        this.roleId = roleId;
        this.resourceId = resourceId;
        this.createdBy = createdBy;
        this.bookId = bookId;
    }

    public String  getUniqueId() {
        return   roleId + "_" + resourceId;
    }

}
