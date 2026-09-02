package com.financial.cloud.service.journal;

import cn.hutool.core.bean.BeanUtil;
import lombok.extern.slf4j.Slf4j;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.financial.cloud.common.Message;
import com.financial.cloud.dto.common.ListIdsDto;
import com.financial.cloud.domain.journal.JournalAccount;
import com.financial.cloud.dto.journal.JournalAccountDto;
import com.financial.cloud.dto.journal.JournalAccountPageDto;
import com.financial.cloud.repository.journal.JournalAccountMapper;
import com.financial.cloud.service.journal.JournalAccountService;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.*;

@Slf4j
@Service
public class JournalAccountService extends ServiceImpl<JournalAccountMapper, JournalAccount>{
    public Message<Page<JournalAccount>> pageList(JournalAccountPageDto dto) {
        Page<JournalAccount> page = this.getBaseMapper().pageList(dto.build(), dto);
        return new Message<>(Message.SUCCESS, page);
    }
    public Message<List <JournalAccount> > findAll(String bookId) {
    	LambdaQueryWrapper<JournalAccount> wrapper = new LambdaQueryWrapper<>();
    	wrapper.eq(JournalAccount::getBookId, bookId);
        return new Message<>(Message.SUCCESS, this.getBaseMapper().selectList(wrapper));
    }
    @Transactional
    public Message<String> save(JournalAccountDto dto) {
        JournalAccount account = new JournalAccount();
        BeanUtil.copyProperties(dto, account);
        //通过新增记录初始化
        account.setOpeningBalance(null);
        boolean saveResult = super.save(account);
        return saveResult ? new Message<>(Message.SUCCESS, "新增成功") : new Message<>(Message.FAIL, "新增失败");
    }
    @Transactional
    public Message<String> update(JournalAccountDto dto) {
        String id = dto.getId();
        JournalAccount account = super.getById(id);
        BeanUtil.copyProperties(dto, account);
        //通过新增记录初始化
        account.setOpeningBalance(null);
        boolean result = super.updateById(account);
        return result ? new Message<>(Message.SUCCESS, "修改成功") : new Message<>(Message.FAIL, "修改失败");
    }
    @Transactional
    public Message<String> delete(ListIdsDto dto) {
        List<String> listIds = dto.getListIds();
        //删除
        boolean result = super.removeByIds(listIds);

        return result ? new Message<>(Message.SUCCESS, "删除成功") : new Message<>(Message.FAIL, "删除失败");
    }
	public int income(String accId, BigDecimal income) {
		return this.getBaseMapper().income(accId, income);
	}
	public int expenditure(String accId, BigDecimal expenditure) {
		return this.getBaseMapper().expenditure(accId, expenditure);
	}

	/**
	 * 结账：备份 opening 后 opening = balance
	 */
	public int checkout(String bookId) {
		return this.getBaseMapper().checkout(bookId);
	}

	/**
	 * 反结账：opening := prev_opening_balance
	 */
	public int restoreOpeningFromPrev(String bookId) {
		return this.getBaseMapper().restoreOpeningFromPrev(bookId);
	}

	/**
	 * 账套是否存在日记账账户，且任一账户缺少结账快照（迁移前已结账无法安全反结）。
	 */
	public boolean hasAccountsMissingPrevOpening(String bookId) {
		LambdaQueryWrapper<JournalAccount> wrapper = new LambdaQueryWrapper<>();
		wrapper.eq(JournalAccount::getBookId, bookId);
		List<JournalAccount> accounts = this.getBaseMapper().selectList(wrapper);
		if (accounts == null || accounts.isEmpty()) {
			return false;
		}
		return accounts.stream().anyMatch(a -> a.getPrevOpeningBalance() == null);
	}
}
