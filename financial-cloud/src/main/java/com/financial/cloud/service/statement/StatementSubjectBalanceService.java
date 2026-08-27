package com.financial.cloud.service.statement;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.core.incrementer.IdentifierGenerator;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import tools.jackson.core.JacksonException;
import tools.jackson.core.type.TypeReference;
import tools.jackson.databind.json.JsonMapper;
import com.financial.cloud.common.Message;
import com.financial.cloud.common.SubjectAuxiliary;
import com.financial.cloud.domain.book.AssistAcc;
import com.financial.cloud.domain.book.BookSubject;
import com.financial.cloud.domain.book.Settlement;
import com.financial.cloud.domain.statement.StatementSubjectBalance;
import com.financial.cloud.domain.voucher.VoucherAuxiliary;
import com.financial.cloud.domain.voucher.VoucherItem;
import com.financial.cloud.enums.statement.StatementPeriodTypeEnum;
import com.financial.cloud.enums.statement.StatementSymbolEnum;
import com.financial.cloud.enums.book.SubjectDirectionEnum;
import com.financial.cloud.enums.common.YesNoEnum;
import com.financial.cloud.repository.book.AssistAccMapper;
import com.financial.cloud.repository.book.BookSubjectMapper;
import com.financial.cloud.repository.statement.StatementSubjectBalanceMapper;
import com.baomidou.mybatisplus.extension.toolkit.Db;
import com.financial.cloud.repository.voucher.VoucherItemMapper;
import com.financial.cloud.service.config.ConfigSysService;
import com.financial.cloud.service.statement.StatementSubjectBalanceService;
import com.financial.cloud.util.SubjectBalanceAncestors;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.*;
import java.util.stream.Collectors;

@RequiredArgsConstructor
@Service
@Slf4j
public class StatementSubjectBalanceService{

    private final ConfigSysService configSysService;
    private final StatementSubjectBalanceMapper subjectBalanceMapper;
    private final BookSubjectMapper bookSubjectMapper;
    private final AssistAccMapper assistAccMapper;
    private final IdentifierGenerator identifierGenerator;
    private final VoucherItemMapper voucherItemMapper;
    private final JsonMapper jsonMapper;
    public StatementSubjectBalance getSubjectBalance(String bookId, String subjectCode) {
        LambdaQueryWrapper<StatementSubjectBalance> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(StatementSubjectBalance::getBookId, bookId);
        queryWrapper.eq(StatementSubjectBalance::getSubjectCode, subjectCode);
        queryWrapper.eq(StatementSubjectBalance::getYearPeriod, configSysService.getCurrentTerm(bookId));
        return subjectBalanceMapper.selectOne(queryWrapper);
    }
    public Message<StatementSubjectBalance> getSubjectBalance(StatementSubjectBalance params) {
        LambdaQueryWrapper<StatementSubjectBalance> lqw = Wrappers.lambdaQuery();
        lqw.eq(StatementSubjectBalance::getBookId, params.getBookId());
        lqw.eq(StringUtils.isNotBlank(params.getSubjectCode()), StatementSubjectBalance::getSubjectCode, params.getSubjectCode());
        lqw.eq(StringUtils.isNotBlank(params.getSourceId()), StatementSubjectBalance::getSourceId, params.getSourceId());
        lqw.eq(StringUtils.isNotBlank(params.getYearPeriod()), StatementSubjectBalance::getYearPeriod, params.getYearPeriod());
        return Message.ok(subjectBalanceMapper.selectOne(lqw));
    }

    /**
     * 判断科目是否有凭证
     *
     * @param bookId     账簿
     * @param subjectIds 科目id
     */
    public boolean hasVoucher(String bookId, List<String> subjectIds) {
        LambdaQueryWrapper<StatementSubjectBalance> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(StatementSubjectBalance::getBookId, bookId);
        queryWrapper.eq(StatementSubjectBalance::getIsVoucher, YesNoEnum.y.name());
        queryWrapper.in(StatementSubjectBalance::getSourceId, subjectIds);
        return subjectBalanceMapper.selectCount(queryWrapper) > 0;
    }

