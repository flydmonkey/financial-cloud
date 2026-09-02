package com.financial.cloud.service.fixedasset;

import cn.hutool.core.collection.CollUtil;
import com.baomidou.mybatisplus.core.incrementer.IdentifierGenerator;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.financial.cloud.common.Message;
import com.financial.cloud.domain.book.Book;
import com.financial.cloud.domain.book.BookSubject;
import com.financial.cloud.domain.fixedasset.FixedAsset;
import com.financial.cloud.domain.fixedasset.FixedAssetAccrual;
import com.financial.cloud.domain.fixedasset.FixedAssetDepr;
import com.financial.cloud.domain.fixedasset.FixedAssetWork;
import com.financial.cloud.domain.voucher.Voucher;
import com.financial.cloud.dto.fixedasset.FixedAssetAccrueDto;
import com.financial.cloud.dto.fixedasset.FixedAssetAccrueResultVo;
import com.financial.cloud.dto.fixedasset.FixedAssetDepreciationParamsDto;
import com.financial.cloud.dto.fixedasset.FixedAssetDepreciationStatusVo;
import com.financial.cloud.dto.fixedasset.FixedAssetWorkItemDto;
import com.financial.cloud.dto.voucher.VoucherChangeDto;
import com.financial.cloud.dto.voucher.VoucherItemChangeDto;
import com.financial.cloud.enums.error.FixedAssetErrorCode;
import com.financial.cloud.enums.fixedasset.DepreciationMethod;
import com.financial.cloud.enums.voucher.VoucherStatusEnum;
import com.financial.cloud.exception.ServiceException;
import com.financial.cloud.repository.book.BookMapper;
import com.financial.cloud.repository.fixedasset.FixedAssetAccrualMapper;
import com.financial.cloud.repository.fixedasset.FixedAssetDeprMapper;
import com.financial.cloud.repository.fixedasset.FixedAssetMapper;
import com.financial.cloud.repository.fixedasset.FixedAssetWorkMapper;
import com.financial.cloud.service.book.BookSubjectService;
import com.financial.cloud.service.config.ConfigSysService;
import com.financial.cloud.service.voucher.VoucherService;
import com.financial.cloud.util.DateUtils;
import com.financial.cloud.util.FixedAssetDepreciationRules;
import com.financial.cloud.util.VoucherUtils;
import lombok.RequiredArgsConstructor;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@RequiredArgsConstructor
@Service
public class FixedAssetDepreciationService extends ServiceImpl<FixedAssetAccrualMapper, FixedAssetAccrual> {

    private static final String DEFAULT_WORD = "记";
    private static final String DEFAULT_SUMMARY = "计提折旧费用";
    private static final String STATUS_ACCRUED = "ACCRUED";

    private final FixedAssetAccrualMapper accrualMapper;
    private final FixedAssetDeprMapper deprMapper;
    private final FixedAssetWorkMapper workMapper;
    private final FixedAssetMapper fixedAssetMapper;
    private final BookMapper bookMapper;
    private final BookSubjectService bookSubjectService;
    private final ConfigSysService configSysService;
    private final VoucherService voucherService;
    private final IdentifierGenerator identifierGenerator;

    public Message<FixedAssetDepreciationStatusVo> status(String bookId, String yearPeriod) {
        String period = resolvePeriod(bookId, yearPeriod);
        FixedAssetDepreciationStatusVo vo = new FixedAssetDepreciationStatusVo();
        vo.setYearPeriod(period);
        FixedAssetAccrual accrual = findAccrual(bookId, period);
        if (accrual == null || StringUtils.isBlank(accrual.getVoucherId())) {
            vo.setAccrued(false);
            vo.setCanReaccrue(false);
            return Message.ok(vo);
        }
        vo.setAccrued(true);
        vo.setVoucherId(accrual.getVoucherId());
        vo.setTotalAmount(accrual.getTotalAmount());
        vo.setVoucherDate(accrual.getVoucherDate());
        vo.setSummary(accrual.getSummary());
        Voucher voucher = voucherService.getById(accrual.getVoucherId());
        if (voucher != null) {
            vo.setVoucherWord(VoucherUtils.displayWord(voucher));
            boolean locked = VoucherStatusEnum.COMPLETED.getValue().equals(voucher.getStatus())
                    || StringUtils.isNotBlank(voucher.getAuditMemberId())
                    || StringUtils.isNotBlank(voucher.getSenderId());
            vo.setCanReaccrue(!locked);
        } else {
            vo.setCanReaccrue(true);
        }
        return Message.ok(vo);
    }

