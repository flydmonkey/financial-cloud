package com.financial.cloud.service.fixedasset;

import cn.hutool.core.bean.BeanUtil;
import cn.hutool.core.collection.CollUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.incrementer.IdentifierGenerator;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.financial.cloud.common.ExcelImport;
import com.financial.cloud.common.Message;
import com.financial.cloud.domain.book.Book;
import com.financial.cloud.domain.book.BookSubject;
import com.financial.cloud.domain.fixedasset.AssetCategory;
import com.financial.cloud.domain.fixedasset.FixedAsset;
import com.financial.cloud.domain.fixedasset.FixedAssetDepr;
import com.financial.cloud.domain.idm.Organizations;
import com.financial.cloud.domain.voucher.Voucher;
import com.financial.cloud.dto.common.ListIdsDto;
import com.financial.cloud.dto.fixedasset.FixedAssetChangeDto;
import com.financial.cloud.dto.fixedasset.FixedAssetDisposeDto;
import com.financial.cloud.dto.fixedasset.FixedAssetDisposeResultVo;
import com.financial.cloud.dto.fixedasset.FixedAssetImportResultVo;
import com.financial.cloud.dto.fixedasset.FixedAssetPageDto;
import com.financial.cloud.dto.fixedasset.FixedAssetSaveResultVo;
import com.financial.cloud.dto.fixedasset.FixedAssetVo;
import com.financial.cloud.dto.voucher.VoucherChangeDto;
import com.financial.cloud.dto.voucher.VoucherItemChangeDto;
import com.financial.cloud.enums.error.FixedAssetErrorCode;
import com.financial.cloud.enums.fixedasset.DepreciationMethod;
import com.financial.cloud.enums.fixedasset.FixedAssetStatus;
import com.financial.cloud.enums.voucher.VoucherStatusEnum;
import com.financial.cloud.exception.ServiceException;
import com.financial.cloud.repository.book.BookMapper;
import com.financial.cloud.repository.fixedasset.AssetCategoryMapper;
import com.financial.cloud.repository.fixedasset.FixedAssetDeprMapper;
import com.financial.cloud.repository.fixedasset.FixedAssetMapper;
import com.financial.cloud.service.book.BookSubjectService;
import com.financial.cloud.service.config.ConfigSysService;
import com.financial.cloud.service.idm.OrganizationsService;
import com.financial.cloud.service.voucher.VoucherService;
import com.financial.cloud.util.ExcelUtils;
import com.financial.cloud.util.FixedAssetCopyRules;
import com.financial.cloud.util.FixedAssetDepreciationRules;
import com.financial.cloud.util.FixedAssetDisposalRules;
import com.financial.cloud.util.FixedAssetPurchaseRules;
import com.financial.cloud.util.SubjectCodeCompat;
import com.financial.cloud.util.VoucherUtils;
import com.financial.cloud.util.excel.ExcelExporter;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.apache.commons.lang3.StringUtils;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.IOException;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.time.YearMonth;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

@RequiredArgsConstructor
@Service
public class FixedAssetService extends ServiceImpl<FixedAssetMapper, FixedAsset> {

    private static final String DEFAULT_WORD = "记";
    private static final String DEFAULT_DISPOSE_SUMMARY = "固定资产清理";
    private static final String DEFAULT_PURCHASE_SUMMARY = "购入固定资产";
    private static final String[] CARD_EXPORT_HEADERS = {
            "编码", "名称", "类别编码", "类别名称", "部门", "启用日期", "数量", "规格型号", "存放地点",
            "折旧方法", "使用月数", "预计总工作量", "残值率%", "原值", "税额", "减值",
            "期初累计折旧", "已提期数", "固定资产科目编码", "累计折旧科目编码", "折旧费用科目编码", "备注"
    };

    private final FixedAssetMapper fixedAssetMapper;
    private final FixedAssetDeprMapper fixedAssetDeprMapper;
    private final AssetCategoryMapper assetCategoryMapper;
    private final IdentifierGenerator identifierGenerator;
    private final ConfigSysService configSysService;
    private final BookSubjectService bookSubjectService;
    private final FixedAssetChangeService fixedAssetChangeService;
    private final BookMapper bookMapper;
    private final VoucherService voucherService;
    private final OrganizationsService organizationsService;

    public Message<FixedAssetVo> getById(String id) {
        FixedAsset entity = fixedAssetMapper.selectById(id);
        if (entity == null) {
            return Message.ok(null);
        }
        Map<String, String> categoryNames = loadCategoryNames(List.of(entity));
        Map<String, String> deptNames = loadDeptNames(List.of(entity));
        Map<String, String> voucherWords = loadVoucherWords(List.of(entity));
        Set<String> lockedIds = loadLockedAssetIds(entity.getBookId(), List.of(entity.getId()));
        return Message.ok(toVo(entity, categoryNames, deptNames, voucherWords, lockedIds));
    }

    public Message<Page<FixedAssetVo>> pageList(FixedAssetPageDto dto) {
        Page<FixedAsset> page = fixedAssetMapper.selectPage(dto.build(), buildQueryWrapper(dto));
        Page<FixedAssetVo> result = new Page<>(page.getCurrent(), page.getSize(), page.getTotal());
        if (CollUtil.isNotEmpty(page.getRecords())) {
            Map<String, String> categoryNames = loadCategoryNames(page.getRecords());
            Map<String, String> deptNames = loadDeptNames(page.getRecords());
            Map<String, String> voucherWords = loadVoucherWords(page.getRecords());
            Set<String> lockedIds = loadLockedAssetIds(dto.getBookId(),
                    page.getRecords().stream().map(FixedAsset::getId).collect(Collectors.toList()));
            result.setRecords(page.getRecords().stream()
                    .map(e -> toVo(e, categoryNames, deptNames, voucherWords, lockedIds))
                    .collect(Collectors.toList()));
        }
        return Message.ok(result);
    }

    @Transactional
    public Message<FixedAssetSaveResultVo> save(FixedAssetChangeDto dto) {
        applyCategoryDefaults(dto);
        applyDefaultSubjects(dto);
        FixedAsset entity = FixedAsset.builder().build();
        BeanUtil.copyProperties(dto, entity);
        normalizeBeforeSave(entity, true);
        validCodeUnique(entity);
        String id = identifierGenerator.nextId(entity).toString();
        entity.setId(id);
        boolean ok = super.save(entity);
        if (!ok) {
            return new Message<>(Message.FAIL, "新增失败");
        }
        FixedAssetSaveResultVo result = new FixedAssetSaveResultVo();
        result.setAssetId(id);
        if (FixedAssetPurchaseRules.shouldCreateVoucher(entity.getOriginalValue(), entity.getTaxAmount())) {
            String voucherId = createPurchaseVoucher(entity);
            entity.setPurchaseVoucherId(voucherId);
            super.updateById(entity);
            result.setPurchaseVoucherId(voucherId);
            return new Message<>(Message.SUCCESS, "新增成功，已生成购入凭证", result);
        }
        return new Message<>(Message.SUCCESS, "新增成功", result);
    }

