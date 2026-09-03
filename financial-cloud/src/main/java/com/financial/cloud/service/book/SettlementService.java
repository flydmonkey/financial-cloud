package com.financial.cloud.service.book;


import lombok.RequiredArgsConstructor;
import com.financial.cloud.service.arap.ArapService;
import com.financial.cloud.service.config.ConfigSysService;
import com.financial.cloud.service.fixedasset.FixedAssetDepreciationService;
import com.financial.cloud.service.statement.StatementSubjectBalanceService;
import com.financial.cloud.service.statement.StatementReportService;
import com.financial.cloud.service.statement.StatementIncomeService;
import com.financial.cloud.service.statement.StatementBalanceSheetService;
import com.financial.cloud.service.journal.JournalAccountService;
import com.financial.cloud.service.voucher.VoucherService;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.incrementer.IdentifierGenerator;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.financial.cloud.common.Message;
import com.financial.cloud.domain.book.Settlement;
import com.financial.cloud.domain.book.SettlementCarryforward;
import com.financial.cloud.domain.journal.JournalEntry;
import com.financial.cloud.domain.voucher.Voucher;
import com.financial.cloud.domain.voucher.VoucherTemplate;
import com.financial.cloud.dto.arap.ArapMonthEndSummaryVo;
import com.financial.cloud.dto.book.SettlementPageDto;
import com.financial.cloud.dto.book.SettlementVerifyVo;
import com.financial.cloud.dto.fixedasset.FixedAssetDepreciationStatusVo;
import com.financial.cloud.dto.statement.StatementParamsDto;
import com.financial.cloud.dto.voucher.VoucherSuccessiveDto;
import com.financial.cloud.dto.voucher.VoucherItemVo;
import com.financial.cloud.enums.statement.StatementPeriodTypeEnum;
import com.financial.cloud.enums.voucher.VoucherStatusEnum;
import com.financial.cloud.repository.book.BookMapper;
import com.financial.cloud.repository.book.SettlementCarryforwardMapper;
import com.financial.cloud.repository.book.SettlementMapper;
import com.financial.cloud.repository.journal.JournalEntryMapper;
import com.financial.cloud.repository.voucher.VoucherItemMapper;
import com.financial.cloud.repository.voucher.VoucherTemplateMapper;
import com.financial.cloud.service.book.BookSubjectService;
import com.financial.cloud.service.book.SettlementService;
import lombok.extern.slf4j.Slf4j;

import java.math.BigDecimal;
import java.time.YearMonth;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;


@RequiredArgsConstructor
@Slf4j
@Service
public class SettlementService extends ServiceImpl<SettlementMapper, Settlement>{

	private final IdentifierGenerator identifierGenerator;

    private final BookMapper bookMapper;

    private final BookSubjectService bookSubjectService;

    private final SettlementMapper settlementMapper;

    private final ConfigSysService configSysService;

    private final VoucherService voucherService;

    private final VoucherItemMapper voucherItemMapper;

    private final StatementIncomeService statementIncomeService;
    
    private final StatementBalanceSheetService statementBalanceSheetService;
    
    private final StatementSubjectBalanceService statementSubjectBalanceService;
    
    private final JournalAccountService journalAccountService;

	private final JournalEntryMapper journalEntryMapper;

	private final SettlementCarryforwardMapper settlementCarryforwardMapper;

	private final VoucherTemplateMapper voucherTemplateMapper;

	@Lazy
	private final FixedAssetDepreciationService fixedAssetDepreciationService;

	@Lazy
	private final ArapService arapService;

