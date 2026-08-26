package com.financial.cloud.controller.config;

import com.financial.cloud.authn.annotation.CurrentUser;
import com.financial.cloud.common.Message;
import com.financial.cloud.common.PageQuery;
import com.financial.cloud.domain.config.ConfigSys;
import com.financial.cloud.dto.common.ListIdsDto;
import com.financial.cloud.domain.idm.UserInfo;
import com.financial.cloud.service.config.ConfigSysService;

import lombok.RequiredArgsConstructor;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Validated
@RequiredArgsConstructor
@RestController
@RequestMapping("/api/config/sys")
public class ConfigSysController {
    private final ConfigSysService configService;

    /**
     * 获取账簿配置参数
     */
    @GetMapping("/books")
    public Message<List<ConfigSys>> getBooksConfigList(@CurrentUser UserInfo userInfo) {
        return configService.getBookShowList(userInfo.getBookId());
    }

    /**
     * 获取参数配置列表
     */
    @GetMapping("/fetch")
    public Message<List<ConfigSys>> list(ConfigSys config, PageQuery pageQuery, @CurrentUser UserInfo userInfo) {
        config.setBookId(userInfo.getBookId());
        return configService.pageList(config, pageQuery);
    }

    /**
     * 根据参数编号获取详细信息
     *
     * @param configId 参数ID
     */
    @GetMapping(value = "/get/{configId}")
    public Message<ConfigSys> getInfo(@PathVariable(name = "configId") String configId) {
        return configService.getById(configId);
    }

    /**
     * 根据参数键名查询参数值
     *
     * @param configKey 参数Key
     */
    @GetMapping(value = "/configKey/{configKey}")
    public Message<String> getConfigKey(@PathVariable(name = "configKey") String configKey, @CurrentUser UserInfo userInfo) {
        return Message.ok(configService.selectConfigByKey(userInfo.getBookId(), configKey));
    }

    /**
     * 新增参数配置
     */
    @PostMapping("/save")
    public Message<String> save(@Validated @RequestBody ConfigSys config, @CurrentUser UserInfo userInfo) {
        config.setBookId(userInfo.getBookId());
        if (!configService.checkConfigKeyUnique(config)) {
            return Message.failed("新增参数'" + config.getConfigName() + "'失败，参数键名已存在");
        }
        configService.save(config);
        return Message.ok("成功");
    }

    /**
     * 修改参数配置
     */
    @PutMapping("/update")
    public Message<String> update(@Validated @RequestBody ConfigSys config, @CurrentUser UserInfo userInfo) {
        config.setBookId(userInfo.getBookId());
        if (!configService.checkConfigKeyUnique(config)) {
            return Message.failed("修改参数'" + config.getConfigName() + "'失败，参数键名已存在");
        }
        configService.update(config);
        return Message.ok("成功");
    }

    /**
     * 根据参数键名修改参数配置
     */
    @PutMapping("/updateByKey")
    public Message<String> updateByKey(@RequestBody ConfigSys config, @CurrentUser UserInfo userInfo) {
        config.setBookId(userInfo.getBookId());
        configService.update(config);
        return Message.ok("成功");
    }

    /**
     * 删除参数配置
     */
    @DeleteMapping("/delete")
    public Message<String> remove(@RequestBody ListIdsDto dto) {
        configService.delete(dto);
        return Message.ok("成功");
    }
}
