package com.financial.cloud.service.voucher;

import com.financial.cloud.repository.hr.EmployeeSalarySummaryMapper;
import com.financial.cloud.repository.standard.StandardSubjectCashFlowMapper;
import com.financial.cloud.repository.book.BookMapper;
import com.financial.cloud.repository.idm.UserInfoMapper;
import cn.hutool.core.bean.BeanUtil;
import cn.hutool.core.collection.CollUtil;
import cn.hutool.json.JSONUtil;
import com.baomidou.mybatisplus.extension.toolkit.Db;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.core.incrementer.IdentifierGenerator;
import com.baomidou.mybatisplus.core.toolkit.ObjectUtils;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.financial.cloud.common.ExcelImport;
import com.financial.cloud.common.Message;
import com.financial.cloud.common.SubjectAuxiliary;
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
import com.financial.cloud.repository.book.SettlementCarryforwardMapper;
import com.financial.cloud.domain.book.SettlementCarryforward;
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
import com.financial.cloud.service.book.BookSealGuard;
import com.financial.cloud.service.book.BookSubjectService;
import com.financial.cloud.service.journal.JournalEntryService;
import com.financial.cloud.util.DateUtils;
import com.financial.cloud.util.ExcelUtils;
import com.financial.cloud.util.SubjectDisplayNameUtils;
import com.financial.cloud.util.VoucherUtils;
import com.financial.cloud.util.excel.ExcelExporter;
import com.financial.cloud.util.pdf.PdfTableExporter;
import jakarta.servlet.http.HttpServletResponse;
import lombok.*;

import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.poi.ss.usermodel.CellStyle;
import org.apache.poi.ss.usermodel.FillPatternType;
import org.apache.poi.ss.usermodel.Font;
import org.apache.poi.ss.usermodel.IndexedColors;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.annotation.Propagation;

import java.io.IOException;
import java.math.BigDecimal;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.stream.Collectors;


@RequiredArgsConstructor
@Service
public class VoucherService extends ServiceImpl<VoucherMapper, Voucher>{

    /** Shared import/export column contract (one row per journal line). */
    static final String[] VOUCHER_IO_HEADERS = {
            "凭证日期", "凭证字头", "凭证字号", "附单据数", "备注", "摘要", "科目编码", "借方金额", "贷方金额"
    };
    static final String SHEET_INSTRUCTIONS = "填写说明";
    static final String SHEET_DATA = "凭证";
    static final int[] COLUMN_WIDTHS = {14, 10, 10, 10, 20, 28, 14, 14, 14};
    private static final String DEFAULT_WORD_HEAD = "记";

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
    private final SettlementCarryforwardMapper settlementCarryforwardMapper;
    private final BookSealGuard bookSealGuard;
    /** 延迟获取，避免与 JournalEntryService 循环依赖 */
    private final ObjectProvider<JournalEntryService> journalEntryServiceProvider;
    public Message<Page<VoucherItemVo>> subLedger(VoucherItemPageDto paramsDto) {
        paramsDto.parse();
        return Message.ok(voucherItemMapper.subLedgerPage(paramsDto.build(), paramsDto));
    }

    /**
     * 明细账 PDF：按当前筛选条件导出（最多 10000 行）。
     */
    public void exportSubLedgerPdf(VoucherItemPageDto paramsDto, HttpServletResponse response) throws IOException {
        paramsDto.setPageNumber(1);
        paramsDto.setPageSize(10_000);
        paramsDto.parse();
        Page<VoucherItemVo> page = voucherItemMapper.subLedgerPage(paramsDto.build(), paramsDto);
        List<VoucherItemVo> records = page == null || page.getRecords() == null ? List.of() : page.getRecords();
        Book book = bookMapper.selectById(paramsDto.getBookId());
        String company = book != null ? book.getCompanyName() : "";
        String subject = StringUtils.defaultIfBlank(paramsDto.getSubjectCode(), "全部科目");
        SimpleDateFormat dateFmt = new SimpleDateFormat(DateUtils.FORMAT_DATE_DEFAULT);
        List<String[]> rows = new ArrayList<>();
        for (VoucherItemVo row : records) {
            String dateText = "";
            if (row.getVoucherDate() != null) {
                dateText = dateFmt.format(row.getVoucherDate());
            }
            rows.add(new String[]{
                    dateText,
                    PdfTableExporter.nz(row.getWord()),
                    PdfTableExporter.nz(row.getSummary()),
                    PdfTableExporter.formatAmount(row.getDebitAmount()),
                    PdfTableExporter.formatAmount(row.getCreditAmount()),
                    PdfTableExporter.formatAmount(row.getSubjectBalance()),
            });
        }
        PdfTableExporter.write(new PdfTableExporter.PdfTableRequest(
                "明细账",
                "核算单位：" + company + "　科目：" + subject + "　期间：" + PdfTableExporter.nz(paramsDto.getReportDate()),
                new String[]{"日期", "凭证字号", "摘要", "借方金额", "贷方金额", "余额"},
                rows,
                true,
                "明细账" + PdfTableExporter.nz(paramsDto.getReportDate()) + ".pdf"
        ), response);
    }