	@Lazy
	private final StatementReportService statementReportService;
	public Message<Page<Settlement>> pageList(SettlementPageDto dto) {
		String currentTerm = configSysService.getCurrentTerm(dto.getBookId());
		String termStart = configSysService.getTermStart(dto.getBookId());
		if (dto.getYear() <= 0) {
			dto.setYear(Integer.parseInt(currentTerm.split("-")[0]));
		}
		LambdaQueryWrapper<Settlement> wrapper = new LambdaQueryWrapper<>();
		wrapper.eq(Settlement::getBookId, dto.getBookId());
		wrapper.eq(Settlement::getYear, dto.getYear());
        List<Settlement> listSettlement = settlementMapper.selectList(wrapper);
        log.debug("Settlement {}",listSettlement);
        Page<Settlement> pageResult = new Page<>();
        pageResult.setRecords(new ArrayList<>());
        YearMonth currentTermYearMonth = YearMonth.parse(currentTerm);
        YearMonth termStartYearMonth = YearMonth.parse(termStart);
        Integer termStartYear = Integer.valueOf(termStart.split("-")[0]);
        log.debug("term start {} , current {}",termStart,currentTerm);
        if(termStartYear <= dto.getYear()) {
	        for(int i = 1; i<=12 ;i++) {

	        	Settlement settlement = new Settlement();
	        	settlement.setBookId(dto.getBookId());
	        	settlement.setYear(dto.getYear());
	        	settlement.setStatus(6);
	        	settlement.setPeriod(String.format("%02d", i));
	        	settlement.setYearPeriod(dto.getYear()+"-"+settlement.getPeriod());
	        	YearMonth settlementYearMonth = YearMonth.parse(settlement.getYearPeriod());
	        	if(settlementYearMonth.isBefore(termStartYearMonth)) {
	        		//初始化前
	        		settlement.setStatus(4);
	        	}else if(settlementYearMonth.isAfter(currentTermYearMonth)) {
	        		//当前期后面
	        		settlement.setStatus(2);
	        	}else if(settlementYearMonth.equals(currentTermYearMonth)) {
	        		//当前期
	        		settlement.setStatus(1);
	        	}
	        	pageResult.getRecords().add(settlement);
	        }

	        for(Settlement settlement :pageResult.getRecords()) {
	        	for(Settlement s :listSettlement) {
	        		if(settlement.getYearPeriod().equals(s.getYearPeriod())) {
	        			settlement.setStatus(s.getStatus());
	        		}
	        	}
	        }
	        pageResult.setTotal(pageResult.getRecords().size());
	        return new Message<>(Message.SUCCESS, pageResult);
        }
        return Message.failed("账期年份必须大于等于初始化账期");
	}


	/**
	 * 财务软件中关闭当期账务，生成财务报表（资产负债表  利润表）
	 */
	@Transactional
	public Message<Settlement> checkout(Settlement dto) {
		configSysService.ensureBookConfigsComplete(dto.getBookId());
		//结账逻辑检测
		String currentTerm = configSysService.getCurrentTerm(dto.getBookId());
		YearMonth currentTermYearMonth = YearMonth.parse(currentTerm);
		LambdaQueryWrapper<Settlement> wrapper = new LambdaQueryWrapper<>();
		wrapper.eq(Settlement::getBookId, dto.getBookId());
		wrapper.eq(Settlement::getYear, dto.getYear());
		wrapper.eq(Settlement::getYearPeriod, currentTerm);
        Settlement storedSettlement = settlementMapper.selectOne(wrapper);
        if(storedSettlement == null) {
			List<SettlementVerifyVo> hardGates = evaluateHardGates(dto.getBookId(), currentTerm);
			List<SettlementVerifyVo> failed = hardGates.stream()
					.filter(v -> v.isHard() && v.isApplicable() && !v.isResult())
					.collect(Collectors.toList());
			if (!failed.isEmpty()) {
				String detail = failed.stream()
						.map(v -> v.getItem() + (StringUtils.isNotBlank(v.getReason()) ? ("：" + v.getReason()) : ""))
						.collect(Collectors.joining("；"));
				return Message.failed("结账条件未通过：" + detail);
			}
			Settlement settlement = new Settlement();
	    	settlement.setBookId(dto.getBookId());
	    	settlement.setYear(currentTermYearMonth.getYear());
	    	settlement.setYearPeriod(currentTerm);
	    	//当前期
	    	settlement.setCurrentTerm(currentTerm);
	    	//下一期
	    	settlement.setNextTerm(configSysService.getNextTerm(dto.getBookId()));
	    	settlement.setStatus(6);
			//存储当前现金流量报表的期末余额
			StatementParamsDto statementParamsDto = new StatementParamsDto();
			statementParamsDto.setReportDate(currentTerm);
			statementParamsDto.setPeriodType("month");
			statementParamsDto.setBookId(dto.getBookId());
			settlement.setEndingBalance(statementReportService.getEndingBalance(statementParamsDto));
			//资产负债表
			statementBalanceSheetService.checkout(settlement);
			//利润表报表
			statementIncomeService.checkout(settlement);
			//科目余额表
			statementSubjectBalanceService.checkout(settlement);
			//账户结账：本期期初余额=余额
			journalAccountService.checkout(settlement.getBookId());
			//本期结账
			settlementMapper.insert(settlement);
			//更新当前账期
			configSysService.termToNext(dto.getBookId());
        } else {
        	return Message.failed("账期[" + currentTerm + "]已结账");
        }
		return new Message<>(Message.SUCCESS, "结账完成");
	}

