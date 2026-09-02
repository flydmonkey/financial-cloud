package com.financial.cloud.service.fixedasset;

import cn.hutool.core.collection.CollUtil;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.financial.cloud.common.Message;
import com.financial.cloud.domain.fixedasset.AssetCategory;
import com.financial.cloud.domain.fixedasset.FixedAsset;
import com.financial.cloud.domain.fixedasset.FixedAssetChange;
import com.financial.cloud.domain.fixedasset.FixedAssetChangeItem;
import com.financial.cloud.domain.fixedasset.FixedAssetDepr;
import com.financial.cloud.domain.idm.Organizations;
import com.financial.cloud.dto.fixedasset.FixedAssetDepreciationReportQuery;
import com.financial.cloud.dto.fixedasset.FixedAssetDepreciationReportRow;
import com.financial.cloud.dto.fixedasset.FixedAssetDepreciationReportVo;
import com.financial.cloud.enums.fixedasset.FixedAssetStatus;
import com.financial.cloud.repository.fixedasset.AssetCategoryMapper;
import com.financial.cloud.repository.fixedasset.FixedAssetChangeItemMapper;
import com.financial.cloud.repository.fixedasset.FixedAssetChangeMapper;
import com.financial.cloud.repository.fixedasset.FixedAssetDeprMapper;
import com.financial.cloud.repository.fixedasset.FixedAssetMapper;
import com.financial.cloud.service.config.ConfigSysService;
import com.financial.cloud.service.idm.OrganizationsService;
import com.financial.cloud.util.FixedAssetChangeInfoRules;
import com.financial.cloud.util.FixedAssetDepreciationReportRules;
import com.financial.cloud.util.FixedAssetDepreciationRules;
import com.financial.cloud.util.excel.ExcelExporter;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.apache.commons.lang3.StringUtils;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.math.BigDecimal;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

@RequiredArgsConstructor
@Service
public class FixedAssetDepreciationReportService {

    private static final String ROW_ASSET = "ASSET";
    private static final String ROW_SUBTOTAL = "SUBTOTAL";
    private static final String ROW_TOTAL = "TOTAL";

    private final FixedAssetMapper fixedAssetMapper;
    private final FixedAssetDeprMapper fixedAssetDeprMapper;
    private final AssetCategoryMapper assetCategoryMapper;
    private final FixedAssetChangeMapper fixedAssetChangeMapper;
    private final FixedAssetChangeItemMapper fixedAssetChangeItemMapper;
    private final ConfigSysService configSysService;
    private final OrganizationsService organizationsService;

    public Message<FixedAssetDepreciationReportVo> detail(FixedAssetDepreciationReportQuery query) {
        return Message.ok(buildReport(query, true));
    }

    public Message<FixedAssetDepreciationReportVo> summary(FixedAssetDepreciationReportQuery query) {
        FixedAssetDepreciationReportVo detail = buildReport(query, true);
        FixedAssetDepreciationReportVo summary = new FixedAssetDepreciationReportVo();
        summary.setStartPeriod(detail.getStartPeriod());
        summary.setEndPeriod(detail.getEndPeriod());
        summary.setPeriodDeprColumnLabel(detail.getPeriodDeprColumnLabel());

        Map<String, FixedAssetDepreciationReportRow> byCategory = new LinkedHashMap<>();
        FixedAssetDepreciationReportRow total = null;
        for (FixedAssetDepreciationReportRow row : detail.getRows()) {
            if (ROW_TOTAL.equals(row.getRowType())) {
                total = copyAs(row, ROW_TOTAL);
                total.setAssetCode(null);
                total.setAssetName("合计");
                total.setCategoryName("合计");
                continue;
            }
            if (!ROW_SUBTOTAL.equals(row.getRowType())) {
                continue;
            }
            String key = StringUtils.defaultString(row.getCategoryId(), "_");
            FixedAssetDepreciationReportRow agg = byCategory.computeIfAbsent(key, k -> {
                FixedAssetDepreciationReportRow r = new FixedAssetDepreciationReportRow();
                r.setRowType(ROW_ASSET);
                r.setCategoryId(row.getCategoryId());
                r.setCategoryName(row.getCategoryName());
                zeroAmounts(r);
                return r;
            });
            addAmounts(agg, row);
        }
        List<FixedAssetDepreciationReportRow> rows = new ArrayList<>(byCategory.values());
        if (total == null) {
            total = newTotalRow();
            for (FixedAssetDepreciationReportRow r : rows) {
                addAmounts(total, r);
            }
        }
        rows.add(total);
        summary.setRows(rows);
        return Message.ok(summary);
    }

