package com.financial.cloud.service.voucher;

import com.financial.cloud.repository.hr.EmployeeSalarySummaryMapper;
import com.financial.cloud.repository.standard.StandardSubjectCashFlowMapper;
import com.financial.cloud.repository.book.BookMapper;
import com.financial.cloud.repository.idm.UserInfoMapper;
import cn.hutool.core.bean.BeanUtil;
import cn.hutool.core.collection.CollUtil;
import com.baomidou.mybatisplus.extension.toolkit.Db;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.core.incrementer.IdentifierGenerator;
import com.baomidou.mybatisplus.core.toolkit.ObjectUtils;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.financial.cloud.common.Message;
import com.financial.cloud.domain.book.Book;
import com.financial.cloud.domain.book.BookSubject;
import com.financial.cloud.domain.statement.StatementSubjectBalance;
import com.financial.cloud.domain.idm.UserInfo;
import com.financial.cloud.domain.voucher.*;
import com.financial.cloud.dto.voucher.*;
import com.financial.cloud.dto.voucher.VoucherItemVo;
import com.financial.cloud.dto.voucher.VoucherSuccessiveDto;
import com.financial.cloud.dto.voucher.VoucherVo;
import com.financial.cloud.repository.voucher.VoucherMapper;
import com.financial.cloud.repository.voucher.VoucherItemMapper;
import com.financial.cloud.repository.voucher.VoucherWordMapper;
import com.financial.cloud.repository.voucher.VoucherItemAuxiliaryMapper;
import com.financial.cloud.repository.voucher.VoucherItemCashFlowMapper;
import com.financial.cloud.enums.book.SubjectDirectionEnum;
import com.financial.cloud.enums.common.YesNoEnum;
import com.financial.cloud.enums.error.VoucherErrorCode;
import com.financial.cloud.enums.statement.StatementSymbolEnum;
import com.financial.cloud.enums.voucher.VoucherReviewedOnOffEnum;
import com.financial.cloud.enums.voucher.VoucherStatusEnum;
import com.financial.cloud.enums.voucher.VoucherSuccessiveMethodEnum;
import com.financial.cloud.exception.ServiceException;
import com.financial.cloud.service.config.ConfigSysService;
import com.financial.cloud.service.statement.StatementSubjectBalanceService;
import com.financial.cloud.service.voucher.VoucherService;
import com.financial.cloud.service.book.BookSubjectService;
import com.financial.cloud.util.DateUtils;
import com.financial.cloud.util.SubjectDisplayNameUtils;
import com.financial.cloud.util.VoucherUtils;
import com.financial.cloud.util.excel.ExcelExporter;
import com.financial.cloud.util.excel.ExcelParams;
import com.financial.cloud.util.excel.ExportTemplateFiles;
import jakarta.servlet.http.HttpServletResponse;
import lombok.*;

import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.BeanUtils;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.File;
import java.io.IOException;
import java.math.BigDecimal;
import java.util.*;
import java.util.stream.Collectors;


@RequiredArgsConstructor
@Service
public class VoucherService extends ServiceImpl<VoucherMapper, Voucher>{

    private final IdentifierGenerator identifierGenerator;
    private final VoucherItemMapper voucherItemMapper;
    private final VoucherWordMapper voucherWordMapper;
    private final VoucherItemAuxiliaryMapper voucherItemAuxiliaryMapper;
    private final UserInfoMapper userInfoMapper;
    private final BookSubjectService bookSubjectService;
    private final StatementSubjectBalanceService subjectBalanceService;
    private final BookMapper bookMapper;
    private final ConfigSysService configSysService;
    private final StandardSubjectCashFlowMapper standardSubjectCashFlowMapper;
    private final VoucherItemCashFlowMapper voucherItemCashFlowMapper;
    private final EmployeeSalarySummaryMapper employeeSalarySummaryMapper;
    public Message<Page<VoucherItemVo>> subLedger(VoucherItemPageDto paramsDto) {
        paramsDto.parse();
        return Message.ok(voucherItemMapper.subLedgerPage(paramsDto.build(), paramsDto));
    }
    public Message<Page<VoucherItemVo>> fetchByCashFlow(VoucherItemPageDto paramsDto) {
        return Message.ok(voucherItemMapper.fetchByCashFlow(paramsDto.build(), paramsDto));
    }
    public Message<List<VoucherSuccessiveDto>> checkSuccessive(VoucherSuccessiveQueryDto query) {
        // 当前期
        String currentTerm = configSysService.getCurrentTerm(query.getBookId());
        Integer year = Integer.valueOf(currentTerm.substring(0, 4));
        Integer month = Integer.valueOf(currentTerm.substring(5, 7));
        // 查询所有符合的凭证
        LambdaQueryWrapper<Voucher> lqw = Wrappers.lambdaQuery();
        lqw.eq(Voucher::getBookId, query.getBookId());
        lqw.eq(Voucher::getWordHead, query.getWordHead());
        lqw.eq(Voucher::getVoucherYear, year);
        lqw.eq(Voucher::getVoucherMonth, month);
        lqw.ge(Voucher::getWordNum, query.getStartWordNumber());
        // 状态设定
        List<String> statusList = new ArrayList<>();
        statusList.add(VoucherStatusEnum.COMPLETED.getValue());
        if (query.getNullify()) {
            statusList.add(VoucherStatusEnum.CANCELLED.getValue());
        }
        lqw.in(Voucher::getStatus, statusList);
        lqw.orderByAsc(Voucher::getVoucherDate, Voucher::getWordNum);
        lqw.select(Voucher::getId, Voucher::getWordNum, Voucher::getWord, Voucher::getWordHead,
                Voucher::getVoucherDate, Voucher::getVoucherYear, Voucher::getVoucherMonth);
        List<Voucher> vouchers = baseMapper.selectList(lqw);
        if (vouchers.isEmpty()) {
            return Message.ok(new ArrayList<>());
        }

        List<VoucherSuccessiveDto> resList = new ArrayList<>();
        int currentNum = query.getStartWordNumber();
        boolean isData = false;
        for (Voucher voucher : vouchers) {
            VoucherSuccessiveDto voucherSuccessiveDto = VoucherSuccessiveDto.builder().build();
            BeanUtils.copyProperties(voucher, voucherSuccessiveDto);
            String sourceWord = VoucherUtils.createWord(voucher.getWordHead(), voucher.getWordNum());
            voucherSuccessiveDto.setSourceWord(sourceWord);

            // 两种方式 1.顺序补齐 2.按日期补齐
            if (VoucherSuccessiveMethodEnum.sequential.name().equals(query.getSuccessiveMethod())) {
                if (!voucher.getWordNum().equals(currentNum)) {
                    isData = true;
                    voucherSuccessiveDto.setWordNum(currentNum);
                    resList.add(voucherSuccessiveDto);
                }
            } else {
                if (!voucher.getWordNum().equals(currentNum)) {
                    isData = true;
                }
                voucherSuccessiveDto.setWordNum(currentNum);
                resList.add(voucherSuccessiveDto);
            }
            String targetWord = VoucherUtils.createWord(voucher.getWordHead(), voucherSuccessiveDto.getWordNum());
            voucherSuccessiveDto.setTargetWord(targetWord);
            currentNum++;
        }
        if (!isData) {
            resList.clear();
        }
        return Message.ok(resList);
    }
    public Message<List<VoucherSuccessiveDto>> checkSuccessiveAll(String bookId) {
        List<VoucherSuccessiveDto> data = new ArrayList<>();
        for (String wordHead : VoucherSuccessiveQueryDto.WORD_HEADS) {
            VoucherSuccessiveQueryDto queryDto = VoucherSuccessiveQueryDto.builder()
                    .wordHead(wordHead)
                    .successiveMethod(VoucherSuccessiveMethodEnum.sequential.name())
                    .bookId(bookId)
                    .nullify(true)
                    .startWordNumber(1)
                    .build();
            Message<List<VoucherSuccessiveDto>> successiveRes = checkSuccessive(queryDto);
            data.addAll(successiveRes.getData());
        }
        return Message.ok(data);
    }

