package com.financial.cloud.service.config;

import com.baomidou.mybatisplus.core.conditions.AbstractWrapper;
import com.baomidou.mybatisplus.core.conditions.Wrapper;
import com.financial.cloud.common.Message;
import com.financial.cloud.domain.config.ConfigSys;
import com.financial.cloud.repository.config.ConfigSysMapper;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ConfigSysServiceTest {

    @Mock
    private ConfigSysMapper baseMapper;

    @InjectMocks
    private ConfigSysService configSysService;

    @Test
    @SuppressWarnings({"unchecked", "rawtypes"})
    void updateByKeyScopesWrapperToBookId() {
        ConfigSys config = new ConfigSys();
        config.setBookId("book-a");
        config.setConfigKey("sys.payment.term.current");
        config.setConfigValue("2026-08");

        when(baseMapper.update(eq(config), any(Wrapper.class))).thenReturn(1);

        Message<String> result = configSysService.update(config);

        assertEquals(Message.SUCCESS, result.getCode());
        assertEquals("2026-08", result.getData());

        ArgumentCaptor<Wrapper> wrapperCaptor = ArgumentCaptor.forClass(Wrapper.class);
        verify(baseMapper).update(eq(config), wrapperCaptor.capture());
        AbstractWrapper<?, ?, ?> wrapper = (AbstractWrapper<?, ?, ?>) wrapperCaptor.getValue();
        // MP 将每个 eq 展开为 column/eq/value，两条件间还有 AND → 7 段
        assertEquals(7, wrapper.getExpression().getNormal().size());
    }

    @Test
    @SuppressWarnings({"unchecked", "rawtypes"})
    void updateByKeyWithoutBookIdOnlyFiltersConfigKey() {
        ConfigSys config = new ConfigSys();
        config.setConfigKey("sys.payment.term.current");
        config.setConfigValue("2026-08");

        when(baseMapper.update(eq(config), any(Wrapper.class))).thenReturn(1);

        assertEquals(Message.SUCCESS, configSysService.update(config).getCode());

        ArgumentCaptor<Wrapper> wrapperCaptor = ArgumentCaptor.forClass(Wrapper.class);
        verify(baseMapper).update(eq(config), wrapperCaptor.capture());
        AbstractWrapper<?, ?, ?> wrapper = (AbstractWrapper<?, ?, ?>) wrapperCaptor.getValue();
        // 仅 configKey 一条件 → 3 段
        assertEquals(3, wrapper.getExpression().getNormal().size());
    }
}
