package com.financial.cloud.common;

import lombok.Data;

@Data
public class DbTableColumn {
	String column;
	String type;
	int precision;
	int scale;

	public DbTableColumn(String column, String type, int precision, int scale) {
		super();
		this.column = column;
		this.type = type;
		this.precision = precision;
		this.scale = scale;
	}
}
