package com.financial.cloud.service.book;


import lombok.RequiredArgsConstructor;
import com.financial.cloud.service.config.ConfigSysService;
import com.financial.cloud.service.statement.StatementSubjectBalanceService;
import com.financial.cloud.service.voucher.VoucherService;
import com.financial.cloud.service.voucher.VoucherTemplateService;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.incrementer.IdentifierGenerator;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.financial.cloud.common.Message;
import com.financial.cloud.domain.book.Book;
import com.financial.cloud.domain.book.BookSubject;
import com.financial.cloud.domain.book.Settlement;
import com.financial.cloud.domain.book.SettlementCarryforward;
import com.financial.cloud.dto.book.SettlementCarryforwardVo;
import com.financial.cloud.domain.hr.EmployeeSalarySummary;
import com.financial.cloud.domain.statement.StatementSubjectBalance;
import com.financial.cloud.domain.voucher.VoucherTemplate;
import com.financial.cloud.domain.voucher.VoucherTemplateItem;
import com.financial.cloud.dto.voucher.GenerateVoucherDto;
import com.financial.cloud.util.SubjectCodeCompat;
import com.financial.cloud.dto.voucher.VoucherChangeDto;
import com.financial.cloud.dto.voucher.VoucherItemChangeDto;
import com.financial.cloud.dto.voucher.VoucherTemplatePageDto;
import com.financial.cloud.enums.voucher.VoucherStatusEnum;
import com.financial.cloud.repository.book.BookMapper;
import com.financial.cloud.repository.hr.EmployeeSalarySummaryMapper;
import com.financial.cloud.repository.book.SettlementCarryforwardMapper;
import com.financial.cloud.repository.book.SettlementMapper;
import com.financial.cloud.repository.voucher.VoucherTemplateItemMapper;
import com.financial.cloud.repository.voucher.VoucherTemplateMapper;
import com.financial.cloud.service.book.BookSubjectService;
import com.financial.cloud.service.book.SettlementCarryService;
import com.financial.cloud.util.DateUtils;

import lombok.extern.slf4j.Slf4j;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.springframework.stereotype.Service;


@RequiredArgsConstructor
@Slf4j
@Service
public class SettlementCarryService extends ServiceImpl<SettlementMapper, Settlement>{

    private final IdentifierGenerator identifierGenerator;

    private final BookMapper bookMapper;

    private final BookSubjectService bookSubjectService;

    private final ConfigSysService configSysService;

    private final VoucherService voucherService;

    private final VoucherTemplateItemMapper voucherTemplateItemMapper;

    private final VoucherTemplateMapper voucherTemplateMapper;

    private final SettlementCarryforwardMapper settlementCarryforwardMapper;

    private final StatementSubjectBalanceService statementSubjectBalanceService;
    
    private final EmployeeSalarySummaryMapper employeeSalarySummaryMapper;

    private final VoucherTemplateService voucherTemplateService;

