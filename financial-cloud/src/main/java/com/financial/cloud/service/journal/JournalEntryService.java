package com.financial.cloud.service.journal;


import lombok.RequiredArgsConstructor;
import cn.hutool.core.bean.BeanUtil;
import cn.hutool.core.bean.copier.CopyOptions;
import lombok.extern.slf4j.Slf4j;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.core.toolkit.StringUtils;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.financial.cloud.common.Message;
import com.financial.cloud.domain.book.Book;
import com.financial.cloud.domain.book.BookSubject;
import com.financial.cloud.dto.common.ListIdsDto;
import com.financial.cloud.domain.journal.JournalAccount;
import com.financial.cloud.domain.journal.JournalEntry;
import com.financial.cloud.dto.journal.JournalEntryDto;
import com.financial.cloud.dto.journal.JournalEntryPageDto;
import com.financial.cloud.domain.voucher.VoucherItem;
import com.financial.cloud.dto.voucher.GenerateVoucherDto;
import com.financial.cloud.dto.voucher.VoucherChangeDto;
import com.financial.cloud.dto.voucher.VoucherItemChangeDto;
import com.financial.cloud.enums.voucher.VoucherStatusEnum;
import com.financial.cloud.enums.error.JournalErrorCode;
import com.financial.cloud.exception.BusinessException;
import com.financial.cloud.repository.book.BookMapper;
import com.financial.cloud.repository.journal.JournalEntryMapper;
import com.financial.cloud.service.book.BookSubjectService;
import com.financial.cloud.service.book.SettlementService;
import com.financial.cloud.service.voucher.VoucherService;
import com.financial.cloud.util.DateUtils;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.*;
import java.util.stream.Collectors;

@RequiredArgsConstructor
@Slf4j
@Service
public class JournalEntryService extends ServiceImpl<JournalEntryMapper, JournalEntry>{

	private final JournalAccountService journalAccountService;
	
	private final BookMapper bookMapper;

	private final BookSubjectService bookSubjectService;
	
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
        String period = DateUtils.format(journalEntry.getTradeDate(),DateUtils.FORMAT_DATE_YYYY_MM);
        Message<String> check = settlementService.check(dto.getBookId(), period);
        if(check.getCode() != 0) {
        	return check;
        }
        
        JournalAccount  journalAccount  = journalAccountService.getById(journalEntry.getAccId());
        if (journalAccount == null) {
        	throw new BusinessException(JournalErrorCode.ACCOUNT_NOT_FOUND);
        }
        String direction = journalEntry.getDirection();
        if (direction == null || !(direction.equalsIgnoreCase("i")
        		|| direction.equalsIgnoreCase("o") || direction.equalsIgnoreCase("e"))) {
        	throw new BusinessException(JournalErrorCode.DIRECTION_INVALID);
        }

        if(direction.equalsIgnoreCase("i")
        		|| direction.equalsIgnoreCase("o")) {
        	journalEntry.setExpenditure(null);
        	if(direction.equalsIgnoreCase("o")
        			&& nullToZero(journalAccount.getOpeningBalance()).compareTo(BigDecimal.ZERO) == 0) {
        		journalAccount.setOpeningBalance(journalEntry.getIncome());
        		journalAccountService.updateById(journalAccount);
        	}
        	journalAccountService.income(journalEntry.getAccId(), journalEntry.getIncome());

        }else {
        	journalEntry.setIncome(null);
        	if(journalAccount.getBalance().subtract(journalEntry.getExpenditure()).doubleValue() < 0 ) {
        		throw new BusinessException(JournalErrorCode.INSUFFICIENT_BALANCE);
        	}
        	journalAccountService.expenditure(journalEntry.getAccId(), journalEntry.getExpenditure());
        }

        JournalAccount journalAccountBalance  = journalAccountService.getById(journalEntry.getAccId());
        journalEntry.setBalance(journalAccountBalance.getBalance());
        boolean saveResult = super.save(journalEntry);

