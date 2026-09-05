package com.financial.cloud.service.hr;



import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import com.financial.cloud.repository.hr.EmployeeSalaryMapper;
import com.financial.cloud.repository.hr.EmployeeMapper;
import com.financial.cloud.repository.book.BookMapper;
import com.financial.cloud.repository.voucher.VoucherTemplateItemMapper;
import com.financial.cloud.repository.voucher.VoucherTemplateMapper;
import cn.hutool.core.bean.BeanUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.core.toolkit.StringUtils;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.financial.cloud.domain.book.Book;
import com.financial.cloud.domain.book.BookSubject;
import com.financial.cloud.domain.hr.*;
import com.financial.cloud.domain.voucher.VoucherTemplate;
import com.financial.cloud.domain.voucher.VoucherTemplateItem;
import com.financial.cloud.dto.voucher.GenerateVoucherDto;
import com.financial.cloud.util.SubjectCodeCompat;
import com.financial.cloud.dto.voucher.VoucherChangeDto;
import com.financial.cloud.dto.voucher.VoucherItemChangeDto;
import com.financial.cloud.enums.voucher.VoucherStatusEnum;
import com.financial.cloud.service.voucher.VoucherService;
import jakarta.servlet.ServletOutputStream;
import jakarta.servlet.http.HttpServletResponse;

import com.baomidou.mybatisplus.core.toolkit.ObjectUtils;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.financial.cloud.constants.common.ConstsHttpHeader;
import com.financial.cloud.constants.auth.ConstsUser;
import com.financial.cloud.constants.common.ContentType;
import com.financial.cloud.common.Message;
import com.financial.cloud.dto.common.ListIdsDto;
import com.financial.cloud.dto.hr.SalaryDetailChangeDto;
import com.financial.cloud.dto.hr.SalaryDetailPageDto;
import com.financial.cloud.dto.hr.SalarySummaryChangeDto;
import com.financial.cloud.common.PeriodStr;
import com.financial.cloud.dto.hr.TaxDeductionExportVo;
import com.financial.cloud.enums.error.HrErrorCode;
import com.financial.cloud.exception.BusinessException;
import com.financial.cloud.service.book.BookSubjectService;
import com.financial.cloud.service.config.ConfigSysService;
import com.financial.cloud.service.hr.EmployeeSalaryService;
import com.financial.cloud.util.PeriodDateUtils;
import com.financial.cloud.util.DateUtils;

import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.CellStyle;
import org.apache.poi.ss.usermodel.DataFormat;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.ss.util.CellRangeAddress;
import org.apache.poi.xssf.streaming.SXSSFWorkbook;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.IOException;
import java.io.Serializable;
import java.math.BigDecimal;
import java.net.URLEncoder;
import java.text.SimpleDateFormat;
import java.time.YearMonth;
import java.util.*;
import java.util.stream.Collectors;

@RequiredArgsConstructor
@Slf4j
@Service
public class EmployeeSalaryService extends ServiceImpl<EmployeeSalaryMapper, EmployeeSalary>{
    private final EmployeeSalaryMapper employeeSalaryMapper;

    private final EmployeeMapper employeeMapper;

    private final BookMapper bookMapper;

    private final VoucherTemplateMapper voucherTemplateMapper; 
    
    private final VoucherTemplateItemMapper voucherTemplateItemMapper;

    private final VoucherService voucherService;
    
    private final ConfigSysService configSysService;
    