    public Message<Page<SettlementCarryforwardVo>> fetchCarry(VoucherTemplatePageDto dto) {
        dto.setYearPeriod(configSysService.getCurrentTerm(dto.getBookId()));
        voucherTemplateService.rematchSalaryAccrualParentSubjects(dto.getBookId());
        Page<SettlementCarryforwardVo> page = settlementCarryforwardMapper.pageList(dto.build(), dto);
        return Message.ok(page);
    }
    public Message<String> generateVoucher(GenerateVoucherDto dto) {
        log.debug("GenerateVoucherDto {}", dto);
        String bookId = dto.getBookId();
        Book book = bookMapper.selectById(bookId);
        String currentTerm = configSysService.getCurrentTerm(bookId);
        voucherTemplateService.rematchSalaryAccrualParentSubjects(bookId);
        VoucherTemplate voucherTemplate = voucherTemplateMapper.selectById(dto.getTemplateId());
        log.debug("voucherTemplate {}", voucherTemplate);
        LambdaQueryWrapper<VoucherTemplateItem> itemLqw = Wrappers.lambdaQuery();
        itemLqw.eq(VoucherTemplateItem::getRelatedId, voucherTemplate.getRelatedId());
        itemLqw.eq(VoucherTemplateItem::getTemplateId, voucherTemplate.getId());
        List<VoucherTemplateItem> items = voucherTemplateItemMapper.selectList(itemLqw);
        log.debug("VoucherTemplateItems {}", items);

        BigDecimal debitAmount = BigDecimal.ZERO;
        BigDecimal creditAmount = BigDecimal.ZERO;

        Date voucherDate = null;
        if(voucherTemplate.getVoucherDate().equals(0)) {
        	voucherDate = configSysService.getCurrentTermLastDate(bookId);
        }else if(0 < voucherTemplate.getVoucherDate() && voucherTemplate.getVoucherDate()< 31 ){
        	String voucherDateString = "";
        	if(voucherTemplate.getVoucherDate() < 10) {
        		voucherDateString = currentTerm+"-0"+voucherTemplate.getVoucherDate();
        	}else {
        		voucherDateString = currentTerm+"-"+voucherTemplate.getVoucherDate();
        	}
        	voucherDate =DateUtils.parse(voucherDateString, DateUtils.FORMAT_DATE_YYYY_MM_DD);
        }

        int year = Integer.parseInt(currentTerm.split("-")[0]);
        int month = Integer.parseInt(currentTerm.split("-")[1]);

        VoucherChangeDto voucherChangeDto = createVoucherChangeDto(book, bookId, voucherTemplate.getWordHead(), voucherDate, year, month, debitAmount);
        voucherChangeDto.setRemark(voucherTemplate.getRemark().replace("{yyyy}", year + "").replace("{mm}", month + ""));

        List<VoucherItemChangeDto> voucherItems = new ArrayList<>();

        Map<String, VoucherTemplateItem> itemsMap = new HashMap<>();
        for (VoucherTemplateItem item : items) {
            itemsMap.put(item.getSubjectCode(), item);
        }

        try {
        if (voucherTemplate.getCode().startsWith("qm_jz_")) {
           
            //凭证 不转结
            voucherChangeDto.setCarryForward("y");
            String standardId = book != null ? book.getStandardId() : MonthEndCloseRules.STANDARD_SMALL_BUSINESS;
            String yearProfitCode = MonthEndCloseRules.yearProfitSubjectForStandard(standardId);

            if (voucherTemplate.getCode().equals("qm_jz_sr")) {//结转收入
                for (String root : MonthEndCloseRules.incomeCarryRootsForStandard(standardId)) {
                    addCarryVoucherItems(bookId, root, voucherItems, itemsMap, 1);
                }
                if (voucherItems.isEmpty()) {
                    return Message.failed("本期无收入余额，无需结转");
                }
                for (VoucherItemChangeDto vt : voucherItems) {
                    debitAmount = debitAmount.add(vt.getDebitAmount());
                }
                creditAmount = debitAmount;
                voucherItems.add(createCarryVoucherItemDto(bookId, itemsMap, yearProfitCode, debitAmount, 2));
            } else if (voucherTemplate.getCode().equals("qm_jz_cbfy")) {//结转成本
                for (String root : MonthEndCloseRules.costCarryRootsForStandard(standardId)) {
                    addCarryVoucherItems(bookId, root, voucherItems, itemsMap, 2);
                }
                if (voucherItems.isEmpty()) {
                    return Message.failed("本期无成本费用余额，无需结转");
                }
                for (VoucherItemChangeDto vt : voucherItems) {
                    creditAmount = creditAmount.add(vt.getCreditAmount());
                }
                debitAmount = creditAmount;
                voucherItems.add(createCarryVoucherItemDto(bookId, itemsMap, yearProfitCode, debitAmount, 1));
            } else if (voucherTemplate.getCode().equals("qm_jz_sds")) {//结转所得税
                addCarryVoucherItems(bookId, MonthEndCloseRules.incomeTaxExpenseSubjectForStandard(standardId),
                        voucherItems, itemsMap, 2);
                if (voucherItems.isEmpty()) {
                    return Message.failed("本期无所得税费用余额，无需结转");
                }
                for (VoucherItemChangeDto vt : voucherItems) {
                    creditAmount = creditAmount.add(vt.getCreditAmount());
                }
                debitAmount = creditAmount;
                voucherItems.add(createCarryVoucherItemDto(bookId, itemsMap, yearProfitCode, debitAmount, 1));
            } else if (voucherTemplate.getCode().equals("qm_jz_bnlr")) {//年末 结转本年利润
                if (month == 12) {
                    StatementSubjectBalance profitBalance = getCarrySubjectBalance(bookId, yearProfitCode);
                    if (profitBalance == null || profitBalance.getBalance() == null
                            || profitBalance.getBalance().compareTo(BigDecimal.ZERO) == 0) {
                        return Message.failed("本年利润无余额，无需结转");
                    }
                    BigDecimal amount = profitBalance.getBalance().abs();
                    String undistributed = MonthEndCloseRules.undistributedProfitSubjectForStandard(standardId);
                    voucherItems.add(createCarryVoucherItemDto(bookId, itemsMap, yearProfitCode, amount, 1));
                    voucherItems.add(createCarryVoucherItemDto(bookId, itemsMap, undistributed, amount, 2));
                } else {
                    return Message.failed("非年末，无需结转本年利润");
                }
            }
        }else if (voucherTemplate.getCode().startsWith("jt_gz")||voucherTemplate.getCode().startsWith("jt_shebao")) {
        	//计提本月
        	LambdaQueryWrapper<EmployeeSalarySummary> salaryWrapper = new LambdaQueryWrapper<>();
        	salaryWrapper.eq(EmployeeSalarySummary::getBelongDate, currentTerm);
        	salaryWrapper.eq(EmployeeSalarySummary::getBookId, bookId);
        	salaryWrapper.eq(EmployeeSalarySummary::getLabel, "salary");
        	salaryWrapper.eq(EmployeeSalarySummary::getDeleted, "n");
        	EmployeeSalarySummary summary = employeeSalarySummaryMapper.selectOne(salaryWrapper);
        	if(summary != null) {
	        	 if (voucherTemplate.getCode().startsWith("jt_gz")){
	        		 for (VoucherTemplateItem item : items) {
	                     voucherItems.add(createVoucherItemDto(bookId, item, summary.getPayAmount()));
	                 }
	        	 }else if (voucherTemplate.getCode().startsWith("jt_shebao")){
	        		 for (VoucherTemplateItem item : items) {
	                     voucherItems.add(createVoucherItemDto(bookId, item, summary.getBusinessSocialInsurance()));
	                 }
	        	 }
        	}
        }else if (voucherTemplate.getCode().startsWith("zf_shebao")||voucherTemplate.getCode().startsWith("zf_gz")) {
        	//支付上月
        	String prevTerm = configSysService.getPrevTerm(bookId);
        	LambdaQueryWrapper<EmployeeSalarySummary> salaryWrapper = new LambdaQueryWrapper<>();
        	salaryWrapper.eq(EmployeeSalarySummary::getBelongDate, prevTerm);
        	salaryWrapper.eq(EmployeeSalarySummary::getBookId, bookId);
        	salaryWrapper.eq(EmployeeSalarySummary::getLabel, "salary");
        	salaryWrapper.eq(EmployeeSalarySummary::getDeleted, "n");
        	EmployeeSalarySummary summary = employeeSalarySummaryMapper.selectOne(salaryWrapper);
        	if(summary != null) {
	        	 if (voucherTemplate.getCode().startsWith("zf_gz")){
	        		//银行存款计算
	        		 BigDecimal creditYyckAmount = BigDecimal.ZERO;
	        		 //应付职工薪酬
	        		 if(SubjectCodeCompat.mapContains(itemsMap, "221101")) {
	        			 debitAmount = summary.getPayAmount();
	        			 creditYyckAmount = debitAmount;
	        			 voucherItems.add(createVoucherItemDto(bookId, SubjectCodeCompat.resolveFromMap(itemsMap, "221101"), debitAmount));
	        		 }
	        		 //个人社保
	        		 if(SubjectCodeCompat.mapContains(itemsMap, "122102")) {
	        			 voucherItems.add(createVoucherItemDto(bookId, SubjectCodeCompat.resolveFromMap(itemsMap, "122102"), summary.getTotalSocialInsurance()));
	        			 creditYyckAmount = creditYyckAmount.subtract(summary.getTotalSocialInsurance());
	        		 }
	        		 //个人所得税
	        		 if(SubjectCodeCompat.mapContains(itemsMap, "222114")) {
	        			 voucherItems.add(createVoucherItemDto(bookId, SubjectCodeCompat.resolveFromMap(itemsMap, "222114"), summary.getBusinessSocialInsurance()));
	        			 creditYyckAmount = creditYyckAmount.subtract(summary.getBusinessSocialInsurance());
	        		 }
	        		 
	        		 for (VoucherTemplateItem item : items) {
	        			 if(item.getSubjectCode().startsWith("1002")) {
	        				 voucherItems.add(createVoucherItemDto(bookId, item, creditYyckAmount));
	        			 }
	                 }
	        		 creditAmount = debitAmount;
	        	 }else if (voucherTemplate.getCode().startsWith("zf_shebao")){
		        	if(SubjectCodeCompat.mapContains(itemsMap, "122102")) {
		        		 //社保-个人
		        		 debitAmount = debitAmount.add(summary.getTotalSocialInsurance());
		        		 voucherItems.add(createVoucherItemDto(bookId, SubjectCodeCompat.resolveFromMap(itemsMap, "122102"), summary.getTotalSocialInsurance()));
		        	}
		        	if(SubjectCodeCompat.mapContains(itemsMap, "221103")) {
		        		 //社保-单位
		        		 debitAmount = debitAmount.add(summary.getBusinessSocialInsurance());
		        		 voucherItems.add(createVoucherItemDto(bookId, SubjectCodeCompat.resolveFromMap(itemsMap, "221103"), summary.getBusinessSocialInsurance()));
		        	}
	        		 for (VoucherTemplateItem item : items) {
	        			 if(item.getSubjectCode().startsWith("1002")) {
	        				 voucherItems.add(createVoucherItemDto(bookId, item, debitAmount));
	        			 }
	                 }
	        		 creditAmount = debitAmount;
	        	 }
        	}
        }else {
            for (VoucherTemplateItem item : items) {
                voucherItems.add(createVoucherItemDto(bookId, item, BigDecimal.ZERO));
            }
        }

        } catch (IllegalStateException ex) {
            return Message.failed(ex.getMessage());
        }

        voucherChangeDto.setItems(voucherItems);
        debitAmount = BigDecimal.ZERO;
        creditAmount = BigDecimal.ZERO;
        for (VoucherItemChangeDto item : voucherItems) {
            debitAmount = debitAmount.add(item.getDebitAmount() != null ? item.getDebitAmount() : BigDecimal.ZERO);
            creditAmount = creditAmount.add(item.getCreditAmount() != null ? item.getCreditAmount() : BigDecimal.ZERO);
        }
        voucherChangeDto.setDebitAmount(debitAmount);
        voucherChangeDto.setCreditAmount(creditAmount);
        //草稿阶段
        voucherChangeDto.setStatus(VoucherStatusEnum.DRAFT.getValue());
        log.debug("voucherChangeDto {}", voucherChangeDto);
        Message<String> saveResult = voucherService.save(voucherChangeDto);
        if (saveResult.getCode() != Message.SUCCESS) {
            return saveResult;
        }

        //结转记录
        SettlementCarryforward settlementCarryforward = new SettlementCarryforward();
        settlementCarryforward.setBookId(bookId);
        settlementCarryforward.setYear(year);
        settlementCarryforward.setYearPeriod(currentTerm);
        settlementCarryforward.setVoucherId(voucherChangeDto.getId());
        settlementCarryforward.setVoucherTemplateId(voucherTemplate.getId());
        //保存结转记录
        settlementCarryforwardMapper.insert(settlementCarryforward);
        //返回凭证ID编码
        return Message.ok(voucherChangeDto.getId());
    }