    private String createPurchaseVoucher(FixedAsset entity) {
        String bookId = entity.getBookId();
        BigDecimal original = FixedAssetPurchaseRules.nz(entity.getOriginalValue()).setScale(2, RoundingMode.HALF_UP);
        BigDecimal tax = FixedAssetPurchaseRules.nz(entity.getTaxAmount()).setScale(2, RoundingMode.HALF_UP);
        BigDecimal credit = FixedAssetPurchaseRules.creditAmount(original, tax);

        BookSubject faSubject = requireSubjectById(bookId, entity.getFixedAssetSubjectId());
        if (faSubject == null) {
            faSubject = resolveSubject(bookId, null, "1601");
        }
        BookSubject counterpart = resolveSubject(bookId, entity.getPurchaseCounterpartSubjectId(), "1002", "1001");
        BookSubject taxSubject = null;
        if (tax.compareTo(BigDecimal.ZERO) > 0) {
            taxSubject = resolveSubject(bookId, entity.getTaxSubjectId(),
                    "2221.01.01", "2221.01", "2171.01.01", "2221");
        }
        if (faSubject == null || counterpart == null || (tax.compareTo(BigDecimal.ZERO) > 0 && taxSubject == null)) {
            throw new ServiceException(FixedAssetErrorCode.PURCHASE_SUBJECT_REQUIRED);
        }
        if (original.compareTo(BigDecimal.ZERO) <= 0 && tax.compareTo(BigDecimal.ZERO) <= 0) {
            throw new ServiceException(FixedAssetErrorCode.PURCHASE_SUBJECT_REQUIRED);
        }

        String period = StringUtils.isNotBlank(entity.getEntryPeriod())
                ? entity.getEntryPeriod()
                : (entity.getStartUseDate() != null
                ? FixedAssetDepreciationRules.periodOf(entity.getStartUseDate())
                : configSysService.getCurrentTerm(bookId));
        Date voucherDate = entity.getStartUseDate() != null ? entity.getStartUseDate() : new Date();
        String summary = DEFAULT_PURCHASE_SUMMARY + "：" + entity.getCode() + " " + entity.getName();

        Book book = bookMapper.selectById(bookId);
        int year;
        int month;
        if (StringUtils.isNotBlank(period) && period.contains("-")) {
            year = Integer.parseInt(period.split("-")[0]);
            month = Integer.parseInt(period.split("-")[1]);
        } else {
            java.util.Calendar cal = java.util.Calendar.getInstance();
            cal.setTime(voucherDate);
            year = cal.get(java.util.Calendar.YEAR);
            month = cal.get(java.util.Calendar.MONTH) + 1;
        }
        Integer wordNum = voucherService.getAbleWordNum(bookId, DEFAULT_WORD, year, month).getData();

        List<VoucherItemChangeDto> items = new ArrayList<>();
        if (original.compareTo(BigDecimal.ZERO) > 0) {
            items.add(createItem(faSubject, summary, original, true));
        }
        if (tax.compareTo(BigDecimal.ZERO) > 0) {
            items.add(createItem(taxSubject, summary + "（进项税）", tax, true));
        }
        items.add(createItem(counterpart, summary, credit, false));

        BigDecimal debit = items.stream()
                .map(i -> FixedAssetDisposalRules.nz(i.getDebitAmount()))
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        VoucherChangeDto voucherDto = new VoucherChangeDto();
        voucherDto.setWordHead(DEFAULT_WORD);
        voucherDto.setWordNum(wordNum);
        voucherDto.setBookId(bookId);
        voucherDto.setCompanyName(book != null ? book.getCompanyName() : "");
        voucherDto.setVoucherDate(voucherDate);
        voucherDto.setVoucherYear(year);
        voucherDto.setVoucherMonth(month);
        voucherDto.setDebitAmount(debit);
        voucherDto.setCreditAmount(credit);
        voucherDto.setReceiptNum(0);
        voucherDto.setRemark(summary);
        voucherDto.setStatus(VoucherStatusEnum.DRAFT.getValue());
        voucherDto.setItems(items);
        Message<String> voucherMsg = voucherService.save(voucherDto);
        if (voucherMsg.getCode() != Message.SUCCESS) {
            throw new IllegalStateException(StringUtils.defaultIfBlank(voucherMsg.getMessage(), "购入凭证生成失败"));
        }
        return voucherMsg.getData();
    }

    @Transactional
    public Message<String> update(FixedAssetChangeDto dto) {
        FixedAsset existing = fixedAssetMapper.selectById(dto.getId());
        if (existing == null) {
            throw new ServiceException(FixedAssetErrorCode.ASSET_NOT_FOUND);
        }
        boolean locked = hasDepr(existing.getId());
        if (locked) {
            assertCalcFieldsUnchanged(existing, dto);
        }
        applyCategoryDefaults(dto);
        applyDefaultSubjects(dto);
        FixedAsset entity = FixedAsset.builder().build();
        BeanUtil.copyProperties(dto, entity);
        if (locked) {
            // 强制保留计算字段
            entity.setOriginalValue(existing.getOriginalValue());
            entity.setUsefulLifeMonths(existing.getUsefulLifeMonths());
            entity.setExpectedTotalWork(existing.getExpectedTotalWork());
            entity.setResidualRate(existing.getResidualRate());
            entity.setDepreciationMethod(existing.getDepreciationMethod());
        }
        normalizeBeforeSave(entity, false);
        validCodeUnique(entity);
        boolean ok = super.updateById(entity);
        if (ok) {
            FixedAsset after = fixedAssetMapper.selectById(entity.getId());
            fixedAssetChangeService.recordAutoChange(dto.getBookId(), existing, after, "卡片编辑");
        }
        return ok ? new Message<>(Message.SUCCESS, "修改成功", dto.getId()) : new Message<>(Message.FAIL, "修改失败");
    }

    @Transactional
    public Message<String> delete(ListIdsDto dto) {
        List<String> ids = dto.getListIds();
        if (CollUtil.isEmpty(ids)) {
            return new Message<>(Message.FAIL, "删除失败");
        }
        Long deprCount = fixedAssetDeprMapper.selectCount(Wrappers.<FixedAssetDepr>lambdaQuery()
                .in(FixedAssetDepr::getAssetId, ids));
        if (deprCount != null && deprCount > 0) {
            throw new ServiceException(FixedAssetErrorCode.ASSET_HAS_DEPR);
        }
        boolean ok = super.removeBatchByIds(ids);
        return ok ? new Message<>(Message.SUCCESS, "删除成功") : new Message<>(Message.FAIL, "删除失败");
    }

    @Transactional
    public Message<String> copy(String id, String bookId) {
        FixedAsset source = fixedAssetMapper.selectById(id);
        if (source == null || !Objects.equals(source.getBookId(), bookId)) {
            throw new ServiceException(FixedAssetErrorCode.ASSET_NOT_FOUND);
        }
        FixedAsset entity = BeanUtil.copyProperties(source, FixedAsset.class);
        entity.setId(null);
        entity.setCode(FixedAssetCopyRules.nextCopyCode(source.getCode(),
                code -> codeExists(bookId, code)));
        entity.setStatus(FixedAssetStatus.IN_USE.name());
        entity.setDisposedPeriod(null);
        entity.setDisposeVoucherId(null);
        entity.setPurchaseVoucherId(null);
        entity.setSuspendedPeriod(null);
        entity.setDepreciatedPeriods(0);
        entity.setOpeningAccumDepr(BigDecimal.ZERO);
        entity.setAccumDepr(BigDecimal.ZERO);
        entity.setYearDepr(BigDecimal.ZERO);
        String term = configSysService.getCurrentTerm(bookId);
        entity.setEntryPeriod(term);
        entity.setStartUseDate(firstDayOfPeriod(term));
        normalizeBeforeSave(entity, true);
        validCodeUnique(entity);
        String newId = identifierGenerator.nextId(entity).toString();
        entity.setId(newId);
        boolean ok = super.save(entity);
        return ok ? new Message<>(Message.SUCCESS, "复制成功", newId) : new Message<>(Message.FAIL, "复制失败");
    }