    public void exportDetail(FixedAssetDepreciationReportQuery query, HttpServletResponse response) throws IOException {
        export(buildReport(query, true), "折旧明细表", response, Boolean.TRUE.equals(query.getIncludeChangeInfo()));
    }

    public void exportSummary(FixedAssetDepreciationReportQuery query, HttpServletResponse response) throws IOException {
        export(summary(query).getData(), "折旧汇总表", response, false);
    }

    private void export(FixedAssetDepreciationReportVo vo, String title, HttpServletResponse response,
                        boolean withChangeInfo) throws IOException {
        try (Workbook workbook = new XSSFWorkbook()) {
            Sheet sheet = workbook.createSheet(title);
            int rowIndex = 0;
            Row header = sheet.createRow(rowIndex++);
            List<String> headList = new ArrayList<>(List.of(
                    "类别", "编码", "名称", "部门", "原值", "期初累计折旧",
                    vo.getPeriodDeprColumnLabel(), "本年折旧额", "期末累计折旧", "期末减值准备", "期末净值"
            ));
            if (withChangeInfo) {
                headList.add("期间变动");
            }
            String[] heads = headList.toArray(new String[0]);
            for (int i = 0; i < heads.length; i++) {
                header.createCell(i).setCellValue(heads[i]);
            }
            for (FixedAssetDepreciationReportRow r : vo.getRows()) {
                Row excelRow = sheet.createRow(rowIndex++);
                String name = ROW_SUBTOTAL.equals(r.getRowType()) ? "小计"
                        : ROW_TOTAL.equals(r.getRowType()) ? "合计" : StringUtils.defaultString(r.getAssetName());
                excelRow.createCell(0).setCellValue(StringUtils.defaultString(r.getCategoryName()));
                excelRow.createCell(1).setCellValue(StringUtils.defaultString(r.getAssetCode()));
                excelRow.createCell(2).setCellValue(name);
                excelRow.createCell(3).setCellValue(StringUtils.defaultString(r.getDeptName()));
                setAmount(excelRow, 4, r.getOriginalValue());
                setAmount(excelRow, 5, r.getOpeningAccumDepr());
                setAmount(excelRow, 6, r.getPeriodDepr());
                setAmount(excelRow, 7, r.getYearDepr());
                setAmount(excelRow, 8, r.getEndingAccumDepr());
                setAmount(excelRow, 9, r.getEndingImpairment());
                setAmount(excelRow, 10, r.getEndingNetValue());
                if (withChangeInfo) {
                    excelRow.createCell(11).setCellValue(StringUtils.defaultString(r.getChangeInfo()));
                }
            }
            for (int i = 0; i < heads.length; i++) {
                sheet.autoSizeColumn(i);
            }
            response.setContentType(ExcelExporter.APPLICATION_MS_EXCEL);
            String fileName = title + "_" + vo.getStartPeriod() + "-" + vo.getEndPeriod() + ".xlsx";
            response.setHeader("Content-Disposition", "attachment; filename="
                    + URLEncoder.encode(fileName, StandardCharsets.UTF_8));
            workbook.write(response.getOutputStream());
            response.getOutputStream().flush();
        }
    }

    private void setAmount(Row row, int col, BigDecimal amount) {
        row.createCell(col).setCellValue(amount == null ? 0D : amount.doubleValue());
    }

