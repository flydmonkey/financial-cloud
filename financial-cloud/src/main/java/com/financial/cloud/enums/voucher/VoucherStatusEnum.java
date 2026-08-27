package com.financial.cloud.enums.voucher;

import com.financial.cloud.enums.common.BaseEnum;
import lombok.Getter;

import java.util.LinkedHashMap;
import java.util.Map;

@Getter
public enum VoucherStatusEnum implements BaseEnum {
    DRAFT("draft", "暂存"),
    UNDER_REVIEW("reviewing", "审核中"),
    COMPLETED("completed", "已审核"),
    REJECTED("rejected", "被拒绝"),
    CANCELLED("cancelled", "已取消");

    private final String value;
    private final String label;

    VoucherStatusEnum(String value, String label) {
        this.value = value;
        this.label = label;
    }

    public static VoucherStatusEnum get(String value) {
        for (VoucherStatusEnum status : VoucherStatusEnum.values()) {
            if (status.getValue().equals(value)) {
                return status;
            }
        }
        return null;
    }

    @Override
    public Map<Object, Object> getMap() {
        Map<Object, Object> map = new LinkedHashMap<>();
        for (VoucherStatusEnum status : VoucherStatusEnum.values()) {
            map.put(status.getValue(), status.getLabel());
        }
        return map;
    }
}