    private void addCarryVoucherItems(String bookId, String templateSubjectCode,
                                      List<VoucherItemChangeDto> items,
                                      Map<String, VoucherTemplateItem> itemsMap,
                                      int fallbackDirection) {
        VoucherTemplateItem templateItem = SubjectCodeCompat.resolveFromMap(itemsMap, templateSubjectCode);
        for (String subjectCode : SubjectCodeCompat.carryForwardSubjectCodes(templateSubjectCode)) {
            List<BookSubject> subjectList = bookSubjectService.selectSubjectAndChild(bookId, subjectCode);
            if (subjectList.isEmpty()) {
                continue;
            }
            boolean added = false;
            for (BookSubject s : subjectList) {
                if (isLeafSubject(s, subjectList) && s.getBalance().compareTo(BigDecimal.ZERO) != 0) {
                    VoucherTemplateItem item = templateItem != null
                            ? templateItem
                            : fallbackTemplateItem(s.getCode(), fallbackDirection);
                    items.add(createVoucherItemDtoBySubject(bookId, s, item, s.getBalance().abs()));
                    added = true;
                }
            }
            if (!added) {
                for (BookSubject s : subjectList) {
                    if (s.getCode().equals(subjectCode) && s.getBalance().compareTo(BigDecimal.ZERO) != 0) {
                        VoucherTemplateItem item = templateItem != null
                                ? templateItem
                                : fallbackTemplateItem(s.getCode(), fallbackDirection);
                        items.add(createVoucherItemDtoBySubject(bookId, s, item, s.getBalance().abs()));
                        break;
                    }
                }
            }
            return;
        }
    }

