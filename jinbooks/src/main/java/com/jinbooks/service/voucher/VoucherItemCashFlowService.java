package com.jinbooks.service.voucher;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.jinbooks.common.Message;
import com.jinbooks.domain.config.ConfigCashFlowBalance;
import com.jinbooks.domain.voucher.VoucherItemCashFlow;
import com.jinbooks.dto.voucher.VoucherItemCashFlowDto;
import com.jinbooks.dto.voucher.VoucherItemPageDto;
import com.jinbooks.dto.voucher.VoucherItemVo;
import com.jinbooks.repository.config.ConfigCashFlowBalanceMapper;
import com.jinbooks.repository.voucher.VoucherItemCashFlowMapper;
import com.jinbooks.service.voucher.VoucherItemCashFlowService;
import lombok.RequiredArgsConstructor;
import org.apache.commons.lang3.ObjectUtils;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;

/**
 * @description:
 * @author: orangeBabu
 * @time: 2025/3/26 18:20
 */

@Service
@RequiredArgsConstructor
public class VoucherItemCashFlowService extends ServiceImpl<VoucherItemCashFlowMapper, VoucherItemCashFlow>{

    private final VoucherItemCashFlowMapper voucherItemCashFlowMapper;

    private final ConfigCashFlowBalanceMapper configCashFlowBalanceMapper;
    @Transactional
    public Message<String> specifyCashFlowItems(VoucherItemCashFlowDto dto) {
        List<VoucherItemCashFlow> voucherItemCashFlowDtos = dto.getVoucherItemCashFlowDtos();


        // 使用 partitioningBy 进行分区
        Map<Boolean, List<VoucherItemCashFlow>> partitionedMap = voucherItemCashFlowDtos.stream()
                .peek(item -> {
                    if (item.getId() == null) {
                        item.setBookId(dto.getBookId());
                        item.setCashFlowItemType(dto.getCashFlowItemType());
                    }
                    if ("no-select".equals(item.getCashFlowItemCode())) {
                        item.setCashFlowItemCode(null);
                    }
                })
                .collect(Collectors.partitioningBy(item -> item.getId() != null));

        // 获取 id 不为 null 的列表 修改
        List<VoucherItemCashFlow> nonNullIdList = partitionedMap.get(true);
        // 从 nonNullIdList 提取已存在的 ID
        Set<String> existingIds = nonNullIdList.stream()
                .map(VoucherItemCashFlow::getId)
                .collect(Collectors.toSet());


        // 获取 id 为 null 的列表 新增
        List<VoucherItemCashFlow> nullIdList = partitionedMap.get(false);

        if (Boolean.TRUE.equals(dto.getIsEdit())) {
            VoucherItemPageDto voucherItemPageDto = new VoucherItemPageDto();
            voucherItemPageDto.setBookId(dto.getBookId());
            voucherItemPageDto.setVoucherId(voucherItemCashFlowDtos.get(0).getVoucherId());
            voucherItemPageDto.setCashFlowItemType(dto.getCashFlowItemType());
            List<String> idsWhenDelete = voucherItemCashFlowMapper.getIdsWhenDelete(voucherItemPageDto);
            Set<String> idsToDeleteSet = new HashSet<>(idsWhenDelete);
            // 需要删除的 ID = idsWhenDelete 中存在，但 existingIds 不存在的 ID
            List<String> idsToBeDeleted = idsToDeleteSet.stream()
                    .filter(id -> !existingIds.contains(id))
                    .toList();
            super.removeBatchByIds(idsToBeDeleted);
        } else {
            VoucherItemCashFlow voucherItemCashFlow = nonNullIdList.get(0);
            //获取当前所选项目的余额方向
            Integer direction = configCashFlowBalanceMapper.selectList(Wrappers.<ConfigCashFlowBalance>lambdaQuery()
                    .eq(ConfigCashFlowBalance::getItemCode, voucherItemCashFlow.getCashFlowItemCode())
                    .eq(ConfigCashFlowBalance::getBookId, dto.getBookId())).get(0).getDirection();
            //获取之前项目的余额方向
            VoucherItemCashFlow voucherItemCashFlowOld = super.getById(voucherItemCashFlow.getId());
            Integer directionOld = configCashFlowBalanceMapper.selectList(Wrappers.<ConfigCashFlowBalance>lambdaQuery()
                    .eq(ConfigCashFlowBalance::getItemCode, voucherItemCashFlowOld.getCashFlowItemCode())
                    .eq(ConfigCashFlowBalance::getBookId, dto.getBookId())).get(0).getDirection();
            if (!Objects.equals(direction, directionOld)) {
                voucherItemCashFlow.setCashFlowBalance(voucherItemCashFlowOld.getCashFlowBalance().negate());
            }
        }
        super.saveBatch(nullIdList);

        super.updateBatchById(nonNullIdList);


        return Message.ok("操作成功");
    }
    public Message<List<VoucherItemVo>> getCashFlowItems(VoucherItemPageDto dto) {
        List<VoucherItemVo> cashFlowItems = voucherItemCashFlowMapper.getCashFlowItems(dto);
        assignEntryNumbers(cashFlowItems);
        return Message.ok(cashFlowItems);
    }


    private void assignEntryNumbers(List<VoucherItemVo> items) {
        if (ObjectUtils.isEmpty(items)) {
            return;
        }

        // 使用Java Stream API分组并收集
        Map<String, List<VoucherItemVo>> groupedItems = items.stream()
                .collect(Collectors.groupingBy(VoucherItemVo::getVoucherItemId));

        // 对键进行排序并使用forEach和AtomicInteger处理
        AtomicInteger entryNo = new AtomicInteger(1);

        groupedItems.keySet().stream()
                .sorted()
                .forEach(voucherItemId -> {
                    int currentEntryNo = entryNo.getAndIncrement();
                    groupedItems.get(voucherItemId).forEach(item -> item.setEntryNo(currentEntryNo));
                });
    }
}
