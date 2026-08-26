package com.financial.cloud.dto.hr;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.financial.cloud.validation.AddGroup;
import com.financial.cloud.validation.EditGroup;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import lombok.Data;
import com.financial.cloud.constants.common.MessageKeys;

import java.math.BigDecimal;
import java.util.Date;

@Data
public class EmployeeChangeDto {
    /**
     * 主键
     */
    @NotNull(message = MessageKeys.Validation.COMMON_EDIT_TARGET_REQUIRED, groups = {EditGroup.class})
    private String id;

    /**
     * 姓名
     */
    @NotEmpty(message = MessageKeys.Validation.HR_NAME_REQUIRED, groups = {AddGroup.class, EditGroup.class})
    private String displayName;

    /**
     * 电话号码
     */
//    @NotEmpty(message = MessageKeys.Validation.USER_PHONE_REQUIRED, groups = {AddGroup.class, EditGroup.class})
    private String mobile;

    /**
     * 邮箱地址
     */
//    @NotEmpty(message = MessageKeys.Validation.USER_EMAIL_REQUIRED, groups = {AddGroup.class, EditGroup.class})
    private String email;

    /**
     * 性别:0-其他；1-男；2-女
     */
    @NotNull(message = MessageKeys.Validation.HR_GENDER_REQUIRED, groups = {AddGroup.class, EditGroup.class})
    private Integer gender;

    /**
     * 出生日期
     */
//    @NotNull(message = MessageKeys.Validation.HR_BIRTH_DATE_REQUIRED, groups = {AddGroup.class, EditGroup.class})
    @JsonFormat(pattern = "yyyy-MM-dd", timezone = "GMT+8")
    private Date birthDate;

    /**
     * 证件类型
     */
    @NotNull(message = MessageKeys.Validation.HR_ID_TYPE_REQUIRED, groups = {AddGroup.class, EditGroup.class})
    private Integer idType;

    /**
     * 证件编码
     */
    @NotEmpty(message = MessageKeys.Validation.HR_ID_NUMBER_REQUIRED, groups = {AddGroup.class, EditGroup.class})
    private String idCardNo;

    /**
     * 学历
     */
//    @NotEmpty(message = MessageKeys.Validation.HR_EDUCATION_REQUIRED, groups = {AddGroup.class, EditGroup.class})
    private String education;

    /**
     * 毕业院校
     */
    private String graduateFrom;

    /**
     * 毕业时间
     */
    @JsonFormat(pattern = "yyyy-MM-dd", timezone = "GMT+8")
    private Date graduateDate;

    /**
     * 银行名称
     */
    private String bankName;

    /**
     * 银行卡
     */
    private String bankCardNo;

    /**
     * 住址
     */
    private String homeAddress;

    /**
     * 基本工资
     */
    private BigDecimal payBasic;

    /**
     * 绩效奖金
     */
    private BigDecimal payMerit;

    /**
     * 岗位工资
     */
    private BigDecimal payPost;
    
    /**
     * 劳务费
     */
    private BigDecimal laborFee;

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
     * 工伤保险
     */
    private BigDecimal insuranceEmploymentInjury;

    /**
     * 生育保险
     */
    private BigDecimal insuranceMaternity;

    /**
     * 住房公积金
     */
    private BigDecimal housingProvidentFund;

    /**
     * 养老保险-补充
     */
    private BigDecimal insuranceEndowmentSup;

    /**
     * 医疗保险-补充
     */
    private BigDecimal insuranceMedicalSup;

    /**
     * 住房公积金-补充
     */
    private BigDecimal housingProvidentFundSup;

    /**
     * 工号
     */
    private String employeeNumber;

    /**
     * 部门ID
     */
    @NotEmpty(message = MessageKeys.Validation.HR_DEPARTMENT_REQUIRED, groups = {AddGroup.class, EditGroup.class})
    private String departmentId;

    /**
     * 职务
     */
    private String jobTitle;

    /**
     * 经理编号
     */
    private String managerId;

    /**
     * 入职日期
     */
    @JsonFormat(pattern = "yyyy-MM-dd", timezone = "GMT+8")
    private Date entryDate;

    /**
     * 离职日期
     */
    @JsonFormat(pattern = "yyyy-MM-dd", timezone = "GMT+8")
    private Date quitDate;

    /**
     * 状态:1-启用;0-禁用
     */
    @NotNull(message = MessageKeys.Validation.COMMON_STATUS_REQUIRED, groups = {AddGroup.class, EditGroup.class})
    private Integer status;

    /**
     * 缴费标准-基数统一:0-系统;1-自定义
     */
    private Integer payBaseRule;

    /**
     * 缴费基数
     */
    private BigDecimal payBaseNumber;

    /**
     * 社保卡账户
     */
    private String insuranceFundCard;

    /**
     * 医保账户
     */
    private String medicalCard;

    /**
     * 员工类型
     */
    @NotEmpty(message = MessageKeys.Validation.HR_EMPLOYEE_TYPE_REQUIRED, groups = {AddGroup.class, EditGroup.class})
    private String employeeType;

    /**
     * 员工状态
     */
    @NotEmpty(message = MessageKeys.Validation.HR_EMPLOYEE_STATUS_REQUIRED, groups = {AddGroup.class, EditGroup.class})
    private String employeeStatus;

    private String bookId;
}
