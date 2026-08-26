package com.financial.cloud.domain.hr;

import com.baomidou.mybatisplus.annotation.FieldFill;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableLogic;
import com.baomidou.mybatisplus.annotation.TableName;
import com.financial.cloud.common.BaseEntity;

import lombok.Data;
import lombok.EqualsAndHashCode;

@EqualsAndHashCode(callSuper = true)
@TableName("employee_tax_deduction")
@Data
public class EmployeeTaxDeduction  extends BaseEntity {

	/**
	 *
	 */
	private static final long serialVersionUID = -4470698555495529250L;

	String id;
	 /**
     * 账套ID
     */
    String bookId;
	/**
	 * 工号
	 */
	String employeeNo;
	/**
	 * 姓名
	 */
	String employeeName;
	/**
	 * 证件类型
	 */
	String idCardType;
	/**
	 * 证件号
	 */
	String idCardNo;
	/**
	 * 子女教育
	 */
	Double education;
	/**
	 * 继续教育
	 */
	Double continuingEducation;
	/**
	 * 大病医疗
	 */
	Double medical;
	/**
	 * 住房贷款利息
	 */
	Double housingLoan;
	/**
	 * 住房租金
	 */
	Double rent;
	/**
	 * 赡养老人
	 */
	Double elderlyCare;
	/**
	 * 3岁以下婴幼儿照护
	 */
	Double infantsCare;
	/**
	 * 个人养老金
	 */
	Double individualPension;
	/**
	 * 企业(职业)年金
	 */
	Double enterprisePension;
	/**
	 * 商业健康保险
	 */
	Double commercialHealth;
	/**
	 * 税延养老保险
	 */
	Double deferredPension;
	/**
	 * 准予扣除的捐赠额
	 */
	Double donationAllowed;
	/**
	 * 其他费用扣除
	 */
	Double others;
	/**
	 * 年度期间
	 */
	int yearPeriod;
	/**
	 * 年度
	 */
	int years;
	/**
	 * 月份
	 */
	int periods;

	/**
     * 删除标记
     */
    @TableField(fill = FieldFill.INSERT)
    @TableLogic(value = "n", delval = "y")
    String deleted;

	public Double getTotalTaxDeduction() {
		return this.education + this.continuingEducation + this.medical + this.housingLoan + this.rent + this.elderlyCare + this.infantsCare +
				this.individualPension + this.enterprisePension + this.commercialHealth + this.deferredPension + this.donationAllowed +
				this.others;
	}
}