    public Message<FixedAssetDepreciationParamsDto> getParams(String bookId, String yearPeriod) {
        String period = resolvePeriod(bookId, yearPeriod);
        FixedAssetDepreciationParamsDto dto = new FixedAssetDepreciationParamsDto();
        dto.setYearPeriod(period);
        FixedAssetAccrual accrual = findAccrual(bookId, period);
        if (accrual != null) {
            dto.setVoucherDate(accrual.getVoucherDate());
            dto.setVoucherWord(StringUtils.defaultIfBlank(accrual.getVoucherWord(), DEFAULT_WORD));
            dto.setSummary(StringUtils.defaultIfBlank(accrual.getSummary(), DEFAULT_SUMMARY));
            return Message.ok(dto);
        }
        FixedAssetAccrual latest = accrualMapper.selectOne(Wrappers.<FixedAssetAccrual>lambdaQuery()
                .eq(FixedAssetAccrual::getBookId, bookId)
                .orderByDesc(FixedAssetAccrual::getYearPeriod)
                .last("limit 1"));
        dto.setVoucherWord(latest != null && StringUtils.isNotBlank(latest.getVoucherWord())
                ? latest.getVoucherWord() : DEFAULT_WORD);
        dto.setSummary(latest != null && StringUtils.isNotBlank(latest.getSummary())
                ? latest.getSummary() : DEFAULT_SUMMARY);
        dto.setVoucherDate(lastDayOfPeriod(period));
        return Message.ok(dto);
    }

    @Transactional
    public Message<FixedAssetDepreciationParamsDto> saveParams(String bookId, FixedAssetDepreciationParamsDto dto) {
        String period = resolvePeriod(bookId, dto.getYearPeriod());
        dto.setYearPeriod(period);
        if (StringUtils.isBlank(dto.getVoucherWord())) {
            dto.setVoucherWord(DEFAULT_WORD);
        }
        if (StringUtils.isBlank(dto.getSummary())) {
            dto.setSummary(DEFAULT_SUMMARY);
        }
        if (dto.getVoucherDate() == null) {
            dto.setVoucherDate(lastDayOfPeriod(period));
        }
        FixedAssetAccrual accrual = findAccrual(bookId, period);
        if (accrual == null) {
            accrual = FixedAssetAccrual.builder()
                    .bookId(bookId)
                    .yearPeriod(period)
                    .voucherDate(dto.getVoucherDate())
                    .voucherWord(dto.getVoucherWord())
                    .summary(dto.getSummary())
                    .totalAmount(BigDecimal.ZERO)
                    .status("PENDING")
                    .build();
            accrual.setId(identifierGenerator.nextId(accrual).toString());
            accrualMapper.insert(accrual);
        } else if (StringUtils.isBlank(accrual.getVoucherId())) {
            accrual.setVoucherDate(dto.getVoucherDate());
            accrual.setVoucherWord(dto.getVoucherWord());
            accrual.setSummary(dto.getSummary());
            accrualMapper.updateById(accrual);
        }
        return Message.ok(dto);
    }

    public Message<List<FixedAssetWorkItemDto>> listWork(String bookId, String yearPeriod) {
        String period = resolvePeriod(bookId, yearPeriod);
        List<FixedAsset> assets = loadAccruableUopAssets(bookId, period);
        Map<String, BigDecimal> workMap = loadWorkMap(bookId, period);
        List<FixedAssetWorkItemDto> list = new ArrayList<>();
        for (FixedAsset asset : assets) {
            FixedAssetWorkItemDto item = new FixedAssetWorkItemDto();
            item.setAssetId(asset.getId());
            item.setCode(asset.getCode());
            item.setName(asset.getName());
            item.setExpectedTotalWork(asset.getExpectedTotalWork());
            item.setPeriodWork(workMap.get(asset.getId()));
            list.add(item);
        }
        return Message.ok(list);
    }