    @Transactional
    public Message<String> suspend(String id, String bookId) {
        FixedAsset entity = fixedAssetMapper.selectById(id);
        if (entity == null || !Objects.equals(entity.getBookId(), bookId)) {
            throw new ServiceException(FixedAssetErrorCode.ASSET_NOT_FOUND);
        }
        if (FixedAssetStatus.DISPOSED.name().equals(entity.getStatus())) {
            throw new ServiceException(FixedAssetErrorCode.CANNOT_SUSPEND_DISPOSED);
        }
        if (FixedAssetStatus.SUSPENDED.name().equals(entity.getStatus())) {
            throw new ServiceException(FixedAssetErrorCode.ALREADY_SUSPENDED);
        }
        FixedAsset before = BeanUtil.copyProperties(entity, FixedAsset.class);
        String term = configSysService.getCurrentTerm(bookId);
        entity.setStatus(FixedAssetStatus.SUSPENDED.name());
        entity.setSuspendedPeriod(term);
        boolean ok = super.updateById(entity);
        if (ok) {
            FixedAsset after = fixedAssetMapper.selectById(id);
            fixedAssetChangeService.recordAutoChange(bookId, before, after, "暂停计提");
        }
        return ok ? new Message<>(Message.SUCCESS, "已暂停计提", id) : new Message<>(Message.FAIL, "暂停失败");
    }

    @Transactional
    public Message<String> resume(String id, String bookId) {
        FixedAsset entity = fixedAssetMapper.selectById(id);
        if (entity == null || !Objects.equals(entity.getBookId(), bookId)) {
            throw new ServiceException(FixedAssetErrorCode.ASSET_NOT_FOUND);
        }
        if (!FixedAssetStatus.SUSPENDED.name().equals(entity.getStatus())) {
            throw new ServiceException(FixedAssetErrorCode.NOT_SUSPENDED);
        }
        FixedAsset before = BeanUtil.copyProperties(entity, FixedAsset.class);
        entity.setStatus(FixedAssetStatus.IN_USE.name());
        entity.setSuspendedPeriod(null);
        boolean ok = super.updateById(entity);
        if (ok) {
            FixedAsset after = fixedAssetMapper.selectById(id);
            fixedAssetChangeService.recordAutoChange(bookId, before, after, "恢复计提");
        }
        return ok ? new Message<>(Message.SUCCESS, "已恢复计提", id) : new Message<>(Message.FAIL, "恢复失败");
    }

    public void export(FixedAssetPageDto dto, HttpServletResponse response) throws IOException {
        List<FixedAsset> assets = fixedAssetMapper.selectList(buildQueryWrapper(dto));
        Map<String, String> categoryCodes = new HashMap<>();
        Map<String, String> categoryNames = loadCategoryNames(assets);
        if (CollUtil.isNotEmpty(assets)) {
            Set<String> catIds = assets.stream().map(FixedAsset::getCategoryId)
                    .filter(StringUtils::isNotBlank).collect(Collectors.toSet());
            if (!catIds.isEmpty()) {
                List<AssetCategory> cats = assetCategoryMapper.selectBatchIds(catIds);
                for (AssetCategory c : cats) {
                    categoryCodes.put(c.getId(), c.getCode());
                }
            }
        }
        Map<String, String> deptNames = loadDeptNames(assets);
        Map<String, String> subjectCodes = loadSubjectCodes(assets);

        try (Workbook workbook = new XSSFWorkbook()) {
            Sheet sheet = workbook.createSheet("固定资产卡片");
            String[] heads = CARD_EXPORT_HEADERS;
            Row header = sheet.createRow(0);
            for (int i = 0; i < heads.length; i++) {
                header.createCell(i).setCellValue(heads[i]);
            }
            DateTimeFormatter df = DateTimeFormatter.ofPattern("yyyy-MM-dd");
            int rowIndex = 1;
            for (FixedAsset a : assets) {
                Row row = sheet.createRow(rowIndex++);
                int col = 0;
                row.createCell(col++).setCellValue(StringUtils.defaultString(a.getCode()));
                row.createCell(col++).setCellValue(StringUtils.defaultString(a.getName()));
                row.createCell(col++).setCellValue(StringUtils.defaultString(categoryCodes.get(a.getCategoryId())));
                row.createCell(col++).setCellValue(StringUtils.defaultString(categoryNames.get(a.getCategoryId())));
                row.createCell(col++).setCellValue(StringUtils.defaultString(deptNames.get(a.getDeptId())));
                if (a.getStartUseDate() != null) {
                    LocalDate d = a.getStartUseDate().toInstant().atZone(ZoneId.systemDefault()).toLocalDate();
                    row.createCell(col++).setCellValue(d.format(df));
                } else {
                    row.createCell(col++).setCellValue("");
                }
                row.createCell(col++).setCellValue(a.getQuantity() == null ? 0 : a.getQuantity());
                row.createCell(col++).setCellValue(StringUtils.defaultString(a.getSpec()));
                row.createCell(col++).setCellValue(StringUtils.defaultString(a.getLocation()));
                String methodLabel;
                try {
                    methodLabel = DepreciationMethod.from(a.getDepreciationMethod()).getLabel();
                } catch (Exception e) {
                    methodLabel = StringUtils.defaultString(a.getDepreciationMethod());
                }
                row.createCell(col++).setCellValue(methodLabel);
                if (a.getUsefulLifeMonths() != null) {
                    row.createCell(col++).setCellValue(a.getUsefulLifeMonths());
                } else {
                    row.createCell(col++).setCellValue("");
                }
                setAmountCell(row, col++, a.getExpectedTotalWork());
                setAmountCell(row, col++, a.getResidualRate());
                setAmountCell(row, col++, a.getOriginalValue());
                setAmountCell(row, col++, a.getTaxAmount());
                setAmountCell(row, col++, a.getImpairment());
                setAmountCell(row, col++, a.getOpeningAccumDepr());
                row.createCell(col++).setCellValue(a.getDepreciatedPeriods() == null ? 0 : a.getDepreciatedPeriods());
                row.createCell(col++).setCellValue(StringUtils.defaultString(subjectCodes.get(a.getFixedAssetSubjectId())));
                row.createCell(col++).setCellValue(StringUtils.defaultString(subjectCodes.get(a.getAccumDeprSubjectId())));
                row.createCell(col++).setCellValue(StringUtils.defaultString(subjectCodes.get(a.getExpenseSubjectId())));
                row.createCell(col).setCellValue(StringUtils.defaultString(a.getRemark()));
            }
            for (int i = 0; i < heads.length; i++) {
                sheet.autoSizeColumn(i);
            }
            response.setContentType(ExcelExporter.APPLICATION_MS_EXCEL);
            String fileName = "固定资产卡片.xlsx";
            response.setHeader("Content-Disposition", "attachment; filename="
                    + URLEncoder.encode(fileName, StandardCharsets.UTF_8));
            workbook.write(response.getOutputStream());
            response.getOutputStream().flush();
        }
    }

    public void downloadImportTemplate(HttpServletResponse response) throws IOException {
        try (Workbook workbook = new XSSFWorkbook()) {
            Sheet sheet = workbook.createSheet("固定资产卡片导入");
            Row header = sheet.createRow(0);
            for (int i = 0; i < CARD_EXPORT_HEADERS.length; i++) {
                header.createCell(i).setCellValue(CARD_EXPORT_HEADERS[i]);
            }
            Row sample = sheet.createRow(1);
            sample.createCell(0).setCellValue("FA001");
            sample.createCell(1).setCellValue("示例电脑");
            sample.createCell(2).setCellValue("01");
            sample.createCell(3).setCellValue("电子设备");
            sample.createCell(4).setCellValue("");
            sample.createCell(5).setCellValue("2026-01-01");
            sample.createCell(6).setCellValue(1);
            sample.createCell(7).setCellValue("");
            sample.createCell(8).setCellValue("");
            sample.createCell(9).setCellValue("平均年限法");
            sample.createCell(10).setCellValue(60);
            sample.createCell(11).setCellValue("");
            sample.createCell(12).setCellValue(5);
            sample.createCell(13).setCellValue(10000);
            sample.createCell(14).setCellValue(0);
            sample.createCell(15).setCellValue(0);
            sample.createCell(16).setCellValue(0);
            sample.createCell(17).setCellValue(0);
            sample.createCell(18).setCellValue("1601");
            sample.createCell(19).setCellValue("1602");
            sample.createCell(20).setCellValue("5602");
            sample.createCell(21).setCellValue("模板示例行，导入前请删除或改成真实数据");
            for (int i = 0; i < CARD_EXPORT_HEADERS.length; i++) {
                sheet.autoSizeColumn(i);
            }
            response.setContentType(ExcelExporter.APPLICATION_MS_EXCEL);
            response.setHeader("Content-Disposition", "attachment; filename="
                    + URLEncoder.encode("固定资产卡片导入模板.xlsx", StandardCharsets.UTF_8));
            workbook.write(response.getOutputStream());
            response.getOutputStream().flush();
        }
    }