    /**
     * 判断科目是否有凭证
     *
     * @param bookId 账簿
     * @param codes  科目code
     */
    public boolean hasVoucherByCodes(String bookId, List<String> codes) {
        return !selectIsVoucherByCode(bookId, codes).isEmpty();
    }

    /**
     * 查询科目是否有凭证
     *
     * @param bookId 账簿
     * @param codes  科目code
     */
    public List<StatementSubjectBalance> selectIsVoucherByCode(String bookId, List<String> codes) {
        LambdaQueryWrapper<StatementSubjectBalance> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(StatementSubjectBalance::getBookId, bookId);
        queryWrapper.eq(StatementSubjectBalance::getIsVoucher, YesNoEnum.y.name());
        queryWrapper.in(StatementSubjectBalance::getSubjectCode, codes);
        queryWrapper.select(StatementSubjectBalance::getSourceId, StatementSubjectBalance::getIsVoucher, StatementSubjectBalance::getSubjectCode);
        return subjectBalanceMapper.selectList(queryWrapper);
    }
    public void save(BookSubject subject) {
        String currentTerm = configSysService.getCurrentTerm(subject.getBookId());
        StatementSubjectBalance subjectBalance = create(subject, currentTerm);
        subjectBalanceMapper.insert(subjectBalance);
    }
    public void update(BookSubject subject) {
        LambdaUpdateWrapper<StatementSubjectBalance> updateWrapper = new LambdaUpdateWrapper<>();
        updateWrapper.eq(StatementSubjectBalance::getSourceId, subject.getId());
        updateWrapper.set(StringUtils.isNotBlank(subject.getName()), StatementSubjectBalance::getSubjectName, subject.getName());
        updateWrapper.set(StringUtils.isNotBlank(subject.getCode()), StatementSubjectBalance::getSubjectCode, subject.getCode());
        updateWrapper.set(StringUtils.isNotBlank(subject.getParentId()), StatementSubjectBalance::getParentId, subject.getParentId());
        updateWrapper.set(StringUtils.isNotBlank(subject.getBookId()), StatementSubjectBalance::getBookId, subject.getBookId());
        subjectBalanceMapper.update(updateWrapper);
    }