	/**
	 * 反结账：仅允许打开「当前账期的上一月」。
	 * 不级联反过账/删除凭证；迁移前结账（日记账无 prev_opening_balance）拒绝。
	 *
	 * @param bookId     账套
	 * @param yearPeriod 可选，必须等于 currentTerm 上一月
	 * @param userId     操作人（日志）
	 */
	@Transactional
	public Message<String> uncheckout(String bookId, String yearPeriod, String userId) {
		String currentTerm = configSysService.getCurrentTerm(bookId);
		YearMonth currentYm = YearMonth.parse(currentTerm);
		String targetTerm = currentYm.minusMonths(1).toString();

		if (StringUtils.isNotBlank(yearPeriod) && !targetTerm.equals(yearPeriod.trim())) {
			return rejectUncheckout(bookId, userId, yearPeriod, currentTerm,
					"只能反结账最近已结期间[" + targetTerm + "]，不能反[" + yearPeriod + "]");
		}

		LambdaQueryWrapper<Settlement> wrapper = new LambdaQueryWrapper<>();
		wrapper.eq(Settlement::getBookId, bookId);
		wrapper.eq(Settlement::getYearPeriod, targetTerm);
		Settlement stored = settlementMapper.selectOne(wrapper);
		if (stored == null) {
			return rejectUncheckout(bookId, userId, targetTerm, currentTerm,
					"账期[" + targetTerm + "]未结账或不存在结账记录，无法反结账");
		}

		long voucherCount = voucherService.count(new LambdaQueryWrapper<Voucher>()
				.eq(Voucher::getBookId, bookId)
				.apply("DATE_FORMAT(voucher_date, '%Y-%m') = {0}", currentTerm));
		if (voucherCount > 0) {
			return rejectUncheckout(bookId, userId, targetTerm, currentTerm,
					"当前账期[" + currentTerm + "]已有凭证，请先清理后再反结账");
		}

		long journalCount = journalEntryMapper.selectCount(new LambdaQueryWrapper<JournalEntry>()
				.eq(JournalEntry::getBookId, bookId)
				.apply("DATE_FORMAT(trade_date, '%Y-%m') = {0}", currentTerm));
		if (journalCount > 0) {
			return rejectUncheckout(bookId, userId, targetTerm, currentTerm,
					"当前账期[" + currentTerm + "]已有日记账流水，请先清理后再反结账");
		}

		if (journalAccountService.hasAccountsMissingPrevOpening(bookId)) {
			return rejectUncheckout(bookId, userId, targetTerm, currentTerm,
					"该期结账未保存日记账期初快照，无法安全反结账（请使用升级后结账的账期）");
		}

		String month = StatementPeriodTypeEnum.MONTH.getValue();
		statementSubjectBalanceService.deleteByBookAndPeriod(bookId, currentTerm, month);
		journalAccountService.restoreOpeningFromPrev(bookId);

		StatementParamsDto periodProbe = new StatementParamsDto();
		statementIncomeService.deletePeriodSnapshot(bookId, targetTerm, month);
		statementBalanceSheetService.deletePeriodSnapshot(bookId, targetTerm, month);
		if (periodProbe.isQuarterReportMonth(targetTerm)) {
			String quarter = StatementPeriodTypeEnum.QUARTER.getValue();
			statementIncomeService.deletePeriodSnapshot(bookId, targetTerm, quarter);
			statementBalanceSheetService.deletePeriodSnapshot(bookId, targetTerm, quarter);
		}
		if (periodProbe.isYearReportMonth(targetTerm)) {
			String year = StatementPeriodTypeEnum.YEAR.getValue();
			statementIncomeService.deletePeriodSnapshot(bookId, targetTerm, year);
			statementBalanceSheetService.deletePeriodSnapshot(bookId, targetTerm, year);
		}

		settlementMapper.deleteById(stored.getId());
		configSysService.updateCurrentTerm(bookId, targetTerm);

		log.info("uncheckout success bookId={} userId={} reopened={} wasCurrent={}",
				bookId, userId, targetTerm, currentTerm);
		return new Message<>(Message.SUCCESS, "反结账完成，当前账期已回到[" + targetTerm + "]");
	}