    public Message<Page<VoucherItemVo>> fetchByCashFlow(VoucherItemPageDto paramsDto) {
        return Message.ok(voucherItemMapper.fetchByCashFlow(paramsDto.build(), paramsDto));
    }
    public Message<List<VoucherSuccessiveDto>> checkSuccessive(VoucherSuccessiveQueryDto query) {
        // 当前期
        String currentTerm = configSysService.getCurrentTerm(query.getBookId());
        Integer year = Integer.valueOf(currentTerm.substring(0, 4));
        Integer month = Integer.valueOf(currentTerm.substring(5, 7));
        // 查询所有符合的凭证（须含暂存/审核中，否则整理会与占用号的未过账单撞号）
        LambdaQueryWrapper<Voucher> lqw = Wrappers.lambdaQuery();
        lqw.eq(Voucher::getBookId, query.getBookId());
        lqw.eq(Voucher::getWordHead, query.getWordHead());
        lqw.eq(Voucher::getVoucherYear, year);
        lqw.eq(Voucher::getVoucherMonth, month);
        lqw.ge(Voucher::getWordNum, query.getStartWordNumber());
        List<String> statusList = new ArrayList<>();
        statusList.add(VoucherStatusEnum.DRAFT.getValue());
        statusList.add(VoucherStatusEnum.UNDER_REVIEW.getValue());
        statusList.add(VoucherStatusEnum.COMPLETED.getValue());
        statusList.add(VoucherStatusEnum.REJECTED.getValue());
        if (Boolean.TRUE.equals(query.getNullify())) {
            statusList.add(VoucherStatusEnum.CANCELLED.getValue());
        }
        lqw.in(Voucher::getStatus, statusList);
        lqw.orderByAsc(Voucher::getVoucherDate, Voucher::getWordNum, Voucher::getId);
        lqw.select(Voucher::getId, Voucher::getBookId, Voucher::getWordNum, Voucher::getWord, Voucher::getWordHead,
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
        if (dtos == null || dtos.isEmpty()) {
            return Message.ok(null);
        }
        // 两阶段改号，避免中间态互相抢占已占用号
        final int tempBase = 1_000_000;
        int tempIdx = 0;
        for (VoucherSuccessiveDto dto : dtos) {
            int tempNum = tempBase + tempIdx++;
            String tempWord = VoucherUtils.createWord(dto.getWordHead(), tempNum);
            LambdaUpdateWrapper<Voucher> tempUw = Wrappers.lambdaUpdate();
            tempUw.eq(Voucher::getId, dto.getId());
            tempUw.eq(Voucher::getBookId, dto.getBookId());
            tempUw.set(Voucher::getWordNum, tempNum);
            tempUw.set(Voucher::getWord, tempWord);
            baseMapper.update(null, tempUw);
        }

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

        Message<String> validationResult = validateItemsForSubmit(dto.getBookId(), dto.getItems());
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
        bookSealGuard.assertWritable(bookId);
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
        Message<String> validationResult = validateItemsForSave(dto.getBookId(), dto.getItems());
        if (validationResult.getCode() != Message.SUCCESS) {
            return validationResult;
        }
        Message<String> periodLock = rejectClosedPeriodWrite(dto);
        if (periodLock != null) {
            return periodLock;
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
        Message<String> validationResult = validateItemsForSave(dto.getBookId(), dto.getItems());
        if (validationResult.getCode() != Message.SUCCESS) {
            return validationResult;
        }
        Message<String> periodLock = rejectClosedPeriodWrite(dto);
        if (periodLock != null) {
            return periodLock;
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

        String modifyBlock = modifyBlockedReason(currentVoucher, isVoucherInOpenPeriod(currentVoucher));
        if (modifyBlock != null) {
            return new Message<>(Message.FAIL, modifyBlock);
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
        if (update) {
            // 红字冲销凭证：保留日记账对冲流水原备注（冲销：…），勿用凭证备注覆盖
            final String journalSyncRemark = StringUtils.isNotBlank(currentVoucher.getSourceVoucherId())
                    ? null
                    : booksVoucher.getRemark();
            journalEntryServiceProvider.ifAvailable(journal ->
                    journal.syncLinkedEntriesFromVoucher(
                            currentId,
                            dto.getBookId(),
                            booksVoucher.getVoucherDate(),
                            journalSyncRemark,
                            insertItems));
        }
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
        bookSealGuard.assertWritable(userInfo.getBookId());
        List<Voucher> vouchers = baseMapper.selectByIds(ids);
        // 会计基础规范：制单人与审核人不得为同一人（createdBy 填充的是用户 ID）
        long selfCreatedCount = vouchers.stream()
                .filter(item -> VoucherStatusEnum.UNDER_REVIEW.getValue().equals(item.getStatus()))
                .filter(item -> Objects.equals(item.getCreatedBy(), userInfo.getId()))
                .count();
        List<Voucher> auditVouchers = vouchers.stream()
                .filter(item -> VoucherStatusEnum.UNDER_REVIEW.getValue().equals(item.getStatus()))
                .filter(item -> !Objects.equals(item.getCreatedBy(), userInfo.getId()))
                .toList();
        Map<String, VoucherVo> voucherMap = auditVouchers.isEmpty()
                ? Map.of()
                : queryByIds(auditVouchers.stream().map(Voucher::getId).toList());
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
                        + (selfCreatedCount > 0 ? "; 其中制单人与审核人相同被拒：" + selfCreatedCount : "")
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
        bookSealGuard.assertWritable(bookId);
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
        bookSealGuard.assertWritable(userInfo.getBookId());
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
        bookSealGuard.assertWritable(bookId);
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
        List<VoucherVo> data = listAllMatching(dto);
        try (Workbook workbook = new XSSFWorkbook()) {
            Sheet sheet = workbook.createSheet(SHEET_DATA);
            Row header = sheet.createRow(0);
            for (int i = 0; i < VOUCHER_IO_HEADERS.length; i++) {
                header.createCell(i).setCellValue(VOUCHER_IO_HEADERS[i]);
            }
            styleHeaderRow(workbook, header);
            sheet.createFreezePane(0, 1);
            applyColumnWidths(sheet);
            CellStyle amountStyle = createAmountStyle(workbook, null);
            int rowIdx = 1;
            SimpleDateFormat dateFmt = new SimpleDateFormat(DateUtils.FORMAT_DATE_DEFAULT);
            for (VoucherVo voucher : data) {
                List<VoucherItemVo> items = voucher.getItems();
                if (CollectionUtils.isEmpty(items)) {
                    Row row = sheet.createRow(rowIdx++);
                    writeExportHeaderCells(row, voucher, dateFmt);
                    continue;
                }
                for (int i = 0; i < items.size(); i++) {
                    VoucherItemVo item = items.get(i);
                    Row row = sheet.createRow(rowIdx++);
                    if (i == 0) {
                        writeExportHeaderCells(row, voucher, dateFmt);
                    }
                    row.createCell(5).setCellValue(StringUtils.defaultString(item.getSummary()));
                    row.createCell(6).setCellValue(StringUtils.defaultString(item.getSubjectCode()));
                    setAmountCell(row, 7, item.getDebitAmount(), amountStyle);
                    setAmountCell(row, 8, item.getCreditAmount(), amountStyle);
                }
            }
            response.setContentType(ExcelExporter.APPLICATION_MS_EXCEL);
            response.setHeader("Content-Disposition", "attachment; filename="
                    + URLEncoder.encode("凭证.xlsx", StandardCharsets.UTF_8));
            workbook.write(response.getOutputStream());
            response.getOutputStream().flush();
        }
    }

    public void downloadImportTemplate(HttpServletResponse response) throws IOException {
        try (Workbook workbook = new XSSFWorkbook()) {
            writeInstructionSheet(workbook);
            Sheet sheet = workbook.createSheet(SHEET_DATA);
            Row header = sheet.createRow(0);
            for (int i = 0; i < VOUCHER_IO_HEADERS.length; i++) {
                header.createCell(i).setCellValue(VOUCHER_IO_HEADERS[i]);
            }
            styleHeaderRow(workbook, header);
            sheet.createFreezePane(0, 1);
            applyColumnWidths(sheet);
            CellStyle sampleStyle = createFillStyle(workbook, IndexedColors.LIGHT_YELLOW);
            CellStyle sampleAmountStyle = createAmountStyle(workbook, IndexedColors.LIGHT_YELLOW);
            Row sampleDebit = sheet.createRow(1);
            for (int i = 0; i < VOUCHER_IO_HEADERS.length; i++) {
                sampleDebit.createCell(i).setCellStyle(sampleStyle);
            }
            sampleDebit.getCell(0).setCellValue("2026-01-15");
            sampleDebit.getCell(1).setCellValue(DEFAULT_WORD_HEAD);
            sampleDebit.getCell(2).setCellValue(1);
            sampleDebit.getCell(3).setCellValue(0);
            sampleDebit.getCell(4).setCellValue("");
            sampleDebit.getCell(5).setCellValue("示例摘要");
            sampleDebit.getCell(6).setCellValue("1001");
            sampleDebit.getCell(7).setCellValue(100);
            sampleDebit.getCell(7).setCellStyle(sampleAmountStyle);
            sampleDebit.getCell(8).setCellValue(0);
            sampleDebit.getCell(8).setCellStyle(sampleAmountStyle);
            Row sampleCredit = sheet.createRow(2);
            for (int i = 0; i < VOUCHER_IO_HEADERS.length; i++) {
                sampleCredit.createCell(i).setCellStyle(sampleStyle);
            }
            sampleCredit.getCell(5).setCellValue("示例摘要");
            sampleCredit.getCell(6).setCellValue("1002");
            sampleCredit.getCell(7).setCellValue(0);
            sampleCredit.getCell(7).setCellStyle(sampleAmountStyle);
            sampleCredit.getCell(8).setCellValue(100);
            sampleCredit.getCell(8).setCellStyle(sampleAmountStyle);
            response.setContentType(ExcelExporter.APPLICATION_MS_EXCEL);
            response.setHeader("Content-Disposition", "attachment; filename="
                    + URLEncoder.encode("凭证导入模板.xlsx", StandardCharsets.UTF_8));
            workbook.write(response.getOutputStream());
            response.getOutputStream().flush();
        }
    }

    public Message<VoucherImportResultVo> importFromExcel(
            String bookId, ExcelImport excelImportFile, String conflictMode) {
        VoucherImportResultVo result = new VoucherImportResultVo();
        if (excelImportFile == null || !excelImportFile.isExcelNotEmpty()) {
            result.setFailed(1);
            VoucherImportResultVo.RowError err = new VoucherImportResultVo.RowError();
            err.setRow(0);
            err.setMessage("请上传 Excel 文件");
            result.getErrors().add(err);
            return new Message<>(Message.FAIL, "导入失败", result);
        }
        Book book = bookMapper.selectById(bookId);
        if (book == null) {
            result.setFailed(1);
            addImportError(result, 0, "", "账套不存在");
            return new Message<>(Message.FAIL, "导入失败", result);
        }
        bookSealGuard.assertWritable(bookId);
        Map<String, BookSubject> subjectByCode = new HashMap<>();
        try {
            Workbook workbook = excelImportFile.biuldWorkbook();
            Sheet sheet = workbook.getSheet(SHEET_DATA);
            if (sheet == null) {
                sheet = workbook.getSheetAt(0);
            }
            if (isLegacyVoucherExportSheet(sheet)) {
                addImportError(result, 1, "",
                        "文件列格式不正确（疑似旧版导出）。请使用「下载模板」或重新「导出」后再导入");
                excelImportFile.closeWorkbook();
                return new Message<>(Message.FAIL, "导入失败：模板不匹配", result);
            }
            List<ImportLine> lines = readImportLines(sheet);
            List<ImportGroup> groups = groupImportLines(lines);
            if (groups.isEmpty()) {
                addImportError(result, 0, "", "未识别到可导入的凭证行，请确认使用最新模板");
                excelImportFile.closeWorkbook();
                return new Message<>(Message.FAIL, "导入失败", result);
            }
            Map<ImportGroup, Voucher> existingByGroup = new IdentityHashMap<>();
            boolean hasUnpostedConflict = false;
            for (ImportGroup group : groups) {
                Voucher existing = findImportConflict(bookId, group);
                if (existing == null) {
                    continue;
                }
                existingByGroup.put(group, existing);
                if (!isPosted(existing)) {
                    hasUnpostedConflict = true;
                    result.getConflicts().add(toConflictItem(group, existing));
                }
            }
            if (StringUtils.isBlank(conflictMode) && hasUnpostedConflict) {
                result.setNeedsConflictDecision(true);
                excelImportFile.closeWorkbook();
                return new Message<>(Message.SUCCESS, "存在字号冲突，请选择处理方式。", result);
            }
            for (ImportGroup group : groups) {
                try {
                    Voucher existing = existingByGroup.get(group);
                    if (isPosted(existing)) {
                        addImportError(result, group.firstExcelRow, groupLabel(group),
                                "字号已过账，不可覆盖。");
                        continue;
                    }
                    if (existing != null && "skip".equalsIgnoreCase(StringUtils.trim(conflictMode))) {
                        result.setSkipped(result.getSkipped() + 1);
                        continue;
                    }
                    Message<String> saveMsg = saveImportGroup(
                            book, subjectByCode, group, existing, conflictMode);
                    if (saveMsg.getCode() != Message.SUCCESS) {
                        addImportError(result, group.firstExcelRow, groupLabel(group),
                                StringUtils.defaultIfBlank(saveMsg.getMessage(), "导入失败"));
                    } else {
                        result.setSuccess(result.getSuccess() + 1);
                    }
                } catch (Exception ex) {
                    addImportError(result, group.firstExcelRow, groupLabel(group),
                            StringUtils.defaultIfBlank(ex.getMessage(), "导入失败"));
                }
            }
            excelImportFile.closeWorkbook();
        } catch (Exception ex) {
            addImportError(result, 0, "", StringUtils.defaultIfBlank(ex.getMessage(), "解析 Excel 失败"));
            return new Message<>(Message.FAIL, "导入失败", result);
        }
        String msg = "导入完成：成功 " + result.getSuccess() + " 条，失败 " + result.getFailed()
                + " 条，跳过 " + result.getSkipped() + " 条";
        return new Message<>(Message.SUCCESS, msg, result);
    }

    private Voucher findImportConflict(String bookId, ImportGroup group) {
        if (group.voucherDate == null || group.wordNum == null) {
            return null;
        }
        String wordHead = StringUtils.defaultIfBlank(group.wordHead, DEFAULT_WORD_HEAD);
        int year = Integer.parseInt(DateUtils.format(group.voucherDate, "yyyy"));
        int month = Integer.parseInt(DateUtils.format(group.voucherDate, "MM"));
        return baseMapper.selectOne(Wrappers.<Voucher>lambdaQuery()
                .eq(Voucher::getBookId, bookId)
                .eq(Voucher::getWordHead, wordHead)
                .eq(Voucher::getVoucherYear, year)
                .eq(Voucher::getVoucherMonth, month)
                .eq(Voucher::getWordNum, group.wordNum)
                .last("LIMIT 1"));
    }

    static boolean isPosted(Voucher voucher) {
        return voucher != null && StringUtils.isNotBlank(voucher.getSenderId());
    }

    private static VoucherImportResultVo.ConflictItem toConflictItem(
            ImportGroup group, Voucher existing) {
        VoucherImportResultVo.ConflictItem item = new VoucherImportResultVo.ConflictItem();
        String wordHead = StringUtils.defaultIfBlank(group.wordHead, DEFAULT_WORD_HEAD);
        item.setRow(group.firstExcelRow);
        item.setWordHead(wordHead);
        item.setWordNum(group.wordNum);
        item.setWordLabel(VoucherUtils.createWord(wordHead, group.wordNum));
        item.setExistingStatus(existing.getStatus());
        item.setExistingId(existing.getId());
        item.setPosted(isPosted(existing));
        return item;
    }

    /**
     * Load all vouchers matching filters (ignores pageNumber/pageSize).
     */
    List<VoucherVo> listAllMatching(VoucherPageDto dto) {
        LambdaQueryWrapper<Voucher> lqw = buildQueryWrapper(dto);
        lqw.orderByAsc(Voucher::getVoucherDate, Voucher::getWordHead, Voucher::getWordNum, Voucher::getId);
        List<Voucher> vouchers = baseMapper.selectList(lqw);
        if (CollUtil.isEmpty(vouchers)) {
            return List.of();
        }
        List<VoucherVo> vos = BeanUtil.copyToList(vouchers, VoucherVo.class);
        vos.forEach(this::normalizeDisplayWord);
        List<String> ids = vos.stream().map(VoucherVo::getId).toList();
        Map<String, VoucherVo> loaded = loadVouchers(ids).vouchers();
        for (VoucherVo v : vos) {
            VoucherVo full = loaded.get(v.getId());
            if (full != null) {
                v.setItems(full.getItems());
                v.setCreatedName(full.getCreatedName());
                v.setWord(full.getWord());
            } else {
                v.setItems(List.of());
            }
        }
        return vos;
    }

    private Message<String> saveImportGroup(Book book, Map<String, BookSubject> subjectByCode, ImportGroup group) {
        return saveImportGroup(book, subjectByCode, group, null, null);
    }

    private Message<String> saveImportGroup(
            Book book,
            Map<String, BookSubject> subjectByCode,
            ImportGroup group,
            Voucher existing,
            String conflictMode) {
        if (group.voucherDate == null) {
            return Message.failed("凭证日期不能为空");
        }
        Message<String> periodLock = rejectClosedPeriodWrite(VoucherChangeDto.builder()
                .bookId(book.getId())
                .voucherDate(group.voucherDate)
                .build());
        if (periodLock != null) {
            return periodLock;
        }
        List<VoucherItemChangeDto> items = new ArrayList<>();
        for (ImportLine line : group.lines) {
            String code = StringUtils.trimToEmpty(line.subjectCode);
            if (StringUtils.isBlank(code)) {
                return Message.failed("科目编码不能为空");
            }
            BookSubject subject = resolveSubject(book.getId(), code, subjectByCode);
            if (subject == null) {
                return Message.failed("科目编码不存在：" + code);
            }
            items.add(VoucherItemChangeDto.builder()
                    .summary(line.summary)
                    .subjectId(subject.getId())
                    .subjectCode(subject.getCode())
                    .subjectName(subject.getCode() + "-" + subject.getName())
                    .debitAmount(nz(line.debitAmount))
                    .creditAmount(nz(line.creditAmount))
                    .build());
        }
        Message<String> itemValidation = validateItemsForSave(book.getId(), items);
        if (itemValidation.getCode() != Message.SUCCESS) {
            return itemValidation;
        }
        String wordHead = StringUtils.defaultIfBlank(group.wordHead, DEFAULT_WORD_HEAD);
        Integer wordNum = group.wordNum;
        int year = Integer.parseInt(DateUtils.format(group.voucherDate, "yyyy"));
        int month = Integer.parseInt(DateUtils.format(group.voucherDate, "MM"));
        if (wordNum == null) {
            Message<Integer> able = getAbleWordNum(book.getId(), wordHead, year, month);
            wordNum = able.getData() == null ? 1 : able.getData();
        }
        VoucherChangeDto dto = VoucherChangeDto.builder()
                .bookId(book.getId())
                .companyName(StringUtils.defaultIfBlank(book.getCompanyName(), book.getName()))
                .wordHead(wordHead)
                .wordNum(wordNum)
                .receiptNum(group.receiptNum == null ? 0 : group.receiptNum)
                .voucherDate(group.voucherDate)
                .remark(group.remark)
                .status(VoucherStatusEnum.DRAFT.getValue())
                .carryForward(YesNoEnum.n.name())
                .items(items)
                .build();
        // Force draft-only path: never carry audit/sender/manager from Excel
        dto.setId(existing != null && "overwrite".equalsIgnoreCase(StringUtils.trim(conflictMode))
                ? existing.getId() : null);
        dto.setAuditMemberId(null);
        dto.setAuditMemberName(null);
        dto.setAuditDate(null);
        dto.setSenderId(null);
        dto.setSenderName(null);
        dto.setSenderDate(null);
        dto.setManagerId(null);
        dto.setManagerName(null);
        dto.setManagerDate(null);
        if (dto.getId() == null) {
            return save(dto);
        }
        Message<String> updateResult = update(dto);
        if (updateResult.getCode() == Message.SUCCESS) {
            clearImportWorkflowFields(dto.getId());
        }
        return updateResult;
    }

    private void clearImportWorkflowFields(String voucherId) {
        baseMapper.update(null, Wrappers.<Voucher>lambdaUpdate()
                .eq(Voucher::getId, voucherId)
                .set(Voucher::getStatus, VoucherStatusEnum.DRAFT.getValue())
                .set(Voucher::getAuditMemberId, null)
                .set(Voucher::getAuditMemberName, null)
                .set(Voucher::getAuditDate, null)
                .set(Voucher::getSenderId, null)
                .set(Voucher::getSenderName, null)
                .set(Voucher::getSenderDate, null)
                .set(Voucher::getManagerId, null)
                .set(Voucher::getManagerName, null)
                .set(Voucher::getManagerDate, null));
    }

    private BookSubject resolveSubject(String bookId, String code, Map<String, BookSubject> cache) {
        if (cache.containsKey(code)) {
            return cache.get(code);
        }
        BookSubject subject = bookSubjectService.selectSubject(bookId, code);
        cache.put(code, subject);
        return subject;
    }

    private static List<ImportLine> readImportLines(Sheet sheet) {
        List<ImportLine> lines = new ArrayList<>();
        int last = sheet.getLastRowNum();
        for (int r = 0; r <= last; r++) {
            Row row = sheet.getRow(r);
            if (row == null || isBlankImportRow(row) || isHeaderLikeRow(row)) {
                continue;
            }
            ImportLine line = new ImportLine();
            line.excelRow = r + 1;
            line.dateRaw = StringUtils.trimToEmpty(getDateCellRaw(row, 0));
            line.wordHead = StringUtils.trimToEmpty(ExcelUtils.getValue(row, 1));
            line.wordNumRaw = StringUtils.trimToEmpty(ExcelUtils.getValue(row, 2));
            line.receiptRaw = StringUtils.trimToEmpty(ExcelUtils.getValue(row, 3));
            line.remark = StringUtils.trimToNull(ExcelUtils.getValue(row, 4));
            line.summary = StringUtils.trimToEmpty(ExcelUtils.getValue(row, 5));
            line.subjectCode = StringUtils.trimToEmpty(ExcelUtils.getValue(row, 6));
            line.debitAmount = parseDecimal(ExcelUtils.getValue(row, 7));
            line.creditAmount = parseDecimal(ExcelUtils.getValue(row, 8));
            lines.add(line);
        }
        return lines;
    }

    static boolean isLegacyVoucherExportSheet(Sheet sheet) {
        int max = Math.min(2, sheet.getLastRowNum());
        for (int r = 0; r <= max; r++) {
            Row row = sheet.getRow(r);
            if (row == null) {
                continue;
            }
            String c0 = StringUtils.trimToEmpty(ExcelUtils.getValue(row, 0));
            String c1 = StringUtils.trimToEmpty(ExcelUtils.getValue(row, 1));
            if ("凭证字".equals(c0) || "账套ID".equals(c1) || "账套Id".equals(c1)) {
                return true;
            }
        }
        return false;
    }

    static boolean isHeaderLikeRow(Row row) {
        String c0 = StringUtils.trimToEmpty(ExcelUtils.getValue(row, 0));
        String c1 = StringUtils.trimToEmpty(ExcelUtils.getValue(row, 1));
        if (VOUCHER_IO_HEADERS[0].equals(c0) || VOUCHER_IO_HEADERS[1].equals(c1)) {
            return true;
        }
        return "凭证字".equals(c0) || "账套ID".equals(c1) || "账套Id".equals(c1);
    }

    static List<ImportGroup> groupImportLines(List<ImportLine> lines) {
        List<ImportGroup> groups = new ArrayList<>();
        ImportGroup current = null;
        for (ImportLine line : lines) {
            boolean startsGroup = StringUtils.isNotBlank(line.dateRaw)
                    || StringUtils.isNotBlank(line.wordHead)
                    || StringUtils.isNotBlank(line.wordNumRaw);
            if (startsGroup || current == null) {
                current = new ImportGroup();
                current.firstExcelRow = line.excelRow;
                current.voucherDate = parseDate(line.dateRaw);
                current.wordHead = StringUtils.trimToNull(line.wordHead);
                current.wordNum = parseInteger(line.wordNumRaw);
                current.receiptNum = parseInteger(line.receiptRaw);
                current.remark = line.remark;
                groups.add(current);
            }
            current.lines.add(line);
        }
        return groups;
    }

    private static boolean isBlankImportRow(Row row) {
        for (int i = 0; i < VOUCHER_IO_HEADERS.length; i++) {
            if (StringUtils.isNotBlank(ExcelUtils.getValue(row, i))) {
                return false;
            }
        }
        return true;
    }

    private static String getDateCellRaw(Row row, int col) {
        org.apache.poi.ss.usermodel.Cell cell = row.getCell(col);
        if (cell == null) {
            return "";
        }
        if (cell.getCellType() == org.apache.poi.ss.usermodel.CellType.NUMERIC
                && org.apache.poi.ss.usermodel.DateUtil.isCellDateFormatted(cell)) {
            return new SimpleDateFormat(DateUtils.FORMAT_DATE_DEFAULT).format(cell.getDateCellValue());
        }
        Object value = cn.hutool.poi.excel.cell.CellUtil.getCellValue(cell);
        if (value instanceof Date date) {
            return new SimpleDateFormat(DateUtils.FORMAT_DATE_DEFAULT).format(date);
        }
        return StringUtils.trimToEmpty(cn.hutool.core.convert.Convert.toStr(value, ""));
    }

    private static void writeExportHeaderCells(Row row, VoucherVo voucher, SimpleDateFormat dateFmt) {
        if (voucher.getVoucherDate() != null) {
            row.createCell(0).setCellValue(dateFmt.format(voucher.getVoucherDate()));
        } else {
            row.createCell(0).setCellValue("");
        }
        row.createCell(1).setCellValue(StringUtils.defaultString(voucher.getWordHead()));
        if (voucher.getWordNum() != null) {
            row.createCell(2).setCellValue(voucher.getWordNum());
        } else {
            row.createCell(2).setCellValue("");
        }
        if (voucher.getReceiptNum() != null) {
            row.createCell(3).setCellValue(voucher.getReceiptNum());
        } else {
            row.createCell(3).setCellValue(0);
        }
        row.createCell(4).setCellValue(StringUtils.defaultString(voucher.getRemark()));
    }

    private static void setAmountCell(Row row, int col, BigDecimal amount) {
        if (amount == null) {
            row.createCell(col).setCellValue("");
        } else {
            row.createCell(col).setCellValue(amount.doubleValue());
        }
    }

    private static void setAmountCell(Row row, int col, BigDecimal amount, CellStyle style) {
        setAmountCell(row, col, amount);
        row.getCell(col).setCellStyle(style);
    }

    private static void styleHeaderRow(Workbook workbook, Row header) {
        CellStyle style = createFillStyle(workbook, IndexedColors.GREY_25_PERCENT);
        Font font = workbook.createFont();
        font.setBold(true);
        style.setFont(font);
        for (int i = 0; i < VOUCHER_IO_HEADERS.length; i++) {
            header.getCell(i).setCellStyle(style);
        }
    }

    private static void applyColumnWidths(Sheet sheet) {
        for (int i = 0; i < COLUMN_WIDTHS.length; i++) {
            sheet.setColumnWidth(i, COLUMN_WIDTHS[i] * 256);
        }
    }

    private static void writeInstructionSheet(Workbook workbook) {
        Sheet sheet = workbook.createSheet(SHEET_INSTRUCTIONS);
        String[] lines = {
                "凭证导入填写说明",
                "• 列含义：凭证日期、凭证字头、凭证字号、附单据数、备注、摘要、科目编码、借方金额、贷方金额。",
                "• 同一凭证有多条分录时，续行只填写摘要、科目编码及借贷金额，凭证基本信息留空。",
                "• 凭证日期请使用 yyyy-MM-dd 格式。",
                "• 科目编码必须是当前账套中已存在的科目编码。",
                "• 凭证字号冲突时，导入界面可选择覆盖未过账凭证或跳过冲突；已过账凭证不可覆盖。",
                "• “凭证”页中的黄色示例行可删除后再填写。"
        };
        CellStyle titleStyle = workbook.createCellStyle();
        Font titleFont = workbook.createFont();
        titleFont.setBold(true);
        titleStyle.setFont(titleFont);
        for (int i = 0; i < lines.length; i++) {
            Row row = sheet.createRow(i);
            row.createCell(0).setCellValue(lines[i]);
            if (i == 0) {
                row.getCell(0).setCellStyle(titleStyle);
            }
        }
        sheet.setColumnWidth(0, 100 * 256);
    }

    private static CellStyle createFillStyle(Workbook workbook, IndexedColors fillColor) {
        CellStyle style = workbook.createCellStyle();
        style.setFillForegroundColor(fillColor.getIndex());
        style.setFillPattern(FillPatternType.SOLID_FOREGROUND);
        return style;
    }

    private static CellStyle createAmountStyle(Workbook workbook, IndexedColors fillColor) {
        CellStyle style = fillColor == null ? workbook.createCellStyle() : createFillStyle(workbook, fillColor);
        style.setDataFormat(workbook.createDataFormat().getFormat("0.00"));
        return style;
    }

    private static BigDecimal nz(BigDecimal v) {
        return v == null ? BigDecimal.ZERO : v;
    }

    private static BigDecimal parseDecimal(String raw) {
        String text = StringUtils.trimToEmpty(raw);
        if (StringUtils.isBlank(text)) {
            return BigDecimal.ZERO;
        }
        try {
            return new BigDecimal(text.replace(",", ""));
        } catch (Exception e) {
            return BigDecimal.ZERO;
        }
    }

    private static Integer parseInteger(String raw) {
        String text = StringUtils.trimToEmpty(raw);
        if (StringUtils.isBlank(text)) {
            return null;
        }
        try {
            return new BigDecimal(text.replace(",", "")).intValue();
        } catch (Exception e) {
            return null;
        }
    }

    private static Date parseDate(String raw) {
        String text = StringUtils.trimToEmpty(raw);
        if (StringUtils.isBlank(text)) {
            return null;
        }
        // Excel serial date (e.g. 45840 or 45840.0)
        if (text.matches("\\d{5}(\\.\\d+)?")) {
            try {
                double serial = Double.parseDouble(text);
                if (serial > 20000 && serial < 80000) {
                    return org.apache.poi.ss.usermodel.DateUtil.getJavaDate(serial);
                }
            } catch (Exception ignored) {
                // fall through
            }
        }
        String[] patterns = {
                DateUtils.FORMAT_DATE_DEFAULT,
                "yyyy/MM/dd",
                "yyyy-M-d",
                "yyyy/M/d",
                "yyyy年M月d日",
                "EEE MMM dd HH:mm:ss zzz yyyy"
        };
        for (String pattern : patterns) {
            try {
                SimpleDateFormat sdf = new SimpleDateFormat(pattern, pattern.startsWith("EEE")
                        ? Locale.ENGLISH : Locale.CHINA);
                sdf.setLenient(false);
                return sdf.parse(text);
            } catch (ParseException ignored) {
                // try next
            }
        }
        return null;
    }

    private static void addImportError(VoucherImportResultVo result, int row, String code, String message) {
        result.setFailed(result.getFailed() + 1);
        VoucherImportResultVo.RowError err = new VoucherImportResultVo.RowError();
        err.setRow(row);
        err.setCode(code);
        err.setMessage(message);
        result.getErrors().add(err);
    }

    private static String groupLabel(ImportGroup group) {
        String head = StringUtils.defaultString(group.wordHead, DEFAULT_WORD_HEAD);
        if (group.wordNum != null) {
            return head + "-" + group.wordNum;
        }
        return head;
    }

    static final class ImportLine {
        int excelRow;
        String dateRaw;
        String wordHead;
        String wordNumRaw;
        String receiptRaw;
        String remark;
        String summary;
        String subjectCode;
        BigDecimal debitAmount;
        BigDecimal creditAmount;
    }

    static final class ImportGroup {
        int firstExcelRow;
        Date voucherDate;
        String wordHead;
        Integer wordNum;
        Integer receiptNum;
        String remark;
        List<ImportLine> lines = new ArrayList<>();
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
        return deleteInternal(ids, bookId, false);
    }

    @Transactional(propagation = Propagation.MANDATORY)
    public Message<String> deletePayrollVoucher(String id, String bookId) {
        return deleteInternal(List.of(id), bookId, true);
    }

    private Message<String> deleteInternal(List<String> ids, String bookId, boolean currentRead) {
        if (ids == null || ids.isEmpty()) {
            return new Message<>(Message.SUCCESS);
        }

        LambdaQueryWrapper<Voucher> checkLqw = Wrappers.lambdaQuery();
        checkLqw.in(Voucher::getId, ids);
        checkLqw.eq(Voucher::getBookId, bookId);
        if (currentRead) {
            checkLqw.last("FOR UPDATE");
        }
        List<Voucher> toDelete = baseMapper.selectList(checkLqw);
        if (toDelete.size() != ids.size()) {
            return new Message<>(Message.FAIL, "部分凭证不存在");
        }
        for (Voucher voucher : toDelete) {
            boolean unpostedCompleted = VoucherStatusEnum.COMPLETED.getValue().equals(voucher.getStatus())
                    && StringUtils.isBlank(voucher.getSenderId());
            boolean draft = VoucherStatusEnum.DRAFT.getValue().equals(voucher.getStatus());
            if (!draft && !unpostedCompleted) {
                return new Message<>(Message.FAIL, "仅暂存或待过账（未过账）的凭证可以删除");
            }
            if (StringUtils.isNotBlank(voucher.getSenderId())) {
                return new Message<>(Message.FAIL, "已过账的凭证不能删除");
            }
            if (!isVoucherInOpenPeriod(voucher)) {
                return new Message<>(Message.FAIL, "已结账期间的凭证不允许删除");
            }
        }

        // 删除凭证项和现金流量的关系
        LambdaQueryWrapper<VoucherItem> itemsLqw = Wrappers.<VoucherItem>lambdaQuery()
                .in(VoucherItem::getVoucherId, ids);
        if (currentRead) {
            itemsLqw.last("FOR UPDATE");
        }
        var voucherItems = voucherItemMapper.selectList(itemsLqw);
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

        if (update == ids.size()) {
            journalEntryServiceProvider.ifAvailable(journal -> journal.clearLinksByVoucherIds(ids));
            // OBS-CARRY-STALE-POINTER: clear carryforward rows that pointed at deleted vouchers
            settlementCarryforwardMapper.delete(
                    Wrappers.<SettlementCarryforward>lambdaQuery()
                            .in(SettlementCarryforward::getVoucherId, ids));
            return new Message<>(Message.SUCCESS, "删除成功");
        }
        return new Message<>(Message.FAIL, "删除失败");
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

        bookSealGuard.assertWritable(bookId);

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
     * 作废凭证：保留字号、不参与账表与结账检查，可恢复为暂存。
     * 仅暂存/被拒绝且未过账的凭证可作废；已过账凭证须走红字冲销。
     */
    @Transactional
    public Message<String> voidById(String id, String bookId) {
        Voucher voucher = baseMapper.selectById(id);
        if (voucher == null || !bookId.equals(voucher.getBookId())) {
            return Message.failed("凭证不存在或不属于当前账套");
        }
        Message<String> periodLock = rejectClosedPeriodWrite(VoucherChangeDto.builder()
                .bookId(bookId)
                .voucherDate(voucher.getVoucherDate())
                .build());
        if (periodLock != null) {
            return periodLock;
        }
        if (StringUtils.isNotBlank(voucher.getSenderId())) {
            return Message.failed("已过账凭证不能作废，请使用红字冲销");
        }
        String status = voucher.getStatus();
        if (VoucherStatusEnum.CANCELLED.getValue().equals(status)) {
            return Message.failed("凭证已是作废状态");
        }
        if (!VoucherStatusEnum.DRAFT.getValue().equals(status)
                && !VoucherStatusEnum.REJECTED.getValue().equals(status)) {
            return Message.failed("仅暂存或被拒绝的凭证可作废（审核中请先撤回，已审核请先取消审核）");
        }
        Voucher update = new Voucher();
        update.setId(voucher.getId());
        update.setStatus(VoucherStatusEnum.CANCELLED.getValue());
        baseMapper.updateById(update);
        journalEntryServiceProvider.ifAvailable(journal ->
                journal.clearLinksByVoucherIds(List.of(id)));
        return Message.ok("作废成功");
    }

    /**
     * 恢复作废：已作废凭证恢复为暂存，字号不变。
     */
    @Transactional
    public Message<String> unvoidById(String id, String bookId) {
        Voucher voucher = baseMapper.selectById(id);
        if (voucher == null || !bookId.equals(voucher.getBookId())) {
            return Message.failed("凭证不存在或不属于当前账套");
        }
        Message<String> periodLock = rejectClosedPeriodWrite(VoucherChangeDto.builder()
                .bookId(bookId)
                .voucherDate(voucher.getVoucherDate())
                .build());
        if (periodLock != null) {
            return periodLock;
        }
        if (!VoucherStatusEnum.CANCELLED.getValue().equals(voucher.getStatus())) {
            return Message.failed("仅已作废的凭证可以恢复");
        }
        Voucher update = new Voucher();
        update.setId(voucher.getId());
        update.setStatus(VoucherStatusEnum.DRAFT.getValue());
        baseMapper.updateById(update);
        return Message.ok("已恢复为暂存");
    }

    /**
     * 红字冲销：为已过账凭证生成一张金额全负的冲销凭证（暂存态，走正常审核/过账流程后生效）。
     * 冲销凭证落在当前开放账期，通过 sourceVoucherId 关联原凭证；一张凭证只允许冲销一次。
     */
    @Transactional
    public Message<String> reverseById(String id, String bookId) {
        bookSealGuard.assertWritable(bookId);
        Voucher source = baseMapper.selectById(id);
        if (source == null || !bookId.equals(source.getBookId())) {
            return Message.failed("凭证不存在或不属于当前账套");
        }
        if (!VoucherStatusEnum.COMPLETED.getValue().equals(source.getStatus())
                || StringUtils.isBlank(source.getSenderId())) {
            return Message.failed("仅已过账凭证可以红字冲销（未过账凭证请直接作废或删除）");
        }
        Long reversedCount = baseMapper.selectCount(Wrappers.<Voucher>lambdaQuery()
                .eq(Voucher::getSourceVoucherId, id));
        if (reversedCount != null && reversedCount > 0) {
            return Message.failed("该凭证已生成过冲销凭证，请勿重复冲销");
        }
        Message<VoucherVo> voResult = queryById(id);
        if (voResult.getCode() != Message.SUCCESS || voResult.getData() == null) {
            return Message.failed(voResult.getMessage());
        }
        VoucherVo vo = voResult.getData();

        // 冲销凭证必须落在当前开放账期：系统日早于或晚于开放账期时，均钳到该账期首日
        // （BUG-JEXT-REVERSE-POST：仅处理「系统日 < 开放账期」时，历史账套回测会把冲销落到未来月）
        String currentTerm = configSysService.getCurrentTerm(bookId);
        Date reversalDate = new Date();
        String todayTerm = DateUtils.format(reversalDate, DateUtils.FORMAT_DATE_YYYY_MM);
        if (StringUtils.isNotBlank(currentTerm) && !currentTerm.equals(todayTerm)) {
            int y = Integer.parseInt(currentTerm.substring(0, 4));
            int m = Integer.parseInt(currentTerm.substring(5, 7));
            reversalDate = new java.util.GregorianCalendar(y, m - 1, 1).getTime();
        }
        final Date journalTradeDate = reversalDate;
        java.util.Calendar cal = java.util.Calendar.getInstance();
        cal.setTime(reversalDate);
        int year = cal.get(java.util.Calendar.YEAR);
        int month = cal.get(java.util.Calendar.MONTH) + 1;

        VoucherChangeDto dto = toChangeDto(vo);
        dto.setId(null);
        dto.setBookId(bookId);
        dto.setWord(null);
        dto.setWordNum(getAbleWordNum(bookId, source.getWordHead(), year, month).getData());
        dto.setVoucherDate(reversalDate);
        dto.setVoucherYear(year);
        dto.setVoucherMonth(month);
        dto.setStatus(VoucherStatusEnum.DRAFT.getValue());
        dto.setRemark("红字冲销「" + StringUtils.defaultString(vo.getWord()) + "」凭证");
        dto.setAuditMemberId(null);
        dto.setAuditMemberName(null);
        dto.setAuditDate(null);
        dto.setSenderId(null);
        dto.setSenderName(null);
        dto.setSenderDate(null);
        dto.setManagerId(null);
        dto.setManagerName(null);
        dto.setManagerDate(null);
        for (VoucherItemChangeDto item : dto.getItems()) {
            item.setId(null);
            item.setVoucherId(null);
            item.setDebitAmount(item.getDebitAmount() != null ? item.getDebitAmount().negate() : null);
            item.setCreditAmount(item.getCreditAmount() != null ? item.getCreditAmount().negate() : null);
            item.setNum(item.getNum() != null ? -item.getNum() : null);
            item.setSummary("冲销：" + StringUtils.defaultString(item.getSummary()));
        }
        Message<String> saveResult = save(dto);
        if (saveResult.getCode() != Message.SUCCESS) {
            return saveResult;
        }
        final String reverseId = saveResult.getData();
        Voucher link = new Voucher();
        link.setId(reverseId);
        link.setSourceVoucherId(id);
        baseMapper.updateById(link);
        journalEntryServiceProvider.ifAvailable(journal ->
                journal.createReversalEntriesForVoucher(id, reverseId, bookId, journalTradeDate));
        return new Message<>(Message.SUCCESS, "红字冲销凭证已生成（暂存），审核过账后生效", reverseId);
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

    private Message<String> validateItemsForSubmit(String bookId, List<VoucherItemChangeDto> items) {
        Message<String> saveValidation = validateItemsForSave(bookId, items);
        if (saveValidation.getCode() != Message.SUCCESS) {
            return saveValidation;
        }
        for (VoucherItemChangeDto item : filterValidVoucherItems(items)) {
            prepareVoucherItem(item);
        }
        return new Message<>(Message.SUCCESS);
    }

    private Message<String> validateItemsForSave(String bookId, List<VoucherItemChangeDto> items) {
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
        boolean assistEnabled = StringUtils.isNotBlank(bookId) && configSysService.isAssistAccEnabled(bookId);
        for (VoucherItemChangeDto item : validItems) {
            prepareVoucherItem(item);
            if (StringUtils.isBlank(item.getSubjectId())) {
                return Message.failed("存在未选择科目的分录");
            }
            if (isBlankAmount(item.getDebitAmount()) && isBlankAmount(item.getCreditAmount())) {
                return Message.failed("存在未填写金额的分录");
            }
            if (assistEnabled) {
                Message<String> auxCheck = validateRequiredAuxiliary(item);
                if (auxCheck.getCode() != Message.SUCCESS) {
                    return auxCheck;
                }
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

    /**
     * When subject auxiliary config marks {@code must=true}, require a non-empty selection
     * (matches voucher-edit UI {@code checkAuxiliary}).
     */
    private Message<String> validateRequiredAuxiliary(VoucherItemChangeDto item) {
        BookSubject subject = bookSubjectService.getById(item.getSubjectId());
        if (subject == null || StringUtils.isBlank(subject.getAuxiliary())
                || "[]".equals(subject.getAuxiliary().trim())) {
            return new Message<>(Message.SUCCESS);
        }
        List<SubjectAuxiliary> cfg;
        try {
            cfg = JSONUtil.toList(subject.getAuxiliary(), SubjectAuxiliary.class);
        } catch (Exception ex) {
            return new Message<>(Message.SUCCESS);
        }
        if (CollUtil.isEmpty(cfg)) {
            return new Message<>(Message.SUCCESS);
        }
        List<VoucherItemAuxiliaryDto> selected = item.getAuxiliary() == null ? List.of() : item.getAuxiliary();
        for (SubjectAuxiliary aux : cfg) {
            if (aux == null || !Boolean.TRUE.equals(aux.getMust())) {
                continue;
            }
            String typeId = StringUtils.isNotBlank(aux.getValue()) ? aux.getValue() : aux.getId();
            if (StringUtils.isBlank(typeId)) {
                continue;
            }
            boolean present = selected.stream().anyMatch(sel ->
                    sel != null
                            && typeId.equals(sel.getId())
                            && CollUtil.isNotEmpty(sel.getValue())
                            && sel.getValue().stream().anyMatch(v ->
                                    v != null && StringUtils.isNotBlank(v.getValue())));
            if (!present) {
                String label = StringUtils.defaultIfBlank(aux.getLabel(), typeId);
                return Message.failed("存在未选择辅助核算的分录（" + label + "）");
            }
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
     * @return null 表示可改；否则为用户可见拒绝原因
     */
    static String modifyBlockedReason(Voucher voucher, boolean inOpenPeriod) {
        if (voucher == null) {
            return "凭证不存在";
        }
        if (StringUtils.isNotBlank(voucher.getSenderId())) {
            return "已过账凭证不能直接修改，请先反过账后再改，或使用红字冲销";
        }
        if (VoucherStatusEnum.CANCELLED.getValue().equals(voucher.getStatus())) {
            return "已作废凭证不能修改";
        }
        if (VoucherStatusEnum.COMPLETED.getValue().equals(voucher.getStatus())) {
            return "已审核凭证不能直接修改，请先反审核后再改";
        }
        if (VoucherStatusEnum.UNDER_REVIEW.getValue().equals(voucher.getStatus())) {
            return "审核中的凭证不能直接修改，请先撤回审核申请后再改";
        }
        if (!inOpenPeriod) {
            return "已结账期间的凭证不能修改";
        }
        return null;
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

    /**
     * Reject create/update when voucher period is before the book's current open term.
     * @return failed Message when locked; null when allowed
     */
    private Message<String> rejectClosedPeriodWrite(VoucherChangeDto dto) {
        if (dto == null || StringUtils.isBlank(dto.getBookId())) {
            return null;
        }
        // 封存账套整体只读（优先于期间锁）
        bookSealGuard.assertWritable(dto.getBookId());
        String voucherTerm = null;
        if (dto.getVoucherDate() != null) {
            voucherTerm = DateUtils.format(dto.getVoucherDate(), DateUtils.FORMAT_DATE_YYYY_MM);
        } else if (dto.getVoucherYear() != null && dto.getVoucherMonth() != null) {
            voucherTerm = String.format("%d-%02d", dto.getVoucherYear(), dto.getVoucherMonth());
        }
        if (StringUtils.isBlank(voucherTerm)) {
            return null;
        }
        String currentTerm = configSysService.getCurrentTerm(dto.getBookId());
        if (StringUtils.isBlank(currentTerm)) {
            return null;
        }
        if (currentTerm.compareTo(voucherTerm) > 0) {
            return Message.failed("已结账期间不允许新增或修改凭证（当前开放账期 " + currentTerm + "）");
        }
        return null;
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