    @Transactional
    public Message<String> saveWork(String bookId, String yearPeriod, List<FixedAssetWorkItemDto> items) {
        String period = resolvePeriod(bookId, yearPeriod);
        if (CollUtil.isEmpty(items)) {
            return new Message<>(Message.SUCCESS, "保存成功");
        }
        for (FixedAssetWorkItemDto item : items) {
            if (StringUtils.isBlank(item.getAssetId())) {
                continue;
            }
            FixedAssetWork existing = workMapper.selectOne(Wrappers.<FixedAssetWork>lambdaQuery()
                    .eq(FixedAssetWork::getBookId, bookId)
                    .eq(FixedAssetWork::getAssetId, item.getAssetId())
                    .eq(FixedAssetWork::getYearPeriod, period));
            if (existing == null) {
                FixedAssetWork row = FixedAssetWork.builder()
                        .bookId(bookId)
                        .assetId(item.getAssetId())
                        .yearPeriod(period)
                        .periodWork(item.getPeriodWork() == null ? BigDecimal.ZERO : item.getPeriodWork())
                        .build();
                row.setId(identifierGenerator.nextId(row).toString());
                workMapper.insert(row);
            } else {
                existing.setPeriodWork(item.getPeriodWork() == null ? BigDecimal.ZERO : item.getPeriodWork());
                workMapper.updateById(existing);
            }
        }
        return new Message<>(Message.SUCCESS, "保存成功");
    }

    @Transactional
    public Message<FixedAssetAccrueResultVo> accrue(String bookId, FixedAssetAccrueDto dto) {
        if (dto == null) {
            dto = new FixedAssetAccrueDto();
        }
        String period = resolvePeriod(bookId, dto.getYearPeriod());
        String voucherWord = StringUtils.defaultIfBlank(dto.getVoucherWord(), DEFAULT_WORD);
        String summary = StringUtils.defaultIfBlank(dto.getSummary(), DEFAULT_SUMMARY);
        Date voucherDate = dto.getVoucherDate() != null ? dto.getVoucherDate() : lastDayOfPeriod(period);

        rollbackIfNeeded(bookId, period);

        List<FixedAsset> assets = fixedAssetMapper.selectList(Wrappers.<FixedAsset>lambdaQuery()
                .eq(FixedAsset::getBookId, bookId));
        Map<String, BigDecimal> workMap = loadWorkMap(bookId, period);

        List<String> missingWorkCodes = new ArrayList<>();
        List<CalcRow> calcRows = new ArrayList<>();
        for (FixedAsset asset : assets) {
            String startPeriod = asset.getStartUseDate() != null
                    ? FixedAssetDepreciationRules.periodOf(asset.getStartUseDate())
                    : asset.getEntryPeriod();
            if (!FixedAssetDepreciationRules.shouldAccrue(period, startPeriod, asset.getDisposedPeriod(),
                    asset.getSuspendedPeriod(), asset.getStatus())) {
                continue;
            }
            DepreciationMethod method = DepreciationMethod.from(asset.getDepreciationMethod());
            if (!method.isDepreciable()) {
                continue;
            }
            BigDecimal periodWork = workMap.get(asset.getId());
            if (method == DepreciationMethod.UNITS_OF_PRODUCTION) {
                if (periodWork == null) {
                    missingWorkCodes.add(asset.getCode());
                    continue;
                }
            }
            BigDecimal amount = calcAmount(asset, method, periodWork);
            if (amount.compareTo(BigDecimal.ZERO) <= 0) {
                continue;
            }
            if (StringUtils.isBlank(asset.getExpenseSubjectId()) || StringUtils.isBlank(asset.getAccumDeprSubjectId())) {
                throw new ServiceException(FixedAssetErrorCode.SUBJECT_REQUIRED);
            }
            calcRows.add(new CalcRow(asset, amount, method, periodWork));
        }
        if (!missingWorkCodes.isEmpty()) {
            throw new ServiceException(FixedAssetErrorCode.WORK_REQUIRED, String.join(",", missingWorkCodes));
        }
        if (calcRows.isEmpty()) {
            throw new ServiceException(FixedAssetErrorCode.NOTHING_TO_ACCRUE);
        }

        BigDecimal total = calcRows.stream().map(CalcRow::amount).reduce(BigDecimal.ZERO, BigDecimal::add);

        FixedAssetAccrual accrual = FixedAssetAccrual.builder()
                .bookId(bookId)
                .yearPeriod(period)
                .voucherDate(voucherDate)
                .voucherWord(voucherWord)
                .summary(summary)
                .totalAmount(total)
                .status(STATUS_ACCRUED)
                .build();
        accrual.setId(identifierGenerator.nextId(accrual).toString());
        accrualMapper.insert(accrual);

        for (CalcRow row : calcRows) {
            FixedAssetDepr depr = FixedAssetDepr.builder()
                    .bookId(bookId)
                    .assetId(row.asset().getId())
                    .yearPeriod(period)
                    .accrualId(accrual.getId())
                    .deprAmount(row.amount())
                    .expenseSubjectId(row.asset().getExpenseSubjectId())
                    .accumDeprSubjectId(row.asset().getAccumDeprSubjectId())
                    .deptId(row.asset().getDeptId())
                    .method(row.method().name())
                    .periodWork(row.periodWork())
                    .build();
            depr.setId(identifierGenerator.nextId(depr).toString());
            deprMapper.insert(depr);

            FixedAsset asset = row.asset();
            BigDecimal accum = asset.getAccumDepr() == null ? BigDecimal.ZERO : asset.getAccumDepr();
            BigDecimal yearDepr = asset.getYearDepr() == null ? BigDecimal.ZERO : asset.getYearDepr();
            int periods = asset.getDepreciatedPeriods() == null ? 0 : asset.getDepreciatedPeriods();
            asset.setAccumDepr(accum.add(row.amount()));
            asset.setYearDepr(yearDepr.add(row.amount()));
            asset.setDepreciatedPeriods(periods + 1);
            fixedAssetMapper.updateById(asset);
        }

        Book book = bookMapper.selectById(bookId);
        Message<String> voucherMsg = createVoucher(book, bookId, period, voucherDate, voucherWord, summary, total, calcRows);
        if (voucherMsg.getCode() != Message.SUCCESS) {
            throw new IllegalStateException(StringUtils.defaultIfBlank(voucherMsg.getMessage(), "折旧凭证生成失败"));
        }
        accrual.setVoucherId(voucherMsg.getData());
        accrualMapper.updateById(accrual);

        Voucher voucher = voucherService.getById(voucherMsg.getData());
        FixedAssetAccrueResultVo result = new FixedAssetAccrueResultVo();
        result.setVoucherId(voucherMsg.getData());
        result.setVoucherWord(VoucherUtils.displayWord(voucher));
        result.setTotalAmount(total);
        result.setYearPeriod(period);
        return Message.ok(result);
    }