    private FixedAssetDepreciationReportVo buildReport(FixedAssetDepreciationReportQuery query, boolean withSubtotals) {
        String bookId = query.getBookId();
        String current = configSysService.getCurrentTerm(bookId);
        String start = StringUtils.defaultIfBlank(query.getStartPeriod(), current);
        String end = StringUtils.defaultIfBlank(query.getEndPeriod(), current);
        if (start.compareTo(end) > 0) {
            String tmp = start;
            start = end;
            end = tmp;
        }
        boolean includeDisposed = Boolean.TRUE.equals(query.getIncludeDisposed());
        boolean groupByDept = Boolean.TRUE.equals(query.getGroupByDept());
        boolean includeChangeInfo = Boolean.TRUE.equals(query.getIncludeChangeInfo());

        List<FixedAsset> assets = fixedAssetMapper.selectList(Wrappers.<FixedAsset>lambdaQuery()
                .eq(FixedAsset::getBookId, bookId)
                .orderByAsc(FixedAsset::getCode));
        if (!includeDisposed) {
            assets = assets.stream()
                    .filter(a -> !FixedAssetStatus.DISPOSED.name().equalsIgnoreCase(a.getStatus()))
                    .collect(Collectors.toList());
        }

        Map<String, String> categoryNames = loadCategoryNames(assets);
        Map<String, String> deptNames = loadDeptNames(assets);
        Map<String, Map<String, BigDecimal>> deprMap = loadDeprByAsset(bookId);
        Map<String, String> changeInfoMap = includeChangeInfo
                ? loadChangeInfoByAsset(bookId, start, end, assets) : Map.of();

        List<AssetAmount> amounts = new ArrayList<>();
        for (FixedAsset asset : assets) {
            Map<String, BigDecimal> byPeriod = deprMap.getOrDefault(asset.getId(), Map.of());
            BigDecimal opening = FixedAssetDepreciationReportRules.openingAccum(
                    asset.getOpeningAccumDepr(), byPeriod, start);
            BigDecimal period = FixedAssetDepreciationReportRules.periodDepr(byPeriod, start, end);
            BigDecimal year = FixedAssetDepreciationReportRules.yearDepr(byPeriod, end);
            BigDecimal ending = FixedAssetDepreciationReportRules.endingAccum(opening, period);
            BigDecimal impairment = asset.getImpairment() == null ? BigDecimal.ZERO : asset.getImpairment();
            BigDecimal original = asset.getOriginalValue() == null ? BigDecimal.ZERO : asset.getOriginalValue();
            BigDecimal net = FixedAssetDepreciationReportRules.endingNetValue(original, ending, impairment);

            FixedAssetDepreciationReportRow row = new FixedAssetDepreciationReportRow();
            row.setRowType(ROW_ASSET);
            row.setAssetId(asset.getId());
            row.setAssetCode(asset.getCode());
            row.setAssetName(asset.getName());
            row.setCategoryId(asset.getCategoryId());
            row.setCategoryName(categoryNames.getOrDefault(asset.getCategoryId(), ""));
            row.setDeptId(asset.getDeptId());
            row.setDeptName(StringUtils.defaultString(deptNames.get(asset.getDeptId())));
            row.setOriginalValue(original);
            row.setOpeningAccumDepr(opening);
            row.setPeriodDepr(period);
            row.setYearDepr(year);
            row.setEndingAccumDepr(ending);
            row.setEndingImpairment(impairment);
            row.setEndingNetValue(net);
            if (includeChangeInfo) {
                row.setChangeInfo(changeInfoMap.getOrDefault(asset.getId(), ""));
            }
            amounts.add(new AssetAmount(row));
        }

        amounts.sort(Comparator
                .comparing((AssetAmount a) -> StringUtils.defaultString(a.row.getCategoryName()))
                .thenComparing(a -> StringUtils.defaultString(a.row.getDeptName()))
                .thenComparing(a -> StringUtils.defaultString(a.row.getAssetCode())));

        List<FixedAssetDepreciationReportRow> rows = new ArrayList<>();
        FixedAssetDepreciationReportRow grand = newTotalRow();

        if (!withSubtotals) {
            for (AssetAmount a : amounts) {
                rows.add(a.row);
                addAmounts(grand, a.row);
            }
            rows.add(grand);
        } else if (groupByDept) {
            String prevKey = null;
            FixedAssetDepreciationReportRow sub = null;
            for (AssetAmount a : amounts) {
                String key = StringUtils.defaultString(a.row.getCategoryId()) + "|"
                        + StringUtils.defaultString(a.row.getDeptId());
                if (prevKey == null || !prevKey.equals(key)) {
                    if (sub != null) {
                        rows.add(sub);
                    }
                    sub = newSubtotalRow(a.row.getCategoryId(), a.row.getCategoryName(),
                            a.row.getDeptId(), a.row.getDeptName());
                    prevKey = key;
                }
                rows.add(a.row);
                addAmounts(sub, a.row);
                addAmounts(grand, a.row);
            }
            if (sub != null) {
                rows.add(sub);
            }
            rows.add(grand);
        } else {
            String prevCat = null;
            FixedAssetDepreciationReportRow sub = null;
            for (AssetAmount a : amounts) {
                String cat = StringUtils.defaultString(a.row.getCategoryId());
                if (prevCat == null || !prevCat.equals(cat)) {
                    if (sub != null) {
                        rows.add(sub);
                    }
                    sub = newSubtotalRow(a.row.getCategoryId(), a.row.getCategoryName(), null, null);
                    prevCat = cat;
                }
                rows.add(a.row);
                addAmounts(sub, a.row);
                addAmounts(grand, a.row);
            }
            if (sub != null) {
                rows.add(sub);
            }
            rows.add(grand);
        }

        FixedAssetDepreciationReportVo vo = new FixedAssetDepreciationReportVo();
        vo.setStartPeriod(start);
        vo.setEndPeriod(end);
        vo.setPeriodDeprColumnLabel(FixedAssetDepreciationReportRules.periodDeprColumnLabel(end));
        vo.setIncludeChangeInfo(includeChangeInfo);
        vo.setRows(rows);
        return vo;
    }