    /**
     * 更新凭证号
     *
     * @param dtos 更新凭证号
     */

    @Transactional
    public Message<Void> updateSuccessive(List<VoucherSuccessiveDto> dtos) {
        Map<String, VoucherSuccessiveDto> maxWordNumMap = new HashMap<>();
        for (VoucherSuccessiveDto dto : dtos) {
            LambdaUpdateWrapper<Voucher> luw = Wrappers.lambdaUpdate();
            luw.eq(Voucher::getId, dto.getId());
            luw.eq(Voucher::getBookId, dto.getBookId());
            luw.set(Voucher::getWordNum, dto.getWordNum());
            luw.set(Voucher::getWord, dto.getTargetWord());
            baseMapper.update(null, luw);

            LambdaQueryWrapper<VoucherWord> wordLqw = Wrappers.lambdaQuery();
            wordLqw.eq(VoucherWord::getBookId, dto.getBookId());
            wordLqw.eq(VoucherWord::getWordHead, dto.getWordHead());
            wordLqw.eq(VoucherWord::getWordYear, dto.getVoucherYear());
            wordLqw.eq(VoucherWord::getWordMonth, dto.getVoucherMonth());
            wordLqw.eq(VoucherWord::getWordNum, dto.getWordNum());
            List<VoucherWord> voucherWords = voucherWordMapper.selectList(wordLqw);
            if (voucherWords.isEmpty()) {
                // 更新最新的凭证字以便后续使用
                VoucherWord nextWord = VoucherWord.builder()
                        .bookId(dto.getBookId())
                        .wordNum(dto.getWordNum())
                        .wordYear(dto.getVoucherYear())
                        .wordMonth(dto.getVoucherMonth())
                        .wordHead(dto.getWordHead())
                        .word(dto.getTargetWord())
                        .printTitle(dto.getTargetWord())
                        .build();
                nextWord.setId(identifierGenerator.nextId(nextWord).toString());
                voucherWordMapper.insert(nextWord);
            }

            // 根据凭证字头找出最大凭证字号
            if (maxWordNumMap.containsKey(dto.getWordHead())) {
                VoucherSuccessiveDto maxWordNumDto = maxWordNumMap.get(dto.getWordHead());
                if (maxWordNumDto.getWordNum() < dto.getWordNum()) {
                    maxWordNumMap.put(dto.getWordHead(), dto);
                }
            } else {
                maxWordNumMap.put(dto.getWordHead(), dto);
            }
        }

        // 移除多余的凭证字
        maxWordNumMap.forEach((wordHead, dto) -> {
            LambdaQueryWrapper<VoucherWord> wordLqw = Wrappers.lambdaQuery();
            wordLqw.eq(VoucherWord::getBookId, dto.getBookId());
            wordLqw.eq(VoucherWord::getWordHead, dto.getWordHead());
            wordLqw.eq(VoucherWord::getWordYear, dto.getVoucherYear());
            wordLqw.eq(VoucherWord::getWordMonth, dto.getVoucherMonth());
            wordLqw.gt(VoucherWord::getWordNum, dto.getWordNum());
            voucherWordMapper.delete(wordLqw);
        });
        return Message.ok(null);
    }

    /**
     * 生成一个可用凭证子号
     *
     * @param head 字头
     * @param year 年份
     * @return 新的可用字号
     */
    public Message<Integer> getAbleWordNum(String bookId, String head, Integer year, Integer month) {
        if (year == null) {
            year = Integer.valueOf(DateUtils.format(new Date(), "yyyy"));
        }
        if (month == null) {
            month = Integer.valueOf(DateUtils.format(new Date(), "MM"));
        }
        Integer latestWordNum = getLatestWordNum(bookId, head, year, month);
        if (latestWordNum == null) {
            return new Message<>(Message.SUCCESS, 1);
        } else {
            return new Message<>(Message.SUCCESS, latestWordNum + 1);
        }
    }

    /**
     * 根据ID查询
     *
     * @param id 主键
     * @return 结果
     */
    public Message<VoucherVo> queryById(String id) {
        Voucher voucher = baseMapper.selectById(id);
        if (voucher == null) {
            return new Message<>(Message.FAIL, "查询对象不存在");
        }
        VoucherVo booksVoucherVo = BeanUtil.copyProperties(voucher, VoucherVo.class);
        normalizeDisplayWord(booksVoucherVo);
        UserInfo userInfo = userInfoMapper.selectById(booksVoucherVo.getCreatedBy());
        if (userInfo != null) {
            booksVoucherVo.setCreatedName(userInfo.getDisplayName());
        }
        List<VoucherItemVo> booksVoucherItemVos = queryItems(booksVoucherVo.getId());
        booksVoucherVo.setItems(booksVoucherItemVos);
        return new Message<>(Message.SUCCESS, booksVoucherVo);
    }

    private record VoucherBatchLoad(
            Map<String, VoucherVo> vouchers,
            Map<String, List<VoucherAuxiliary>> auxiliariesByVoucher) {
    }

    private VoucherBatchLoad loadVouchers(Collection<String> ids) {
        if (ids == null || ids.isEmpty()) {
            return new VoucherBatchLoad(Map.of(), Map.of());
        }
        List<String> idList = ids.stream()
                .filter(Objects::nonNull)
                .distinct()
                .toList();
        if (idList.isEmpty()) {
            return new VoucherBatchLoad(Map.of(), Map.of());
        }

        Map<String, VoucherVo> vouchersById = BeanUtil.copyToList(baseMapper.selectBatchIds(idList), VoucherVo.class).stream()
                .collect(Collectors.toMap(VoucherVo::getId, voucher -> voucher));
        List<VoucherVo> vouchers = idList.stream()
                .map(vouchersById::get)
                .filter(Objects::nonNull)
                .toList();
        if (vouchers.isEmpty()) {
            return new VoucherBatchLoad(Map.of(), Map.of());
        }

        List<String> voucherIds = vouchers.stream()
                .map(VoucherVo::getId)
                .toList();
        List<String> userIds = vouchers.stream()
                .map(VoucherVo::getCreatedBy)
                .filter(Objects::nonNull)
                .distinct()
                .toList();
        Map<String, String> userMap = new HashMap<>();
        if (!userIds.isEmpty()) {
            userInfoMapper.selectByIds(userIds)
                    .forEach(user -> userMap.put(user.getId(), user.getDisplayName()));
        }

        List<VoucherItemVo> allItems = BeanUtil.copyToList(
                voucherItemMapper.selectList(
                Wrappers.<VoucherItem>lambdaQuery().in(VoucherItem::getVoucherId, voucherIds)), VoucherItemVo.class);
        List<VoucherAuxiliary> allAuxiliaries = voucherItemAuxiliaryMapper.selectList(
                Wrappers.<VoucherAuxiliary>lambdaQuery().in(VoucherAuxiliary::getVoucherId, voucherIds));
        Map<String, List<VoucherItemVo>> itemsByVoucher = allItems.stream()
                .collect(Collectors.groupingBy(VoucherItemVo::getVoucherId));
        Map<String, List<VoucherAuxiliary>> auxiliariesByVoucher = allAuxiliaries.stream()
                .collect(Collectors.groupingBy(VoucherAuxiliary::getVoucherId));

        Map<String, VoucherVo> result = new LinkedHashMap<>();
        for (VoucherVo voucher : vouchers) {
            normalizeDisplayWord(voucher);
            voucher.setCreatedName(userMap.get(voucher.getCreatedBy()));
            List<VoucherItemVo> itemVos = new ArrayList<>(
                    itemsByVoucher.getOrDefault(voucher.getId(), List.of()));
            enrichItemVos(itemVos, auxiliariesByVoucher.getOrDefault(voucher.getId(), List.of()));
            voucher.setItems(itemVos);
            result.put(voucher.getId(), voucher);
        }
        return new VoucherBatchLoad(result, auxiliariesByVoucher);
    }

    private Map<String, VoucherVo> queryByIds(Collection<String> ids) {
        return loadVouchers(ids).vouchers();
    }