    private void rollbackIfNeeded(String bookId, String period) {
        FixedAssetAccrual accrual = findAccrual(bookId, period);
        if (accrual == null) {
            return;
        }
        if (StringUtils.isBlank(accrual.getVoucherId())) {
            deprMapper.physicalDeleteByBookPeriod(bookId, period);
            accrualMapper.physicalDeleteById(accrual.getId());
            return;
        }
        Voucher voucher = voucherService.getById(accrual.getVoucherId());
        if (voucher != null
                && (VoucherStatusEnum.COMPLETED.getValue().equals(voucher.getStatus())
                || StringUtils.isNotBlank(voucher.getAuditMemberId())
                || StringUtils.isNotBlank(voucher.getSenderId()))) {
            throw new ServiceException(FixedAssetErrorCode.REACCRUE_FORBIDDEN);
        }
        if (voucher != null
                && VoucherStatusEnum.UNDER_REVIEW.getValue().equals(voucher.getStatus())) {
            // 删除仅允许暂存：先撤销提交再删
            Message<Integer> cancel = voucherService.cancelByIds(List.of(voucher.getId()), bookId);
            if (cancel.getCode() != Message.SUCCESS) {
                throw new ServiceException(FixedAssetErrorCode.REACCRUE_FORBIDDEN);
            }
            voucher = voucherService.getById(accrual.getVoucherId());
        }
        if (voucher != null) {
            Message<String> del = voucherService.delete(List.of(voucher.getId()), bookId);
            if (del.getCode() != Message.SUCCESS) {
                throw new ServiceException(FixedAssetErrorCode.REACCRUE_FORBIDDEN);
            }
        }
        List<FixedAssetDepr> deprs = deprMapper.selectList(Wrappers.<FixedAssetDepr>lambdaQuery()
                .eq(FixedAssetDepr::getBookId, bookId)
                .eq(FixedAssetDepr::getYearPeriod, period));
        for (FixedAssetDepr depr : deprs) {
            FixedAsset asset = fixedAssetMapper.selectById(depr.getAssetId());
            if (asset == null) {
                continue;
            }
            BigDecimal amount = depr.getDeprAmount() == null ? BigDecimal.ZERO : depr.getDeprAmount();
            BigDecimal accum = asset.getAccumDepr() == null ? BigDecimal.ZERO : asset.getAccumDepr();
            BigDecimal yearDepr = asset.getYearDepr() == null ? BigDecimal.ZERO : asset.getYearDepr();
            int periods = asset.getDepreciatedPeriods() == null ? 0 : asset.getDepreciatedPeriods();
            asset.setAccumDepr(accum.subtract(amount).max(BigDecimal.ZERO));
            asset.setYearDepr(yearDepr.subtract(amount).max(BigDecimal.ZERO));
            asset.setDepreciatedPeriods(Math.max(0, periods - 1));
            fixedAssetMapper.updateById(asset);
        }
        deprMapper.physicalDeleteByBookPeriod(bookId, period);
        accrualMapper.physicalDeleteById(accrual.getId());
    }