    private VoucherTemplateItem fallbackTemplateItem(String subjectCode, int direction) {
        VoucherTemplateItem item = new VoucherTemplateItem();
        item.setSubjectCode(subjectCode);
        item.setSummary("结转");
        item.setDirection(direction);
        return item;
    }

    private VoucherItemChangeDto createCarryVoucherItemDto(String bookId,
                                                           Map<String, VoucherTemplateItem> itemsMap,
                                                           String templateSubjectCode,
                                                           BigDecimal amount,
                                                           int fallbackDirection) {
        VoucherTemplateItem templateItem = SubjectCodeCompat.resolveFromMap(itemsMap, templateSubjectCode);
        if (templateItem == null) {
            for (String code : SubjectCodeCompat.carryForwardSubjectCodes(templateSubjectCode)) {
                if (!bookSubjectService.selectSubjectAndChild(bookId, code).isEmpty()) {
                    templateItem = fallbackTemplateItem(code, fallbackDirection);
                    break;
                }
            }
        }
        if (templateItem == null) {
            templateItem = fallbackTemplateItem(templateSubjectCode, fallbackDirection);
        }
        return createVoucherItemDto(bookId, templateItem, amount);
    }

    /**
     * Whether P&amp;L carry template has non-zero source balances (same roots as generate).
     * Used by month-end verify to treat empty periods as N/A instead of hard-fail.
     */
    public boolean hasPnlCarrySourceBalance(String bookId, String templateCode) {
        Book book = bookMapper.selectById(bookId);
        String standardId = book != null ? book.getStandardId() : MonthEndCloseRules.STANDARD_SMALL_BUSINESS;
        if (MonthEndCloseRules.CODE_CARRY_INCOME.equals(templateCode)) {
            return hasLeafBalance(bookId, MonthEndCloseRules.incomeCarryRootsForStandard(standardId));
        }
        if (MonthEndCloseRules.CODE_CARRY_COST.equals(templateCode)) {
            return hasLeafBalance(bookId, MonthEndCloseRules.costCarryRootsForStandard(standardId));
        }
        if (MonthEndCloseRules.CODE_CARRY_YEAR_PROFIT.equals(templateCode)) {
            String yearProfit = MonthEndCloseRules.yearProfitSubjectForStandard(standardId);
            StatementSubjectBalance profitBalance = getCarrySubjectBalance(bookId, yearProfit);
            return profitBalance != null && profitBalance.getBalance() != null
                    && profitBalance.getBalance().compareTo(BigDecimal.ZERO) != 0;
        }
        return true;
    }

