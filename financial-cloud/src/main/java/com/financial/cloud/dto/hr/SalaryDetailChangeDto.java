package com.financial.cloud.dto.hr;

import com.financial.cloud.validation.EditGroup;
import jakarta.validation.constraints.NotEmpty;
import lombok.Data;

import java.math.BigDecimal;

/**
 * @description:
 * @author: orangeBabu
 * @time: 2025/2/20 10:35
 */

@Data
public class SalaryDetailChangeDto {

    @NotEmpty(message = "ID不能为空", groups = {EditGroup.class})
    private String id;

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
}