    private Message<String> createVoucher(Book book, String bookId, String period, Date voucherDate,
                                          String wordHead, String summary, BigDecimal total, List<CalcRow> rows) {
        int year = Integer.parseInt(period.split("-")[0]);
        int month = Integer.parseInt(period.split("-")[1]);
        Integer wordNum = voucherService.getAbleWordNum(bookId, wordHead, year, month).getData();

        Map<String, BigDecimal> debitByExpense = new LinkedHashMap<>();
        Map<String, BigDecimal> creditByAccum = new LinkedHashMap<>();
        for (CalcRow row : rows) {
            debitByExpense.merge(row.asset().getExpenseSubjectId(), row.amount(), BigDecimal::add);
            creditByAccum.merge(row.asset().getAccumDeprSubjectId(), row.amount(), BigDecimal::add);
        }

        List<VoucherItemChangeDto> items = new ArrayList<>();
        for (Map.Entry<String, BigDecimal> e : debitByExpense.entrySet()) {
            items.add(createItem(bookId, e.getKey(), summary, e.getValue(), true));
        }
        for (Map.Entry<String, BigDecimal> e : creditByAccum.entrySet()) {
            items.add(createItem(bookId, e.getKey(), summary, e.getValue(), false));
        }

        VoucherChangeDto voucherDto = new VoucherChangeDto();
        voucherDto.setWordHead(wordHead);
        voucherDto.setWordNum(wordNum);
        voucherDto.setBookId(bookId);
        voucherDto.setCompanyName(book != null ? book.getCompanyName() : "");
        voucherDto.setVoucherDate(voucherDate);
        voucherDto.setVoucherYear(year);
        voucherDto.setVoucherMonth(month);
        voucherDto.setDebitAmount(total);
        voucherDto.setCreditAmount(total);
        voucherDto.setReceiptNum(0);
        voucherDto.setRemark(summary);
        voucherDto.setStatus(VoucherStatusEnum.DRAFT.getValue());
        voucherDto.setItems(items);
        return voucherService.save(voucherDto);
    }

    private VoucherItemChangeDto createItem(String bookId, String subjectId, String summary,
                                            BigDecimal amount, boolean debit) {
        BookSubject subject = bookSubjectService.getById(subjectId);
        if (subject == null) {
            throw new ServiceException(FixedAssetErrorCode.SUBJECT_REQUIRED);
        }
        VoucherItemChangeDto item = new VoucherItemChangeDto();
        item.setSummary(summary);
        item.setSubjectId(subject.getId());
        item.setSubjectCode(subject.getCode());
        item.setSubjectName(subject.getCode() + "-" + subject.getName());
        item.setSubjectBalance(subject.getBalance());
        item.setAuxiliary(List.of());
        item.setDetailedAccounts("");
        if (debit) {
            item.setDebitAmount(amount);
            item.setCreditAmount(BigDecimal.ZERO);
        } else {
            item.setCreditAmount(amount);
            item.setDebitAmount(BigDecimal.ZERO);
        }
        return item;
    }