    private boolean hasLeafBalance(String bookId, List<String> templateRoots) {
        for (String root : templateRoots) {
            for (String subjectCode : SubjectCodeCompat.carryForwardSubjectCodes(root)) {
                List<BookSubject> subjectList = bookSubjectService.selectSubjectAndChild(bookId, subjectCode);
                for (BookSubject s : subjectList) {
                    if (s.getBalance() != null && s.getBalance().compareTo(BigDecimal.ZERO) != 0) {
                        return true;
                    }
                }
            }
        }
        return false;
    }

    private StatementSubjectBalance getCarrySubjectBalance(String bookId, String templateSubjectCode) {
        for (String code : SubjectCodeCompat.carryForwardSubjectCodes(templateSubjectCode)) {
            StatementSubjectBalance balance = statementSubjectBalanceService.getSubjectBalance(bookId, code);
            if (balance != null) {
                return balance;
            }
        }
        return statementSubjectBalanceService.getSubjectBalance(bookId, templateSubjectCode);
    }

    private boolean addVoucherItems(String bookId, String subjectCode, List<VoucherItemChangeDto> items, VoucherTemplateItem templateItem) {
        List<BookSubject> subjectList = bookSubjectService.selectSubjectAndChild(bookId, subjectCode);
        for (BookSubject s : subjectList) {
            if (isLeafSubject(s, subjectList) && s.getBalance().compareTo(BigDecimal.ZERO) != 0) {
                items.add(createVoucherItemDtoBySubject(bookId, s, templateItem, s.getBalance().abs()));
            }
        }
        return true;
    }

