package com.financial.cloud.service.standard;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.toolkit.StringUtils;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.financial.cloud.common.Message;
import com.financial.cloud.domain.book.BookSubject;
import com.financial.cloud.domain.standard.StandardSubjectCashFlow;
import com.financial.cloud.dto.standard.StandardSubjectCashFlowDto;
import com.financial.cloud.dto.standard.StandardSubjectCashFlowVo;
import com.financial.cloud.exception.BusinessException;
import com.financial.cloud.repository.book.BookSubjectMapper;
import com.financial.cloud.repository.standard.StandardSubjectCashFlowMapper;
import com.financial.cloud.service.standard.StandardSubjectCashFlowService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/**
 * @description:
 * @author: orangeBabu
 * @time: 2025/4/18 10:33
 */

@Service
@RequiredArgsConstructor
public class StandardSubjectCashFlowService extends ServiceImpl<StandardSubjectCashFlowMapper, StandardSubjectCashFlow>{

    private final BookSubjectMapper bookSubjectMapper;

    private final StandardSubjectCashFlowMapper standardSubjectCashFlowMapper;
    public Message<String> save(StandardSubjectCashFlowDto dto) {
        final String inputItemCodeMain = dto.getItemCodeMain();
        final String inputItemCodeSupple = dto.getItemCodeSupple();
        final String subjectCode = dto.getSubjectCode();
        final String bookId = dto.getBookId();
        final String direction = dto.getDirection();

        // 查询目标科目
        var subject = bookSubjectMapper.selectList(Wrappers.<BookSubject>lambdaQuery()
                        .eq(BookSubject::getBookId, bookId)
                        .eq(BookSubject::getCode, subjectCode))
                .stream()
                .findFirst()
                .orElseThrow(() -> new BusinessException(50001, "无法获取所选择的科目"));

        final String subjectId = subject.getId();

        // 判断是否为最下级科目
        var hasChildSubjects = bookSubjectMapper.exists(Wrappers.<BookSubject>lambdaQuery()
                .ne(BookSubject::getId, subjectId)
                .like(BookSubject::getIdPath, subjectId));

        if (hasChildSubjects) {
            throw new BusinessException(50001, "当前层级无法设置科目默认现金流量项目，请选择最下级的会计科目");
        }

        // 查询已经存在的关系
        var standardSubjectCashFlowVo = standardSubjectCashFlowMapper.fetchRelationships(dto);
        String returnedItemCodeMain = standardSubjectCashFlowVo != null ? standardSubjectCashFlowVo.getItemCodeMain() : null;
        String returnedItemCodeSupple = standardSubjectCashFlowVo != null ? standardSubjectCashFlowVo.getItemCodeSupple() : null;

        var cashFlowToAddOrUpdate = new ArrayList<StandardSubjectCashFlow>();
        var codesToDelete = new ArrayList<String>();

        // 处理主项目和辅助项目
        processItemCode(inputItemCodeMain, returnedItemCodeMain, bookId, subjectCode, direction, cashFlowToAddOrUpdate, codesToDelete);
        processItemCode(inputItemCodeSupple, returnedItemCodeSupple, bookId, subjectCode, direction, cashFlowToAddOrUpdate, codesToDelete);

        // 删除老数据
        if (!codesToDelete.isEmpty()) {
            remove(Wrappers.<StandardSubjectCashFlow>lambdaQuery()
                    .eq(StandardSubjectCashFlow::getBookId, bookId)
                    .eq(StandardSubjectCashFlow::getSubjectCode, subjectCode)
                    .in(StandardSubjectCashFlow::getItemCode, codesToDelete));
        }

        // 新增或更新
        boolean result = cashFlowToAddOrUpdate.isEmpty() || super.saveBatch(cashFlowToAddOrUpdate);

        return result ? Message.ok("操作成功") : Message.failed("操作失败");
    }
    public Message<StandardSubjectCashFlowVo> fetchRelationships(StandardSubjectCashFlowDto dto) {
        var standardSubjectCashFlowVo = standardSubjectCashFlowMapper.fetchRelationships(dto);

        return Message.ok(standardSubjectCashFlowVo);
    }

    /**
     * 操作类型枚举
     */
    private enum OperationType {
        ADD, DELETE, UPDATE, NONE
    }

    /**
     * 确定操作类型
     */
    private OperationType determineOperation(String inputCode, String returnedCode) {
        if (inputCode != null && returnedCode == null) {
            return OperationType.ADD;
        } else if (Objects.equals(inputCode, returnedCode)) {
            return OperationType.NONE;
        } else if (inputCode == null) {
            return OperationType.DELETE;
        } else {
            return OperationType.UPDATE;
        }
    }

    /**
     * 处理项目代码操作
     */
    private void processItemCode(
            String inputCode,
            String returnedCode,
            String bookId,
            String subjectCode,
            String direction,
            List<StandardSubjectCashFlow> cashFlowToAddOrUpdate,
            List<String> codesToDelete) {

        var operation = determineOperation(inputCode, returnedCode);

        switch (operation) {
            case ADD -> {
                if (StringUtils.isNotBlank(inputCode)) {
                    var cashFlow = new StandardSubjectCashFlow();
                    cashFlow.setBookId(bookId);
                    cashFlow.setSubjectCode(subjectCode);
                    cashFlow.setItemCode(inputCode);
                    cashFlow.setDirection(direction);
                    cashFlowToAddOrUpdate.add(cashFlow);
                }
            }
            case UPDATE -> {
                // UPDATE 同时需要删除旧值和添加新值
                if (StringUtils.isNotBlank(returnedCode)) {
                    codesToDelete.add(returnedCode);
                }
                if (StringUtils.isNotBlank(inputCode)) {
                    var cashFlow = new StandardSubjectCashFlow();
                    cashFlow.setBookId(bookId);
                    cashFlow.setSubjectCode(subjectCode);
                    cashFlow.setItemCode(inputCode);
                    cashFlow.setDirection(direction);
                    cashFlowToAddOrUpdate.add(cashFlow);
                }
            }
            case DELETE -> {
                if (StringUtils.isNotBlank(returnedCode)) {
                    codesToDelete.add(returnedCode);
                }
            }
            case NONE -> {
                // 无操作
            }
        }
    }
    public boolean saveTemplateRelationships(String bookId) {
        LambdaQueryWrapper<StandardSubjectCashFlow> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(StandardSubjectCashFlow::getIsTemplate, 1);
        wrapper.isNull(StandardSubjectCashFlow::getBookId);
        List<StandardSubjectCashFlow> list = super.list(wrapper);
        list.forEach(item -> {
            item.setBookId(bookId);
            item.setId(null);
            item.setIsTemplate(0);
            item.setCreatedBy(null);
            item.setCreatedDate(null);
            item.setModifiedBy(null);
            item.setModifiedDate(null);
        });
        return super.saveBatch(list);
    }
    public boolean deleteByBookIds(List<String> bookIds) {
        LambdaQueryWrapper<StandardSubjectCashFlow> wrapper = new LambdaQueryWrapper<>();
        wrapper.in(StandardSubjectCashFlow::getBookId, bookIds);
        this.getBaseMapper().delete(wrapper);
        return true;
    }
}
