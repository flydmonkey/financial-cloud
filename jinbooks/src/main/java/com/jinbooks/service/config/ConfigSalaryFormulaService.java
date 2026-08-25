package com.jinbooks.service.config;

import cn.hutool.core.bean.BeanUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.jinbooks.common.Message;
import com.jinbooks.domain.config.ConfigSalaryFormula;
import com.jinbooks.dto.config.ConfigSalaryFormulaChangeDto;
import com.jinbooks.dto.config.ConfigSalaryFormulaPageDto;
import com.jinbooks.dto.config.ConfigSalaryItem;
import com.jinbooks.dto.config.ConfigSalaryJson;
import com.jinbooks.dto.config.ConfigSalaryFormulaVo;
import com.jinbooks.dto.common.ListIdsDto;
import com.jinbooks.enums.BookBusinessExceptionEnum;
import com.jinbooks.exception.BusinessException;
import com.jinbooks.repository.config.ConfigSalaryFormulaMapper;
import com.jinbooks.service.config.ConfigSalaryFormulaService;
import com.jinbooks.util.JsonUtils;
import org.apache.commons.lang3.ObjectUtils;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/**
 * @description:
 * @author: orangeBabu
 * @time: 2025/2/8 17:55
 */

@Service
public class ConfigSalaryFormulaService extends ServiceImpl<ConfigSalaryFormulaMapper, ConfigSalaryFormula>{
    public Message<ConfigSalaryFormulaVo> getById(String id) {
        ConfigSalaryFormula configSalaryFormula = super.getById(id);
        ConfigSalaryFormulaVo configSalaryFormulaVo = BeanUtil.copyProperties(configSalaryFormula, ConfigSalaryFormulaVo.class);
        String formula = configSalaryFormula.getFormula();
        configSalaryFormulaVo.setFormulaItems(new ArrayList<>());
        if (StringUtils.isNotBlank(formula)) {
            ConfigSalaryJson configSalaryJson = JsonUtils.stringToObject(formula, ConfigSalaryJson.class);
            if (Objects.nonNull(configSalaryJson)) {
                configSalaryFormulaVo.setFormulaItems(configSalaryJson.getFormulaItems());
            }
        }

        return Message.ok(configSalaryFormulaVo);
    }
    public Message<Page<ConfigSalaryFormula>> pageList(ConfigSalaryFormulaPageDto dto) {
        LambdaQueryWrapper<ConfigSalaryFormula> wrapper = new LambdaQueryWrapper<>();
        if (StringUtils.isNotEmpty(dto.getRuleName())) {
            wrapper.like(ConfigSalaryFormula::getRuleName, dto.getRuleName());
        }

        Page<ConfigSalaryFormula> page = super.page(dto.build(), wrapper);
        return Message.ok(page);
    }
    @Transactional
    public Message<String> save(ConfigSalaryFormulaChangeDto dto) {
        checkRuleName(dto, false);

        ConfigSalaryFormula configSalaryFormula = BeanUtil.copyProperties(dto, ConfigSalaryFormula.class);
        List<ConfigSalaryItem> formulaItems = dto.getFormulaItems();
        if (ObjectUtils.isNotEmpty(formulaItems)) {
            configSalaryFormula.setFormula(JsonUtils.toString(new ConfigSalaryJson(formulaItems)));
        }
        configSalaryFormula.setFormulaText(dto.getFormulaString());
        boolean result = super.save(configSalaryFormula);
        return result ? Message.ok("新增成功") : Message.failed("新增失败");
    }
    @Transactional
    public Message<String> update(ConfigSalaryFormulaChangeDto dto) {
        checkRuleName(dto, true);

        ConfigSalaryFormula configSalaryFormula = BeanUtil.copyProperties(dto, ConfigSalaryFormula.class);
        List<ConfigSalaryItem> formulaItems = dto.getFormulaItems();
        if (ObjectUtils.isNotEmpty(formulaItems)) {
            configSalaryFormula.setFormula(JsonUtils.toString(new ConfigSalaryJson(formulaItems)));
        } else {
            configSalaryFormula.setFormula("");
        }
        configSalaryFormula.setFormulaText(dto.getFormulaString());

        boolean result = super.updateById(configSalaryFormula);
        return result ? Message.ok("修改成功") : Message.failed("修改失败");
    }

    /**
     * @Description: 检查公式名称
     * @Param: [dto, isEdit]
     * @return: void
     * @Author: xZen
     * @Date: 2025/2/11 14:57
     */
    private void checkRuleName(ConfigSalaryFormulaChangeDto dto, boolean isEdit) {
        LambdaQueryWrapper<ConfigSalaryFormula> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(ConfigSalaryFormula::getRuleName, dto.getRuleName());
        if (isEdit) {
            wrapper.ne(ConfigSalaryFormula::getId, dto.getId());
        }
        List<ConfigSalaryFormula> list = super.list(wrapper);
        if (ObjectUtils.isNotEmpty(list)) {
            throw new BusinessException(50001, "薪资计算公式的规则名称不能重复，请修改");
        }
    }
    @Transactional
    public Message<String> delete(ListIdsDto dto) {
        List<String> listIds = dto.getListIds();

        isDisable(listIds);

        boolean result = super.removeByIds(listIds);
        return result ? new Message<>(Message.SUCCESS, "删除成功") : new Message<>(Message.FAIL, "删除失败");
    }

    /**
     * @Description: 判断状态
     * @Param: [ids]
     * @return: void
     * @Author: xZen
     * @Date: 2025/2/11 15:13
     */
    private void isDisable(List<String> ids) {
        LambdaQueryWrapper<ConfigSalaryFormula> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(ConfigSalaryFormula::getStatus, 1);
        wrapper.in(ConfigSalaryFormula::getId, ids);

        List<ConfigSalaryFormula> formulas = super.list(wrapper);
        if (ObjectUtils.isNotEmpty(formulas)) {
            throw new BusinessException(
                    BookBusinessExceptionEnum.DISABLE_BEFORE_DELETE.getCode(),
                    BookBusinessExceptionEnum.DISABLE_BEFORE_DELETE.getMsg());
        }
    }
}
