package com.financial.cloud.service.hr;

import org.apache.commons.lang3.StringUtils;

/**
 * Deduplicate salary vouchers across re-push and per-employee-per-month generate.
 */
public final class SalaryVoucherDedupeRules {

    public static final String BLOCK_PUSH_BECAUSE_VOUCHERS =
            "本月工资明细已生成计提或发放凭证，请先在工资明细中删除对应凭证后再重新推送";

    private SalaryVoucherDedupeRules() {
    }

    public static boolean hasLinkedVoucher(String accrualVoucherId, String salaryVoucherId) {
        return StringUtils.isNotBlank(accrualVoucherId) || StringUtils.isNotBlank(salaryVoucherId);
    }

    /** voucherType 2=计提/收票，3=发放 */
    public static boolean hasLinkedVoucherForType(int voucherType, String accrualVoucherId, String salaryVoucherId) {
        if (voucherType == 2 || voucherType == 0) {
            return StringUtils.isNotBlank(accrualVoucherId);
        }
        if (voucherType == 3 || voucherType == 1) {
            return StringUtils.isNotBlank(salaryVoucherId);
        }
        return false;
    }

    public static boolean shouldBlockPush(int linkedRowCount) {
        return linkedRowCount > 0;
    }

    public static String generateBlockedMessage(String employeeType, int voucherType) {
        if (voucherType == 2 || voucherType == 0) {
            return SalaryVoucherTemplateRules.isLaborEmployee(employeeType)
                    ? "该员工本月已生成收票凭证，请勿重复生成"
                    : "该员工本月已生成计提凭证，请勿重复生成";
        }
        return "该员工本月已生成发放凭证，请勿重复生成";
    }
}
