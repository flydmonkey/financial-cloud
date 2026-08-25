package com.jinbooks.service.hr;


import lombok.extern.slf4j.Slf4j;
import cn.hutool.core.bean.BeanUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.google.common.collect.Lists;
import com.jinbooks.common.ExcelImport;
import com.jinbooks.common.Message;
import com.jinbooks.dto.common.ListIdsDto;
import com.jinbooks.domain.hr.EmployeeTaxDeduction;
import com.jinbooks.dto.hr.EmployeeTaxDeductionDto;
import com.jinbooks.dto.hr.EmployeeTaxDeductionPageDto;
import com.jinbooks.domain.idm.UserInfo;
import com.jinbooks.repository.hr.EmployeeTaxDeductionMapper;
import com.jinbooks.service.hr.EmployeeTaxDeductionService;
import com.jinbooks.util.ExcelUtils;

import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.CollectionUtils;

import java.io.IOException;
import java.util.*;
import java.util.stream.Collectors;


@Slf4j
@Service
public class EmployeeTaxDeductionService extends ServiceImpl<EmployeeTaxDeductionMapper, EmployeeTaxDeduction>{
    public Message<Page<EmployeeTaxDeduction>> pageList(EmployeeTaxDeductionPageDto dto) {
        Page<EmployeeTaxDeduction> page = this.getBaseMapper().pageList(dto.build(), dto);
        return new Message<>(Message.SUCCESS, page);
    }
    @Transactional
    public Message<String> save(EmployeeTaxDeductionDto dto) {

        EmployeeTaxDeduction model = new EmployeeTaxDeduction();
        BeanUtil.copyProperties(dto, model);


        boolean saveResult = super.save(model);

        return saveResult ? new Message<>(Message.SUCCESS, "新增成功") : new Message<>(Message.FAIL, "新增失败");
    }
    @Transactional
    public Message<String> update(EmployeeTaxDeductionDto dto) {
        String id = dto.getId();
        EmployeeTaxDeduction model = super.getById(id);

        BeanUtil.copyProperties(dto, model);
        boolean result = super.updateById(model);
        return result ? new Message<>(Message.SUCCESS, "修改成功") : new Message<>(Message.FAIL, "修改失败");
    }
    @Transactional
    public Message<String> delete(ListIdsDto dto) {
        List<String> listIds = dto.getListIds();
        boolean result = false;
        result = super.removeBatchByIds(listIds);

        return result ? new Message<>(Message.SUCCESS, "删除成功") : new Message<>(Message.FAIL, "删除失败");
    }
    @Transactional
    public void importFromExcel(ExcelImport excelImportFile, UserInfo currentUser) {
        if (excelImportFile.isExcelNotEmpty()) {
            try {
                //删除历史数据
                LambdaQueryWrapper<EmployeeTaxDeduction> wrapper = new LambdaQueryWrapper<>();
                wrapper.isNotNull(EmployeeTaxDeduction::getIdCardNo);
                this.remove(wrapper);

                List<EmployeeTaxDeduction> dataList = Lists.newArrayList();
                Workbook workbook = excelImportFile.biuldWorkbook();
                int recordCount = 0;
                int sheetSize = workbook.getNumberOfSheets();
                for (int i = 0; i < sheetSize; i++) {
                    //遍历sheet页
                    Sheet sheet = workbook.getSheetAt(i);
                    int rowSize = sheet.getLastRowNum() + 1;
                    for (int j = 1; j < rowSize; j++) {     // 略过首行
                        //遍历行
                        Row row = sheet.getRow(j);
                        if (row == null) {
                            //略过首行
                            //continue;
                        } else {
                            //其他行是数据行
                            EmployeeTaxDeduction excelModel = buildFromSheetRow(row, currentUser);
                            dataList.add(excelModel);
                            recordCount++;
                            log.debug("record {} user {} account {}", recordCount, excelModel.getEmployeeName(), excelModel.getIdCardNo());
                        }
                    }
                }
                // 数据去重
                if (!CollectionUtils.isEmpty(dataList)) {
                    dataList = dataList.stream().collect(Collectors.collectingAndThen(Collectors.toCollection(() -> new TreeSet<>(Comparator.comparing(o -> o.getIdCardNo()))), ArrayList::new));
                    this.saveBatch(dataList);
                }
            } catch (IOException e) {
                //log.error(e.getMessage());
                log.error(e.getMessage(), e);
            } finally {
                excelImportFile.closeWorkbook();
            }
        }
    }

    /**
     * 江苏税务个人扣除
     * @param row
     * @param currentUser
     * @return
     */
    private EmployeeTaxDeduction buildFromSheetRow(Row row, UserInfo currentUser) {
        EmployeeTaxDeduction taxDeduction = new EmployeeTaxDeduction();
        taxDeduction.setCreatedDate(new Date());
        taxDeduction.setBookId(currentUser.getBookId());
        taxDeduction.setEmployeeNo(ExcelUtils.getValue(row, 0));
        taxDeduction.setEmployeeName(ExcelUtils.getValue(row, 1));
        taxDeduction.setIdCardType(ExcelUtils.getValue(row, 2));
        taxDeduction.setIdCardNo(ExcelUtils.getValue(row, 3));

        taxDeduction.setEducation(ExcelUtils.getDoubleValue(row, 12));
        taxDeduction.setContinuingEducation(ExcelUtils.getDoubleValue(row, 13));
        taxDeduction.setHousingLoan(ExcelUtils.getDoubleValue(row, 14));
        taxDeduction.setRent(ExcelUtils.getDoubleValue(row, 15));
        taxDeduction.setElderlyCare(ExcelUtils.getDoubleValue(row, 16));
        taxDeduction.setInfantsCare(ExcelUtils.getDoubleValue(row, 17));
        taxDeduction.setIndividualPension(ExcelUtils.getDoubleValue(row, 18));
        taxDeduction.setEnterprisePension(ExcelUtils.getDoubleValue(row, 19));
        taxDeduction.setCommercialHealth(ExcelUtils.getDoubleValue(row, 20));
        taxDeduction.setDeferredPension(ExcelUtils.getDoubleValue(row, 21));
        taxDeduction.setOthers(ExcelUtils.getDoubleValue(row, 21));

        return taxDeduction;
    }
}