    /**
     * 更新科目余额
     */
    @Transactional
    public void update(BookSubject bookSubject, BigDecimal balance,
                       StatementSymbolEnum symbol, SubjectDirectionEnum direction,
                       List<VoucherAuxiliary> auxiliaries,
                       String yearPeriod) {
        String bookId = bookSubject.getBookId();
        String subjectId = bookSubject.getId();
        String code = bookSubject.getCode();
        String name = bookSubject.getName();
        String auxiliaryCfg = bookSubject.getAuxiliary();

        List<SubjectAuxiliary> auxiliaryCfgList = new ArrayList<>();

        // 对于辅助项，则获取辅助项配置，更新编号、名称
        if (StringUtils.isNotBlank(auxiliaryCfg)) {
            try {
                auxiliaryCfgList = jsonMapper.readValue(auxiliaryCfg, new TypeReference<>() {
                });
                if (!auxiliaryCfgList.isEmpty()) {
                    // 按"value"升序排序
                    auxiliaryCfgList.sort((o1, o2) -> {
                        String value1 = o1.getValue();
                        String value2 = o2.getValue();
                        return value1.compareTo(value2);
                    });
                    // 匹配辅助项
                    List<String> auxiliaryIds = new ArrayList<>();
                    Map<String, VoucherAuxiliary> auxiliaryMap = auxiliaries.stream()
                            .peek(item -> auxiliaryIds.add(item.getItemId()))
                            .collect(Collectors.toMap(VoucherAuxiliary::getAuxiliary, item -> item));
                    if (!auxiliaryIds.isEmpty()) {
                        List<AssistAcc> assistAccs = assistAccMapper.selectByIds(auxiliaryIds);
                        assistAccs.forEach(item -> {
                            auxiliaryMap.get(item.getAssistType()).setItemCode(item.getAssistCode());
                        });
                    }

                    for (SubjectAuxiliary aux : auxiliaryCfgList) {
                        if (auxiliaryMap.containsKey(aux.getValue())) {
                            String itemCode = auxiliaryMap.get(aux.getValue()).getItemCode();
                            String itemName = auxiliaryMap.get(aux.getValue()).getItemName();
                            code = "%s_%s".formatted(code, itemCode);
                            name = "%s_%s".formatted(name, itemName);
                        }
                    }
                }
            } catch (JacksonException e) {
                throw new RuntimeException(e);
            }
        }

        LambdaQueryWrapper<StatementSubjectBalance> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(StatementSubjectBalance::getBookId, bookId);
        queryWrapper.eq(StatementSubjectBalance::getSubjectCode, code);
        queryWrapper.eq(StatementSubjectBalance::getYearPeriod, yearPeriod);
        queryWrapper.eq(StatementSubjectBalance::getPeriodType, StatementPeriodTypeEnum.MONTH.getValue());
        StatementSubjectBalance subjectBalance = subjectBalanceMapper.selectOne(queryWrapper);
        // 未查到，创建新记录
        if (subjectBalance == null) {
            // 新增当前科目
            BookSubject subject = new BookSubject();
            subject.setBookId(bookId);
            subject.setName(name);
            subject.setCode(code);
            subject.setDirection(bookSubject.getDirection());
            subjectBalance = create(subject, yearPeriod);
            String currentId = identifierGenerator.nextId(subjectBalance).toString();
            subjectBalance.setId(currentId);
            if (auxiliaryCfgList.isEmpty()) {
                subjectBalance.setIsAuxiliary(YesNoEnum.n.name());
                subjectBalance.setParentId(bookSubject.getParentId());
                subjectBalance.setSourceId(subjectId);
            } else {
                subjectBalance.setIsAuxiliary(YesNoEnum.y.name());
                subjectBalance.setParentId(subjectId);
                subjectBalance.setSourceId(identifierGenerator.nextId(subject).toString());
            }
            subjectBalanceMapper.insert(subjectBalance);

            // 查找是否存在（忽略 idPath 前导 / 产生的空段，避免匹配 source_id='' 脏行）
            List<String> subjectParentIds = SubjectBalanceAncestors.sourceIdsFromIdPath(bookSubject.getIdPath());
            if (!subjectParentIds.isEmpty()) {
                queryWrapper = new LambdaQueryWrapper<>();
                queryWrapper.eq(StatementSubjectBalance::getBookId, bookId);
                queryWrapper.eq(StatementSubjectBalance::getYearPeriod, yearPeriod);
                queryWrapper.in(StatementSubjectBalance::getSourceId, subjectParentIds);
                queryWrapper.select(StatementSubjectBalance::getSourceId);
                List<String> existSubjectIds = subjectBalanceMapper.selectList(queryWrapper)
                        .stream()
                        .map(StatementSubjectBalance::getSourceId)
                        .toList();
                // 去重
                subjectParentIds = subjectParentIds.stream().filter(id -> !existSubjectIds.contains(id)).toList();
            }
            if (!subjectParentIds.isEmpty()) {
                LambdaQueryWrapper<BookSubject> lqwSubject = new LambdaQueryWrapper<>();
                lqwSubject.eq(BookSubject::getBookId, bookId);
                lqwSubject.in(BookSubject::getId, subjectParentIds);
                List<BookSubject> insertSubjects = bookSubjectMapper.selectList(lqwSubject);
                for (BookSubject insertSubject : insertSubjects) {
                    StatementSubjectBalance parentBalance = create(insertSubject, yearPeriod);
                    parentBalance.setIsVoucher(YesNoEnum.y.name());
                    String parentId = identifierGenerator.nextId(parentBalance).toString();
                    parentBalance.setId(parentId);
                    subjectBalanceMapper.insert(parentBalance);
                }
            }
        }

        // 获取包括父级所有科目,都需要同步余额
        StatementSubjectBalance finalSubjectBalance = subjectBalance;
        List<String> subjectParentIds = SubjectBalanceAncestors.ancestorSourceIds(
                bookSubject.getIdPath(), finalSubjectBalance.getSourceId());
        List<StatementSubjectBalance> subjectBalances;
        if (subjectParentIds.isEmpty()) {
            subjectBalances = new ArrayList<>();
        } else {
            queryWrapper = new LambdaQueryWrapper<>();
            queryWrapper.eq(StatementSubjectBalance::getBookId, bookId);
            queryWrapper.eq(StatementSubjectBalance::getYearPeriod, yearPeriod);
            queryWrapper.in(StatementSubjectBalance::getSourceId, subjectParentIds);
            subjectBalances = subjectBalanceMapper.selectList(queryWrapper);
        }

        // 查询同级科目凭证，用于判定记录是否存在凭证
        LambdaQueryWrapper<VoucherItem> itemLqw = Wrappers.lambdaQuery();
        itemLqw.eq(VoucherItem::getBookId, bookId);
        itemLqw.likeRight(VoucherItem::getVoucherDate, yearPeriod);
        itemLqw.eq(VoucherItem::getSubjectCode, subjectBalance.getSubjectCode());

        boolean hasVoucher = voucherItemMapper.selectCount(itemLqw) > 0;
        boolean hasVoucherParent = true;
        if (bookSubject.getLevel() > 1) {
            // 查找父级下的所有数据
            itemLqw = Wrappers.lambdaQuery();
            itemLqw.eq(VoucherItem::getBookId, bookId);
            itemLqw.likeRight(VoucherItem::getVoucherDate, yearPeriod);
            StatementSubjectBalance parentSubjectBalance = subjectBalances.stream()
                    .filter(item -> item.getSourceId().equals(bookSubject.getParentId()))
                    .toList().get(0);
            itemLqw.likeRight(VoucherItem::getSubjectCode, parentSubjectBalance.getSubjectCode());
            hasVoucherParent = voucherItemMapper.selectCount(itemLqw) > 0;
        }

        subjectBalances.add(subjectBalance);
        // 更新余额
        for (StatementSubjectBalance statementSubjectBalance : subjectBalances) {
            switch (direction) {
                // 借方
                case DEBIT:
                    switch (symbol) {
                        case PLUS:
                            statementSubjectBalance.setBalance(statementSubjectBalance.getBalance().add(balance));
                            statementSubjectBalance.setCurrentPeriodDebit(statementSubjectBalance.getCurrentPeriodDebit().add(balance));
                            statementSubjectBalance.setYearToDateDebit(statementSubjectBalance.getYearToDateDebit().add(balance));
//                            statementSubjectBalance.setClosingBalanceDebit(statementSubjectBalance.getClosingBalanceDebit().add(balance));
                            break;
                        case MINUS:
                            statementSubjectBalance.setBalance(statementSubjectBalance.getBalance().subtract(balance));
                            statementSubjectBalance.setCurrentPeriodDebit(statementSubjectBalance.getCurrentPeriodDebit().subtract(balance));
                            statementSubjectBalance.setYearToDateDebit(statementSubjectBalance.getYearToDateDebit().subtract(balance));
//                            statementSubjectBalance.setClosingBalanceDebit(statementSubjectBalance.getClosingBalanceDebit().subtract(balance));
                            break;
                        default:
                            break;
                    }
                    break;
                case CREDIT:
                    switch (symbol) {
                        case PLUS:
                            statementSubjectBalance.setBalance(statementSubjectBalance.getBalance().add(balance));
                            statementSubjectBalance.setCurrentPeriodCredit(statementSubjectBalance.getCurrentPeriodCredit().subtract(balance));
                            statementSubjectBalance.setYearToDateCredit(statementSubjectBalance.getYearToDateCredit().subtract(balance));
//                            statementSubjectBalance.setClosingBalanceCredit(statementSubjectBalance.getClosingBalanceCredit().subtract(balance));
                            break;
                        case MINUS:
                            statementSubjectBalance.setBalance(statementSubjectBalance.getBalance().subtract(balance));
                            statementSubjectBalance.setCurrentPeriodCredit(statementSubjectBalance.getCurrentPeriodCredit().add(balance));
                            statementSubjectBalance.setYearToDateCredit(statementSubjectBalance.getYearToDateCredit().add(balance));
//                            statementSubjectBalance.setClosingBalanceCredit(statementSubjectBalance.getClosingBalanceCredit().add(balance));
                            break;
                        default:
                            break;
                    }
                default:
                    break;
            }

            // 期末余额，根据借贷方向
            statementSubjectBalance.setClosingBalanceCredit(BigDecimal.ZERO);
            statementSubjectBalance.setClosingBalanceDebit(BigDecimal.ZERO);
            if (SubjectDirectionEnum.DEBIT.getValue().equals(statementSubjectBalance.getDirection())) {
                // 借方公式：期初借方 + 本期借方 - 期初贷方 - 本期贷方
                statementSubjectBalance.setClosingBalanceDebit(
                        statementSubjectBalance.getOpeningBalanceDebit()
                                .add(statementSubjectBalance.getCurrentPeriodDebit())
                                .subtract(statementSubjectBalance.getOpeningBalanceCredit())
                                .subtract(statementSubjectBalance.getCurrentPeriodCredit())
                );
            } else {
                // 贷方公式：期初贷方 + 本期贷方 - 期初借方 - 本期借方
                statementSubjectBalance.setClosingBalanceCredit(
                        statementSubjectBalance.getOpeningBalanceCredit()
                                .add(statementSubjectBalance.getCurrentPeriodCredit())
                                .subtract(statementSubjectBalance.getOpeningBalanceDebit())
                                .subtract(statementSubjectBalance.getCurrentPeriodDebit())
                );
            }

            // 核查该科目是否存在凭证，并更新状态
            if (statementSubjectBalance.getId().equals(subjectBalance.getId())) {
                if (hasVoucher) {
                    statementSubjectBalance.setIsVoucher(YesNoEnum.y.name());
                } else {
                    statementSubjectBalance.setIsVoucher(YesNoEnum.n.name());
                }
            } else {
                if (hasVoucherParent) {
                    statementSubjectBalance.setIsVoucher(YesNoEnum.y.name());
                } else {
                    statementSubjectBalance.setIsVoucher(YesNoEnum.n.name());
                }
            }

            subjectBalanceMapper.updateById(statementSubjectBalance);
        }
    }
    public void delete(List<String> listIds) {
        LambdaQueryWrapper<StatementSubjectBalance> wrapper = new LambdaQueryWrapper<>();
        wrapper.in(StatementSubjectBalance::getSourceId, listIds);
        subjectBalanceMapper.delete(wrapper);
    }