        return saveResult
        		? new Message<>(Message.SUCCESS, "新增成功", journalEntry.getId())
        		: new Message<>(Message.FAIL, "新增失败");
    }

    @Transactional
    public Message<String> update(JournalEntryDto dto) {
        String id = dto.getId();
        JournalEntry oldEntry = super.getById(id);
        if (oldEntry == null) {
            return new Message<>(Message.FAIL, "记录不存在");
        }

        String oldPeriod = DateUtils.format(oldEntry.getTradeDate(), DateUtils.FORMAT_DATE_YYYY_MM);
        Message<String> oldCheck = settlementService.check(oldEntry.getBookId(), oldPeriod);
        if (oldCheck.getCode() != 0) {
            return oldCheck;
        }

        Date newTradeDate = dto.getTradeDate() != null ? dto.getTradeDate() : oldEntry.getTradeDate();
        String newPeriod = DateUtils.format(newTradeDate, DateUtils.FORMAT_DATE_YYYY_MM);
        if (!Objects.equals(oldPeriod, newPeriod)) {
            Message<String> newCheck = settlementService.check(dto.getBookId() != null ? dto.getBookId() : oldEntry.getBookId(), newPeriod);
            if (newCheck.getCode() != 0) {
                return newCheck;
            }
        }

        boolean moneyFieldChange = moneyFieldsChanged(oldEntry, dto);
        if (moneyFieldChange && StringUtils.isNotBlank(oldEntry.getVoucherId())) {
            throw new BusinessException(JournalErrorCode.ENTRY_LINKED_LOCKED);
        }

        boolean balanceAffecting = balanceAffectingChange(oldEntry, dto, newTradeDate);

        String linkedVoucherId = oldEntry.getVoucherId();
        JournalEntry journalEntry = new JournalEntry();
        BeanUtil.copyProperties(oldEntry, journalEntry);
        BeanUtil.copyProperties(dto, journalEntry, CopyOptions.create()
                .setIgnoreNullValue(true)
                .setIgnoreProperties("voucherId", "balance", "id", "bookId", "createdBy", "createdDate"));
        journalEntry.setId(oldEntry.getId());
        journalEntry.setBookId(oldEntry.getBookId());
        journalEntry.setVoucherId(linkedVoucherId);
        journalEntry.setTradeDate(newTradeDate);
        normalizeDirectionAmounts(journalEntry);

        boolean result = super.updateById(journalEntry);
        if (!result) {
            return new Message<>(Message.FAIL, "修改失败");
        }

        if (balanceAffecting) {
            Set<String> accountIds = new LinkedHashSet<>();
            accountIds.add(oldEntry.getAccId());
            if (StringUtils.isNotBlank(journalEntry.getAccId())) {
                accountIds.add(journalEntry.getAccId());
            }
            for (String accId : accountIds) {
                recalculateAccountBalances(accId);
            }
        }

        return new Message<>(Message.SUCCESS, "修改成功");
    }

    @Transactional
    public Message<String> delete(ListIdsDto dto) {
        List<String> listIds = dto.getListIds();
        List<String> removeableListIds = new ArrayList<>();
        List<String> voucherIdsToDelete = new ArrayList<>();
        int skippedClosed = 0;
        Set<String> affectedAccounts = new LinkedHashSet<>();
        String bookId = null;
        for (String id : listIds) {
	        JournalEntry journalEntry = super.getById(id);
	        if (journalEntry == null) {
	            continue;
	        }
	        if (bookId == null) {
	        	bookId = journalEntry.getBookId();
	        }
	        String period = DateUtils.format(journalEntry.getTradeDate(), DateUtils.FORMAT_DATE_YYYY_MM);
	        Message<String> check = settlementService.check(journalEntry.getBookId(), period);
	        if (check.getCode() != 0) {
	            skippedClosed++;
	            continue;
	        }
	        if (StringUtils.isNotBlank(journalEntry.getVoucherId())) {
	        	var voucher = voucherService.getById(journalEntry.getVoucherId());
	        	if (voucher != null && StringUtils.isNotBlank(voucher.getSenderId())) {
	        		return Message.failed("流水已关联已过账凭证，请先反过账后再删除");
	        	}
	        	if (voucher != null) {
	        		voucherIdsToDelete.add(journalEntry.getVoucherId());
	        	}
	        }
	        removeableListIds.add(id);
	        affectedAccounts.add(journalEntry.getAccId());
        }
        if (removeableListIds.isEmpty()) {
            return new Message<>(Message.FAIL,
                    skippedClosed > 0 ? "没有可删除的记录（所选流水均在已结账期间）" : "删除失败");
        }
        if (!voucherIdsToDelete.isEmpty()) {
        	Message<String> voucherDelete = voucherService.delete(
        			voucherIdsToDelete.stream().distinct().toList(), bookId);
        	if (voucherDelete.getCode() != Message.SUCCESS) {
        		return voucherDelete;
        	}
        }
        boolean result = super.removeBatchByIds(removeableListIds);
        if (!result) {
            return new Message<>(Message.FAIL, "删除失败");
        }
        for (String accId : affectedAccounts) {
            recalculateAccountBalances(accId);
        }
        if (skippedClosed > 0) {
            Message<String> partial = new Message<>(Message.SUCCESS);
            partial.setMessage("成功删除 " + removeableListIds.size() + " 条，跳过 " + skippedClosed + " 条已结账期间记录");
            return partial;
        }
        return new Message<>(Message.SUCCESS, "删除成功");
    }

	@Transactional
	public Message<String> generateVoucher(GenerateVoucherDto dto) {
		String bookId = dto.getBookId();
		Book book = bookMapper.selectById(bookId);
		JournalEntry journalEntry = super.getById(dto.getId());
		if (journalEntry == null) {
			return Message.failed("流水不存在");
		}
		if (StringUtils.isNotBlank(journalEntry.getVoucherId())) {
			throw new BusinessException(JournalErrorCode.VOUCHER_ALREADY_LINKED);
		}
		if ("o".equalsIgnoreCase(journalEntry.getDirection())) {
			throw new BusinessException(JournalErrorCode.OPENING_CANNOT_GENERATE);
		}

		Date tradeDate = journalEntry.getTradeDate() != null ? journalEntry.getTradeDate() : new Date();
		String period = DateUtils.format(tradeDate, DateUtils.FORMAT_DATE_YYYY_MM);
		Message<String> check = settlementService.check(bookId, period);
		if (check.getCode() != 0) {
			return check;
		}

		JournalAccount journalAccount = journalAccountService.getById(journalEntry.getAccId());
		if (journalAccount == null
				|| StringUtils.isBlank(journalAccount.getSubjectId())
				|| StringUtils.isBlank(journalEntry.getSubjectId())) {
			throw new BusinessException(JournalErrorCode.SUBJECT_REQUIRED);
		}
		if (Objects.equals(journalAccount.getSubjectId(), journalEntry.getSubjectId())) {
			throw new BusinessException(JournalErrorCode.SUBJECT_SAME_AS_FUND);
		}

		BookSubject fundSubject = bookSubjectService.getById(journalAccount.getSubjectId());
		BookSubject counterpartSubject = bookSubjectService.getById(journalEntry.getSubjectId());
		if (fundSubject == null || counterpartSubject == null) {
			throw new BusinessException(JournalErrorCode.SUBJECT_NOT_FOUND);
		}

		BigDecimal amount;
		boolean income = "i".equalsIgnoreCase(journalEntry.getDirection());
		if (income) {
			amount = nullToZero(journalEntry.getIncome());
		} else {
			amount = nullToZero(journalEntry.getExpenditure());
		}

		Calendar calendar = Calendar.getInstance();
		calendar.setTime(tradeDate);
		Integer year = calendar.get(Calendar.YEAR);
		Integer month = calendar.get(Calendar.MONTH) + 1;

		Integer wordNum = voucherService.getAbleWordNum(bookId, "记", null, null).getData();

		VoucherChangeDto voucherDto = new VoucherChangeDto();
		voucherDto.setWordHead("记");
		voucherDto.setWordNum(wordNum);
		voucherDto.setBookId(bookId);
		voucherDto.setCompanyName(book.getCompanyName());
		voucherDto.setVoucherDate(tradeDate);
		voucherDto.setVoucherYear(year);
		voucherDto.setVoucherMonth(month);
		voucherDto.setDebitAmount(amount);
		voucherDto.setCreditAmount(amount);
		voucherDto.setRemark(journalEntry.getRemark());
		voucherDto.setStatus(VoucherStatusEnum.DRAFT.getValue());

		List<VoucherItemChangeDto> voucherItems = new ArrayList<>();
		VoucherItemChangeDto debitItemDto = new VoucherItemChangeDto();
		debitItemDto.setSummary(journalEntry.getRemark());
		debitItemDto.setDebitAmount(amount);
		debitItemDto.setAuxiliary(List.of());
		debitItemDto.setDetailedAccounts("");

		VoucherItemChangeDto creditItemDto = new VoucherItemChangeDto();
		creditItemDto.setSummary(journalEntry.getRemark());
		creditItemDto.setCreditAmount(amount);
		creditItemDto.setAuxiliary(List.of());
		creditItemDto.setDetailedAccounts("");

		if (income) {
			fillSubject(debitItemDto, fundSubject);
			fillSubject(creditItemDto, counterpartSubject);
		} else {
			fillSubject(debitItemDto, counterpartSubject);
			fillSubject(creditItemDto, fundSubject);
		}
		voucherItems.add(debitItemDto);
		voucherItems.add(creditItemDto);
		voucherDto.setItems(voucherItems);

		Message<String> saveResult = voucherService.save(voucherDto);
		if (saveResult.getCode() != Message.SUCCESS) {
			return saveResult;
		}

		LambdaUpdateWrapper<JournalEntry> updateWrapper = new LambdaUpdateWrapper<>();
		updateWrapper.set(JournalEntry::getVoucherId, voucherDto.getId());
		updateWrapper.eq(JournalEntry::getId, dto.getId());
		super.update(updateWrapper);

		return Message.ok(voucherDto.getId());
	}

	/**
	 * 凭证删除/作废后解绑流水，保留出纳记录以便再生成。
	 */
	@Transactional
	public void clearLinksByVoucherIds(Collection<String> voucherIds) {
		if (voucherIds == null || voucherIds.isEmpty()) {
			return;
		}
		List<String> ids = voucherIds.stream()
				.filter(StringUtils::isNotBlank)
				.distinct()
				.collect(Collectors.toList());
		if (ids.isEmpty()) {
			return;
		}
		LambdaUpdateWrapper<JournalEntry> uw = new LambdaUpdateWrapper<>();
		uw.set(JournalEntry::getVoucherId, null);
		uw.in(JournalEntry::getVoucherId, ids);
		super.update(uw);
	}

	/**
	 * 未过账关联凭证修改后，按资金科目分录回写流水并重算余额。
	 */
	@Transactional
	public void syncLinkedEntriesFromVoucher(String voucherId, String bookId,
											 Date voucherDate, String remark,
											 List<VoucherItem> items) {
		if (StringUtils.isBlank(voucherId) || StringUtils.isBlank(bookId)) {
			return;
		}
		List<JournalEntry> linked = list(new LambdaQueryWrapper<JournalEntry>()
				.eq(JournalEntry::getVoucherId, voucherId)
				.eq(JournalEntry::getBookId, bookId));
		if (linked.isEmpty()) {
			return;
		}
		if (items == null || items.isEmpty()) {
			throw new BusinessException(JournalErrorCode.VOUCHER_SYNC_STRUCTURE);
		}
		Set<String> affectedAccounts = new LinkedHashSet<>();
		for (JournalEntry entry : linked) {
			JournalAccount account = journalAccountService.getById(entry.getAccId());
			if (account == null || StringUtils.isBlank(account.getSubjectId())) {
				throw new BusinessException(JournalErrorCode.VOUCHER_SYNC_STRUCTURE);
			}
			String fundSubjectId = account.getSubjectId();
			VoucherItem fundLine = null;
			VoucherItem counterpartLine = null;
			for (VoucherItem item : items) {
				if (item == null || StringUtils.isBlank(item.getSubjectId())) {
					continue;
				}
				if (fundSubjectId.equals(item.getSubjectId())) {
					fundLine = item;
				} else if (counterpartLine == null) {
					counterpartLine = item;
				}
			}
			if (fundLine == null) {
				throw new BusinessException(JournalErrorCode.VOUCHER_SYNC_STRUCTURE);
			}
			BigDecimal debit = nullToZero(fundLine.getDebitAmount());
			BigDecimal credit = nullToZero(fundLine.getCreditAmount());
			boolean incomeSide = debit.compareTo(BigDecimal.ZERO) > 0;
			BigDecimal amount = incomeSide ? debit : credit;
			if (amount.compareTo(BigDecimal.ZERO) <= 0) {
				throw new BusinessException(JournalErrorCode.VOUCHER_SYNC_STRUCTURE);
			}

			entry.setTradeDate(voucherDate != null ? voucherDate : entry.getTradeDate());
			if (remark != null) {
				entry.setRemark(remark);
			}
			if (counterpartLine != null && StringUtils.isNotBlank(counterpartLine.getSubjectId())) {
				entry.setSubjectId(counterpartLine.getSubjectId());
			}
			if (incomeSide) {
				entry.setDirection("i");
				entry.setIncome(amount);
				entry.setExpenditure(null);
			} else {
				entry.setDirection("e");
				entry.setIncome(null);
				entry.setExpenditure(amount);
			}
			super.updateById(entry);
			affectedAccounts.add(entry.getAccId());
		}
		for (String accId : affectedAccounts) {
			recalculateAccountBalances(accId);
		}
	}

	void recalculateAccountBalances(String accId) {
		if (StringUtils.isBlank(accId)) {
			return;
		}
		List<JournalEntry> entries = list(new LambdaQueryWrapper<JournalEntry>()
				.eq(JournalEntry::getAccId, accId)
				.orderByAsc(JournalEntry::getTradeDate)
				.orderByAsc(JournalEntry::getId));
		BigDecimal running = BigDecimal.ZERO;
		for (JournalEntry entry : entries) {
			String direction = entry.getDirection();
			if (direction != null && (direction.equalsIgnoreCase("i") || direction.equalsIgnoreCase("o"))) {
				running = running.add(nullToZero(entry.getIncome()));
			} else if (direction != null && direction.equalsIgnoreCase("e")) {
				running = running.subtract(nullToZero(entry.getExpenditure()));
			}
			if (running.compareTo(BigDecimal.ZERO) < 0) {
				throw new BusinessException(JournalErrorCode.INSUFFICIENT_BALANCE);
			}
			if (entry.getBalance() == null || entry.getBalance().compareTo(running) != 0) {
				entry.setBalance(running);
				super.updateById(entry);
			} else {
				entry.setBalance(running);
			}
		}
		journalAccountService.setBalance(accId, running);
	}

	private static boolean moneyFieldsChanged(JournalEntry oldEntry, JournalEntryDto dto) {
		if (dto.getAccId() != null && !Objects.equals(oldEntry.getAccId(), dto.getAccId())) {
			return true;
		}
		if (dto.getDirection() != null && !equalsIgnoreCase(oldEntry.getDirection(), dto.getDirection())) {
			return true;
		}
		if (dto.getSubjectId() != null && !Objects.equals(oldEntry.getSubjectId(), dto.getSubjectId())) {
			return true;
		}
		if (dto.getIncome() != null && nullToZero(oldEntry.getIncome()).compareTo(nullToZero(dto.getIncome())) != 0) {
			return true;
		}
		if (dto.getExpenditure() != null
				&& nullToZero(oldEntry.getExpenditure()).compareTo(nullToZero(dto.getExpenditure())) != 0) {
			return true;
		}
		return false;
	}

	private static boolean balanceAffectingChange(JournalEntry oldEntry, JournalEntryDto dto, Date newTradeDate) {
		if (dto.getAccId() != null && !Objects.equals(oldEntry.getAccId(), dto.getAccId())) {
			return true;
		}
		if (dto.getDirection() != null && !equalsIgnoreCase(oldEntry.getDirection(), dto.getDirection())) {
			return true;
		}
		if (dto.getIncome() != null && nullToZero(oldEntry.getIncome()).compareTo(nullToZero(dto.getIncome())) != 0) {
			return true;
		}
		if (dto.getExpenditure() != null
				&& nullToZero(oldEntry.getExpenditure()).compareTo(nullToZero(dto.getExpenditure())) != 0) {
			return true;
		}
		return !sameTradeInstant(oldEntry.getTradeDate(), newTradeDate);
	}

	private static void normalizeDirectionAmounts(JournalEntry entry) {
		if (entry.getDirection() == null) {
			return;
		}
		if (entry.getDirection().equalsIgnoreCase("i") || entry.getDirection().equalsIgnoreCase("o")) {
			entry.setExpenditure(null);
		} else if (entry.getDirection().equalsIgnoreCase("e")) {
			entry.setIncome(null);
		}
	}

	private static void fillSubject(VoucherItemChangeDto item, BookSubject subject) {
		item.setSubjectId(subject.getId());
		item.setSubjectCode(subject.getCode());
		item.setSubjectName(subject.getCode() + "-" + subject.getName());
	}

	private static BigDecimal nullToZero(BigDecimal value) {
		return value == null ? BigDecimal.ZERO : value;
	}

	private static boolean equalsIgnoreCase(String a, String b) {
		if (a == null || b == null) {
			return Objects.equals(a, b);
		}
		return a.equalsIgnoreCase(b);
	}

	private static boolean sameTradeInstant(Date a, Date b) {
		if (a == null && b == null) {
			return true;
		}
		if (a == null || b == null) {
			return false;
		}
		return a.getTime() == b.getTime();
	}
}