    private final BookSubjectService bookSubjectService;
    public Message<Page<EmployeeSalary>> pageList(SalaryDetailPageDto dto) {

        Page<EmployeeSalary> employeeSalaryPage = employeeSalaryMapper.pageList(dto.build(), dto);

        return Message.ok(employeeSalaryPage);
    }
    @Transactional
    public Message<String> update(SalaryDetailChangeDto dto) {
        EmployeeSalary employeeSalary = BeanUtil.copyProperties(dto, EmployeeSalary.class);
        boolean result = super.updateById(employeeSalary);

        return result ? Message.ok("修改成功") : Message.failed("修改失败");
    }
    @Transactional
    public Message<String> save(SalaryDetailChangeDto dto) {
        EmployeeSalary employeeSalary = BeanUtil.copyProperties(dto, EmployeeSalary.class);
        boolean result = super.save(employeeSalary);

        return result ? Message.ok("新增成功") : Message.failed("新增失败");
    }
    @Transactional
    public Message<String> delete(ListIdsDto dto) {
        List<String> ids = dto.getListIds();
        boolean result = super.removeBatchByIds(ids);
        return result ? new Message<>(Message.SUCCESS, "删除成功") : new Message<>(Message.FAIL, "删除失败");
    }
    public EmployeeSalary getById(Serializable id) {
        EmployeeSalary employeeSalary = super.getById(id);
        if (Objects.nonNull(employeeSalary)) {
            Employee employee = employeeMapper.selectById(employeeSalary.getEmployeeId());
            if (Objects.nonNull(employee)) {
                employeeSalary.setBankCardNo(employee.getBankCardNo());
                employeeSalary.setEmployeeName(employee.getDisplayName());
                employeeSalary.setEmployeeNumber(employee.getEmployeeNumber());
                return employeeSalary;
            }
            throw new BusinessException(HrErrorCode.EMPLOYEE_NOT_FOUND);
        }

        throw new BusinessException(HrErrorCode.RECORD_NOT_FOUND);
    }
	public EmployeeSalarySummary selectSalarySummary(SalarySummaryChangeDto dto) {
		return employeeSalaryMapper.selectSalarySummary(dto);
	}
    public Message<String> exportTaxItems(SalaryDetailPageDto dto, HttpServletResponse response) {
        List<TaxDeductionExportVo> taxDeductionExportVos = employeeSalaryMapper.exportGetSalaryDetail(dto);
        if (ObjectUtils.isEmpty(taxDeductionExportVos)) {
            throw new BusinessException(HrErrorCode.NO_DATA);
        }
        Workbook workbook = null;
        try {
            String belongDate = dto.getBelongDate();
            // 解析 belongDate 为 Date
            SimpleDateFormat sdf = new SimpleDateFormat("yyyy-M");
            Date date;

            date = sdf.parse(belongDate);
            // 计算 startPeriod 和 endPeriod
            PeriodStr period = PeriodDateUtils.convertToPeriod(date);
            String startPeriod = period.getStartPeriodStr();
            String endPeriod = period.getEndPeriodStr();

            if (taxDeductionExportVos.size() > 5000) {
                // 比如设置最大内存量为5000行， new SXSSFWookbook(5000)，
                // 当行数达到 5000 时，把内存持久化写到文件中，以此逐步写入，避免OOM。解决了大数据下导出的问题
                workbook = new SXSSFWorkbook(5000);
            } else {
                workbook = new XSSFWorkbook();
            }
            int rowCount = 0;
            //创建sheet
            Sheet sheet = workbook.createSheet(dto.getBelongDate() + "_正常工资薪金所得");
            Row row = sheet.createRow(rowCount++);
            row.createCell(0).setCellValue("财务云薪资导出 " + DateUtils.getCurrentDateTimeAsString());
            sheet.addMergedRegion(new CellRangeAddress(0, 0, 0, 30));
            // 2. 创建数据格式（保留两位小数）
            CellStyle cellStyle = workbook.createCellStyle();
            DataFormat dataFormat = workbook.createDataFormat();
            cellStyle.setDataFormat(dataFormat.getFormat("0.00"));
            //构建头
            row = sheet.createRow(rowCount++);
            int headerColumn = 0;
            row.createCell(headerColumn++).setCellValue("工号");
            row.createCell(headerColumn++).setCellValue("姓名");
            row.createCell(headerColumn++).setCellValue("证件类型");
            row.createCell(headerColumn++).setCellValue("证件号码");
            row.createCell(headerColumn++).setCellValue("所得期间起");
            row.createCell(headerColumn++).setCellValue("所得期间止");
            row.createCell(headerColumn++).setCellValue("本期收入");
            row.createCell(headerColumn++).setCellValue("本期免税收入");
            row.createCell(headerColumn++).setCellValue("基本养老保险费");
            row.createCell(headerColumn++).setCellValue("基本医疗保险费");
            row.createCell(headerColumn++).setCellValue("失业保险费");
            row.createCell(headerColumn++).setCellValue("住房公积金");
            row.createCell(headerColumn++).setCellValue("累计子女教育");
            row.createCell(headerColumn++).setCellValue("累计继续教育");
            row.createCell(headerColumn++).setCellValue("大病医疗");
            row.createCell(headerColumn++).setCellValue("累计住房贷款利息");
            row.createCell(headerColumn++).setCellValue("累计住房租金");
            row.createCell(headerColumn++).setCellValue("累计赡养老人");
            row.createCell(headerColumn++).setCellValue("累计3岁以下婴幼儿照护");
            row.createCell(headerColumn++).setCellValue("累计个人养老金");
            row.createCell(headerColumn++).setCellValue("企业(职业)年金");
            row.createCell(headerColumn++).setCellValue("商业健康保险");
            row.createCell(headerColumn++).setCellValue("税延养老保险");
            row.createCell(headerColumn++).setCellValue("其他");
            row.createCell(headerColumn++).setCellValue("准予扣除的捐赠额");
            row.createCell(headerColumn++).setCellValue("税前扣除项目合计");
            row.createCell(headerColumn++).setCellValue("减免税额");
            row.createCell(headerColumn++).setCellValue("减除费用标准");
            row.createCell(headerColumn++).setCellValue("已缴税额");
            row.createCell(headerColumn++).setCellValue("备注");

            for (TaxDeductionExportVo exportVo: taxDeductionExportVos) {
            	//导出正式员工 、实习生、退休返聘
            	if(exportVo.getEmployeeType().equalsIgnoreCase(ConstsUser.EMPLOYEE_TYPE.NORMAL)
            			||exportVo.getEmployeeType().equalsIgnoreCase(ConstsUser.EMPLOYEE_TYPE.INTERN)
            			||exportVo.getEmployeeType().equalsIgnoreCase(ConstsUser.EMPLOYEE_TYPE.RETIREMENT)) {
	                row = sheet.createRow(rowCount++);
	                int column = 0;
	                row.createCell(column++).setCellValue(exportVo.getEmployeeNumber());
	                row.createCell(column++).setCellValue(exportVo.getDisplayName());
	                row.createCell(column++).setCellValue(exportVo.getIdCardType());
	                row.createCell(column++).setCellValue(exportVo.getIdCardNo());
	                row.createCell(column++).setCellValue(startPeriod);
	                row.createCell(column++).setCellValue(endPeriod);
	                // 3. 统一格式化所有数值字段
	                double[] values = {
	                        exportVo.getIncome().doubleValue(),
	                        exportVo.getTaxFreeIncome().doubleValue(),
	                        exportVo.getInsuranceEndowment().doubleValue(),
	                        exportVo.getInsuranceMedical().doubleValue(),
	                        exportVo.getInsuranceUnemployment().doubleValue(),
	                        exportVo.getHousingProvidentFund().doubleValue(),
	                        exportVo.getEducation().doubleValue(),
	                        exportVo.getContinuingEducation().doubleValue(),
	                        exportVo.getMedical().doubleValue(),
	                        exportVo.getHousingLoan().doubleValue(),
	                        exportVo.getRent().doubleValue(),
	                        exportVo.getElderlyCare().doubleValue(),
	                        exportVo.getInfantsCare().doubleValue(),
	                        exportVo.getIndividualPension().doubleValue(),
	                        exportVo.getEnterprisePension().doubleValue(),
	                        exportVo.getCommercialHealth().doubleValue(),
	                        exportVo.getDeferredPension().doubleValue(),
	                        exportVo.getOthers().doubleValue(),
	                        exportVo.getDonationAllowed().doubleValue(),
	                        exportVo.getTotalPreTaxDeduction().doubleValue(),
	                        exportVo.getTaxDeductions().doubleValue(),
	                        exportVo.getDeductingStandards().doubleValue(),
	                        exportVo.getPaidTax().doubleValue()
	                };
	                // 4. 统一创建单元格并应用格式
	                for (int j = 0; j < values.length; j++) {
	                    Cell cell = row.createCell(column++ + j); // 从第6列开始
	                    cell.setCellValue(values[j]);
	                    cell.setCellStyle(cellStyle);
	                }
	                row.createCell(column++).setCellValue(exportVo.getRemark());
	            }
            }
            String fileName = "salary-" + belongDate;
            fileName = URLEncoder.encode(fileName, "UTF8");
            response.setContentType(ContentType.APPLICATION_MS_EXCEL);
            response.setHeader(ConstsHttpHeader.CONTENT_DISPOSITION, ConstsHttpHeader.ATTACHMENT_FILE.formatted(fileName));
            ServletOutputStream out = response.getOutputStream();
            workbook.write(out);
            out.flush();
            out.close();
        } catch (Exception e) {
            log.error("error:", e);
        } finally {
            if (Objects.nonNull(workbook)) {
                try {
                    workbook.close();
                } catch (IOException e) {
                    log.error("error close ", e);
                }
            }
        }
        return null;
    }