    /**
     * 分页查询
     *
     * @param dto 分页参数
     * @return 查询结果
     */
    public Message<Page<VoucherVo>> pageList(VoucherPageDto dto) {
        LambdaQueryWrapper<Voucher> lqw = buildQueryWrapper(dto);
        Page<Voucher> page = baseMapper.selectPage(dto.build(), lqw);
        Page<VoucherVo> result = new Page<>(page.getCurrent(), page.getSize(), page.getTotal());
        if (CollUtil.isNotEmpty(page.getRecords())) {
            result.setRecords(BeanUtil.copyToList(page.getRecords(), VoucherVo.class));
            result.getRecords().forEach(this::normalizeDisplayWord);
        }
        // 更新制单人名称，可选加载分录明细
        if (!result.getRecords().isEmpty()) {
            if (Boolean.TRUE.equals(dto.getIncludeItems())) {
                List<String> voucherIds = result.getRecords().stream().map(VoucherVo::getId).toList();
                Map<String, VoucherVo> loaded = loadVouchers(voucherIds).vouchers();
                result.getRecords().forEach(v -> {
                    VoucherVo full = loaded.get(v.getId());
                    if (full != null) {
                        v.setItems(full.getItems());
                        v.setCreatedName(full.getCreatedName());
                        v.setWord(full.getWord());
                    } else {
                        v.setItems(List.of());
                    }
                });
            } else {
                List<String> userIds = result.getRecords().stream().map(VoucherVo::getCreatedBy).toList();
                Map<String, String> userMap = new HashMap<>();
                userInfoMapper.selectByIds(userIds).forEach(user -> userMap.put(user.getId(), user.getDisplayName()));
                result.getRecords().forEach(t -> t.setCreatedName(userMap.get(t.getCreatedBy())));
            }
        }
        return new Message<>(Message.SUCCESS, result);
    }

    /**
     * 保存&提交
     *
     * @param dto    数据对象
     * @param update 是否更新数据
     * @return 结果
     */
    @Transactional
    public Message<String> submit(VoucherChangeDto dto, boolean update) {
        return submit(dto, update, null, false);
    }

    private Message<String> submit(VoucherChangeDto dto, boolean update, Book book, boolean skipDraftLoad) {
        if (StringUtils.isNotBlank(dto.getId()) && !skipDraftLoad) {
            Voucher voucher = baseMapper.selectById(dto.getId());
            if (voucher == null) {
                return Message.failed("凭证不存在");
            }
            if (!VoucherStatusEnum.DRAFT.getValue().equals(voucher.getStatus())) {
                return Message.failed("凭证已提交，不允许修改");
            }
        }
        if (update) {
            // 先执行暂存操作
            dto.setStatus(VoucherStatusEnum.DRAFT.getValue());
            Message<String> saveRes;
            if (StringUtils.isEmpty(dto.getId())) {
                saveRes = save(dto);
            } else {
                saveRes = update(dto);
            }
            if (saveRes.getCode() != Message.SUCCESS) {
                return saveRes;
            }
            dto.setId(saveRes.getData());
        }

        // 只有当前期的凭证允许提交，因为会影响到余额数据
        String currentTerm = configSysService.getCurrentTerm(dto.getBookId());
        String voucherDate = DateUtils.format(dto.getVoucherDate(), DateUtils.FORMAT_DATE_YYYY_MM);
        if (!currentTerm.equals(voucherDate)) {
            return Message.failed("已暂存，非当前期不允许提交凭证");
        }

        Message<String> validationResult = validateItemsForSubmit(dto.getItems());
        if (validationResult.getCode() != Message.SUCCESS) {
            return validationResult;
        }

        Book resolvedBook = book != null ? book : bookMapper.selectById(dto.getBookId());
        if (VoucherReviewedOnOffEnum.ON.getCode().equals(resolvedBook.getVoucherReviewed())) {
            // 再提交创建审核信息,分配审批人，创建审批记录
            dto.setStatus(VoucherStatusEnum.UNDER_REVIEW.getValue());
            // Todo 创建审批记录...
        } else {
            // 直接完成
            dto.setStatus(VoucherStatusEnum.COMPLETED.getValue());
        }

        // 重新提交变更状态（余额在过账时更新）
        Message<String> updateResult = update(dto);

        return updateResult;
    }

    /**
     * 批量提交
     *
     * @param ids    ids
     * @param bookId 账套id
     * @return 批量提交结果
     */
    @Transactional
    public Message<String> submitBatch(List<String> ids, String bookId) {
        if (ids.isEmpty()) {
            return Message.failed("请选择要提交的凭证");
        }
        Map<String, VoucherVo> voucherMap = queryByIds(ids);
        Book book = bookMapper.selectById(bookId);
        if (book == null) {
            return Message.failed("账套不存在");
        }
        int count = 0;
        // 遍历处理，保证每一个凭证顺序提交
        for (String id : ids) {
            VoucherVo voucherVo = voucherMap.get(id);
            if (voucherVo == null) {
                return Message.failed("凭证不存在");
            }
            if (!VoucherStatusEnum.DRAFT.getValue().equals(voucherVo.getStatus())) {
                continue;
            }
            VoucherChangeDto dto = VoucherChangeDto.builder().build();
            BeanUtils.copyProperties(voucherVo, dto);
            List<VoucherItemVo> items = voucherVo.getItems();
            if (items.isEmpty()) {
                continue;
            }

            // 转换凭证项
            List<VoucherItemChangeDto> voucherItemChangeDtos = items.stream().map(item -> {
                VoucherItemChangeDto itemChangeDto = VoucherItemChangeDto.builder().build();
                BeanUtils.copyProperties(item, itemChangeDto);
                return itemChangeDto;
            }).toList();
            dto.setItems(voucherItemChangeDtos);

            Message<String> submit = submit(dto, false, book, true);
            if (submit.getCode() != Message.SUCCESS) {
                return new Message<>(submit.getCode(), submit.getMessage() + " 成功提交" + count + "条。");
            }
            count++;
        }

        return new Message<>(Message.SUCCESS, "成功提交" + count + "条凭证, 忽略" + (ids.size() - count) + "条");
    }

    /**
     * 插入数据
     *
     * @param dto 插入对象
     * @return 插入结果
     */
    @Transactional
    public Message<String> save(VoucherChangeDto dto) {
        Message<String> validationResult = validateItemsForSave(dto.getItems());
        if (validationResult.getCode() != Message.SUCCESS) {
            return validationResult;
        }
        Voucher voucher = Voucher.builder().build();
        BeanUtil.copyProperties(dto, voucher);
        String currentId = identifierGenerator.nextId(voucher).toString();
        voucher.setId(currentId);
        dto.setId(currentId);

        BooksVoucherItemProvider voucherItemProvider = updateItemsAndCount(voucher, dto, currentId, false);
        List<VoucherItem> insertItems = voucherItemProvider.getItems();
        List<VoucherAuxiliary> insertAuxiliary = voucherItemProvider.getAuxiliary();
        //设置条目所属账套
        for (VoucherItem voucherItem : insertItems) {
            voucherItem.setBookId(dto.getBookId());
        }
        //设置辅助所属账套
        for (VoucherAuxiliary voucherAuxiliary : insertAuxiliary) {
            voucherAuxiliary.setBookId(dto.getBookId());
        }
        // 凭证字校验与建立：记-9
        String word = VoucherUtils.createWord(voucher.getWordHead(), voucher.getWordNum());
        Integer latestWordNum = getLatestWordNum(dto.getBookId(), voucher.getWordHead(), voucher.getVoucherYear(), voucher.getVoucherMonth());
        boolean isRepeat = latestWordNum != null && voucher.getWordNum() <= latestWordNum;
        if (isRepeat) {
            // 凭证字号码重复，重新生成
            voucher.setWordNum(latestWordNum + 1);
            word = VoucherUtils.createWord(voucher.getWordHead(), voucher.getWordNum());
        }
        voucher.setWord(word);

        // 更新最新的凭证字以便后续使用
        VoucherWord nextWord = VoucherWord.builder()
                .bookId(dto.getBookId())
                .wordNum(voucher.getWordNum())
                .wordYear(voucher.getVoucherYear())
                .wordMonth(voucher.getVoucherMonth())
                .wordHead(voucher.getWordHead())
                .word(word)
                .printTitle(word)
                .build();
        nextWord.setId(identifierGenerator.nextId(nextWord).toString());
        voucherWordMapper.insert(nextWord);

        if (!insertItems.isEmpty()) {
            boolean saveItems = Db.saveBatch(insertItems);
            if (!saveItems) {
                return new Message<>(Message.FAIL, "新增失败：凭证明细");
            }
            if (!insertAuxiliary.isEmpty()) {
                Db.saveBatch(insertAuxiliary);
            }
        }
        boolean save = super.save(voucher);
        return save
                ? new Message<>(Message.SUCCESS, isRepeat ? "凭证字号重复，已为您重新编号并保存成功！" : "暂存成功", currentId)
                : new Message<>(Message.FAIL, "暂存失败");
    }

