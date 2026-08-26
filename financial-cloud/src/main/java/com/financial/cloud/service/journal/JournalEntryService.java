package com.financial.cloud.service.journal;


import lombok.RequiredArgsConstructor;
import cn.hutool.core.bean.BeanUtil;
import lombok.extern.slf4j.Slf4j;

import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.financial.cloud.common.Message;
import com.financial.cloud.domain.book.Book;
import com.financial.cloud.dto.common.ListIdsDto;
import com.financial.cloud.domain.journal.JournalAccount;
import com.financial.cloud.domain.journal.JournalEntry;
import com.financial.cloud.dto.journal.JournalEntryDto;
import com.financial.cloud.dto.journal.JournalEntryPageDto;
import com.financial.cloud.dto.voucher.GenerateVoucherDto;
import com.financial.cloud.dto.voucher.VoucherChangeDto;
import com.financial.cloud.dto.voucher.VoucherItemChangeDto;
import com.financial.cloud.enums.voucher.VoucherStatusEnum;
import com.financial.cloud.enums.error.JournalErrorCode;
import com.financial.cloud.exception.BusinessException;
import com.financial.cloud.repository.book.BookMapper;
import com.financial.cloud.repository.journal.JournalEntryMapper;
import com.financial.cloud.service.journal.JournalAccountService;
import com.financial.cloud.service.journal.JournalEntryService;
import com.financial.cloud.service.book.SettlementService;
import com.financial.cloud.service.voucher.VoucherService;
import com.financial.cloud.util.DateUtils;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.*;

@RequiredArgsConstructor
@Slf4j
@Service
public class JournalEntryService extends ServiceImpl<JournalEntryMapper, JournalEntry>{

	private final JournalAccountService journalAccountService;
	
	private final BookMapper bookMapper;
	
	private final VoucherService voucherService;
	