    /**
     * Export bank payment file from confirmed salary details for one belonging month.
     * Blocks when month is empty or any row lacks bankCardNo.
     */
    public Message<String> exportPaymentFile(SalaryDetailPageDto dto, HttpServletResponse response) {
        if (StringUtils.isBlank(dto.getBelongDate())) {
            throw new BusinessException(HrErrorCode.PAYMENT_EXPORT_NO_DATA);
        }
        YearMonth belongMonth = YearMonth.parse(dto.getBelongDate());
        List<EmployeeSalary> salaries = employeeSalaryMapper.selectList(Wrappers.<EmployeeSalary>lambdaQuery()
                .eq(EmployeeSalary::getBookId, dto.getBookId())
                .eq(EmployeeSalary::getBelongDate, belongMonth));
        List<SalaryPaymentExportRules.PaymentRow> paymentRows = new ArrayList<>();
        if (ObjectUtils.isNotEmpty(salaries)) {
            List<String> employeeIds = salaries.stream().map(EmployeeSalary::getEmployeeId).distinct().toList();
            Map<String, Employee> employeeMap = employeeMapper.selectBatchIds(employeeIds).stream()
                    .collect(Collectors.toMap(Employee::getId, e -> e, (a, b) -> a));
            for (EmployeeSalary salary : salaries) {
                Employee employee = employeeMap.get(salary.getEmployeeId());
                String name = employee != null ? employee.getDisplayName() : salary.getEmployeeName();
                String number = employee != null ? employee.getEmployeeNumber() : salary.getEmployeeNumber();
                String bankName = employee != null ? employee.getBankName() : null;
                String bankCardNo = employee != null ? employee.getBankCardNo() : salary.getBankCardNo();
                paymentRows.add(new SalaryPaymentExportRules.PaymentRow(
                        name,
                        number,
                        bankName,
                        bankCardNo,
                        salary.getTotalAmount(),
                        dto.getBelongDate()));
            }
        }
        if (SalaryPaymentExportRules.isEmptyMonth(paymentRows)) {
            throw new BusinessException(HrErrorCode.PAYMENT_EXPORT_NO_DATA);
        }
        List<String> missing = SalaryPaymentExportRules.missingBankAccountNames(paymentRows);
        if (!missing.isEmpty()) {
            throw new BusinessException(HrErrorCode.PAYMENT_EXPORT_MISSING_BANK, String.join("、", missing));
        }

        Workbook workbook = new XSSFWorkbook();
        try {
            Sheet sheet = workbook.createSheet("代发盘");
            Row header = sheet.createRow(0);
            header.createCell(0).setCellValue("工号");
            header.createCell(1).setCellValue("姓名");
            header.createCell(2).setCellValue("开户行");
            header.createCell(3).setCellValue("账号");
            header.createCell(4).setCellValue("实发金额");
            header.createCell(5).setCellValue("所属月");

            CellStyle amountStyle = workbook.createCellStyle();
            DataFormat dataFormat = workbook.createDataFormat();
            amountStyle.setDataFormat(dataFormat.getFormat("0.00"));

            int rowIdx = 1;
            for (SalaryPaymentExportRules.PaymentRow paymentRow : paymentRows) {
                Row row = sheet.createRow(rowIdx++);
                row.createCell(0).setCellValue(paymentRow.employeeNumber() != null ? paymentRow.employeeNumber() : "");
                row.createCell(1).setCellValue(paymentRow.employeeName() != null ? paymentRow.employeeName() : "");
                row.createCell(2).setCellValue(paymentRow.bankName() != null ? paymentRow.bankName() : "");
                row.createCell(3).setCellValue(paymentRow.bankCardNo() != null ? paymentRow.bankCardNo() : "");
                Cell amountCell = row.createCell(4);
                amountCell.setCellValue(paymentRow.netPay() != null ? paymentRow.netPay().doubleValue() : 0);
                amountCell.setCellStyle(amountStyle);
                row.createCell(5).setCellValue(paymentRow.belongDate() != null ? paymentRow.belongDate() : "");
            }

            String fileName = URLEncoder.encode("salary-payment-" + dto.getBelongDate(), "UTF8");
            response.setContentType(ContentType.APPLICATION_MS_EXCEL);
            response.setHeader(ConstsHttpHeader.CONTENT_DISPOSITION, ConstsHttpHeader.ATTACHMENT_FILE.formatted(fileName));
            ServletOutputStream out = response.getOutputStream();
            workbook.write(out);
            out.flush();
            out.close();
        } catch (IOException e) {
            log.error("export payment file error", e);
            throw new BusinessException(HrErrorCode.NO_DATA);
        } finally {
            try {
                workbook.close();
            } catch (IOException e) {
                log.error("error close payment workbook", e);
            }
        }
        return null;
    }