    public Message<FixedAssetImportResultVo> importFromExcel(String bookId, ExcelImport excelImportFile) {
        FixedAssetImportResultVo result = new FixedAssetImportResultVo();
        if (excelImportFile == null || !excelImportFile.isExcelNotEmpty()) {
            result.setFailed(1);
            FixedAssetImportResultVo.RowError err = new FixedAssetImportResultVo.RowError();
            err.setRow(0);
            err.setMessage("请上传 Excel 文件");
            result.getErrors().add(err);
            return new Message<>(Message.FAIL, "导入失败", result);
        }
        Map<String, String> categoryIdByCode = loadCategoryIdByCode(bookId);
        Map<String, String> deptIdByName = loadDeptIdByName();
        Map<String, String> subjectIdByCode = new HashMap<>();
        Set<String> existingCodes = fixedAssetMapper.selectList(Wrappers.<FixedAsset>lambdaQuery()
                        .eq(FixedAsset::getBookId, bookId)
                        .select(FixedAsset::getCode))
                .stream().map(FixedAsset::getCode).filter(StringUtils::isNotBlank).collect(Collectors.toSet());
        Set<String> batchCodes = new HashSet<>();
        String currentTerm = configSysService.getCurrentTerm(bookId);
        Date defaultStart = firstDayOfPeriod(currentTerm);

        try {
            Workbook workbook = excelImportFile.biuldWorkbook();
            Sheet sheet = workbook.getSheetAt(0);
            int last = sheet.getLastRowNum();
            for (int r = 1; r <= last; r++) {
                Row row = sheet.getRow(r);
                if (row == null || isBlankRow(row)) {
                    continue;
                }
                int excelRow = r + 1;
                try {
                    String code = StringUtils.trimToEmpty(ExcelUtils.getValue(row, 0));
                    String name = StringUtils.trimToEmpty(ExcelUtils.getValue(row, 1));
                    if (StringUtils.isBlank(code) || StringUtils.isBlank(name)) {
                        addImportError(result, excelRow, code, "编码和名称不能为空");
                        continue;
                    }
                    if (existingCodes.contains(code) || batchCodes.contains(code)) {
                        addImportError(result, excelRow, code, "编码已存在，已跳过");
                        continue;
                    }
                    String categoryCode = StringUtils.trimToEmpty(ExcelUtils.getValue(row, 2));
                    String categoryId = categoryIdByCode.get(categoryCode);
                    if (StringUtils.isBlank(categoryCode) || StringUtils.isBlank(categoryId)) {
                        addImportError(result, excelRow, code, "类别编码无效");
                        continue;
                    }
                    String deptName = StringUtils.trimToEmpty(ExcelUtils.getValue(row, 4));
                    String deptId = StringUtils.isBlank(deptName) ? null : deptIdByName.get(deptName);

                    DepreciationMethod method = parseMethod(ExcelUtils.getValue(row, 9));
                    if (method == null) {
                        addImportError(result, excelRow, code, "折旧方法无效");
                        continue;
                    }
                    Integer lifeMonths = parseInteger(ExcelUtils.getValue(row, 10));
                    if (method.isAccelerated() && !FixedAssetDepreciationRules.isValidAcceleratedLife(lifeMonths)) {
                        addImportError(result, excelRow, code, "加速折旧要求使用月数≥24且为12的整数倍");
                        continue;
                    }

                    String faSubjectId = resolveSubjectId(bookId, ExcelUtils.getValue(row, 18), subjectIdByCode, "1601");
                    String accumSubjectId = resolveSubjectId(bookId, ExcelUtils.getValue(row, 19), subjectIdByCode, "1602");
                    String expenseSubjectId = resolveSubjectId(bookId, ExcelUtils.getValue(row, 20), subjectIdByCode, null);
                    if (StringUtils.isBlank(faSubjectId) || StringUtils.isBlank(accumSubjectId)) {
                        addImportError(result, excelRow, code, "固定资产/累计折旧科目无效");
                        continue;
                    }
                    if (method.isDepreciable() && StringUtils.isBlank(expenseSubjectId)) {
                        addImportError(result, excelRow, code, "折旧费用科目不能为空");
                        continue;
                    }

                    Date startUse = parseDate(ExcelUtils.getValue(row, 5), defaultStart);
                    BigDecimal opening = parseDecimal(ExcelUtils.getValue(row, 16));
                    Integer depreciatedPeriods = parseInteger(ExcelUtils.getValue(row, 17));
                    if (depreciatedPeriods == null) {
                        depreciatedPeriods = 0;
                    }

                    FixedAsset entity = FixedAsset.builder()
                            .bookId(bookId)
                            .code(code)
                            .name(name)
                            .categoryId(categoryId)
                            .deptId(deptId)
                            .startUseDate(startUse)
                            .entryPeriod(FixedAssetDepreciationRules.periodOf(startUse))
                            .quantity(parseInteger(ExcelUtils.getValue(row, 6)) == null
                                    ? 1 : parseInteger(ExcelUtils.getValue(row, 6)))
                            .spec(StringUtils.trimToNull(ExcelUtils.getValue(row, 7)))
                            .location(StringUtils.trimToNull(ExcelUtils.getValue(row, 8)))
                            .status(FixedAssetStatus.IN_USE.name())
                            .depreciationMethod(method.name())
                            .usefulLifeMonths(lifeMonths)
                            .expectedTotalWork(parseDecimal(ExcelUtils.getValue(row, 11)))
                            .residualRate(parseDecimal(ExcelUtils.getValue(row, 12)))
                            .originalValue(parseDecimal(ExcelUtils.getValue(row, 13)))
                            .taxAmount(parseDecimal(ExcelUtils.getValue(row, 14)))
                            .impairment(parseDecimal(ExcelUtils.getValue(row, 15)))
                            .openingAccumDepr(opening == null ? BigDecimal.ZERO : opening)
                            .accumDepr(opening == null ? BigDecimal.ZERO : opening)
                            .yearDepr(BigDecimal.ZERO)
                            .depreciatedPeriods(depreciatedPeriods)
                            .fixedAssetSubjectId(faSubjectId)
                            .accumDeprSubjectId(accumSubjectId)
                            .expenseSubjectId(expenseSubjectId)
                            .remark(StringUtils.trimToNull(ExcelUtils.getValue(row, 21)))
                            .build();
                    normalizeBeforeSave(entity, true);
                    String id = identifierGenerator.nextId(entity).toString();
                    entity.setId(id);
                    super.save(entity);
                    batchCodes.add(code);
                    existingCodes.add(code);
                    result.setSuccess(result.getSuccess() + 1);
                } catch (Exception ex) {
                    addImportError(result, excelRow, ExcelUtils.getValue(row, 0),
                            StringUtils.defaultIfBlank(ex.getMessage(), "导入失败"));
                }
            }
            excelImportFile.closeWorkbook();
        } catch (IOException e) {
            throw new IllegalStateException("读取 Excel 失败", e);
        }

        String msg = "导入完成：成功 " + result.getSuccess() + " 条，失败 " + result.getFailed() + " 条";
        return new Message<>(Message.SUCCESS, msg, result);
    }

