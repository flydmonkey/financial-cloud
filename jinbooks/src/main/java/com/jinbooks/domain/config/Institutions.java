package com.jinbooks.domain.config;

import com.jinbooks.common.BaseEntity;
import java.io.Serializable;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;

@Data
@EqualsAndHashCode(callSuper=false)
@NoArgsConstructor
@TableName("institutions")
public class Institutions extends BaseEntity implements Serializable {
	static final long serialVersionUID = -2375872012431214098L;

	@TableId(type = IdType.ASSIGN_ID)
	String id;

	String fullName;

	String division;

	String country;

	String region;

	String locality;

	String street;

	String address;

	String contact;

	String postalCode;

	String phone;

	String fax;

	String email;

	String description;

	String logo;

	String backgroundImage;

	String domain;

	int status;

	@TableField(fill = FieldFill.INSERT)
	@TableLogic(value="n",delval="y")
	String deleted;

}