	private Message<String> rejectUncheckout(String bookId, String userId, String targetTerm,
			String currentTerm, String reason) {
		log.warn("uncheckout rejected bookId={} userId={} target={} current={} reason={}",
				bookId, userId, targetTerm, currentTerm, reason);
		return Message.failed(reason);
	}

	public Message<String> check(String bookId,String period) {
		String currentTerm = configSysService.getCurrentTerm(bookId);
        String termStart = configSysService.getTermStart(bookId);
        YearMonth currentTermYearMonth = YearMonth.parse(currentTerm);
        YearMonth termStartYearMonth = YearMonth.parse(termStart);
        //账期参数
        YearMonth termYearMonth = YearMonth.parse(period);
        log.debug("term start {} , current {}",termStart,currentTerm);
        if(termYearMonth.isBefore(termStartYearMonth)) {
    		//初始化前
        	return Message.failed("账期["+period+"]不能小于初始化账期["+termStart+"]");
    	}

        LambdaQueryWrapper<Settlement> wrapper = new LambdaQueryWrapper<>();
		wrapper.eq(Settlement::getBookId, bookId);
		wrapper.eq(Settlement::getYear, termYearMonth.getYear());
		wrapper.eq(Settlement::getYearPeriod, period);
        Settlement storedSettlement = settlementMapper.selectOne(wrapper);
        if(storedSettlement != null) {
        	return Message.failed("账期["+period+"]结账完成");
        }

        if(termYearMonth.equals(currentTermYearMonth)) {
        	return Message.ok("当前账期["+period+"]未结账");
        }else {
        	return Message.ok("账期["+period+"]未结账");
        }
	}
	public Message<List<SettlementVerifyVo>> verify(String bookId) {
		String currentTerm = configSysService.getCurrentTerm(bookId);
		List<SettlementVerifyVo> settlementVerifyList = evaluateHardGates(bookId, currentTerm);
		boolean hardFailed = settlementVerifyList.stream()
				.anyMatch(v -> v.isHard() && v.isApplicable() && !v.isResult());
		if (hardFailed) {
			return new Message<>(Message.FAIL, "结账条件项检查失败", settlementVerifyList);
		}
		return new Message<>(Message.SUCCESS, "结账条件项检查完成", settlementVerifyList);
	}

