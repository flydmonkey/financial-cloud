package com.jinbooks.service.journal;

import cn.hutool.core.bean.BeanUtil;
import lombok.extern.slf4j.Slf4j;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.jinbooks.common.Message;
import com.jinbooks.dto.common.ListIdsDto;
import com.jinbooks.domain.journal.JournalAccount;
import com.jinbooks.dto.journal.JournalAccountDto;
import com.jinbooks.dto.journal.JournalAccountPageDto;
import com.jinbooks.repository.journal.JournalAccountMapper;
import com.jinbooks.service.journal.JournalAccountService;

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
	 * 结账：本期期初余额=余额
	 */
	public int checkout(String bookId) {
		return this.getBaseMapper().checkout(bookId);
	}
}
