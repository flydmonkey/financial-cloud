package com.jinbooks.util;

import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.Row;

import cn.hutool.core.convert.Convert;
import cn.hutool.poi.excel.cell.CellUtil;

public class ExcelUtils {

	private ExcelUtils() {
	}

	public static String getValue(Cell cell) {
		if (cell == null) {
			return "";
		}
		return Convert.toStr(CellUtil.getCellValue(cell), "");
	}

	public static String getValue(Row row, int i) {
		return getValue(row.getCell(i));
	}

	public static Double getDoubleValue(Row row, int i) {
		return Convert.toDouble(getValue(row.getCell(i)), 0.0);
	}
}
