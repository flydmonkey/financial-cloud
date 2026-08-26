package com.financial.cloud.domain.hr;

import com.baomidou.mybatisplus.annotation.*;
import com.fasterxml.jackson.annotation.JsonFormat;
import com.financial.cloud.common.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.io.Serial;
import java.math.BigDecimal;
import java.time.YearMonth;

@EqualsAndHashCode(callSuper = true)
@TableName("employee_salary")
@Data
public class EmployeeSalary extends BaseEntity {
    @Serial
    private static final long serialVersionUID = 6649477256823988480L;

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

    /*公司支出成本*/
    private BigDecimal businessExpenditureCosts;

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

    /**
     * 收票工资凭证编码
     */
    @TableField(updateStrategy = FieldStrategy.ALWAYS)
    private String accrualVoucherId;

    /**
     * 发放工资凭证编码
     */
    @TableField(updateStrategy = FieldStrategy.ALWAYS)
    private String salaryVoucherId;


    @TableField(fill = FieldFill.INSERT)
    @TableLogic(value = "n", delval = "y")
    private String deleted;

    @TableField(exist = false)
    private String employeeName;

    @TableField(exist = false)
    private String bankCardNo;

    @TableField(exist = false)
    private String employeeNumber;

    @TableField(exist = false)
    private String employeeType;
}
