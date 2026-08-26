package com.financial.cloud.dto.hr;

import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.Data;

import java.math.BigDecimal;
import java.util.Date;

@Data
public class TaxDeductionExportVo {

    /*工号*/
    private String employeeNumber;
    
    /*员工类型*/
    private String employeeType;

    /*姓名*/
    private String displayName;

    /**
     * 证件类型
     */
    private String idCardType;

    /**
     * 证件编码
     */
    private String idCardNo;

    /*所得期间起*/
    @JsonFormat(pattern = "yyyy-MM-dd", timezone = "GMT+8")
    private Date startPeriod;

    /*所得期间止*/
    @JsonFormat(pattern = "yyyy-MM-dd", timezone = "GMT+8")
    private Date endPeriod;

    /*本期收入*/
    private BigDecimal income;

    /*本期免税收入*/
    private BigDecimal taxFreeIncome;

    /**
     * 基本养老保险费
     */
    private BigDecimal insuranceEndowment;

    /**
     * 基本医疗保险费
     */
    private BigDecimal insuranceMedical;

    /**
     * 失业保险费
     */
    private BigDecimal insuranceUnemployment;

    /**
     * 住房公积金
     */
    private BigDecimal housingProvidentFund;

    /**
     * 累计子女教育
     */
    private BigDecimal education;

    /**
     * 累计继续教育
     */
    private BigDecimal continuingEducation;

    /**
     * 大病医疗
     */
    private BigDecimal medical;

    /**
     * 累计住房贷款利息
     */
    private BigDecimal housingLoan;

    /**
     * 累计住房租金
     */
    private BigDecimal rent;

    /**
     * 累计赡养老人
     */
    private BigDecimal elderlyCare;

    /**
     * 累计3岁以下婴幼儿照护
     */
    private BigDecimal infantsCare;

    /**
     * 累计个人养老金
     */
    private BigDecimal individualPension;

    /**
     * 企业(职业)年金
     */
    private BigDecimal enterprisePension;

    /**
     * 商业健康保险
     */
    private BigDecimal commercialHealth;

    /**
     * 税延养老保险
     */
    private BigDecimal deferredPension;

    /**
     * 其他
     */
    private BigDecimal others;

    /**
     * 准予扣除的捐赠额
     */
    private BigDecimal donationAllowed;

    /**
     * 税前扣除项目合计
     */
    private BigDecimal totalPreTaxDeduction;

    /**
     * 减免税额
     */
    private BigDecimal taxDeductions;

    /**
     * 减除费用标准
     */
    private BigDecimal deductingStandards;

    /**
     * 已缴税额
     */
    private BigDecimal paidTax;

    /**
     * 备注
     */
    private String remark;
}
