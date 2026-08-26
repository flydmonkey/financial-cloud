package com.financial.cloud.common;

import com.financial.cloud.common.BaseEntity;
import com.fasterxml.jackson.annotation.JsonIgnore;

import java.io.IOException;
import java.io.InputStream;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;

import lombok.extern.slf4j.Slf4j;
import org.apache.poi.hssf.usermodel.HSSFWorkbook;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.web.multipart.MultipartFile;

/**
 * .
 *
 * @author Crystal.Sea
 */
@EqualsAndHashCode(callSuper = true)
@Data
@NoArgsConstructor
@AllArgsConstructor
@Slf4j
public class ExcelImport extends BaseEntity {

    /**
	 * 
	 */
	private static final long serialVersionUID = -2764463764570801771L;
	String id;
    @JsonIgnore
    protected MultipartFile excelFile;
    String updateExist;
    InputStream inputStream = null;
    Workbook workbook = null;

    public boolean isExcelNotEmpty() {
        return excelFile != null && !excelFile.isEmpty();
    }


    public Workbook biuldWorkbook() throws IOException {
        workbook = null;
        inputStream = excelFile.getInputStream();
        if (excelFile.getOriginalFilename().toLowerCase().endsWith(".xls")) {
            workbook = new HSSFWorkbook(inputStream);
        } else if (excelFile.getOriginalFilename().toLowerCase().endsWith(".xlsx")) {
            workbook = new XSSFWorkbook(inputStream);
        } else {
            throw new RuntimeException("Excel suffix error.");
        }
        return workbook;
    }

    public void closeWorkbook() {
//        if (inputStream != null) {
//            try {
//                inputStream.close();
//            } catch (IOException e) {
//                log.error(e.getMessage(), e);
//            }
//        }
        if (workbook != null) {
            try {
                workbook.close();
            } catch (IOException e) {
                log.error(e.getMessage(), e);
            }
        }
    }

}
