package com.financial.cloud.service.config;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.financial.cloud.common.Message;
import com.financial.cloud.domain.book.BookInitBalance;
import com.financial.cloud.dto.book.BookChangeDto;
import com.financial.cloud.domain.config.ConfigCashFlowBalance;
import com.financial.cloud.dto.config.ConfigCashFlowChangeDto;
import com.financial.cloud.dto.config.ConfigCashFlowPageDto;
import com.financial.cloud.dto.report.CashFlowSubjectBalanceVo;
import com.financial.cloud.enums.error.StatementErrorCode;
import com.financial.cloud.exception.BusinessException;
import com.financial.cloud.repository.book.BookInitBalanceMapper;
import com.baomidou.mybatisplus.extension.toolkit.Db;
import com.financial.cloud.repository.config.ConfigCashFlowBalanceMapper;
import com.financial.cloud.service.config.ConfigCashFlowBalanceService;

import cn.hutool.core.bean.BeanUtil;
import cn.hutool.core.bean.copier.CopyOptions;
import lombok.RequiredArgsConstructor;
import org.apache.commons.lang3.ObjectUtils;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.math.BigDecimal;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class ConfigCashFlowBalanceService extends ServiceImpl<ConfigCashFlowBalanceMapper, ConfigCashFlowBalance>{

    private final BookInitBalanceMapper bookInitBalanceMapper;
    public Message<CashFlowSubjectBalanceVo> pageList(ConfigCashFlowPageDto dto) {
        LambdaQueryWrapper<ConfigCashFlowBalance> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(ConfigCashFlowBalance::getBookId, dto.getBookId())
                .orderByAsc(ConfigCashFlowBalance::getSortIndex);
        List<ConfigCashFlowBalance> list = super.list(wrapper);

        //获取科目初始余额
        LambdaQueryWrapper<BookInitBalance> wrapperBookInit = new LambdaQueryWrapper<>();
        wrapperBookInit.eq(BookInitBalance::getBookId, dto.getBookId());
        wrapperBookInit.eq(BookInitBalance::getLevel, 1);
        wrapperBookInit.eq(BookInitBalance::getIsCash, 1);

        List<BookInitBalance> bookInitBalances = bookInitBalanceMapper.selectList(wrapperBookInit);

        BigDecimal yearBalanceBeginning = bookInitBalances.stream()
                .map(BookInitBalance::getBalance)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        // 用于存储中间计算结果的Map
        Map<String, ConfigCashFlowBalance> itemMap = new HashMap<>();
        for (ConfigCashFlowBalance item : list) {
            itemMap.put(item.getItemCode(), item);
        }

        // 根据itemCode找到对应的项目并赋值
        // 37-xj-qcye: 现金期初余额
        ConfigCashFlowBalance cashBeginningBalance = itemMap.get("37-xj-qcye");
        if (cashBeginningBalance != null) {
            cashBeginningBalance.setBalance(yearBalanceBeginning);
        }

        // 64-xj-xjqc: 现金及现金等价物期初余额
        ConfigCashFlowBalance cashEquivalentsBeginning = itemMap.get("64-xj-xjqc");
        if (cashEquivalentsBeginning != null) {
            cashEquivalentsBeginning.setBalance(yearBalanceBeginning);
        }

        // 计算 38-xj-qmye: 现金期末余额 = 36-xj-djje(当期净增加额) + 37-xj-qcye(期初余额)
        ConfigCashFlowBalance netIncrease = itemMap.get("36-xj-djje");
        ConfigCashFlowBalance cashEndingBalance = itemMap.get("38-xj-qmye");

        if (netIncrease != null && cashBeginningBalance != null && cashEndingBalance != null) {
            BigDecimal netIncreaseAmount = netIncrease.getBalance() != null ? netIncrease.getBalance() : BigDecimal.ZERO;
            BigDecimal endingAmount = netIncreaseAmount.add(yearBalanceBeginning);
            cashEndingBalance.setBalance(endingAmount);

            // 63-xj-xjqm: 现金及现金等价物期末余额
            ConfigCashFlowBalance cashEquivalentsEnding = itemMap.get("63-xj-xjqm");
            if (cashEquivalentsEnding != null) {
                cashEquivalentsEnding.setBalance(endingAmount);
            }

            // 67-xj-djqm: 当期净增加额(另一处) = 63-xj-xjqm(期末) - 64-xj-xjqc(期初)
            ConfigCashFlowBalance netIncrease2 = itemMap.get("67-xj-djqm");
            if (netIncrease2 != null && cashEquivalentsBeginning != null) {
                BigDecimal netIncrease2Amount = endingAmount.subtract(yearBalanceBeginning);
                netIncrease2.setBalance(netIncrease2Amount);
            }
        }

        CashFlowSubjectBalanceVo cashFlowSubjectBalanceVo = new CashFlowSubjectBalanceVo(list, bookInitBalances);
        return Message.ok(cashFlowSubjectBalanceVo);

    }
    @Transactional
    public Message<String> save(ConfigCashFlowChangeDto dto) {
        List<ConfigCashFlowBalance> cashFlowItemDtos = dto.getCashFlowItemDtos();
        boolean result = super.updateBatchById(cashFlowItemDtos);

        return result ? Message.ok("保存成功") : Message.failed("保存失败");
    }
    public Message<List<ConfigCashFlowBalance>> getSelectItem(Integer cashFlowItemType) {
        LambdaQueryWrapper<ConfigCashFlowBalance> wrapper = new LambdaQueryWrapper<>();
        wrapper.isNull(ConfigCashFlowBalance::getBookId);
        wrapper.orderByAsc(ConfigCashFlowBalance::getSortIndex);
        if (cashFlowItemType == 0) {
            wrapper.eq(ConfigCashFlowBalance::getIsMain, 1);
        } else {
            wrapper.eq(ConfigCashFlowBalance::getIsAdditional, 1);
        }


        // Fetch the list
        List<ConfigCashFlowBalance> list = super.list(wrapper);

        if (ObjectUtils.isEmpty(list)) {
            throw new BusinessException(StatementErrorCode.CASH_FLOW_SQL_REQUIRED);
        }

        return Message.ok(list);
    }
	public boolean deleteByBookIds(List<String> bookIds) {
		LambdaQueryWrapper<ConfigCashFlowBalance> wrapper = new LambdaQueryWrapper<>();
		wrapper.in(ConfigCashFlowBalance::getBookId, bookIds);
		this.getBaseMapper().delete(wrapper);
		return true;
	}
	public boolean configCashFlowBalance(BookChangeDto dto) {
		String bookId = dto.getId();

        // 使用count查询判断记录是否存在
        Long count = this.getBaseMapper().selectCount(
                Wrappers.<ConfigCashFlowBalance>lambdaQuery()
                        .eq(ConfigCashFlowBalance::getBookId, bookId));

        if (count == 0) {
            List<ConfigCashFlowBalance> configList = this.getBaseMapper().selectList(Wrappers.<ConfigCashFlowBalance>lambdaQuery()
                    .isNull(ConfigCashFlowBalance::getBookId));
            if (ObjectUtils.isEmpty(configList)) {
                throw new BusinessException(StatementErrorCode.CASH_FLOW_SQL_REQUIRED);
            } else {
                // 使用Java 17特性优化批量复制并设置bookId
                CopyOptions copyOptions = CopyOptions.create()
                        .setIgnoreProperties("id", "createdBy", "createdDate", "modifiedBy", "modifiedDate", "book_id");

                // 一步完成复制和设置bookId
                List<ConfigCashFlowBalance> configCashFlowBalances = configList.stream()
                        .map(source -> {
                            ConfigCashFlowBalance target = new ConfigCashFlowBalance();
                            BeanUtil.copyProperties(source, target, copyOptions);
                            target.setBookId(bookId);
                            return target;
                        })
                        .toList();

                // 执行批量插入
                Db.saveBatch(configCashFlowBalances);
            }
        }
		return false;
	}
}