	/**
	 * Hard gates for month-end close. Same evaluation is used by {@link #verify} and {@link #checkout}.
	 */
	List<SettlementVerifyVo> evaluateHardGates(String bookId, String currentTerm) {
		List<SettlementVerifyVo> list = new ArrayList<>();
		int checkIndex = 1;

		long unposted = countUnpostedVouchers(bookId, currentTerm);
		if (unposted > 0) {
			list.add(SettlementVerifyVo.hardFail(checkIndex++, "未完成凭证检查",
					"当前账期仍有 " + unposted + " 张未过账凭证"));
		} else {
			list.add(SettlementVerifyVo.hardPass(checkIndex++, "未完成凭证检查"));
		}

		Message<List<VoucherSuccessiveDto>> voucherSuccessiveMsg = voucherService.checkSuccessiveAll(bookId);
		if (CollectionUtils.isEmpty(voucherSuccessiveMsg.getData())) {
			list.add(SettlementVerifyVo.hardPass(checkIndex++, "凭证号连续性检查"));
		} else {
			list.add(SettlementVerifyVo.hardFail(checkIndex++, "凭证号连续性检查", "存在断号或凭证号不连续"));
		}

		StatementParamsDto statementParamsDto = new StatementParamsDto();
		statementParamsDto.setPeriodType(StatementPeriodTypeEnum.MONTH.getValue());
		statementParamsDto.setBookId(bookId);
		statementParamsDto.setReportDate(currentTerm);
		statementParamsDto.parse();
		BigDecimal creditAmount = BigDecimal.ZERO;
		BigDecimal debitAmount = BigDecimal.ZERO;
		List<VoucherItemVo> voucherItemVos = voucherItemMapper.selectSubjectAmount(statementParamsDto);
		for (VoucherItemVo voucherItemVo : voucherItemVos) {
			debitAmount = debitAmount.add(voucherItemVo.getDebitAmount() == null ? BigDecimal.ZERO : voucherItemVo.getDebitAmount());
			creditAmount = creditAmount.add(voucherItemVo.getCreditAmount() == null ? BigDecimal.ZERO : voucherItemVo.getCreditAmount());
		}
		if (creditAmount.compareTo(debitAmount) == 0) {
			list.add(SettlementVerifyVo.hardPass(checkIndex++, "凭证借贷方余额的检查"));
		} else {
			list.add(SettlementVerifyVo.hardFail(checkIndex++, "凭证借贷方余额的检查",
					"借方合计 " + debitAmount + " 与贷方合计 " + creditAmount + " 不相等"));
		}

		checkIndex = appendRequiredCarryChecks(list, checkIndex, bookId, currentTerm);
		checkIndex = appendDepreciationCheck(list, checkIndex, bookId, currentTerm);
		appendArapSummaryCheck(list, checkIndex, bookId, currentTerm);
		return list;
	}

	private long countUnpostedVouchers(String bookId, String currentTerm) {
		YearMonth ym = YearMonth.parse(currentTerm);
		LambdaQueryWrapper<Voucher> wrapper = Wrappers.lambdaQuery();
		wrapper.eq(Voucher::getBookId, bookId);
		wrapper.eq(Voucher::getVoucherYear, ym.getYear());
		wrapper.eq(Voucher::getVoucherMonth, ym.getMonthValue());
		wrapper.ne(Voucher::getStatus, VoucherStatusEnum.CANCELLED.getValue());
		wrapper.and(w -> w.isNull(Voucher::getSenderId).or().eq(Voucher::getSenderId, ""));
		return voucherService.count(wrapper);
	}

	private int appendRequiredCarryChecks(List<SettlementVerifyVo> list, int checkIndex,
			String bookId, String currentTerm) {
		List<String> requiredCodes = MonthEndCloseRules.requiredCarryCodesForTerm(currentTerm);
		List<VoucherTemplate> templates = voucherTemplateMapper.selectList(
				Wrappers.<VoucherTemplate>lambdaQuery()
						.eq(VoucherTemplate::getRelatedId, bookId)
						.in(VoucherTemplate::getCode, requiredCodes));
		Set<String> foundCodes = templates.stream().map(VoucherTemplate::getCode).collect(Collectors.toSet());
		Set<String> doneTemplateIds = new HashSet<>();
		if (!templates.isEmpty()) {
			List<String> templateIds = templates.stream().map(VoucherTemplate::getId).collect(Collectors.toList());
			List<SettlementCarryforward> carries = settlementCarryforwardMapper.selectList(
					Wrappers.<SettlementCarryforward>lambdaQuery()
							.eq(SettlementCarryforward::getBookId, bookId)
							.eq(SettlementCarryforward::getYearPeriod, currentTerm)
							.in(SettlementCarryforward::getVoucherTemplateId, templateIds));
			for (SettlementCarryforward carry : carries) {
				if (StringUtils.isNotBlank(carry.getVoucherId())) {
					doneTemplateIds.add(carry.getVoucherTemplateId());
				}
			}
		}

		for (String code : requiredCodes) {
			String label = carryLabel(code);
			if (!foundCodes.contains(code)) {
				if (MonthEndCloseRules.isDecemberYearProfitCode(code)) {
					list.add(SettlementVerifyVo.hardNa(checkIndex++, label, "账套无该结转模板，跳过"));
				} else {
					list.add(SettlementVerifyVo.hardFail(checkIndex++, label, "缺少必做结转模板 " + code));
				}
				continue;
			}
			VoucherTemplate template = templates.stream()
					.filter(t -> code.equals(t.getCode()))
					.findFirst()
					.orElse(null);
			if (template != null && doneTemplateIds.contains(template.getId())) {
				list.add(SettlementVerifyVo.hardPass(checkIndex++, label));
			} else if (MonthEndCloseRules.isDecemberYearProfitCode(code)) {
				var profitBalance = statementSubjectBalanceService.getSubjectBalance(bookId, "4103");
				boolean zeroProfit = profitBalance == null || profitBalance.getBalance() == null
						|| profitBalance.getBalance().compareTo(BigDecimal.ZERO) == 0;
				if (zeroProfit) {
					list.add(SettlementVerifyVo.hardNa(checkIndex++, label, "本年利润无余额，无需结转"));
				} else {
					list.add(SettlementVerifyVo.hardFail(checkIndex++, label, "尚未生成结转本年利润凭证"));
				}
			} else {
				list.add(SettlementVerifyVo.hardFail(checkIndex++, label, "尚未生成结转凭证"));
			}
		}
		return checkIndex;
	}

