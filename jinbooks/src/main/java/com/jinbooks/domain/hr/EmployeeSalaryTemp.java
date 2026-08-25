package com.jinbooks.domain.hr;

import com.baomidou.mybatisplus.annotation.*;
import com.fasterxml.jackson.annotation.JsonFormat;
import com.jinbooks.common.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.io.Serial;
import java.math.BigDecimal;
import java.time.YearMonth;

/**
 * @description:
 * @author: orangeBabu
 * @time: 2025/2/5 16:09
 */

@EqualsAndHashCode(callSuper = true)
@TableName("jbx_employee_salary_temp")
@Data
public class EmployeeSalaryTemp extends BaseEntity {
    @Serial
    private static final long serialVersionUID = 3065934083413732845L;

    /**
     * 主键
     */
    @TableId(type = IdType.ASSIGN_ID)
    private String id;

    private String employeeId;

    @JsonFormat(pattern="yyyy-MM")
    YearMonth belongDate;

    private BigDecimal payBasic;

    private BigDecimal payMerit;

    private BigDecimal payPost;

    private BigDecimal bonus;

    private BigDecimal overtime;

    private BigDecimal allowance;

    private BigDecimal backPay;

    private BigDecimal totalSocialInsurance;

    /*劳务费*/
    private BigDecimal laborFee;

    private BigDecimal providentFund;

    private BigDecimal attendance;

    private BigDecimal otherDeductions;

    private BigDecimal personalTax;

    /**
     * 应发工资 = 工资+应增-应扣
     */
    private BigDecimal payAmount;
    
    /**
     * 实发合计
     */
    private BigDecimal totalAmount;

    private String bookId;

    /*公司社保*/
    private BigDecimal businessSocialInsurance;

    /*公司公积金*/
    private BigDecimal businessProvidentFund;

    /*应税工资*/
    private BigDecimal taxableWages;

    /*税务抵扣*/
    private BigDecimal taxDeduction;

    /**
     * 养老保险
     */
    private BigDecimal insuranceEndowment;

    /**
     * 医疗保险
     */
    private BigDecimal insuranceMedical;

    /**
     * 失业保险
     */
    private BigDecimal insuranceUnemployment;


    /*公司支出成本*/
    private BigDecimal businessExpenditureCosts;

    @TableField(fill = FieldFill.INSERT)
    @TableLogic(value = "n", delval = "y")
    private String deleted;

    @TableField(exist = false)
    private String employeeName;

    @TableField(exist = false)
    private String bankCardNo;
    
    @TableField(exist = false)
    private String bankName;

    @TableField(exist = false)
    private String employeeNumber;
    
    @TableField(exist = false)
    private String employeeType;
}