    private Map<String, String> loadChangeInfoByAsset(String bookId, String start, String end, List<FixedAsset> assets) {
        Map<String, String> result = new HashMap<>();
        if (CollUtil.isEmpty(assets)) {
            return result;
        }
        Set<String> assetIds = assets.stream().map(FixedAsset::getId).filter(StringUtils::isNotBlank).collect(Collectors.toSet());
        if (assetIds.isEmpty()) {
            return result;
        }
        List<FixedAssetChange> changes = fixedAssetChangeMapper.selectList(Wrappers.<FixedAssetChange>lambdaQuery()
                .eq(FixedAssetChange::getBookId, bookId)
                .in(FixedAssetChange::getAssetId, assetIds)
                .ge(FixedAssetChange::getYearPeriod, start)
                .le(FixedAssetChange::getYearPeriod, end)
                .orderByAsc(FixedAssetChange::getYearPeriod)
                .orderByAsc(FixedAssetChange::getId));
        if (CollUtil.isEmpty(changes)) {
            return result;
        }
        // Period string compare works for yyyy-MM; also filter with comparePeriods for safety
        changes = changes.stream()
                .filter(c -> FixedAssetDepreciationRules.comparePeriods(c.getYearPeriod(), start) >= 0
                        && FixedAssetDepreciationRules.comparePeriods(c.getYearPeriod(), end) <= 0)
                .collect(Collectors.toList());
        if (changes.isEmpty()) {
            return result;
        }
        List<String> changeIds = changes.stream().map(FixedAssetChange::getId).toList();
        List<FixedAssetChangeItem> items = fixedAssetChangeItemMapper.selectList(
                Wrappers.<FixedAssetChangeItem>lambdaQuery()
                        .eq(FixedAssetChangeItem::getBookId, bookId)
                        .in(FixedAssetChangeItem::getChangeId, changeIds)
                        .orderByAsc(FixedAssetChangeItem::getId));
        Map<String, List<FixedAssetChangeItem>> itemsByChange = items.stream()
                .collect(Collectors.groupingBy(FixedAssetChangeItem::getChangeId, LinkedHashMap::new, Collectors.toList()));

        Map<String, List<String>> textsByAsset = new LinkedHashMap<>();
        for (FixedAssetChange change : changes) {
            List<FixedAssetChangeItem> changeItems = itemsByChange.getOrDefault(change.getId(), List.of());
            List<String> itemTexts = changeItems.stream()
                    .map(i -> FixedAssetChangeInfoRules.formatItem(i.getFieldLabel(), i.getBeforeValue(), i.getAfterValue()))
                    .collect(Collectors.toList());
            String one = FixedAssetChangeInfoRules.formatOneChange(change.getRemark(), itemTexts);
            if (StringUtils.isBlank(one)) {
                continue;
            }
            textsByAsset.computeIfAbsent(change.getAssetId(), k -> new ArrayList<>()).add(one);
        }
        for (Map.Entry<String, List<String>> e : textsByAsset.entrySet()) {
            result.put(e.getKey(), FixedAssetChangeInfoRules.joinAssetChanges(e.getValue()));
        }
        return result;
    }