    private void addImportError(FixedAssetImportResultVo result, int row, String code, String message) {
        result.setFailed(result.getFailed() + 1);
        FixedAssetImportResultVo.RowError err = new FixedAssetImportResultVo.RowError();
        err.setRow(row);
        err.setCode(code);
        err.setMessage(message);
        result.getErrors().add(err);
    }

    private boolean isBlankRow(Row row) {
        for (int i = 0; i < CARD_EXPORT_HEADERS.length; i++) {
            if (StringUtils.isNotBlank(ExcelUtils.getValue(row, i))) {
                return false;
            }
        }
        return true;
    }

    private Map<String, String> loadCategoryIdByCode(String bookId) {
        Map<String, String> map = new HashMap<>();
        List<AssetCategory> list = assetCategoryMapper.selectList(Wrappers.<AssetCategory>lambdaQuery()
                .eq(AssetCategory::getBookId, bookId));
        for (AssetCategory c : list) {
            if (StringUtils.isNotBlank(c.getCode())) {
                map.put(c.getCode(), c.getId());
            }
        }
        return map;
    }

    private Map<String, String> loadDeptIdByName() {
        Map<String, String> map = new HashMap<>();
        List<Organizations> list = organizationsService.list();
        for (Organizations org : list) {
            if (org != null && StringUtils.isNotBlank(org.getOrgName())) {
                map.putIfAbsent(org.getOrgName(), org.getId());
            }
        }
        return map;
    }

    private String resolveSubjectId(String bookId, String codeRaw, Map<String, String> cache, String fallbackCode) {
        String code = StringUtils.trimToEmpty(codeRaw);
        if (StringUtils.isBlank(code)) {
            code = StringUtils.defaultString(fallbackCode);
        }
        if (StringUtils.isBlank(code)) {
            return null;
        }
        if (cache.containsKey(code)) {
            return cache.get(code);
        }
        BookSubject subject = null;
        for (String candidate : SubjectCodeCompat.lookupCandidates(code)) {
            subject = bookSubjectService.selectSubject(bookId, candidate);
            if (subject != null) {
                break;
            }
        }
        if (subject == null) {
            subject = bookSubjectService.selectSubject(bookId, code);
        }
        String id = subject == null ? null : subject.getId();
        cache.put(code, id);
        return id;
    }

    private DepreciationMethod parseMethod(String raw) {
        String text = StringUtils.trimToEmpty(raw);
        if (StringUtils.isBlank(text)) {
            return DepreciationMethod.STRAIGHT_LINE;
        }
        for (DepreciationMethod method : DepreciationMethod.values()) {
            if (method.name().equalsIgnoreCase(text) || method.getLabel().equals(text)) {
                return method;
            }
        }
        return null;
    }

    private BigDecimal parseDecimal(String raw) {
        String text = StringUtils.trimToEmpty(raw);
        if (StringUtils.isBlank(text)) {
            return null;
        }
        try {
            return new BigDecimal(text.replace(",", ""));
        } catch (Exception e) {
            return null;
        }
    }