    /**
     * 创建实体类
     *
     * @param subject     科目对象
     * @param currentTerm 当前期数
     * @return 结果
     */
    public StatementSubjectBalance create(BookSubject subject, String currentTerm) {
        return StatementSubjectBalance.builder()
                .bookId(subject.getBookId())
                .sourceId(subject.getId())
                .yearPeriod(currentTerm)
                .periodType(StatementPeriodTypeEnum.MONTH.getValue())
                .subjectCode(subject.getCode())
                .parentId(subject.getParentId())
                .subjectName(subject.getName())
                .direction(subject.getDirection())
                .sortIndex(1)
                .balance(BigDecimal.ZERO)
                .openingBalanceCredit(BigDecimal.ZERO)
                .openingBalanceDebit(BigDecimal.ZERO)
                .openingYearBalanceDebit(BigDecimal.ZERO)
                .openingYearBalanceCredit(BigDecimal.ZERO)
                .currentPeriodCredit(BigDecimal.ZERO)
                .currentPeriodDebit(BigDecimal.ZERO)
                .yearToDateCredit(BigDecimal.ZERO)
                .yearToDateDebit(BigDecimal.ZERO)
                .closingBalanceCredit(BigDecimal.ZERO)
                .closingBalanceDebit(BigDecimal.ZERO)
                .prevBalance(BigDecimal.ZERO)
                .prevClosingBalanceCredit(BigDecimal.ZERO)
                .prevClosingBalanceDebit(BigDecimal.ZERO)
                .prevYearToDateCredit(BigDecimal.ZERO)
                .prevYearToDateDebit(BigDecimal.ZERO)
                .isAuxiliary(YesNoEnum.n.name())
                .isVoucher(YesNoEnum.n.name())
                .build();
    }
    public boolean deleteByBookIds(List<String> bookIds) {
        LambdaQueryWrapper<StatementSubjectBalance> wrapper = new LambdaQueryWrapper<>();
        wrapper.in(StatementSubjectBalance::getBookId, bookIds);
        subjectBalanceMapper.delete(wrapper);
        return true;
    }
    @Transactional
    public boolean checkout(Settlement dto) {
        LambdaQueryWrapper<StatementSubjectBalance> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(StatementSubjectBalance::getBookId, dto.getBookId());
        queryWrapper.eq(StatementSubjectBalance::getYearPeriod, dto.getCurrentTerm());
        queryWrapper.eq(StatementSubjectBalance::getPeriodType, StatementPeriodTypeEnum.MONTH.getValue());
        List<StatementSubjectBalance> subjectBalanceList = subjectBalanceMapper.selectList(queryWrapper);
        if (subjectBalanceList.isEmpty()) {
            log.warn("账套：{}，期间：{}，科目余额为空，请先创建科目余额", dto.getBookId(), dto.getCurrentTerm());
            return false;
        }

        for (StatementSubjectBalance statementSubjectBalance : subjectBalanceList) {
            statementSubjectBalance.setId(null);
            statementSubjectBalance.setYearPeriod(dto.getNextTerm());
            // 期初余额
            statementSubjectBalance.setOpeningBalanceDebit(statementSubjectBalance.getClosingBalanceDebit());
            statementSubjectBalance.setOpeningBalanceCredit(statementSubjectBalance.getClosingBalanceCredit());
            // 当前
            statementSubjectBalance.setCurrentPeriodDebit(BigDecimal.ZERO);
            statementSubjectBalance.setCurrentPeriodCredit(BigDecimal.ZERO);
            //本年累计 01月清除本年累计
            if (dto.getNextTerm().endsWith("01")) {
                statementSubjectBalance.setYearToDateDebit(BigDecimal.ZERO);
                statementSubjectBalance.setYearToDateCredit(BigDecimal.ZERO);
            }
            //上月期末余额
            statementSubjectBalance.setPrevBalance(statementSubjectBalance.getBalance());
            //上月期末借贷余额
            statementSubjectBalance.setPrevClosingBalanceDebit(statementSubjectBalance.getPrevClosingBalanceDebit());
            statementSubjectBalance.setPrevClosingBalanceCredit(statementSubjectBalance.getPrevClosingBalanceCredit());
            //上月期末年度累计
            statementSubjectBalance.setPrevYearToDateDebit(statementSubjectBalance.getYearToDateDebit());
            statementSubjectBalance.setPrevYearToDateCredit(statementSubjectBalance.getYearToDateCredit());
            //状态
            statementSubjectBalance.setIsVoucher(YesNoEnum.n.name());
            String currentId = identifierGenerator.nextId(statementSubjectBalance).toString();
            statementSubjectBalance.setId(currentId);
        }
        Db.saveBatch(subjectBalanceList);

        return true;
    }
	public List<StatementSubjectBalance> selectSubjectBalance(String bookId, List<String> subjectCodes) {
		String currentTerm = configSysService.getCurrentTerm(bookId);
		 LambdaQueryWrapper<StatementSubjectBalance> queryWrapper = new LambdaQueryWrapper<>();
		 queryWrapper.eq(StatementSubjectBalance::getBookId, bookId);
		 queryWrapper.eq(StatementSubjectBalance::getYearPeriod, currentTerm);
		 queryWrapper.eq(StatementSubjectBalance::getPeriodType, StatementPeriodTypeEnum.MONTH.getValue());
		 queryWrapper.in(CollectionUtils.isNotEmpty(subjectCodes),StatementSubjectBalance::getSubjectCode, subjectCodes);
		 return subjectBalanceMapper.selectList(queryWrapper);
	}
}