    public long countByBelongDate(String bookId, String belongDate) {
        if (StringUtils.isBlank(belongDate)) {
            return 0L;
        }
        return employeeSalaryMapper.selectCount(Wrappers.<EmployeeSalary>lambdaQuery()
                .eq(EmployeeSalary::getBookId, bookId)
                .eq(EmployeeSalary::getBelongDate, YearMonth.parse(belongDate)));
    }

    @Transactional
    public Message<String> generateVoucher(GenerateVoucherDto dto) {
        String bookId = dto.getBookId();
        Book book = bookMapper.selectById(bookId);
        Integer voucherType = dto.getVoucherType();
        EmployeeSalary salary = super.getById(dto.getId());
        Employee employee = employeeMapper.selectById(salary.getEmployeeId());
        String employeeType = employee != null ? employee.getEmployeeType() : null;
        String tplCode = SalaryVoucherTemplateRules.resolveTemplateCode(employeeType, voucherType);
        if (voucherType == 2 && StringUtils.isNotBlank(salary.getAccrualVoucherId())) {
            return Message.ok(SalaryVoucherTemplateRules.alreadyGeneratedMessage(employeeType, voucherType));
        } else if (voucherType == 3 && StringUtils.isNotBlank(salary.getSalaryVoucherId())) {
            return Message.ok(SalaryVoucherTemplateRules.alreadyGeneratedMessage(employeeType, voucherType));
        }
        
        String currentTerm = configSysService.getCurrentTerm(bookId);

        int year = Integer.parseInt(currentTerm.split("-")[0]);
        int month = Integer.parseInt(currentTerm.split("-")[1]);

        LambdaQueryWrapper<VoucherTemplate> itemTpl = Wrappers.lambdaQuery();
        itemTpl.eq(VoucherTemplate::getRelatedId, bookId);
        itemTpl.eq(VoucherTemplate::getCode, tplCode);
        itemTpl.eq(VoucherTemplate::getDeleted, "n");
        
        VoucherTemplate voucherTemplate = voucherTemplateMapper.selectOne(itemTpl);
        if(voucherTemplate == null) {
        	return Message.failed("凭证模板["+tplCode+"]未设置！");
        }
        
        Date voucherDate = null;
        if(voucherTemplate.getVoucherDate().equals(0)) {
        	voucherDate = configSysService.getCurrentTermLastDate(bookId);
        }else if(0 < voucherTemplate.getVoucherDate() && voucherTemplate.getVoucherDate()< 31 ){
        	String voucherDateString = "";
        	if(voucherTemplate.getVoucherDate() < 10) {
        		voucherDateString = currentTerm+"-0"+voucherTemplate.getVoucherDate();
        	}else {
        		voucherDateString = currentTerm+"-"+voucherTemplate.getVoucherDate();
        	}
        	voucherDate = DateUtils.parse(voucherDateString, DateUtils.FORMAT_DATE_YYYY_MM_DD);
        }
        
        log.debug("voucherTemplate {}", voucherTemplate);
        LambdaQueryWrapper<VoucherTemplateItem> itemLqw = Wrappers.lambdaQuery();
        itemLqw.eq(VoucherTemplateItem::getRelatedId, voucherTemplate.getRelatedId());
        itemLqw.eq(VoucherTemplateItem::getTemplateId, voucherTemplate.getId());
        List<VoucherTemplateItem> items = voucherTemplateItemMapper.selectList(itemLqw);
        
        BigDecimal debitAmount = BigDecimal.ZERO;
        BigDecimal creditAmount = BigDecimal.ZERO;

        List<VoucherItemChangeDto> voucherItems = new ArrayList<>();
        Map<String, VoucherTemplateItem> itemsMap = new HashMap<>();
        for (VoucherTemplateItem item : items) {
            itemsMap.put(item.getSubjectCode(), item);
        }

        try {
        if (voucherTemplate.getCode().equals("fp_lwf")) {//收发票
        	if(SubjectCodeCompat.mapContains(itemsMap, "660222")) {
	       		 //劳务费
	       		 debitAmount = debitAmount.add(salary.getPayAmount());
	       		 voucherItems.add(createVoucherItemDto(bookId, SubjectCodeCompat.resolveFromMap(itemsMap, "660222"), salary.getPayAmount()));
        	}
        	if(SubjectCodeCompat.mapContains(itemsMap, "222114")) {
	       		 //个税
        		creditAmount = creditAmount.add(salary.getPersonalTax());
	       		 voucherItems.add(createVoucherItemDto(bookId, SubjectCodeCompat.resolveFromMap(itemsMap, "222114"), salary.getPersonalTax()));
        	}
        	if(SubjectCodeCompat.mapContains(itemsMap, "224101")) {
	       		 //应付个人
        		 creditAmount = creditAmount.add(salary.getTotalAmount());
	       		 voucherItems.add(createVoucherItemDto(bookId, SubjectCodeCompat.resolveFromMap(itemsMap, "224101"), salary.getTotalAmount()));
        	}
        }else if (voucherTemplate.getCode().equals("zf_lwf")) {//发放劳务费
        	if(SubjectCodeCompat.mapContains(itemsMap, "224101")) {
	       		 //发放金额
	       		 debitAmount = debitAmount.add(salary.getTotalAmount());
	       		 voucherItems.add(createVoucherItemDto(bookId, SubjectCodeCompat.resolveFromMap(itemsMap, "224101"), salary.getTotalAmount()));
        	}
       	
	   		for (VoucherTemplateItem item : items) {
	   			 if(item.getSubjectCode().startsWith("1002")) {
	   				 voucherItems.add(createVoucherItemDto(bookId, item, debitAmount));
	   			 }
	         }
	   		creditAmount = debitAmount;
        } else if ("jt_gz".equals(voucherTemplate.getCode())) {
            BigDecimal amount = salary.getPayAmount() != null ? salary.getPayAmount() : BigDecimal.ZERO;
            for (VoucherTemplateItem item : items) {
                voucherItems.add(createVoucherItemDto(bookId, item, amount));
                if (item.getDirection() != null && item.getDirection() == 1) {
                    debitAmount = debitAmount.add(amount);
                } else {
                    creditAmount = creditAmount.add(amount);
                }
            }
        } else if ("zf_gz".equals(voucherTemplate.getCode())) {
            BigDecimal netPay = salary.getTotalAmount() != null ? salary.getTotalAmount() : BigDecimal.ZERO;
            String payableCode = null;
            if (SubjectCodeCompat.mapContains(itemsMap, "221101")
                    || SubjectCodeCompat.mapContains(itemsMap, "2211")
                    || SubjectCodeCompat.mapContains(itemsMap, "2151")) {
                if (SubjectCodeCompat.mapContains(itemsMap, "221101")) {
                    payableCode = "221101";
                } else if (SubjectCodeCompat.mapContains(itemsMap, "2211")) {
                    payableCode = "2211";
                } else {
                    payableCode = "2151";
                }
                debitAmount = debitAmount.add(netPay);
                voucherItems.add(createVoucherItemDto(
                        bookId, SubjectCodeCompat.resolveFromMap(itemsMap, payableCode), netPay));
            }
            for (VoucherTemplateItem item : items) {
                if (item.getSubjectCode() != null && item.getSubjectCode().startsWith("1002")) {
                    voucherItems.add(createVoucherItemDto(bookId, item, netPay));
                }
            }
            creditAmount = debitAmount;
        }
        } catch (IllegalStateException ex) {
            return Message.failed(ex.getMessage());
        }

        VoucherChangeDto voucherChangeDto = createVoucherChangeDto(book, bookId, voucherDate, year, month, debitAmount);
        voucherChangeDto.setRemark(voucherTemplate.getRemark().replace("{yyyy}", year + "").replace("{mm}", month + "").replace("{name}", employee.getDisplayName()));
        voucherChangeDto.setItems(voucherItems);
        voucherChangeDto.setStatus(VoucherStatusEnum.DRAFT.getValue());

        Message<String> saveResult = voucherService.save(voucherChangeDto);
        if (saveResult.getCode() != Message.SUCCESS) {
            return saveResult;
        }

        LambdaUpdateWrapper<EmployeeSalary> updateWrapper = new LambdaUpdateWrapper<>();
        if (voucherType == 0 || voucherType == 2) {
            updateWrapper.set(EmployeeSalary::getAccrualVoucherId, voucherChangeDto.getId());
        } else if (voucherType == 1 || voucherType == 3) {
            updateWrapper.set(EmployeeSalary::getSalaryVoucherId, voucherChangeDto.getId());
        }
        updateWrapper.eq(EmployeeSalary::getId, dto.getId());
        super.update(updateWrapper);

        return Message.ok(voucherChangeDto.getId());
    }

