package com.jinbooks.service.config;

import com.baomidou.mybatisplus.core.toolkit.ObjectUtils;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.jinbooks.common.Message;
import com.jinbooks.domain.config.ConfigInsuranceFund;
import com.jinbooks.repository.config.ConfigInsuranceFundMapper;
import com.jinbooks.service.config.ConfigInsuranceFundService;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * @description:
 * @author: orangeBabu
 * @time: 2025/2/12 15:16
 */

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