	private final SettlementService settlementService;
    public Message<Page<JournalEntry>> pageList(JournalEntryPageDto dto) {
        Page<JournalEntry> page = this.getBaseMapper().pageList(dto.build(), dto);

        return new Message<>(Message.SUCCESS, page);
    }
    @Transactional
    public Message<String> save(JournalEntryDto dto) {

    	JournalEntry journalEntry = new JournalEntry();
        BeanUtil.copyProperties(dto, journalEntry);
        if(dto.getTradeDate() == null) {
        	journalEntry.setTradeDate(new Date());
        }
        //判断账期是否结账
        String period = DateUtils.format(journalEntry.getTradeDate(),DateUtils.FORMAT_DATE_YYYY_MM);
        Message<String> check = settlementService.check(dto.getBookId(), period);
        if(check.getCode() != 0) {
        	return check;
        }
        
        JournalAccount  journalAccount  = journalAccountService.getById(journalEntry.getAccId());
        
        if(journalEntry.getDirection().equalsIgnoreCase("i") 
        		|| journalEntry.getDirection().equalsIgnoreCase("o")) {
        	//i收入和o期初
        	journalEntry.setExpenditure(null);
        	if(journalEntry.getDirection().equalsIgnoreCase("o")&&journalAccount.getOpeningBalance().equals(BigDecimal.ZERO)) {
        		journalAccount.setOpeningBalance(journalEntry.getIncome());
        	}
        	journalAccountService.income(journalEntry.getAccId(), journalEntry.getIncome());
        	
        }else if(journalEntry.getDirection().equalsIgnoreCase("e")){
        	journalEntry.setIncome(null);
        	//支出
        	if(journalAccount.getBalance().subtract(journalEntry.getExpenditure()).doubleValue() < 0 ) {
        		throw new BusinessException(JournalErrorCode.INSUFFICIENT_BALANCE);
        	}
        	journalAccountService.expenditure(journalEntry.getAccId(), journalEntry.getExpenditure());
        }
        
        if(journalAccount != null) {
        	JournalAccount journalAccountBalance  = journalAccountService.getById(journalEntry.getAccId());
        	//设置余额
        	journalEntry.setBalance(journalAccountBalance.getBalance());
        }
        boolean saveResult = super.save(journalEntry);

        return saveResult ? new Message<>(Message.SUCCESS, "新增成功") : new Message<>(Message.FAIL, "新增失败");
    }
    @Transactional
    public Message<String> update(JournalEntryDto dto) {
        String id = dto.getId();
        JournalEntry journalEntry = super.getById(id);
        //判断账期是否结账
        String period = DateUtils.format(journalEntry.getTradeDate(),DateUtils.FORMAT_DATE_YYYY_MM);
        Message<String> check = settlementService.check(dto.getBookId(), period);
        if(check.getCode() != 0) {
        	return check;
        }
        
        BeanUtil.copyProperties(dto, journalEntry);
        boolean result = super.updateById(journalEntry);
        return result ? new Message<>(Message.SUCCESS, "修改成功") : new Message<>(Message.FAIL, "修改失败");
    }
    @Transactional
    public Message<String> delete(ListIdsDto dto) {
        List<String> listIds = dto.getListIds();
        List<String> removeableListIds = new ArrayList<>();
        boolean result = false;
        for(String id : listIds) {
	        JournalEntry journalEntry = super.getById(id);
	        //判断账期是否结账
	        String period = DateUtils.format(journalEntry.getTradeDate(),DateUtils.FORMAT_DATE_YYYY_MM);
	        Message<String> check = settlementService.check(journalEntry.getBookId(), period);
	        if(check.getCode() == 0) {
	        	removeableListIds.add(id);
		        if(journalEntry.getDirection().equalsIgnoreCase("i")
		        		||journalEntry.getDirection().equalsIgnoreCase("o")) {
		        	//支出退回
		        	journalAccountService.expenditure(journalEntry.getAccId(), journalEntry.getIncome());
		        }else {
		        	//收入减去
		        	journalAccountService.income(journalEntry.getAccId(), journalEntry.getExpenditure());
		        }
	        }
        }
        result = super.removeBatchByIds(removeableListIds);

        return result ? new Message<>(Message.SUCCESS, "删除成功") : new Message<>(Message.FAIL, "删除失败");
    }
	public Message<String> generateVoucher(GenerateVoucherDto dto) {
		String bookId = dto.getBookId();
		Book book = bookMapper.selectById(bookId);
		JournalEntry journalEntry = super.getById(dto.getId());
		
	     // 格式化日期
        Date formattedDate = DateUtils.getCurrentDate();
        
        //判断账期是否结账
        String period = DateUtils.format(formattedDate,DateUtils.FORMAT_DATE_YYYY_MM);
        Message<String> check = settlementService.check(bookId, period);
        if(check.getCode() != 0) {
        	return check;
        }
        
        BigDecimal amount = null;
		if(journalEntry.getDirection().equalsIgnoreCase("i")) {
			amount = journalEntry.getIncome();
		}else {
			amount = journalEntry.getExpenditure();
		}
		
        Calendar calendar = Calendar.getInstance();
        calendar.setTime(formattedDate);
        Integer year = calendar.get(Calendar.YEAR);
        Integer month = calendar.get(Calendar.MONTH) + 1;

        // 封装凭证入参数据
        Integer wordNum = voucherService.getAbleWordNum(bookId,"记", null, null).getData();

        VoucherChangeDto voucherDto = new VoucherChangeDto();
        voucherDto.setWordHead("记");
        voucherDto.setWordNum(wordNum);
        voucherDto.setBookId(bookId);
        voucherDto.setCompanyName(book.getCompanyName());
        voucherDto.setVoucherDate(formattedDate);
        voucherDto.setVoucherYear(year);
        voucherDto.setVoucherMonth(month);
        voucherDto.setDebitAmount(amount);
        voucherDto.setCreditAmount(amount);
        
	     // 创建凭证项
        List<VoucherItemChangeDto> voucherItems = new ArrayList<>();
        //
        VoucherItemChangeDto debitItemDto = new VoucherItemChangeDto();
        debitItemDto.setSummary(journalEntry.getRemark());
        debitItemDto.setSubjectId(bookId);
        debitItemDto.setSubjectName(bookId);
        debitItemDto.setSubjectCode(bookId);
        debitItemDto.setDebitAmount(amount);
        debitItemDto.setAuxiliary(List.of());
        debitItemDto.setDetailedAccounts("");
        voucherItems.add(debitItemDto);
        //
        VoucherItemChangeDto creditItemDto = new VoucherItemChangeDto();
        creditItemDto.setSummary(journalEntry.getRemark());
        creditItemDto.setCreditAmount(amount);
        creditItemDto.setSubjectId(bookId);
        creditItemDto.setSubjectName(bookId);
        creditItemDto.setSubjectCode(bookId);
        creditItemDto.setAuxiliary(List.of());
        creditItemDto.setDetailedAccounts("");
        voucherItems.add(creditItemDto);
        
        voucherDto.setRemark(journalEntry.getRemark());
        // 暂存凭证
        voucherDto.setItems(voucherItems);
        //设置状态为暂存
        voucherDto.setStatus(VoucherStatusEnum.DRAFT.getValue());
        voucherService.save(voucherDto);
        
    	LambdaUpdateWrapper<JournalEntry> updateWrapper = new LambdaUpdateWrapper<>();
    	updateWrapper.set(JournalEntry::getVoucherId, voucherDto.getId());
    	updateWrapper.eq(JournalEntry::getId, dto.getId());
    	super.update(updateWrapper);
    	
		return Message.ok(voucherDto.getId());
	}
}