    private boolean isLeafSubject(BookSubject subject, List<BookSubject> subjectList) {
        boolean isLeaf = true;
        //仅有一条数据
        if (subjectList.size() == 1) {
            return true;
        }
        //多条数据
        for (BookSubject s : subjectList) {
            //跳过自己
            if (subject.getCode().equals(s.getCode())) {
                continue;
            }
            //有节点以当前节点开头认为不是叶节点
            if (s.getCode().startsWith(subject.getCode())) {
                isLeaf = false;
                break;
            }
        }
        return isLeaf;
    }


    /**
     * Creates the voucher change dto with common fields
     */
    private VoucherChangeDto createVoucherChangeDto(Book book, String bookId, String wordHead,
                                                    Date voucherDate, Integer year, Integer month, BigDecimal amount) {

        Integer wordNum = voucherService.getAbleWordNum(bookId, wordHead, null, null).getData();

        VoucherChangeDto dto = new VoucherChangeDto();
        dto.setWordHead(wordHead);
        dto.setWordNum(wordNum);
        dto.setBookId(bookId);
        dto.setCompanyName(book.getCompanyName());
        dto.setVoucherDate(voucherDate);
        dto.setVoucherYear(year);
        dto.setVoucherMonth(month);
        dto.setDebitAmount(amount);
        dto.setCreditAmount(amount);

        return dto;
    }

    /**
     * Creates a voucher item dto based on rule and direction
     */
    private VoucherItemChangeDto createVoucherItemDto(String bookId,
                                                      VoucherTemplateItem item, BigDecimal amount) {
        BookSubject bookSubject = bookSubjectService.resolvePostableSubject(bookId, item.getSubjectCode());
        if (bookSubject == null) {
            throw new IllegalStateException("凭证模板科目[" + item.getSubjectCode() + "]在账套中无可用末级科目");
        }

        VoucherItemChangeDto itemDto = new VoucherItemChangeDto();
        itemDto.setSummary(item.getSummary());
        itemDto.setSubjectId(bookSubject.getId());
        if (item.getDirection() == 1) {
            itemDto.setDebitAmount(amount);
        } else {
            itemDto.setCreditAmount(amount);
        }
        itemDto.setSubjectBalance(bookSubject.getBalance());
        itemDto.setAuxiliary(List.of());
        itemDto.setSubjectCode(bookSubject.getCode());
        itemDto.setSubjectName(bookSubject.getCode() + "-" + bookSubject.getName());
        itemDto.setDetailedAccounts("");

        return itemDto;
    }

    /**
     * Creates a voucher item dto based on rule and direction
     */
    private VoucherItemChangeDto createVoucherItemDtoBySubject(String bookId, BookSubject bookSubject,
                                                               VoucherTemplateItem item, BigDecimal amount) {
        VoucherItemChangeDto itemDto = new VoucherItemChangeDto();
        itemDto.setSummary(item.getSummary());
        itemDto.setSubjectId(bookSubject.getId());
        if (item.getDirection() == 1) {
            itemDto.setDebitAmount(amount);
        } else {
            itemDto.setCreditAmount(amount);
        }
        itemDto.setSubjectBalance(bookSubject.getBalance());
        itemDto.setAuxiliary(List.of());
        itemDto.setSubjectCode(bookSubject.getCode());
        itemDto.setSubjectName(bookSubject.getCode() + "-" + bookSubject.getName());
        itemDto.setDetailedAccounts("");

        return itemDto;
    }
    public Message<String> delete(String bookId, String voucherId) {
        LambdaQueryWrapper<SettlementCarryforward> carryLqw = Wrappers.lambdaQuery();
        carryLqw.eq(SettlementCarryforward::getBookId, bookId);
        carryLqw.eq(SettlementCarryforward::getVoucherId, voucherId);
        SettlementCarryforward settlementCarryforward = settlementCarryforwardMapper.selectOne(carryLqw);
        if (settlementCarryforward == null) {
            return Message.failed("结转记录不存在");
        }
        ArrayList<String> voucherIds = new ArrayList<String>();
        voucherIds.add(settlementCarryforward.getVoucherId());
        Message<String> deleteResult = voucherService.delete(voucherIds, bookId);
        if (deleteResult.getCode() != Message.SUCCESS) {
            return deleteResult;
        }

        settlementCarryforwardMapper.delete(carryLqw);
        return Message.ok("删除成功");
    }

}