    private BigDecimal calcAmount(FixedAsset asset, DepreciationMethod method, BigDecimal periodWork) {
        if (!method.isDepreciable()) {
            return BigDecimal.ZERO.setScale(2);
        }
        BigDecimal residual = FixedAssetDepreciationRules.residualValue(asset.getOriginalValue(), asset.getResidualRate());
        BigDecimal base = FixedAssetDepreciationRules.depreciableBase(
                asset.getOriginalValue(), asset.getImpairment(), residual);
        BigDecimal accum = asset.getAccumDepr() == null ? BigDecimal.ZERO : asset.getAccumDepr();
        BigDecimal remaining = FixedAssetDepreciationRules.remainingDepreciable(base, accum);
        if (method.isAccelerated() && !FixedAssetDepreciationRules.isValidAcceleratedLife(asset.getUsefulLifeMonths())) {
            throw new ServiceException(FixedAssetErrorCode.ACCELERATED_LIFE_INVALID,
                    asset.getCode() == null ? "" : asset.getCode());
        }
        if (method == DepreciationMethod.UNITS_OF_PRODUCTION) {
            return FixedAssetDepreciationRules.unitsOfProductionAmount(
                    asset.getOriginalValue(), residual, asset.getExpectedTotalWork(), periodWork, remaining);
        }
        if (method == DepreciationMethod.DOUBLE_DECLINING) {
            return FixedAssetDepreciationRules.doubleDecliningAmount(
                    asset.getOriginalValue(), asset.getImpairment(), residual,
                    asset.getUsefulLifeMonths(), asset.getDepreciatedPeriods(), accum, remaining);
        }
        if (method == DepreciationMethod.SUM_OF_YEARS) {
            return FixedAssetDepreciationRules.sumOfYearsAmount(
                    asset.getOriginalValue(), asset.getImpairment(), residual,
                    asset.getUsefulLifeMonths(), asset.getDepreciatedPeriods(), remaining);
        }
        return FixedAssetDepreciationRules.straightLineAmount(
                asset.getOriginalValue(), residual, asset.getUsefulLifeMonths(),
                asset.getDepreciatedPeriods(), remaining);
    }

    private List<FixedAsset> loadAccruableUopAssets(String bookId, String period) {
        List<FixedAsset> assets = fixedAssetMapper.selectList(Wrappers.<FixedAsset>lambdaQuery()
                .eq(FixedAsset::getBookId, bookId)
                .eq(FixedAsset::getDepreciationMethod, DepreciationMethod.UNITS_OF_PRODUCTION.name()));
        return assets.stream().filter(asset -> {
            String startPeriod = asset.getStartUseDate() != null
                    ? FixedAssetDepreciationRules.periodOf(asset.getStartUseDate())
                    : asset.getEntryPeriod();
            return FixedAssetDepreciationRules.shouldAccrue(period, startPeriod, asset.getDisposedPeriod(),
                    asset.getSuspendedPeriod(), asset.getStatus());
        }).collect(Collectors.toList());
    }

    private Map<String, BigDecimal> loadWorkMap(String bookId, String period) {
        List<FixedAssetWork> works = workMapper.selectList(Wrappers.<FixedAssetWork>lambdaQuery()
                .eq(FixedAssetWork::getBookId, bookId)
                .eq(FixedAssetWork::getYearPeriod, period));
        Map<String, BigDecimal> map = new HashMap<>();
        for (FixedAssetWork w : works) {
            map.put(w.getAssetId(), w.getPeriodWork());
        }
        return map;
    }

    private FixedAssetAccrual findAccrual(String bookId, String period) {
        return accrualMapper.selectOne(Wrappers.<FixedAssetAccrual>lambdaQuery()
                .eq(FixedAssetAccrual::getBookId, bookId)
                .eq(FixedAssetAccrual::getYearPeriod, period));
    }

    private String resolvePeriod(String bookId, String yearPeriod) {
        return StringUtils.isNotBlank(yearPeriod) ? yearPeriod : configSysService.getCurrentTerm(bookId);
    }

    private Date lastDayOfPeriod(String period) {
        String last = DateUtils.lastDay(period + "-01").toString();
        return DateUtils.parse(last, DateUtils.FORMAT_DATE_YYYY_MM_DD);
    }

    private record CalcRow(FixedAsset asset, BigDecimal amount, DepreciationMethod method, BigDecimal periodWork) {
    }
}