    /**
     * 更新信息
     *
     * @param dto 更新对象
     * @return 结果
     */
    @Transactional
    public Message<String> update(VoucherChangeDto dto) {
        Message<String> validationResult = validateItemsForSave(dto.getItems());
        if (validationResult.getCode() != Message.SUCCESS) {
            return validationResult;
        }
        String currentId = dto.getId();
//        dto.setWord(null);
//        dto.setWordNum(null);
//        dto.setWordHead(null);
//        dto.setVoucherYear(null);
        Voucher currentVoucher = baseMapper.selectById(currentId);
        if (currentVoucher == null) {
            return new Message<>(Message.FAIL, "凭证不存在");
        }

        Voucher booksVoucher = Voucher.builder().build();
        BeanUtil.copyProperties(dto, booksVoucher);

        BooksVoucherItemProvider booksVoucherItemProvider = updateItemsAndCount(booksVoucher, dto, currentId, false);
        List<VoucherItem> insertItems = booksVoucherItemProvider.getItems();
        List<VoucherAuxiliary> insertAuxiliary = booksVoucherItemProvider.getAuxiliary();

        //设置条目所属账套
        for (VoucherItem voucherItem : insertItems) {
            voucherItem.setBookId(dto.getBookId());
        }
        //设置辅助所属账套
        for (VoucherAuxiliary voucherAuxiliary : insertAuxiliary) {
            voucherAuxiliary.setBookId(dto.getBookId());
        }
        // 凭证字校验与建立：记-9（不按历史 word 字符串比较，避免旧格式误判重复）
        String word = VoucherUtils.createWord(booksVoucher.getWordHead(), booksVoucher.getWordNum());
        Integer latestWordNum = getLatestWordNum(dto.getBookId(), booksVoucher.getWordHead(), booksVoucher.getVoucherYear(), booksVoucher.getVoucherMonth());
        boolean sameWordSlot = Objects.equals(currentVoucher.getWordHead(), booksVoucher.getWordHead())
                && Objects.equals(currentVoucher.getWordNum(), booksVoucher.getWordNum())
                && Objects.equals(currentVoucher.getVoucherYear(), booksVoucher.getVoucherYear())
                && Objects.equals(currentVoucher.getVoucherMonth(), booksVoucher.getVoucherMonth());
        boolean isRepeat = latestWordNum != null && booksVoucher.getWordNum() <= latestWordNum && !sameWordSlot;
        if (isRepeat) {
            // 凭证字号码重复，重新生成
            booksVoucher.setWordNum(latestWordNum + 1);
            word = VoucherUtils.createWord(booksVoucher.getWordHead(), booksVoucher.getWordNum());
            // 更新最新的凭证字以便后续使用
            VoucherWord nextWord = VoucherWord.builder()
                    .bookId(dto.getBookId())
                    .wordNum(booksVoucher.getWordNum())
                    .wordYear(booksVoucher.getVoucherYear())
                    .wordMonth(booksVoucher.getVoucherMonth())
                    .wordHead(booksVoucher.getWordHead())
                    .word(word)
                    .printTitle(word)
                    .build();
            nextWord.setId(identifierGenerator.nextId(nextWord).toString());
            voucherWordMapper.insert(nextWord);
        }
        booksVoucher.setWord(word);

        if (!canModifyUnpostedVoucher(currentVoucher)) {
            return new Message<>(Message.FAIL, "当前不允许修改");
        }

        // 删除以前的明细数据
        voucherItemMapper.delete(new LambdaQueryWrapper<VoucherItem>().eq(VoucherItem::getVoucherId, currentId));
        voucherItemAuxiliaryMapper.delete(new LambdaQueryWrapper<VoucherAuxiliary>().eq(VoucherAuxiliary::getVoucherId, currentId));

        // 插入新数据
        if (!insertItems.isEmpty()) {
            boolean saveItems = Db.saveBatch(insertItems);
            if (!saveItems) {
                return new Message<>(Message.FAIL, "修改失败:凭证明细");
            }
            if (!insertAuxiliary.isEmpty()) {
                Db.saveBatch(insertAuxiliary);
            }
        }
        boolean update = super.updateById(booksVoucher);
        return update
                ? new Message<>(Message.SUCCESS, isRepeat ? "凭证字号重复，已为您重新编号并保存成功！" : "修改成功", currentId)
                : new Message<>(Message.FAIL, "修改失败");
    }

    /**
     * 审核
     *
     * @param ids      主键组
     * @param userInfo 审核人信息
     */
    @Transactional
    public Message<Void> audit(List<String> ids, UserInfo userInfo) {
        List<Voucher> vouchers = baseMapper.selectByIds(ids);
        List<Voucher> auditVouchers = vouchers.stream()
                .filter(item -> VoucherStatusEnum.UNDER_REVIEW.getValue().equals(item.getStatus()))
                .toList();
        Map<String, VoucherVo> voucherMap = queryByIds(
                auditVouchers.stream().map(Voucher::getId).toList());
        for (Voucher auditVoucher : auditVouchers) {
            VoucherVo voucher = voucherMap.get(auditVoucher.getId());
            if (voucher == null) {
                continue;
            }
            voucher.setStatus(VoucherStatusEnum.COMPLETED.getValue());
            voucher.setAuditDate(new Date());
            voucher.setAuditMemberId(userInfo.getId());
            voucher.setAuditMemberName(userInfo.getDisplayName());

            // 转换对象
            VoucherChangeDto voucherChangeDto = new VoucherChangeDto();
            BeanUtils.copyProperties(voucher, voucherChangeDto);
            List<VoucherItemVo> itemVos = voucher.getItems();
            List<VoucherItemChangeDto> items = new ArrayList<>();
            for (VoucherItemVo itemVo : itemVos) {
                VoucherItemChangeDto itemChangeDto = new VoucherItemChangeDto();
                BeanUtils.copyProperties(itemVo, itemChangeDto);
                items.add(itemChangeDto);
            }
            voucherChangeDto.setItems(items);
            // 更新凭证明细（余额在过账时更新）
            BooksVoucherItemProvider booksVoucherItemProvider = updateItemsAndCount(auditVoucher, voucherChangeDto, auditVoucher.getId(), true);
            List<VoucherItem> insertItems = booksVoucherItemProvider.getItems();

            Db.updateBatchById(insertItems);
            super.updateById(voucher);
        }

        return new Message<>(Message.SUCCESS,
                "操作总数：" + ids.size()
                        + "; 成功：" + auditVouchers.size()
                        + "; 失败：" + (vouchers.size() - auditVouchers.size())
                        + "; 不存在项：" + (ids.size() - vouchers.size())
        );
    }