    private Integer parseInteger(String raw) {
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

    private Date parseDate(String raw, Date fallback) {
        String text = StringUtils.trimToEmpty(raw);
        if (StringUtils.isBlank(text)) {
            return fallback;
        }
        try {
            LocalDate d = LocalDate.parse(text.substring(0, Math.min(10, text.length())));
            return Date.from(d.atStartOfDay(ZoneId.systemDefault()).toInstant());
        } catch (Exception e) {
            return fallback;
        }
    }

    private boolean codeExists(String bookId, String code) {
        Long count = fixedAssetMapper.selectCount(Wrappers.<FixedAsset>lambdaQuery()
                .eq(FixedAsset::getBookId, bookId)
                .eq(FixedAsset::getCode, code));
        return count != null && count > 0;
    }

    private Date firstDayOfPeriod(String period) {
        if (StringUtils.isBlank(period) || !period.contains("-")) {
            return new Date();
        }
        YearMonth ym = YearMonth.parse(period);
        return Date.from(ym.atDay(1).atStartOfDay(ZoneId.systemDefault()).toInstant());
    }

    private void setAmountCell(Row row, int col, BigDecimal value) {
        if (value == null) {
            row.createCell(col).setCellValue("");
        } else {
            row.createCell(col).setCellValue(value.doubleValue());
        }
    }

    private Map<String, String> loadDeptNames(List<FixedAsset> assets) {
        Map<String, String> map = new HashMap<>();
        if (CollUtil.isEmpty(assets)) {
            return map;
        }
        Set<String> ids = assets.stream().map(FixedAsset::getDeptId)
                .filter(StringUtils::isNotBlank).collect(Collectors.toSet());
        for (String id : ids) {
            Organizations org = organizationsService.getById(id);
            if (org != null) {
                map.put(id, org.getOrgName());
            }
        }
        return map;
    }

    private Map<String, String> loadSubjectCodes(List<FixedAsset> assets) {
        Map<String, String> map = new HashMap<>();
        if (CollUtil.isEmpty(assets)) {
            return map;
        }
        Set<String> ids = new HashSet<>();
        for (FixedAsset a : assets) {
            if (StringUtils.isNotBlank(a.getFixedAssetSubjectId())) {
                ids.add(a.getFixedAssetSubjectId());
            }
            if (StringUtils.isNotBlank(a.getAccumDeprSubjectId())) {
                ids.add(a.getAccumDeprSubjectId());
            }
            if (StringUtils.isNotBlank(a.getExpenseSubjectId())) {
                ids.add(a.getExpenseSubjectId());
            }
        }
        for (String id : ids) {
            BookSubject subject = bookSubjectService.getById(id);
            if (subject != null) {
                map.put(id, subject.getCode());
            }
        }
        return map;
    }

    @Transactional
    public Message<FixedAssetDisposeResultVo> dispose(String id, String bookId, FixedAssetDisposeDto dto) {
        if (dto == null) {
            dto = new FixedAssetDisposeDto();
        }
        FixedAsset entity = fixedAssetMapper.selectById(id);
        if (entity == null || !Objects.equals(entity.getBookId(), bookId)) {
            throw new ServiceException(FixedAssetErrorCode.ASSET_NOT_FOUND);
        }
        if (FixedAssetStatus.DISPOSED.name().equals(entity.getStatus())) {
            throw new ServiceException(FixedAssetErrorCode.ALREADY_DISPOSED);
        }

        BigDecimal original = FixedAssetDisposalRules.nz(entity.getOriginalValue());
        BigDecimal accum = FixedAssetDisposalRules.nz(entity.getAccumDepr());
        BigDecimal impairment = FixedAssetDisposalRules.nz(entity.getImpairment());
        BigDecimal bookValue = FixedAssetDisposalRules.bookValue(original, accum, impairment);
        BigDecimal income = FixedAssetDisposalRules.nz(dto.getDisposeIncome()).setScale(2, RoundingMode.HALF_UP);
        BigDecimal expense = FixedAssetDisposalRules.nz(dto.getDisposeExpense()).setScale(2, RoundingMode.HALF_UP);
        BigDecimal clearBal = FixedAssetDisposalRules.clearBalance(bookValue, income, expense);

        BookSubject faSubject = requireSubjectById(bookId, entity.getFixedAssetSubjectId());
        BookSubject accumSubject = requireSubjectById(bookId, entity.getAccumDeprSubjectId());
        BookSubject disposalSubject = resolveSubject(bookId, firstNonBlank(dto.getDisposalSubjectId(), entity.getDisposalSubjectId()),
                "1606", "1701");
        BookSubject impairmentSubject = null;
        if (impairment.compareTo(BigDecimal.ZERO) > 0) {
            impairmentSubject = resolveSubject(bookId, entity.getImpairmentSubjectId(), "1603");
        }
        BookSubject counterpart = null;
        if (income.compareTo(BigDecimal.ZERO) > 0 || expense.compareTo(BigDecimal.ZERO) > 0) {
            counterpart = resolveSubject(bookId, firstNonBlank(dto.getCounterpartSubjectId(), entity.getPurchaseCounterpartSubjectId()),
                    "1002", "1001");
        }
        BookSubject gainSubject = null;
        BookSubject lossSubject = null;
        if (clearBal.compareTo(BigDecimal.ZERO) < 0) {
            gainSubject = resolveSubject(bookId, dto.getGainSubjectId(), "5301.01", "5301", "6301");
        } else if (clearBal.compareTo(BigDecimal.ZERO) > 0) {
            lossSubject = resolveSubject(bookId, dto.getLossSubjectId(), "5711.02", "5711", "5601", "6711");
        }
        if (faSubject == null || accumSubject == null || disposalSubject == null) {
            throw new ServiceException(FixedAssetErrorCode.DISPOSE_SUBJECT_REQUIRED);
        }
        if ((income.compareTo(BigDecimal.ZERO) > 0 || expense.compareTo(BigDecimal.ZERO) > 0) && counterpart == null) {
            throw new ServiceException(FixedAssetErrorCode.DISPOSE_SUBJECT_REQUIRED);
        }
        if (clearBal.compareTo(BigDecimal.ZERO) < 0 && gainSubject == null) {
            throw new ServiceException(FixedAssetErrorCode.DISPOSE_SUBJECT_REQUIRED);
        }
        if (clearBal.compareTo(BigDecimal.ZERO) > 0 && lossSubject == null) {
            throw new ServiceException(FixedAssetErrorCode.DISPOSE_SUBJECT_REQUIRED);
        }

        String currentTerm = configSysService.getCurrentTerm(bookId);
        String word = StringUtils.defaultIfBlank(dto.getVoucherWord(), DEFAULT_WORD);
        String summary = StringUtils.defaultIfBlank(dto.getSummary(),
                DEFAULT_DISPOSE_SUMMARY + "：" + entity.getCode() + " " + entity.getName());
        Date voucherDate = dto.getVoucherDate() != null ? dto.getVoucherDate() : new Date();

        Book book = bookMapper.selectById(bookId);
        Message<String> voucherMsg = createDisposeVoucher(book, bookId, currentTerm, voucherDate, word, summary,
                original, accum, impairment, bookValue, income, expense, clearBal,
                faSubject, accumSubject, disposalSubject, impairmentSubject, counterpart, gainSubject, lossSubject);
        if (voucherMsg.getCode() != Message.SUCCESS) {
            throw new IllegalStateException(StringUtils.defaultIfBlank(voucherMsg.getMessage(), "清理凭证生成失败"));
        }

        FixedAsset before = BeanUtil.copyProperties(entity, FixedAsset.class);
        entity.setStatus(FixedAssetStatus.DISPOSED.name());
        entity.setDisposedPeriod(currentTerm);
        entity.setDisposeVoucherId(voucherMsg.getData());
        boolean ok = super.updateById(entity);
        if (!ok) {
            return new Message<>(Message.FAIL, "清理失败");
        }
        FixedAsset after = fixedAssetMapper.selectById(id);
        fixedAssetChangeService.recordAutoChange(bookId, before, after, "资产清理");

        Voucher voucher = voucherService.getById(voucherMsg.getData());
        FixedAssetDisposeResultVo result = new FixedAssetDisposeResultVo();
        result.setAssetId(id);
        result.setVoucherId(voucherMsg.getData());
        result.setVoucherWord(VoucherUtils.displayWord(voucher));
        result.setBookValue(bookValue);
        result.setDisposeIncome(income);
        result.setDisposeExpense(expense);
        result.setGainOrLoss(clearBal);
        return new Message<>(Message.SUCCESS, "清理成功", result);
    }

    private Message<String> createDisposeVoucher(Book book, String bookId, String period, Date voucherDate,
                                                 String wordHead, String summary,
                                                 BigDecimal original, BigDecimal accum, BigDecimal impairment,
                                                 BigDecimal bookValue, BigDecimal income, BigDecimal expense,
                                                 BigDecimal clearBal,
                                                 BookSubject faSubject, BookSubject accumSubject,
                                                 BookSubject disposalSubject, BookSubject impairmentSubject,
                                                 BookSubject counterpart, BookSubject gainSubject, BookSubject lossSubject) {
        int year;
        int month;
        if (StringUtils.isNotBlank(period) && period.contains("-")) {
            year = Integer.parseInt(period.split("-")[0]);
            month = Integer.parseInt(period.split("-")[1]);
        } else {
            java.util.Calendar cal = java.util.Calendar.getInstance();
            cal.setTime(voucherDate);
            year = cal.get(java.util.Calendar.YEAR);
            month = cal.get(java.util.Calendar.MONTH) + 1;
        }
        Integer wordNum = voucherService.getAbleWordNum(bookId, wordHead, year, month).getData();

        List<VoucherItemChangeDto> items = new ArrayList<>();
        // 转出累计折旧 / 减值 / 账面价值 → 固定资产清理；贷：固定资产
        if (accum.compareTo(BigDecimal.ZERO) > 0) {
            items.add(createItem(accumSubject, summary, accum, true));
        }
        if (impairment.compareTo(BigDecimal.ZERO) > 0 && impairmentSubject != null) {
            items.add(createItem(impairmentSubject, summary, impairment, true));
        }
        if (bookValue.compareTo(BigDecimal.ZERO) > 0) {
            items.add(createItem(disposalSubject, summary, bookValue, true));
        }
        if (original.compareTo(BigDecimal.ZERO) > 0) {
            items.add(createItem(faSubject, summary, original, false));
        }
        // 处置收入：借对方，贷清理
        if (income.compareTo(BigDecimal.ZERO) > 0 && counterpart != null) {
            items.add(createItem(counterpart, summary + "（处置收入）", income, true));
            items.add(createItem(disposalSubject, summary + "（处置收入）", income, false));
        }
        // 清理费用：借清理，贷对方
        if (expense.compareTo(BigDecimal.ZERO) > 0 && counterpart != null) {
            items.add(createItem(disposalSubject, summary + "（清理费用）", expense, true));
            items.add(createItem(counterpart, summary + "（清理费用）", expense, false));
        }
        // 结转损益
        if (clearBal.compareTo(BigDecimal.ZERO) > 0 && lossSubject != null) {
            items.add(createItem(lossSubject, summary + "（处置净损失）", clearBal, true));
            items.add(createItem(disposalSubject, summary + "（处置净损失）", clearBal, false));
        } else if (clearBal.compareTo(BigDecimal.ZERO) < 0 && gainSubject != null) {
            BigDecimal gain = clearBal.abs();
            items.add(createItem(disposalSubject, summary + "（处置净收益）", gain, true));
            items.add(createItem(gainSubject, summary + "（处置净收益）", gain, false));
        }

        BigDecimal debit = items.stream()
                .map(i -> FixedAssetDisposalRules.nz(i.getDebitAmount()))
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        BigDecimal credit = items.stream()
                .map(i -> FixedAssetDisposalRules.nz(i.getCreditAmount()))
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        VoucherChangeDto voucherDto = new VoucherChangeDto();
        voucherDto.setWordHead(wordHead);
        voucherDto.setWordNum(wordNum);
        voucherDto.setBookId(bookId);
        voucherDto.setCompanyName(book != null ? book.getCompanyName() : "");
        voucherDto.setVoucherDate(voucherDate);
        voucherDto.setVoucherYear(year);
        voucherDto.setVoucherMonth(month);
        voucherDto.setDebitAmount(debit);
        voucherDto.setCreditAmount(credit);
        voucherDto.setReceiptNum(0);
        voucherDto.setRemark(summary);
        voucherDto.setStatus(VoucherStatusEnum.DRAFT.getValue());
        voucherDto.setItems(items);
        return voucherService.save(voucherDto);
    }

    private VoucherItemChangeDto createItem(BookSubject subject, String summary, BigDecimal amount, boolean debit) {
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

    private BookSubject requireSubjectById(String bookId, String subjectId) {
        if (StringUtils.isBlank(subjectId)) {
            return null;
        }
        BookSubject subject = bookSubjectService.getById(subjectId);
        if (subject == null || (StringUtils.isNotBlank(subject.getBookId()) && !Objects.equals(subject.getBookId(), bookId))) {
            return null;
        }
        return subject;
    }

    private BookSubject resolveSubject(String bookId, String preferredId, String... fallbackCodes) {
        BookSubject byId = requireSubjectById(bookId, preferredId);
        if (byId != null) {
            return byId;
        }
        if (fallbackCodes == null) {
            return null;
        }
        for (String code : fallbackCodes) {
            for (String candidate : SubjectCodeCompat.lookupCandidates(code)) {
                BookSubject found = bookSubjectService.selectSubject(bookId, candidate);
                if (found != null) {
                    return found;
                }
            }
            BookSubject found = bookSubjectService.selectSubject(bookId, code);
            if (found != null) {
                return found;
            }
        }
        return null;
    }

    private static String firstNonBlank(String a, String b) {
        if (StringUtils.isNotBlank(a)) {
            return a;
        }
        return b;
    }

    private void applyCategoryDefaults(FixedAssetChangeDto dto) {
        if (StringUtils.isBlank(dto.getCategoryId())) {
            return;
        }
        AssetCategory category = assetCategoryMapper.selectById(dto.getCategoryId());
        if (category == null) {
            throw new ServiceException(FixedAssetErrorCode.CATEGORY_NOT_FOUND);
        }
        if (StringUtils.isBlank(dto.getDepreciationMethod())) {
            dto.setDepreciationMethod(category.getDepreciationMethod());
        }
        if (dto.getUsefulLifeMonths() == null) {
            dto.setUsefulLifeMonths(category.getUsefulLifeMonths());
        }
        if (dto.getResidualRate() == null) {
            dto.setResidualRate(category.getResidualRate());
        }
        if (StringUtils.isBlank(dto.getFixedAssetSubjectId())) {
            dto.setFixedAssetSubjectId(category.getFixedAssetSubjectId());
        }
        if (StringUtils.isBlank(dto.getAccumDeprSubjectId())) {
            dto.setAccumDeprSubjectId(category.getAccumDeprSubjectId());
        }
    }

    private void applyDefaultSubjects(FixedAssetChangeDto dto) {
        if (StringUtils.isBlank(dto.getFixedAssetSubjectId())) {
            var fa = bookSubjectService.selectSubject(dto.getBookId(), "1601");
            if (fa != null) {
                dto.setFixedAssetSubjectId(fa.getId());
            }
        }
        if (StringUtils.isBlank(dto.getAccumDeprSubjectId())) {
            var accum = bookSubjectService.selectSubject(dto.getBookId(), "1602");
            if (accum != null) {
                dto.setAccumDeprSubjectId(accum.getId());
            }
        }
        if (StringUtils.isBlank(dto.getPurchaseCounterpartSubjectId())) {
            BookSubject cp = resolveSubject(dto.getBookId(), null, "1002", "1001");
            if (cp != null) {
                dto.setPurchaseCounterpartSubjectId(cp.getId());
            }
        }
        if (StringUtils.isBlank(dto.getTaxSubjectId())) {
            BookSubject tax = resolveSubject(dto.getBookId(), null, "2221.01.01", "2221.01", "2171.01.01");
            if (tax != null) {
                dto.setTaxSubjectId(tax.getId());
            }
        }
    }

    private void normalizeBeforeSave(FixedAsset entity, boolean isNew) {
        DepreciationMethod method = DepreciationMethod.from(entity.getDepreciationMethod());
        if (method == DepreciationMethod.NONE && entity.getUsefulLifeMonths() == null) {
            entity.setUsefulLifeMonths(0);
        }
        if (StringUtils.isBlank(entity.getStatus())) {
            entity.setStatus(FixedAssetStatus.IN_USE.name());
        }
        if (entity.getQuantity() == null) {
            entity.setQuantity(1);
        }
        if (entity.getTaxAmount() == null) {
            entity.setTaxAmount(BigDecimal.ZERO);
        }
        if (entity.getImpairment() == null) {
            entity.setImpairment(BigDecimal.ZERO);
        }
        if (entity.getDepreciatedPeriods() == null) {
            entity.setDepreciatedPeriods(0);
        }
        if (entity.getOpeningAccumDepr() == null) {
            entity.setOpeningAccumDepr(BigDecimal.ZERO);
        }
        if (entity.getAccumDepr() == null) {
            entity.setAccumDepr(entity.getOpeningAccumDepr());
        }
        if (entity.getYearDepr() == null) {
            entity.setYearDepr(BigDecimal.ZERO);
        }
        if (StringUtils.isBlank(entity.getEntryPeriod())) {
            if (entity.getStartUseDate() != null) {
                entity.setEntryPeriod(FixedAssetDepreciationRules.periodOf(entity.getStartUseDate()));
            } else {
                entity.setEntryPeriod(configSysService.getCurrentTerm(entity.getBookId()));
            }
        }
        if (StringUtils.isBlank(entity.getAccumDeprSubjectId())) {
            throw new ServiceException(FixedAssetErrorCode.SUBJECT_REQUIRED);
        }
        if (method.isDepreciable() && StringUtils.isBlank(entity.getExpenseSubjectId())) {
            throw new ServiceException(FixedAssetErrorCode.SUBJECT_REQUIRED);
        }
        if (method.isAccelerated()
                && !FixedAssetDepreciationRules.isValidAcceleratedLife(entity.getUsefulLifeMonths())) {
            throw new ServiceException(FixedAssetErrorCode.ACCELERATED_LIFE_INVALID);
        }
        if (isNew && StringUtils.isBlank(entity.getDepreciationMethod())) {
            entity.setDepreciationMethod(DepreciationMethod.STRAIGHT_LINE.name());
        }
    }

    private void assertCalcFieldsUnchanged(FixedAsset existing, FixedAssetChangeDto dto) {
        boolean methodChanged = StringUtils.isNotBlank(dto.getDepreciationMethod())
                && !Objects.equals(existing.getDepreciationMethod(), dto.getDepreciationMethod());
        boolean lifeChanged = dto.getUsefulLifeMonths() != null
                && !Objects.equals(existing.getUsefulLifeMonths(), dto.getUsefulLifeMonths());
        if (changedIfPresent(existing.getOriginalValue(), dto.getOriginalValue())
                || lifeChanged
                || changedIfPresent(existing.getExpectedTotalWork(), dto.getExpectedTotalWork())
                || changedIfPresent(existing.getResidualRate(), dto.getResidualRate())
                || methodChanged) {
            throw new ServiceException(FixedAssetErrorCode.CALC_FIELDS_LOCKED);
        }
    }

    private boolean changedIfPresent(BigDecimal existing, BigDecimal incoming) {
        return incoming != null && changed(existing, incoming);
    }

    private boolean changed(BigDecimal a, BigDecimal b) {
        if (a == null && b == null) {
            return false;
        }
        if (a == null || b == null) {
            return true;
        }
        return a.compareTo(b) != 0;
    }

    private void validCodeUnique(FixedAsset entity) {
        FixedAsset exists = fixedAssetMapper.selectOne(Wrappers.<FixedAsset>lambdaQuery()
                .eq(FixedAsset::getBookId, entity.getBookId())
                .eq(FixedAsset::getCode, entity.getCode())
                .ne(StringUtils.isNotBlank(entity.getId()), FixedAsset::getId, entity.getId()));
        if (exists != null) {
            throw new ServiceException(FixedAssetErrorCode.ASSET_CODE_DUPLICATE);
        }
    }

    private boolean hasDepr(String assetId) {
        Long count = fixedAssetDeprMapper.selectCount(Wrappers.<FixedAssetDepr>lambdaQuery()
                .eq(FixedAssetDepr::getAssetId, assetId));
        return count != null && count > 0;
    }

    private LambdaQueryWrapper<FixedAsset> buildQueryWrapper(FixedAssetPageDto dto) {
        boolean includeDisposed = Boolean.TRUE.equals(dto.getIncludeDisposed());
        return Wrappers.<FixedAsset>lambdaQuery()
                .eq(StringUtils.isNotBlank(dto.getBookId()), FixedAsset::getBookId, dto.getBookId())
                .eq(StringUtils.isNotBlank(dto.getCode()), FixedAsset::getCode, dto.getCode())
                .like(StringUtils.isNotBlank(dto.getName()), FixedAsset::getName, dto.getName())
                .eq(StringUtils.isNotBlank(dto.getCategoryId()), FixedAsset::getCategoryId, dto.getCategoryId())
                .eq(StringUtils.isNotBlank(dto.getDeptId()), FixedAsset::getDeptId, dto.getDeptId())
                .eq(StringUtils.isNotBlank(dto.getStatus()), FixedAsset::getStatus, dto.getStatus())
                .ne(!includeDisposed && StringUtils.isBlank(dto.getStatus()), FixedAsset::getStatus, FixedAssetStatus.DISPOSED.name())
                .ne(StringUtils.isNotBlank(dto.getNoId()), FixedAsset::getId, dto.getNoId())
                .orderByAsc(FixedAsset::getCode);
    }

    private FixedAssetVo toVo(FixedAsset entity, Map<String, String> categoryNames,
                              Map<String, String> deptNames, Map<String, String> voucherWords,
                              Set<String> lockedIds) {
        FixedAssetVo vo = BeanUtil.copyProperties(entity, FixedAssetVo.class);
        BigDecimal residual = FixedAssetDepreciationRules.residualValue(entity.getOriginalValue(), entity.getResidualRate());
        BigDecimal openingAccum = entity.getOpeningAccumDepr() == null ? BigDecimal.ZERO : entity.getOpeningAccumDepr();
        BigDecimal accum = entity.getAccumDepr() == null ? openingAccum : entity.getAccumDepr();
        BigDecimal original = entity.getOriginalValue() == null ? BigDecimal.ZERO : entity.getOriginalValue();
        BigDecimal impairment = entity.getImpairment() == null ? BigDecimal.ZERO : entity.getImpairment();

        vo.setResidualValue(residual);
        vo.setOpeningNetValue(original.subtract(impairment).subtract(openingAccum).setScale(2, RoundingMode.HALF_UP));
        vo.setEndingAccumDepr(accum.setScale(2, RoundingMode.HALF_UP));
        vo.setEndingNetValue(original.subtract(impairment).subtract(accum).setScale(2, RoundingMode.HALF_UP));

        DepreciationMethod method = null;
        try {
            method = DepreciationMethod.from(entity.getDepreciationMethod());
            vo.setMethodLabel(method.getLabel());
        } catch (Exception e) {
            vo.setMethodLabel(entity.getDepreciationMethod());
        }
        BigDecimal base = FixedAssetDepreciationRules.depreciableBase(original, impairment, residual);
        BigDecimal remaining = FixedAssetDepreciationRules.remainingDepreciable(base, accum);
        if (method == DepreciationMethod.STRAIGHT_LINE
                && entity.getUsefulLifeMonths() != null && entity.getUsefulLifeMonths() > 0) {
            vo.setMonthlyDepr(FixedAssetDepreciationRules.straightLineAmount(
                    original, residual, entity.getUsefulLifeMonths(), entity.getDepreciatedPeriods(), remaining));
        } else if (method == DepreciationMethod.DOUBLE_DECLINING
                && FixedAssetDepreciationRules.isValidAcceleratedLife(entity.getUsefulLifeMonths())) {
            vo.setMonthlyDepr(FixedAssetDepreciationRules.doubleDecliningAmount(
                    original, impairment, residual, entity.getUsefulLifeMonths(),
                    entity.getDepreciatedPeriods(), accum, remaining));
        } else if (method == DepreciationMethod.SUM_OF_YEARS
                && FixedAssetDepreciationRules.isValidAcceleratedLife(entity.getUsefulLifeMonths())) {
            vo.setMonthlyDepr(FixedAssetDepreciationRules.sumOfYearsAmount(
                    original, impairment, residual, entity.getUsefulLifeMonths(),
                    entity.getDepreciatedPeriods(), remaining));
        }

        vo.setCategoryName(categoryNames.get(entity.getCategoryId()));
        if (deptNames != null && StringUtils.isNotBlank(entity.getDeptId())) {
            vo.setDeptName(deptNames.get(entity.getDeptId()));
        }
        if (voucherWords != null) {
            if (StringUtils.isNotBlank(entity.getPurchaseVoucherId())) {
                vo.setPurchaseVoucherWord(voucherWords.get(entity.getPurchaseVoucherId()));
            }
            if (StringUtils.isNotBlank(entity.getDisposeVoucherId())) {
                vo.setDisposeVoucherWord(voucherWords.get(entity.getDisposeVoucherId()));
            }
        }
        vo.setCalcFieldsLocked(lockedIds.contains(entity.getId()));
        return vo;
    }

    private Map<String, String> loadVoucherWords(List<FixedAsset> assets) {
        Map<String, String> map = new HashMap<>();
        if (CollUtil.isEmpty(assets)) {
            return map;
        }
        Set<String> ids = new HashSet<>();
        for (FixedAsset a : assets) {
            if (StringUtils.isNotBlank(a.getPurchaseVoucherId())) {
                ids.add(a.getPurchaseVoucherId());
            }
            if (StringUtils.isNotBlank(a.getDisposeVoucherId())) {
                ids.add(a.getDisposeVoucherId());
            }
        }
        if (ids.isEmpty()) {
            return map;
        }
        for (Voucher v : voucherService.listByIds(ids)) {
            if (v != null) {
                map.put(v.getId(), StringUtils.defaultIfBlank(VoucherUtils.displayWord(v), v.getId()));
            }
        }
        return map;
    }

    private Map<String, String> loadCategoryNames(List<FixedAsset> records) {
        Map<String, String> map = new HashMap<>();
        if (CollUtil.isEmpty(records)) {
            return map;
        }
        List<String> ids = records.stream().map(FixedAsset::getCategoryId).filter(Objects::nonNull).distinct().toList();
        if (ids.isEmpty()) {
            return map;
        }
        List<AssetCategory> categories = assetCategoryMapper.selectBatchIds(ids);
        for (AssetCategory c : categories) {
            map.put(c.getId(), c.getName());
        }
        return map;
    }

    private Set<String> loadLockedAssetIds(String bookId, List<String> assetIds) {
        Set<String> locked = new HashSet<>();
        if (CollUtil.isEmpty(assetIds)) {
            return locked;
        }
        List<FixedAssetDepr> deprs = fixedAssetDeprMapper.selectList(Wrappers.<FixedAssetDepr>lambdaQuery()
                .eq(StringUtils.isNotBlank(bookId), FixedAssetDepr::getBookId, bookId)
                .in(FixedAssetDepr::getAssetId, assetIds)
                .select(FixedAssetDepr::getAssetId));
        for (FixedAssetDepr d : deprs) {
            locked.add(d.getAssetId());
        }
        return locked;
    }
}
