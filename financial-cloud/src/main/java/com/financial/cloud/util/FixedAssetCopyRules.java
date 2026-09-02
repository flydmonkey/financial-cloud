package com.financial.cloud.util;

import org.apache.commons.lang3.StringUtils;

import java.util.function.Predicate;

/**
 * 固定资产卡片复制编码规则。
 */
public final class FixedAssetCopyRules {

    private FixedAssetCopyRules() {
    }

    /**
     * 生成复制编码：原编码-副本；冲突则 -副本2、-副本3…
     */
    public static String nextCopyCode(String sourceCode, Predicate<String> codeExists) {
        String base = StringUtils.defaultString(sourceCode) + "-副本";
        if (codeExists == null || !codeExists.test(base)) {
            return base;
        }
        for (int i = 2; i < 10000; i++) {
            String candidate = base + i;
            if (!codeExists.test(candidate)) {
                return candidate;
            }
        }
        throw new IllegalStateException("Unable to allocate copy code for " + sourceCode);
    }
}