	private static String carryLabel(String code) {
		if (MonthEndCloseRules.CODE_CARRY_INCOME.equals(code)) {
			return "损益结转-收入";
		}
		if (MonthEndCloseRules.CODE_CARRY_COST.equals(code)) {
			return "损益结转-成本费用";
		}
		if (MonthEndCloseRules.CODE_CARRY_YEAR_PROFIT.equals(code)) {
			return "损益结转-本年利润";
		}
		return "损益结转-" + code;
	}

	private int appendDepreciationCheck(List<SettlementVerifyVo> list, int checkIndex,
			String bookId, String currentTerm) {
		boolean needs = fixedAssetDepreciationService.needsDepreciationAccrual(bookId, currentTerm);
		if (!needs) {
			list.add(SettlementVerifyVo.hardNa(checkIndex++, "固定资产折旧", "本期无应计提折旧的资产"));
			return checkIndex;
		}
		Message<FixedAssetDepreciationStatusVo> statusMsg =
				fixedAssetDepreciationService.status(bookId, currentTerm);
		boolean accrued = statusMsg.getData() != null && statusMsg.getData().isAccrued();
		if (accrued) {
			list.add(SettlementVerifyVo.hardPass(checkIndex++, "固定资产折旧"));
		} else {
			list.add(SettlementVerifyVo.hardFail(checkIndex++, "固定资产折旧", "本期尚有应折旧资产未计提"));
		}
		return checkIndex;
	}

	private void appendArapSummaryCheck(List<SettlementVerifyVo> list, int checkIndex,
			String bookId, String currentTerm) {
		try {
			ArapMonthEndSummaryVo summary = arapService.monthEndSummary(bookId, currentTerm);
			String reason = String.format(
					"应收合计 %s，应付合计 %s；逾期应收 %s，逾期应付 %s（账龄按凭证日期FIFO估算，逾期不阻断结账）",
					summary.getReceivableTotal(), summary.getPayableTotal(),
					summary.getOverdueReceivable(), summary.getOverduePayable());
			if (summary.isHasOverdue()) {
				list.add(SettlementVerifyVo.hardPassWarning(checkIndex, "往来款项（应收应付/账龄）", reason));
			} else {
				SettlementVerifyVo pass = SettlementVerifyVo.hardPass(checkIndex, "往来款项（应收应付/账龄）");
				pass.setReason(reason);
				list.add(pass);
			}
		} catch (Exception ex) {
			log.warn("arap month-end summary failed bookId={} term={}: {}", bookId, currentTerm, ex.getMessage());
			list.add(SettlementVerifyVo.hardPassWarning(checkIndex, "往来款项（应收应付/账龄）",
					"往来汇总查询异常，请人工核对：" + ex.getMessage()));
		}
	}

}