    private Map<String, Map<String, BigDecimal>> loadDeprByAsset(String bookId) {
        List<FixedAssetDepr> list = fixedAssetDeprMapper.selectList(Wrappers.<FixedAssetDepr>lambdaQuery()
                .eq(FixedAssetDepr::getBookId, bookId));
        Map<String, Map<String, BigDecimal>> map = new HashMap<>();
        for (FixedAssetDepr d : list) {
            map.computeIfAbsent(d.getAssetId(), k -> new HashMap<>())
                    .merge(d.getYearPeriod(),
                            d.getDeprAmount() == null ? BigDecimal.ZERO : d.getDeprAmount(),
                            BigDecimal::add);
        }
        return map;
    }

    private Map<String, String> loadCategoryNames(List<FixedAsset> assets) {
        Map<String, String> map = new HashMap<>();
        if (CollUtil.isEmpty(assets)) {
            return map;
        }
        List<String> ids = assets.stream().map(FixedAsset::getCategoryId).filter(Objects::nonNull).distinct().toList();
        if (ids.isEmpty()) {
            return map;
        }
        for (AssetCategory c : assetCategoryMapper.selectBatchIds(ids)) {
            map.put(c.getId(), c.getName());
        }
        return map;
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

    private FixedAssetDepreciationReportRow newSubtotalRow(String categoryId, String categoryName,
                                                          String deptId, String deptName) {
        FixedAssetDepreciationReportRow r = new FixedAssetDepreciationReportRow();
        r.setRowType(ROW_SUBTOTAL);
        r.setCategoryId(categoryId);
        r.setCategoryName(categoryName);
        r.setDeptId(deptId);
        r.setDeptName(deptName);
        r.setAssetName("小计");
        zeroAmounts(r);
        return r;
    }

    private FixedAssetDepreciationReportRow newTotalRow() {
        FixedAssetDepreciationReportRow r = new FixedAssetDepreciationReportRow();
        r.setRowType(ROW_TOTAL);
        r.setCategoryName("合计");
        r.setAssetName("合计");
        zeroAmounts(r);
        return r;
    }

    private void zeroAmounts(FixedAssetDepreciationReportRow r) {
        r.setOriginalValue(BigDecimal.ZERO);
        r.setOpeningAccumDepr(BigDecimal.ZERO);
        r.setPeriodDepr(BigDecimal.ZERO);
        r.setYearDepr(BigDecimal.ZERO);
        r.setEndingAccumDepr(BigDecimal.ZERO);
        r.setEndingImpairment(BigDecimal.ZERO);
        r.setEndingNetValue(BigDecimal.ZERO);
    }

    private void addAmounts(FixedAssetDepreciationReportRow target, FixedAssetDepreciationReportRow src) {
        target.setOriginalValue(nz(target.getOriginalValue()).add(nz(src.getOriginalValue())));
        target.setOpeningAccumDepr(nz(target.getOpeningAccumDepr()).add(nz(src.getOpeningAccumDepr())));
        target.setPeriodDepr(nz(target.getPeriodDepr()).add(nz(src.getPeriodDepr())));
        target.setYearDepr(nz(target.getYearDepr()).add(nz(src.getYearDepr())));
        target.setEndingAccumDepr(nz(target.getEndingAccumDepr()).add(nz(src.getEndingAccumDepr())));
        target.setEndingImpairment(nz(target.getEndingImpairment()).add(nz(src.getEndingImpairment())));
        target.setEndingNetValue(nz(target.getEndingNetValue()).add(nz(src.getEndingNetValue())));
    }

    private FixedAssetDepreciationReportRow copyAs(FixedAssetDepreciationReportRow src, String rowType) {
        FixedAssetDepreciationReportRow r = new FixedAssetDepreciationReportRow();
        r.setRowType(rowType);
        r.setCategoryId(src.getCategoryId());
        r.setCategoryName(src.getCategoryName());
        r.setOriginalValue(src.getOriginalValue());
        r.setOpeningAccumDepr(src.getOpeningAccumDepr());
        r.setPeriodDepr(src.getPeriodDepr());
        r.setYearDepr(src.getYearDepr());
        r.setEndingAccumDepr(src.getEndingAccumDepr());
        r.setEndingImpairment(src.getEndingImpairment());
        r.setEndingNetValue(src.getEndingNetValue());
        return r;
    }

    private BigDecimal nz(BigDecimal v) {
        return v == null ? BigDecimal.ZERO : v;
    }

    private record AssetAmount(FixedAssetDepreciationReportRow row) {
    }
}