    /**
     * 反审核：已审核且未过账的凭证退回待审/暂存
     */
    @Transactional
    public Message<Void> unaudit(List<String> ids, String bookId) {
        Book book = bookMapper.selectById(bookId);
        if (book == null) {
            return Message.failed("账套不存在");
        }
        List<Voucher> vouchers = baseMapper.selectByIds(ids);
        List<Voucher> unauditVouchers = new ArrayList<>();
        for (Voucher voucher : vouchers) {
            if (!bookId.equals(voucher.getBookId())) {
                continue;
            }
            if (!VoucherStatusEnum.COMPLETED.getValue().equals(voucher.getStatus())) {
                continue;
            }
            if (StringUtils.isNotBlank(voucher.getSenderId())) {
                continue;
            }
            if (!isVoucherInOpenPeriod(voucher)) {
                continue;
            }
            if (VoucherReviewedOnOffEnum.ON.getCode().equals(book.getVoucherReviewed())) {
                voucher.setStatus(VoucherStatusEnum.UNDER_REVIEW.getValue());
            } else {
                voucher.setStatus(VoucherStatusEnum.DRAFT.getValue());
            }
            voucher.setAuditMemberId(null);
            voucher.setAuditMemberName(null);
            voucher.setAuditDate(null);
            unauditVouchers.add(voucher);
        }
        if (unauditVouchers.isEmpty()) {
            return Message.failed("没有可以反审核的凭证（需为已审核且未过账）");
        }
        for (Voucher voucher : unauditVouchers) {
            baseMapper.update(null, Wrappers.<Voucher>lambdaUpdate()
                    .eq(Voucher::getId, voucher.getId())
                    .set(Voucher::getStatus, voucher.getStatus())
                    .set(Voucher::getAuditMemberId, null)
                    .set(Voucher::getAuditMemberName, null)
                    .set(Voucher::getAuditDate, null));
        }
        return new Message<>(Message.SUCCESS,
                "操作总数：" + ids.size()
                        + "; 成功：" + unauditVouchers.size()
                        + "; 失败：" + (vouchers.size() - unauditVouchers.size())
        );
    }

    /**
     * 过账：写入过账标记并更新科目余额
     */
    @Transactional
    public Message<Void> sender(List<String> ids, UserInfo userInfo) {
        List<Voucher> vouchers = baseMapper.selectByIds(ids);
        VoucherBatchLoad batchLoad = loadVouchers(ids);
        List<Voucher> senderVouchers = new ArrayList<>();
        for (Voucher voucher : vouchers) {
            if (!VoucherStatusEnum.COMPLETED.getValue().equals(voucher.getStatus())) {
                continue;
            }
            if (StringUtils.isNotBlank(voucher.getSenderId())) {
                continue;
            }
            if (!isVoucherInOpenPeriod(voucher)) {
                continue;
            }
            VoucherVo voucherVo = batchLoad.vouchers().get(voucher.getId());
            if (voucherVo == null) {
                continue;
            }
            List<VoucherAuxiliary> auxiliaries =
                    batchLoad.auxiliariesByVoucher().getOrDefault(voucher.getId(), List.of());
            List<VoucherItem> items = voucherVo.getItems().stream().map(itemVo -> {
                VoucherItem build = VoucherItem.builder().build();
                BeanUtil.copyProperties(itemVo, build);
                return build;
            }).toList();
            updateSubjectBalance(items, auxiliaries, false);
            setVoucherItemCashFlow(toChangeDto(voucherVo));

            voucher.setSenderId(userInfo.getId());
            voucher.setSenderDate(new Date());
            voucher.setSenderName(userInfo.getDisplayName());
            senderVouchers.add(voucher);
        }
        if (senderVouchers.isEmpty()) {
            return Message.failed("没有可以过账的凭证（需为已审核且未过账状态）");
        }
        boolean b = Db.updateBatchById(senderVouchers);
        return b ? new Message<>(Message.SUCCESS,
                "操作总数：" + ids.size()
                        + "; 成功：" + senderVouchers.size()
                        + "; 失败：" + (vouchers.size() - senderVouchers.size())
        ) : Message.failed("操作失败");
    }

    /**
     * 反过账：清除过账标记并回滚科目余额
     */
    @Transactional
    public Message<Void> unsender(List<String> ids, String bookId) {
        List<Voucher> vouchers = baseMapper.selectByIds(ids);
        VoucherBatchLoad batchLoad = loadVouchers(ids);
        List<Voucher> unsenderVouchers = new ArrayList<>();
        for (Voucher voucher : vouchers) {
            if (!bookId.equals(voucher.getBookId())) {
                continue;
            }
            if (StringUtils.isBlank(voucher.getSenderId())) {
                continue;
            }
            if (!isVoucherInOpenPeriod(voucher)) {
                continue;
            }
            VoucherVo voucherVo = batchLoad.vouchers().get(voucher.getId());
            if (voucherVo == null) {
                continue;
            }
            List<VoucherAuxiliary> auxiliaries =
                    batchLoad.auxiliariesByVoucher().getOrDefault(voucher.getId(), List.of());
            List<VoucherItem> items = voucherVo.getItems().stream().map(itemVo -> {
                VoucherItem build = VoucherItem.builder().build();
                BeanUtil.copyProperties(itemVo, build);
                return build;
            }).toList();
            updateSubjectBalance(items, auxiliaries, true);
            removeVoucherItemCashFlow(voucher.getId());

            voucher.setSenderId(null);
            voucher.setSenderDate(null);
            voucher.setSenderName(null);
            unsenderVouchers.add(voucher);
        }
        if (unsenderVouchers.isEmpty()) {
            return Message.failed("没有可以反过账的凭证（需为已过账且所在期间未结账）");
        }
        for (Voucher voucher : unsenderVouchers) {
            baseMapper.update(null, Wrappers.<Voucher>lambdaUpdate()
                    .eq(Voucher::getId, voucher.getId())
                    .set(Voucher::getSenderId, null)
                    .set(Voucher::getSenderDate, null)
                    .set(Voucher::getSenderName, null));
        }
        return new Message<>(Message.SUCCESS,
                "操作总数：" + ids.size()
                        + "; 成功：" + unsenderVouchers.size()
                        + "; 失败：" + (vouchers.size() - unsenderVouchers.size())
        );
    }

    /**
     * 主管复审
     */
    @Transactional
    public Message<Void> manageAudit(List<String> ids, UserInfo userInfo) {
        List<Voucher> vouchers = baseMapper.selectByIds(ids);
        List<Voucher> manageVouchers = vouchers.stream()
                .filter(item -> VoucherStatusEnum.COMPLETED.getValue().equals(item.getStatus()))
                .toList();
        for (Voucher voucher : manageVouchers) {
            voucher.setManagerId(userInfo.getId());
            voucher.setManagerDate(new Date());
            voucher.setManagerName(userInfo.getDisplayName());
        }
        if (manageVouchers.isEmpty()) {
            return Message.failed("没有可以主管复核的凭证（需为已完成状态）");
        }
        boolean b = Db.updateBatchById(manageVouchers);
        return b ? new Message<>(Message.SUCCESS,
                "操作总数：" + ids.size()
                        + "; 成功：" + manageVouchers.size()
                        + "; 失败：" + (vouchers.size() - manageVouchers.size())
        ) : Message.failed("操作失败");
    }
    public void export(VoucherPageDto dto, HttpServletResponse response) throws IOException {
        List<VoucherVo> data = pageList(dto).getData().getRecords();

        File templateSource = ExportTemplateFiles.copyToTemp("static/export-template/template-voucher.xlsx", "template-voucher_");
        ExcelParams<List<VoucherVo>> paramsObj = ExcelParams.<List<VoucherVo>>builder()
                .httpResponse(response)
                .dataModel(data)
//                .outputDirectory("C:\\Users\\Administrator\\Desktop\\")
//                .outputFileName("voucher_exported_temp.xlsx")
                .enableMergeCells(true)
                .autoSizeColumns(false)
                .recalculateFormulas(true)
                .templateFilePath(templateSource.getAbsolutePath())
                .build();
        ExcelExporter.export(paramsObj);
        if (templateSource.exists()) templateSource.delete();
    }

