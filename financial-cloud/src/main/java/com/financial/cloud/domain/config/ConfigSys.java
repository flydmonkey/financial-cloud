package com.financial.cloud.domain.config;

import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.financial.cloud.common.BaseEntity;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;
import lombok.EqualsAndHashCode;
import com.financial.cloud.constants.common.MessageKeys;

@Data
@EqualsAndHashCode(callSuper = true)
@TableName("config")
public class ConfigSys extends BaseEntity {

    /**
	 * 
	 */
	private static final long serialVersionUID = -7790580992219005409L;

	/**
     * 参数主键
     */
    @TableId(value = "config_id")
    private String configId;

    /**
     * 账簿ID
     */
    private String bookId;

    /**
     * 参数名称
     */
    @NotBlank(message = MessageKeys.Validation.CONFIG_PARAM_NAME_REQUIRED)
    @Size(min = 0, max = 100, message = MessageKeys.Validation.CONFIG_PARAM_NAME_MAX_LENGTH)
    private String configName;

    /**
     * 参数键名
     */
    @NotBlank(message = MessageKeys.Validation.CONFIG_PARAM_KEY_REQUIRED)
    @Size(max = 100, message = MessageKeys.Validation.CONFIG_PARAM_KEY_MAX_LENGTH)
    private String configKey;

    /**
     * 参数键值
     */
    @NotBlank(message = MessageKeys.Validation.CONFIG_PARAM_VALUE_REQUIRED)
    @Size(max = 500, message = MessageKeys.Validation.CONFIG_PARAM_VALUE_MAX_LENGTH)
    private String configValue;

    /**
     * 系统内置（y是 n否）
     */
    private String configType;

    /**
     * 备注
     */
    private String remark;

}
