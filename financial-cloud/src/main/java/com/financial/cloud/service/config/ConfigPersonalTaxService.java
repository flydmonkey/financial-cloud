package com.financial.cloud.service.config;

import cn.hutool.core.bean.BeanUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.financial.cloud.common.Message;
import com.financial.cloud.domain.config.ConfigPersonalTax;
import com.financial.cloud.dto.config.ConfigPersonalTaxChangeDto;
import com.financial.cloud.dto.config.ConfigPersonalTaxPageDto;
import com.financial.cloud.dto.common.ListIdsDto;
import com.financial.cloud.enums.error.ConfigErrorCode;
import com.financial.cloud.exception.BusinessException;
import com.financial.cloud.repository.config.ConfigPersonalTaxMapper;
import com.financial.cloud.service.config.ConfigPersonalTaxService;
import org.apache.commons.lang3.ObjectUtils;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/**
 * @description:
 * @author: orangeBabu
 * @time: 2025/2/6 17:48
 */

@Service
public class ConfigPersonalTaxService extends ServiceImpl<ConfigPersonalTaxMapper, ConfigPersonalTax>{
    public Message<Page<ConfigPersonalTax>> pageList(ConfigPersonalTaxPageDto dto) {
        LambdaQueryWrapper<ConfigPersonalTax> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(ConfigPersonalTax::getType, dto.getType());
        Page<ConfigPersonalTax> page = super.page(dto.build(), wrapper);
        return Message.ok(page);
    }


    private void checkNumberInterval(ConfigPersonalTaxChangeDto dto) {
        Integer maxNum = dto.getMaxNum();
        Integer minNum = dto.getMinNum();
        if (Objects.nonNull(maxNum) && minNum >= maxNum) {
            throw new BusinessException(ConfigErrorCode.PERSONAL_TAX_RANGE_INVALID);
        }

        if (Objects.isNull(maxNum) && Objects.isNull(minNum)) {
            throw new BusinessException(ConfigErrorCode.PERSONAL_TAX_RANGE_INVALID);
        }
    }
    @Transactional
    public Message<String> save(ConfigPersonalTaxChangeDto dto) {
        //校验等级是否重复
        checkLevel(dto, false);
        //校验所得额区间
        checkNumberInterval(dto);
        //设置速算扣除数
        ConfigPersonalTax configJbxTax = BeanUtil.copyProperties(dto, ConfigPersonalTax.class);
        configJbxTax.setCalculationDeduction(calculateQuickDeduction(dto));

        boolean result = super.save(configJbxTax);


        return result ? Message.ok("新增成功") : Message.failed("新增失败");
    }
    @Transactional
    public Message<String> update(ConfigPersonalTaxChangeDto dto) {
        //校验所得额区间
        checkNumberInterval(dto);
        ConfigPersonalTax configJbxTax = BeanUtil.copyProperties(dto, ConfigPersonalTax.class);
        boolean result = super.updateById(configJbxTax);
        if (result) {
            //设置速算扣除数
            calculateQuickDeductionSetting(dto);
        }

        return result ? Message.ok("修改成功") : Message.failed("修改失败");
    }
    public Message<String> delete(ListIdsDto dto) {
        List<String> listIds = dto.getListIds();
        boolean result = super.removeByIds(listIds);

        return result ? Message.ok("删除成功") : Message.failed("删除失败");
    }

    /**
     * @Description: 检查税率层级是否重复
     * @Param: [dto, isEdit]
     * @return: void
     * @Author: xZen
     * @Date: 2025/2/7 10:53
     */
    private void checkLevel(ConfigPersonalTaxChangeDto dto, boolean isEdit) {
        LambdaQueryWrapper<ConfigPersonalTax> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(ConfigPersonalTax::getLevel, dto.getLevel());
        wrapper.eq(ConfigPersonalTax::getType, dto.getType());
        if (isEdit) {
            wrapper.ne(ConfigPersonalTax::getId, dto.getId());
        }
        List<ConfigPersonalTax> list = super.list(wrapper);
        if (ObjectUtils.isNotEmpty(list)) {
            throw new BusinessException(ConfigErrorCode.PERSONAL_TAX_LEVEL_DUPLICATE);
        }
    }

    private void calculateQuickDeductionSetting(ConfigPersonalTaxChangeDto dto) {
        Integer level = dto.getLevel();
        LambdaQueryWrapper<ConfigPersonalTax> wrapper = new LambdaQueryWrapper<>();
        wrapper.orderByAsc(ConfigPersonalTax::getLevel);
        wrapper.eq(ConfigPersonalTax::getType, dto.getType());
        List<ConfigPersonalTax> levels = super.list(wrapper);

        List<ConfigPersonalTax> configJbxTaxes = new ArrayList<>(levels);

        if (ObjectUtils.isNotEmpty(levels)) {
            // 从第二级开始计算
            for (int i = level - 1; i < levels.size(); i++) {
                ConfigPersonalTax currentLevel = levels.get(i);
                double deduction = 0;

                // 计算当前层级与前面所有层级的税额差
                for (int j = 0; j < i; j++) {
                    ConfigPersonalTax prevLevel = levels.get(j);
                    int taxableAmount;

                    // 如果前一级有封顶金额，使用封顶金额
                    if (Objects.nonNull(prevLevel.getMaxNum())) {
                        taxableAmount = prevLevel.getMaxNum() - prevLevel.getMinNum();
                    } else {
                        taxableAmount = currentLevel.getMinNum() - prevLevel.getMinNum();
                    }

                    // 累加税额差（使用getRealRate()获取真实税率）
                    deduction += taxableAmount * (currentLevel.getRealRate() - prevLevel.getRealRate());
                }

                // 设置速算扣除数
                currentLevel.setCalculationDeduction(deduction);
                configJbxTaxes.add(currentLevel);
            }
        }

        super.updateBatchById(configJbxTaxes);
    }

    /**
     * @Description: 新增设置速算扣除数
     * @Param: [currentLevel]
     * @return: java.lang.Double
     * @Author: xZen
     * @Date: 2025/2/7 15:39
     */
    private Double calculateQuickDeduction(ConfigPersonalTaxChangeDto currentLevel) {
        if (currentLevel.getLevel() == 1) {
            return (double) 0;
        }

        LambdaQueryWrapper<ConfigPersonalTax> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(ConfigPersonalTax::getType, currentLevel.getType());
        // 小于等于当前level
        wrapper.le(ConfigPersonalTax::getLevel, currentLevel.getLevel())
                //正序排列
                .orderByAsc(ConfigPersonalTax::getLevel);
        List<ConfigPersonalTax> list = super.list(wrapper);
        if (ObjectUtils.isNotEmpty(list)) {
            double deduction = 0;

            // 计算当前层级与前面所有层级的税额差
            for (int j = 0; j < list.size(); j++) {
                ConfigPersonalTax prevLevel = list.get(j);
                int taxableAmount;

                // 如果前一级有封顶金额，使用封顶金额
                if (Objects.nonNull(prevLevel.getMaxNum())) {
                    taxableAmount = prevLevel.getMaxNum() - prevLevel.getMinNum();
                } else {
                    taxableAmount = currentLevel.getMinNum() - prevLevel.getMinNum();
                }

                // 累加税额差（使用getRealRate()获取真实税率）
                deduction += taxableAmount * (currentLevel.getRealRate() - prevLevel.getRealRate());
            }
            return deduction;
        } else {
            return (double) 0;
        }
    }


}