    /**
     * 根据ID删除
     *
     * @param ids    ID组
     * @param bookId 账簿ID
     * @return 结果
     */
    @Transactional
    public Message<String> delete(List<String> ids, String bookId) {
        if (ids == null || ids.isEmpty()) {
            return new Message<>(Message.SUCCESS);
        }

        LambdaQueryWrapper<Voucher> checkLqw = Wrappers.lambdaQuery();
        checkLqw.in(Voucher::getId, ids);
        checkLqw.eq(Voucher::getBookId, bookId);
        List<Voucher> toDelete = baseMapper.selectList(checkLqw);
        if (toDelete.size() != ids.size()) {
            return new Message<>(Message.FAIL, "部分凭证不存在");
        }
        for (Voucher voucher : toDelete) {
            if (!VoucherStatusEnum.DRAFT.getValue().equals(voucher.getStatus())) {
                return new Message<>(Message.FAIL, "仅暂存状态的凭证可以删除");
            }
            if (StringUtils.isNotBlank(voucher.getSenderId())) {
                return new Message<>(Message.FAIL, "已过账的凭证不能删除");
            }
        }

        // 删除凭证项和现金流量的关系
        var voucherItems = voucherItemMapper.selectList(
                Wrappers.<VoucherItem>lambdaQuery().in(VoucherItem::getVoucherId, ids)
        );
        if (ObjectUtils.isNotEmpty(voucherItems)) {
            var voucherItemIds = voucherItems.stream()
                    .map(VoucherItem::getId)
                    .toList();

            voucherItemCashFlowMapper.delete(
                    Wrappers.<VoucherItemCashFlow>lambdaQuery().in(VoucherItemCashFlow::getVoucherItemId, voucherItemIds)
            );
        }

        // 删除凭证项
        voucherItemMapper.delete(new LambdaUpdateWrapper<VoucherItem>().in(VoucherItem::getVoucherId, ids));
        voucherItemAuxiliaryMapper.delete(new LambdaQueryWrapper<VoucherAuxiliary>().in(VoucherAuxiliary::getVoucherId, ids));

        int update = baseMapper.delete(new LambdaUpdateWrapper<Voucher>().in(Voucher::getId, ids));


        return update == ids.size() ? new Message<>(Message.SUCCESS, "删除成功") : new Message<>(Message.FAIL, "删除失败");
    }

    /**
     * 取消
     *
     * @param ids    凭证ID
     * @param bookId 账簿ID
     * @return 结果
     */
    @Transactional
    public Message<Integer> cancelByIds(List<String> ids, String bookId) {
        if (ids == null || ids.isEmpty()) {
            return new Message<>(Message.FAIL, "未选择数据对象");
        }

        // 先查询凭证状态
        LambdaQueryWrapper<Voucher> lqw = Wrappers.lambdaQuery();
        lqw.in(Voucher::getId, ids);
        lqw.eq(Voucher::getStatus, VoucherStatusEnum.UNDER_REVIEW.getValue());
        List<Voucher> booksVouchers = baseMapper.selectList(lqw);

        if (!booksVouchers.isEmpty()) {
            booksVouchers.forEach(t -> t.setStatus(VoucherStatusEnum.DRAFT.getValue()));
            Db.updateBatchById(booksVouchers);
        }

        // 更新审批记录状态...
        return new Message<>(Message.SUCCESS, booksVouchers.size());
    }

    /**
     * 构建查询条件
     *
     * @param bo 查询参数
     */
    private LambdaQueryWrapper<Voucher> buildQueryWrapper(VoucherPageDto bo) {
        LambdaQueryWrapper<Voucher> lqw = Wrappers.lambdaQuery();
        lqw.eq(bo.getBookId() != null, Voucher::getBookId, bo.getBookId());
        if (bo.getVoucherDateStart() != null && bo.getVoucherDateEnd() != null) {
            lqw.ge(Voucher::getVoucherDate, bo.getVoucherDateStart());
            lqw.le(Voucher::getVoucherDate, bo.getVoucherDateEnd());
        } else {
            lqw.eq(bo.getVoucherYear() != null, Voucher::getVoucherYear, bo.getVoucherYear());
            lqw.eq(bo.getVoucherMonth() != null, Voucher::getVoucherMonth, bo.getVoucherMonth());
        }
        lqw.eq(bo.getVoucherDate() != null, Voucher::getVoucherDate, bo.getVoucherDate());
        lqw.likeRight(StringUtils.isNotBlank(bo.getWord()), Voucher::getWord, bo.getWord());
        lqw.like(StringUtils.isNotBlank(bo.getCompanyName()), Voucher::getCompanyName, bo.getCompanyName());
        return lqw;
    }

    /**
     * 新增或更新凭证时使用，用于统计和生成凭证明细
     *
     * @param booksVoucher 凭证对象
     * @param dto          修改对象
     * @param currentId    住建
     * @param isUpdate     操作方式：是否更新操作，更新不重置ID
     * @return 凭证明细
     */
    private BooksVoucherItemProvider updateItemsAndCount(Voucher booksVoucher, VoucherChangeDto dto, String currentId, boolean isUpdate) {
        booksVoucher.setDebitAmount(new BigDecimal(0));
        booksVoucher.setCreditAmount(new BigDecimal(0));
        List<VoucherItemChangeDto> items = dto.getItems();
        List<VoucherAuxiliary> insertAuxiliary = new ArrayList<>();
        List<VoucherItem> insertItems = items.stream().map(t -> {
            prepareVoucherItem(t);
            enrichItemBalance(dto.getBookId(), t);
            if (t.getCarryForward() == null) {
                t.setCarryForward(0);
            }
            if (!isUpdate) {
                String itemId = identifierGenerator.nextId(booksVoucher).toString();
                t.setId(itemId);
            }
            t.setVoucherId(currentId);
            if (t.getDebitAmount() != null) {
                booksVoucher.setDebitAmount(booksVoucher.getDebitAmount().add(t.getDebitAmount()));
            }
            if (t.getCreditAmount() != null) {
                booksVoucher.setCreditAmount(booksVoucher.getCreditAmount().add(t.getCreditAmount()));
            }
            if (StringUtils.isNotBlank(t.getDetailedSubjectCode())) {
                t.setSubjectCode(t.getDetailedSubjectCode());
            } else if (StringUtils.isBlank(t.getSubjectCode())
                    && StringUtils.isNotBlank(t.getSubjectName())
                    && t.getSubjectName().contains("-")) {
                t.setSubjectCode(t.getSubjectName().split("-")[0]);
            }
            t.setVoucherDate(booksVoucher.getVoucherDate());
            VoucherItem item = VoucherItem.builder().build();
            BeanUtil.copyProperties(t, item);

            // 创建辅助核算数据
            List<VoucherItemAuxiliaryDto> auxiliary = t.getAuxiliary();
            if (auxiliary != null) {
                auxiliary.stream().filter(auxiliaryDto -> !auxiliaryDto.getValue().isEmpty())
                        .forEach(auxiliaryDto -> auxiliaryDto.getValue()
                                .forEach(auxiliaryValue -> insertAuxiliary.add(VoucherAuxiliary.builder()
                                        .id(identifierGenerator.nextId(booksVoucher).toString())
                                        .bookId(booksVoucher.getBookId())
                                        .voucherId(currentId)
                                        .voucherItemId(t.getId())
                                        .auxiliary(auxiliaryDto.getId())
                                        .auxiliaryName(auxiliaryDto.getLabel())
                                        .itemId(auxiliaryValue.getValue())
                                        .itemName(auxiliaryValue.getLabel())
                                        .build())));
            }

            return item;
        }).toList();
        if (booksVoucher.getVoucherDate() != null) {
            booksVoucher.setVoucherYear(Integer.valueOf(DateUtils.format(booksVoucher.getVoucherDate(), "yyyy")));
            booksVoucher.setVoucherMonth(Integer.valueOf(DateUtils.format(booksVoucher.getVoucherDate(), "MM")));
        }

        return BooksVoucherItemProvider.builder()
                .items(insertItems)
                .auxiliary(insertAuxiliary)
                .build();
    }

