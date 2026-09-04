package com.financial.cloud.service.hr;

import com.financial.cloud.constants.auth.ConstsUser;

public final class SalaryVoucherTemplateRules {
    private SalaryVoucherTemplateRules() {}

    public static boolean isLaborEmployee(String employeeType) {
        return ConstsUser.EMPLOYEE_TYPE.PARTTIME.equals(employeeType);
    }

    public static String resolveTemplateCode(String employeeType, int voucherType) {
        boolean labor = isLaborEmployee(employeeType);
        if (voucherType == 2) {
            return labor ? "fp_lwf" : "jt_gz";
        }
        // voucherType 3 (and any other payment-side code path)
        return labor ? "zf_lwf" : "zf_gz";
    }

    public static String alreadyGeneratedMessage(String employeeType, int voucherType) {
        if (voucherType == 2) {
            return isLaborEmployee(employeeType) ? "收票凭证已生成" : "计提凭证已生成";
        }
        return "发放凭证已生成";
    }

}
