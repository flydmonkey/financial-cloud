package com.jinbooks.common;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import lombok.Data;

@Data
public class DbTableMetaData {
	String tableName;

	List<DbTableColumn> columns = new ArrayList<>();

	Map<String,DbTableColumn> columnsMap = new HashMap<>();

	public DbTableMetaData(String tableName) {
		super();
		this.tableName = tableName;
	}
	
}