    /**
     * 根据凭证ID获取明细
     *
     * @param voucherId ID
     * @return 凭证明细列表
     */
    private List<VoucherItemVo> queryItems(String voucherId) {
        LambdaQueryWrapper<VoucherItem> lqw = Wrappers.lambdaQuery();
        lqw.eq(VoucherItem::getVoucherId, voucherId);
        List<VoucherItemVo> voucherItemVos = BeanUtil.copyToList(voucherItemMapper.selectList(lqw), VoucherItemVo.class);

        LambdaQueryWrapper<VoucherAuxiliary> lqwAux = Wrappers.lambdaQuery();
        lqwAux.eq(VoucherAuxiliary::getVoucherId, voucherId);
        List<VoucherAuxiliary> voucherAuxiliaries = voucherItemAuxiliaryMapper.selectList(lqwAux);

        enrichItemVos(voucherItemVos, voucherAuxiliaries);
        return voucherItemVos;
    }

    private void enrichItemVos(List<VoucherItemVo> voucherItemVos,
                               List<VoucherAuxiliary> voucherAuxiliaries) {
        Map<String, BookSubject> subjectCache = new HashMap<>();
        // 辅助核算数据
        for (VoucherItemVo voucherItemVo : voucherItemVos) {
            if (SubjectDisplayNameUtils.needsSubjectNameFix(voucherItemVo.getSubjectName())
                    && StringUtils.isNotBlank(voucherItemVo.getSubjectId())) {
                BookSubject subject = subjectCache.computeIfAbsent(
                        voucherItemVo.getSubjectId(),
                        bookSubjectService::getById
                );
                if (subject != null) {
                    voucherItemVo.setSubjectName(SubjectDisplayNameUtils.formatVoucherSubjectName(subject));
                }
            }
            List<VoucherItemAuxiliaryDto> auxiliary = new ArrayList<>();
            voucherAuxiliaries.stream()
                    .filter(t -> t.getVoucherItemId().equals(voucherItemVo.getId()))
                    .collect(Collectors.groupingBy(VoucherAuxiliary::getAuxiliary))
                    .forEach((key, value) -> {
                        VoucherItemAuxiliaryDto itemAuxiliaryDto = VoucherItemAuxiliaryDto.builder()
                                .id(key)
                                .label(value.get(0).getAuxiliaryName())
                                .value(new ArrayList<>())
                                .build();
                        value.forEach(t -> itemAuxiliaryDto.getValue()
                                .add(VoucherItemAuxiliaryDto.BooksVoucherItemAuxiliaryValue.builder()
                                        .label(t.getItemName())
                                        .value(t.getItemId())
                                        .build()
                                ));
                        auxiliary.add(itemAuxiliaryDto);
                    });
            voucherItemVo.setAuxiliary(auxiliary);
        }
    }

    private void prepareVoucherItem(VoucherItemChangeDto item) {
        item.setSummary(SubjectDisplayNameUtils.normalizeSummary(item.getSummary()));
        if (StringUtils.isNotBlank(item.getSubjectId())) {
            BookSubject subject = bookSubjectService.getById(item.getSubjectId());
            if (subject != null) {
                if (StringUtils.isNotBlank(subject.getCode())) {
                    item.setSubjectCode(subject.getCode());
                }
                if (SubjectDisplayNameUtils.needsSubjectNameFix(item.getSubjectName())) {
                    item.setSubjectName(SubjectDisplayNameUtils.formatVoucherSubjectName(subject));
                }
            }
        }
    }

    private void enrichItemBalance(String bookId, VoucherItemChangeDto item) {
        if (item.getSubjectBalance() != null) {
            return;
        }
        if (StringUtils.isBlank(bookId) || StringUtils.isBlank(item.getSubjectId())) {
            item.setSubjectBalance(BigDecimal.ZERO);
            return;
        }
        BookSubject subject = bookSubjectService.getById(item.getSubjectId());
        if (subject == null || StringUtils.isBlank(subject.getCode())) {
            item.setSubjectBalance(BigDecimal.ZERO);
            return;
        }
        List<StatementSubjectBalance> balances = subjectBalanceService.selectSubjectBalance(
                bookId, List.of(subject.getCode()));
        if (CollectionUtils.isEmpty(balances) || balances.get(0).getBalance() == null) {
            item.setSubjectBalance(BigDecimal.ZERO);
        } else {
            item.setSubjectBalance(balances.get(0).getBalance());
        }
    }

    private Message<String> validateItemsForSubmit(List<VoucherItemChangeDto> items) {
        Message<String> saveValidation = validateItemsForSave(items);
        if (saveValidation.getCode() != Message.SUCCESS) {
            return saveValidation;
        }
        for (VoucherItemChangeDto item : filterValidVoucherItems(items)) {
            prepareVoucherItem(item);
        }
        return new Message<>(Message.SUCCESS);
    }

    private Message<String> validateItemsForSave(List<VoucherItemChangeDto> items) {
        List<VoucherItemChangeDto> validItems = filterValidVoucherItems(items);
        if (validItems.isEmpty()) {
            return Message.failed("凭证明细不能为空");
        }
        if (validItems.size() < 2) {
            return Message.failed("至少需要两条分录");
        }
        boolean hasSummary = validItems.stream()
                .map(item -> SubjectDisplayNameUtils.normalizeSummary(item.getSummary()))
                .anyMatch(StringUtils::isNotBlank);
        if (!hasSummary) {
            return Message.failed("请至少输入一项摘要");
        }

        BigDecimal debitTotal = BigDecimal.ZERO;
        BigDecimal creditTotal = BigDecimal.ZERO;
        for (VoucherItemChangeDto item : validItems) {
            prepareVoucherItem(item);
            if (StringUtils.isBlank(item.getSubjectId())) {
                return Message.failed("存在未选择科目的分录");
            }
            if (isBlankAmount(item.getDebitAmount()) && isBlankAmount(item.getCreditAmount())) {
                return Message.failed("存在未填写金额的分录");
            }
            if (item.getDebitAmount() != null) {
                debitTotal = debitTotal.add(item.getDebitAmount());
            }
            if (item.getCreditAmount() != null) {
                creditTotal = creditTotal.add(item.getCreditAmount());
            }
        }
        if (debitTotal.compareTo(creditTotal) != 0 || debitTotal.signum() == 0) {
            return Message.failed("借贷不平衡");
        }
        return new Message<>(Message.SUCCESS);
    }

    private boolean isBlankAmount(BigDecimal amount) {
        return amount == null || amount.signum() == 0;
    }

    private List<VoucherItemChangeDto> filterValidVoucherItems(List<VoucherItemChangeDto> items) {
        if (CollectionUtils.isEmpty(items)) {
            return List.of();
        }
        return items.stream()
                .filter(item -> StringUtils.isNotBlank(item.getSubjectId())
                        || !isBlankAmount(item.getDebitAmount())
                        || !isBlankAmount(item.getCreditAmount())
                        || (item.getAuxiliary() != null && !item.getAuxiliary().isEmpty()))
                .toList();
    }

    private void normalizeDisplayWord(Voucher voucher) {
        String display = VoucherUtils.displayWord(voucher);
        if (display != null) {
            voucher.setWord(display);
        }
    }