    private VoucherItemChangeDto createVoucherItemDto(String bookId,
            VoucherTemplateItem item, BigDecimal amount) {
		BookSubject bookSubject = bookSubjectService.resolvePostableSubject(bookId, item.getSubjectCode());
		if (bookSubject == null) {
			throw new IllegalStateException("凭证模板科目[" + item.getSubjectCode() + "]在账套中无可用末级科目");
		}

		VoucherItemChangeDto itemDto = new VoucherItemChangeDto();
		itemDto.setSummary(item.getSummary());
		itemDto.setSubjectId(bookSubject.getId());
		if (item.getDirection() == 1) {
			itemDto.setDebitAmount(amount);
		} else {
			itemDto.setCreditAmount(amount);
		}
		itemDto.setSubjectBalance(bookSubject.getBalance());
		itemDto.setAuxiliary(List.of());
		itemDto.setSubjectCode(bookSubject.getCode());
		itemDto.setSubjectName(bookSubject.getCode() + "-" + bookSubject.getName());
		itemDto.setDetailedAccounts("");

		return itemDto;
	}

    private VoucherChangeDto createVoucherChangeDto(Book book, String bookId,
                                                    Date voucherDate, Integer year, Integer month, BigDecimal amount) {

        Integer wordNum = voucherService.getAbleWordNum(bookId, "记", null, null).getData();

        VoucherChangeDto dto = new VoucherChangeDto();
        dto.setWordHead("记");
        dto.setWordNum(wordNum);
        dto.setBookId(bookId);
        dto.setCompanyName(book.getCompanyName());
        dto.setVoucherDate(voucherDate);
        dto.setVoucherYear(year);
        dto.setVoucherMonth(month);
        dto.setDebitAmount(amount);
        dto.setCreditAmount(amount);

        return dto;
    }
	public Message<String> deleteVoucher(GenerateVoucherDto dto) {
        Integer voucherType = dto.getVoucherType();
        EmployeeSalary salary = super.getById(dto.getId());
        LambdaUpdateWrapper<EmployeeSalary> updateWrapper = new LambdaUpdateWrapper<>();
        updateWrapper.eq(EmployeeSalary::getId, dto.getId());
        List<String> ids = new ArrayList<>();
        if(voucherType.equals(2)) {
        	updateWrapper.set(EmployeeSalary::getAccrualVoucherId, null);
        	ids.add(salary.getAccrualVoucherId());
        }else if(voucherType.equals(3)) {
        	updateWrapper.set(EmployeeSalary::getSalaryVoucherId, null);
        	ids.add(salary.getSalaryVoucherId());
        }
        voucherService.delete(ids, salary.getBookId());
        super.update(updateWrapper);
        
		return Message.ok("删除成功！");
	}
}
