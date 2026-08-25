/*
 * Copyright [2025] [JinBooks of copyright http://www.jinbooks.com]
 * 
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * 
 *     http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 */
 

package com.jinbooks.service.config.impl;

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
public class ConfigInsuranceFundServiceImpl extends ServiceImpl<ConfigInsuranceFundMapper, ConfigInsuranceFund> implements ConfigInsuranceFundService {


    @Override
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