    /**
     * 获取当前最新凭证号,返回空则不存在最新数据
     *
     * @param head  字头
     * @param year  年份
     * @param month 月份
     * @return 凭证号
     */
    private Integer getLatestWordNum(String bookId, String head, Integer year, Integer month) {
        if (StringUtils.isEmpty(head) || year == null) {
            throw new ServiceException(VoucherErrorCode.ITEM_OR_TIME_INVALID);
        }

        LambdaQueryWrapper<VoucherWord> wordLambdaQueryWrapper = Wrappers.lambdaQuery();
        wordLambdaQueryWrapper.eq(VoucherWord::getBookId, bookId);
        wordLambdaQueryWrapper.eq(VoucherWord::getWordHead, head);
        wordLambdaQueryWrapper.eq(VoucherWord::getWordYear, year);
        wordLambdaQueryWrapper.eq(VoucherWord::getWordMonth, month);
        wordLambdaQueryWrapper.orderByDesc(VoucherWord::getWordNum);
        Page<VoucherWord> page = new Page<>(1, 1);
        Page<VoucherWord> booksVoucherWordPage = voucherWordMapper.selectPage(page, wordLambdaQueryWrapper);
        List<VoucherWord> voucherWordPageRecords = booksVoucherWordPage.getRecords();

        if (!voucherWordPageRecords.isEmpty()) {
            return voucherWordPageRecords.get(0).getWordNum();
        }
        return null;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    private static class BooksVoucherItemProvider {
        /**
         * 凭证明细列表
         */
        private List<VoucherItem> items;

        /**
         * 凭证明细辅助核算配置项
         */
        private List<VoucherAuxiliary> auxiliary;
    }


    /**
     * 未过账凭证是否允许修改（未过账、未作废、所在期间未结账）
     */
    private boolean canModifyUnpostedVoucher(Voucher voucher) {
        if (voucher == null) {
            return false;
        }
        if (StringUtils.isNotBlank(voucher.getSenderId())) {
            return false;
        }
        if (VoucherStatusEnum.CANCELLED.getValue().equals(voucher.getStatus())) {
            return false;
        }
        return isVoucherInOpenPeriod(voucher);
    }

    /**
     * 凭证所在会计期间是否未结账（凭证期间 >= 账套当前期）
     */
    private boolean isVoucherInOpenPeriod(Voucher voucher) {
        if (voucher == null || voucher.getVoucherDate() == null || StringUtils.isBlank(voucher.getBookId())) {
            return false;
        }
        String currentTerm = configSysService.getCurrentTerm(voucher.getBookId());
        String voucherTerm = DateUtils.format(voucher.getVoucherDate(), DateUtils.FORMAT_DATE_YYYY_MM);
        return currentTerm.compareTo(voucherTerm) <= 0;
    }

    private VoucherChangeDto toChangeDto(VoucherVo voucherVo) {
        VoucherChangeDto dto = new VoucherChangeDto();
        BeanUtils.copyProperties(voucherVo, dto);
        List<VoucherItemChangeDto> items = voucherVo.getItems().stream().map(itemVo -> {
            VoucherItemChangeDto itemChangeDto = new VoucherItemChangeDto();
            BeanUtils.copyProperties(itemVo, itemChangeDto);
            return itemChangeDto;
        }).toList();
        dto.setItems(items);
        return dto;
    }

    private void removeVoucherItemCashFlow(String voucherId) {
        var voucherItems = voucherItemMapper.selectList(
                Wrappers.<VoucherItem>lambdaQuery().eq(VoucherItem::getVoucherId, voucherId)
        );
        if (ObjectUtils.isNotEmpty(voucherItems)) {
            var voucherItemIds = voucherItems.stream()
                    .map(VoucherItem::getId)
                    .toList();
            voucherItemCashFlowMapper.delete(
                    Wrappers.<VoucherItemCashFlow>lambdaQuery().in(VoucherItemCashFlow::getVoucherItemId, voucherItemIds)
            );
        }
    }

    /**
     * 更新科目余额
     *
     * @param insertItems     凭证明细
     * @param insertAuxiliary 辅助核算信息
     * @param isCancel        是否取消，true则反向操作，还原科目余额
     */
    @Transactional
    public void updateSubjectBalance(List<VoucherItem> insertItems, List<VoucherAuxiliary> insertAuxiliary, boolean isCancel) {
        if (insertItems.isEmpty()) {
            return;
        }
        List<String> subjectIds = insertItems.stream().map(VoucherItem::getSubjectId).toList();
        List<BookSubject> booksSubjects = bookSubjectService.listByIds(subjectIds);
        Map<String, BookSubject> subjectMap = booksSubjects.stream()
                .collect(Collectors.toMap(BookSubject::getId, item -> item));
        if (CollectionUtils.isNotEmpty(booksSubjects)) {
            insertItems.forEach(item -> {
                List<VoucherAuxiliary> auxiliaries = insertAuxiliary.stream()
                        .filter(auxiliary -> auxiliary.getVoucherItemId().equals(item.getId()))
                        .toList();
                BookSubject setSubject = subjectMap.get(item.getSubjectId());

                // 借方，更新科目余额和科目余额表
                if (item.getDebitAmount() != null && item.getDebitAmount().compareTo(BigDecimal.ZERO) != 0) {
                    if (isCancel) {
                        subjectBalanceService.update(setSubject, item.getDebitAmount(),
                                StatementSymbolEnum.MINUS, SubjectDirectionEnum.DEBIT, auxiliaries,
                                DateUtils.format(item.getVoucherDate(), "yyyy-MM"));
                    } else {
                        subjectBalanceService.update(setSubject, item.getDebitAmount(),
                                StatementSymbolEnum.PLUS, SubjectDirectionEnum.DEBIT, auxiliaries,
                                DateUtils.format(item.getVoucherDate(), "yyyy-MM"));
                    }
                }
                // 贷方，更新科目余额和科目余额表
                else if (item.getCreditAmount() != null && item.getCreditAmount().compareTo(BigDecimal.ZERO) != 0) {
                    if (isCancel) {
                        subjectBalanceService.update(setSubject, item.getCreditAmount(),
                                StatementSymbolEnum.PLUS, SubjectDirectionEnum.CREDIT, auxiliaries,
                                DateUtils.format(item.getVoucherDate(), "yyyy-MM"));
                    } else {
                        subjectBalanceService.update(setSubject, item.getCreditAmount(),
                                StatementSymbolEnum.MINUS, SubjectDirectionEnum.CREDIT, auxiliaries,
                                DateUtils.format(item.getVoucherDate(), "yyyy-MM"));
                    }
                }

            });
        }
    }

    /**
     * {@code @Description:} 根据科目现金流量默认关系添加凭证项和现金流量关系
     * {@code @Param:} [dto]
     * {@code @return:} void
     * {@code @Author:} xZen
     * {@code @Date:} 2025/4/23 9:43
     */
    private void setVoucherItemCashFlow(VoucherChangeDto dto) {
        if (dto == null || StringUtils.isEmpty(dto.getId())) {
            return;
        }

        List<VoucherItemCashFlow> subjectCashFlows = standardSubjectCashFlowMapper.getSubjectCashFlow(dto);

        if (CollectionUtils.isEmpty(subjectCashFlows)) {
            return;
        }

        String bookId = dto.getBookId();
        List<VoucherItemChangeDto> items = dto.getItems();
        List<String> subjectIds = items.stream()
                .map(VoucherItemChangeDto::getSubjectId)
                .toList();
        List<BookSubject> bookSubjects = bookSubjectService.listByIds(subjectIds);

        // 检查凭证中是否包含现金类科目
        boolean hasCashSubject = bookSubjects.stream()
                .anyMatch(subject -> subject.getIsCash() == 1);


        // 如果没有现金类科目，剔除所有主表现金流量项
        if (!hasCashSubject) {
            subjectCashFlows = subjectCashFlows.stream()
                    .filter(flow -> flow.getCashFlowItemType() != 0)
                    .toList();
        }


        for (VoucherItemCashFlow item : subjectCashFlows) {
            // 如果科目方向与现金流方向相同，金额取反
            if (Objects.equals(item.getSubjectDirection(), item.getDirection()) && item.getCashFlowBalance() != null) {
                item.setCashFlowBalance(item.getCashFlowBalance().negate());
            }

            item.setBookId(bookId);
        }

        voucherItemCashFlowMapper.insert(subjectCashFlows);
    }

    /**
     * 删除凭证及相关条目
     */
    public boolean deleteByBookIds(List<String> bookIds) {
        //删除凭证
        LambdaQueryWrapper<Voucher> lqw = Wrappers.lambdaQuery();
        lqw.in(Voucher::getBookId, bookIds);
        baseMapper.delete(lqw);
        //删除凭证条目
        LambdaQueryWrapper<VoucherItem> lqwItem = Wrappers.lambdaQuery();
        lqwItem.in(VoucherItem::getBookId, bookIds);
        voucherItemMapper.delete(lqwItem);
        return false;
    }

}
