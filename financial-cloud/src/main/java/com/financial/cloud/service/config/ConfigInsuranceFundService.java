package com.financial.cloud.service.config;

import com.baomidou.mybatisplus.core.toolkit.ObjectUtils;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.financial.cloud.common.Message;
import com.financial.cloud.domain.config.ConfigInsuranceFund;
import com.financial.cloud.repository.config.ConfigInsuranceFundMapper;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class ConfigInsuranceFundService extends ServiceImpl<ConfigInsuranceFundMapper, ConfigInsuranceFund>{

    public Message<ConfigInsuranceFund> getCurrent(String bookId) {
        List<ConfigInsuranceFund> list = super.list(Wrappers.<ConfigInsuranceFund>lambdaQuery()
                .eq(ConfigInsuranceFund::getBookId, bookId));
        if (ObjectUtils.isNotEmpty(list)) {
            ConfigInsuranceFund existing = list.get(0);
            if (isUninitialized(existing)) {
                return Message.ok(applyNationalMinimumDefaults(existing));
            }
            return Message.ok(existing);
        }
        return Message.ok(ensureDefaults(bookId));
    }

    /**
     * Create and persist national-minimum defaults when the book has no insurance-fund config yet.
     */
    @Transactional
    public ConfigInsuranceFund ensureDefaults(String bookId) {
        List<ConfigInsuranceFund> list = super.list(Wrappers.<ConfigInsuranceFund>lambdaQuery()
                .eq(ConfigInsuranceFund::getBookId, bookId));
        if (ObjectUtils.isNotEmpty(list)) {
            ConfigInsuranceFund existing = list.get(0);
            if (isUninitialized(existing)) {
                return applyNationalMinimumDefaults(existing);
            }
            return existing;
        }
        ConfigInsuranceFund defaults = InsuranceFundDefaults.createForBook(bookId);
        super.save(defaults);
        return defaults;
    }

    private static boolean isUninitialized(ConfigInsuranceFund cfg) {
        return cfg.getPayBase() == null || cfg.getPayBase().signum() <= 0;
    }

    private ConfigInsuranceFund applyNationalMinimumDefaults(ConfigInsuranceFund existing) {
        ConfigInsuranceFund defaults = InsuranceFundDefaults.createForBook(existing.getBookId());
        defaults.setId(existing.getId());
        super.updateById(defaults);
        return defaults;
    }
}
