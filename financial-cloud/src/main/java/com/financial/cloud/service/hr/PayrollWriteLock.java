package com.financial.cloud.service.hr;

import com.baomidou.mybatisplus.core.toolkit.StringUtils;
import com.financial.cloud.common.Message;
import com.financial.cloud.exception.BusinessException;
import com.financial.cloud.repository.book.BookMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.support.TransactionSynchronizationManager;

/** Shared transaction lock for the confirmed payroll write entry points. */
@RequiredArgsConstructor
@Service
public class PayrollWriteLock {
    private final BookMapper bookMapper;

    public void lockBook(String bookId) {
        if (StringUtils.isBlank(bookId)) {
            throw new BusinessException(Message.FAIL, "缺少工资账套范围");
        }
        if (!TransactionSynchronizationManager.isActualTransactionActive()) {
            throw new BusinessException(Message.FAIL, "工资写入需要有效事务");
        }
        if (!bookId.equals(bookMapper.lockActiveBookId(bookId))) {
            throw new BusinessException(Message.FAIL, "工资账套不存在或已删除");
        }
    }
}
