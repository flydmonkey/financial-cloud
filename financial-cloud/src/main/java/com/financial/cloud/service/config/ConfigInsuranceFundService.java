package com.financial.cloud.service.config;

import com.baomidou.mybatisplus.core.toolkit.ObjectUtils;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.financial.cloud.common.Message;
import com.financial.cloud.domain.config.ConfigInsuranceFund;
import com.financial.cloud.repository.config.ConfigInsuranceFundMapper;
import com.financial.cloud.service.config.ConfigInsuranceFundService;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class ConfigInsuranceFundService extends ServiceImpl<ConfigInsuranceFundMapper, ConfigInsuranceFund>{
    public Message<ConfigInsuranceFund> getCurrent(String bookId) {
        ConfigInsuranceFund configInsuranceFund;
        List<ConfigInsuranceFund> list = super.list(Wrappers.<ConfigInsuranceFund>lambdaQuery()
                .eq(ConfigInsuranceFund::getBookId, bookId));
        if (ObjectUtils.isNotEmpty(list)) {
            configInsuranceFund = list.get(0);
        } else {
            return Message.failed("请添加配置");
        }

        return Message.ok(configInsuranceFund);
    }
}
