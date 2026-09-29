package com.financial.cloud.controller.voucher;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.financial.cloud.authn.annotation.CurrentUser;
import com.financial.cloud.common.ExcelImport;
import com.financial.cloud.common.Message;
import com.financial.cloud.domain.idm.UserInfo;
import com.financial.cloud.domain.voucher.Voucher;
import com.financial.cloud.dto.voucher.MultiColumnLedgerVo;
import com.financial.cloud.dto.voucher.QuantityLedgerVo;
import com.financial.cloud.dto.voucher.VoucherChangeDto;
import com.financial.cloud.dto.voucher.VoucherImportResultVo;
import com.financial.cloud.dto.voucher.VoucherItemPageDto;
import com.financial.cloud.dto.voucher.VoucherPageDto;
import com.financial.cloud.dto.voucher.VoucherSuccessiveQueryDto;
import com.financial.cloud.dto.voucher.VoucherItemVo;
import com.financial.cloud.dto.voucher.VoucherSuccessiveDto;
import com.financial.cloud.dto.voucher.VoucherVo;
import com.financial.cloud.constants.auth.ProductRoles;
import com.financial.cloud.enums.voucher.VoucherStatusEnum;
import com.financial.cloud.service.history.HistorySystemLogsService;
import com.financial.cloud.service.voucher.MultiColumnLedgerService;
import com.financial.cloud.service.voucher.QuantityLedgerService;
import com.financial.cloud.service.voucher.VoucherService;
import com.financial.cloud.validation.AddGroup;
import com.financial.cloud.validation.EditGroup;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.io.IOException;
import java.util.List;


@RestController
@RequestMapping("/api/voucher")
@Slf4j
@RequiredArgsConstructor
public class VoucherController {
    private final VoucherService voucherService;
    private final HistorySystemLogsService historySystemLogsService;
    private final MultiColumnLedgerService multiColumnLedgerService;
    private final QuantityLedgerService quantityLedgerService;

    /** 凭证关键操作审计：操作人、动作、对象与结果（IP 由日志服务统一提取） */
    private void auditLog(String action, List<String> ids, Message<?> result, UserInfo operator) {
        try {
            boolean success = result != null && result.getCode() == Message.SUCCESS;
            historySystemLogsService.log("凭证操作", String.join(",", ids),
                    ids.size() + " 张凭证", null,
                    result == null ? "" : result.getMessage(),
                    action, success ? "success" : "fail", operator, null);
        } catch (Exception e) {
            log.warn("凭证操作审计日志写入失败：{}", e.getMessage());
        }
    }

    @GetMapping("/items/fetch")
    public Message<Page<VoucherItemVo>> subLedger(VoucherItemPageDto paramsDto,
                                                  @CurrentUser UserInfo userInfo) {
        paramsDto.setBookId(userInfo.getBookId());
        return voucherService.subLedger(paramsDto);
    }

    @GetMapping("/items/export-pdf")
    public void subLedgerExportPdf(HttpServletResponse response,
                                   VoucherItemPageDto paramsDto,
                                   @CurrentUser UserInfo userInfo) throws IOException {
        paramsDto.setBookId(userInfo.getBookId());
        voucherService.exportSubLedgerPdf(paramsDto, response);
    }

    @GetMapping("/items/fetch-by-cash-flow")
    public Message<Page<VoucherItemVo>> fetchByCashFlow(VoucherItemPageDto paramsDto,
                                                        @CurrentUser UserInfo userInfo) {
        paramsDto.setBookId(userInfo.getBookId());
        return voucherService.fetchByCashFlow(paramsDto);
    }

    /**
     * 多栏账：栏母科目 + 日期区间，逐凭证展示直接子科目栏位净额与余额
     */
    @GetMapping("/multi-column-ledger")
    public Message<MultiColumnLedgerVo> multiColumnLedger(@RequestParam String subjectCode,
                                                          @RequestParam(required = false) String startDate,
                                                          @RequestParam(required = false) String endDate,
                                                          @CurrentUser UserInfo userInfo) {
        return Message.ok(multiColumnLedgerService.query(
                userInfo.getBookId(), subjectCode, startDate, endDate));
    }

    /**
     * 数量金额账：科目 + 日期区间，逐分录展示收入/发出/结存的数量、单价、金额
     */
    @GetMapping("/quantity-ledger")
    public Message<QuantityLedgerVo> quantityLedger(@RequestParam String subjectCode,
                                                    @RequestParam(required = false) String startDate,
                                                    @RequestParam(required = false) String endDate,
                                                    @CurrentUser UserInfo userInfo) {
        return Message.ok(quantityLedgerService.query(
                userInfo.getBookId(), subjectCode, startDate, endDate));
    }

    @GetMapping(value = {"/fetch"})
    public Message<Page<VoucherVo>> fetch(VoucherPageDto dto,
                                          @CurrentUser UserInfo userInfo) {
        dto.setBookId(userInfo.getBookId());
        log.debug("fetch {}", dto);
        return voucherService.pageList(dto);
    }

    @GetMapping("/get/{id}")
    public Message<VoucherVo> getById(@PathVariable(name = "id") String id) {
        return voucherService.queryById(id);
    }

    @GetMapping("/able-word-num")
    public Message<Integer> getAbleWordNum(@RequestParam(name = "head", required = false) String head,
                                           @RequestParam(name = "year", required = false) Integer year,
                                           @RequestParam(name = "month", required = false) Integer month,
                                           @CurrentUser UserInfo userInfo) {
        return voucherService.getAbleWordNum(userInfo.getBookId(), head, year, month);
    }

    @PostMapping("/draft")
    public Message<String> draft(@Validated(value = AddGroup.class) @RequestBody VoucherChangeDto dto,
                                 @CurrentUser UserInfo userInfo) {
        ProductRoles.requireWriteVoucher();
        dto.setBookId(userInfo.getBookId());
        if (StringUtils.isEmpty(dto.getId())) {
            dto.setStatus(VoucherStatusEnum.DRAFT.getValue());
            return voucherService.save(dto);
        }
        Voucher existing = voucherService.getById(dto.getId());
        if (existing != null && StringUtils.isBlank(existing.getSenderId())
                && !VoucherStatusEnum.CANCELLED.getValue().equals(existing.getStatus())) {
            dto.setStatus(existing.getStatus());
        } else {
            dto.setStatus(VoucherStatusEnum.DRAFT.getValue());
        }
        return voucherService.update(dto);
    }

    @PutMapping("/update")
    public Message<String> update(@Validated(value = EditGroup.class) @RequestBody VoucherChangeDto dto,
                                  @CurrentUser UserInfo userInfo) {
        ProductRoles.requireWriteVoucher();
        dto.setBookId(userInfo.getBookId());
        return voucherService.update(dto);
    }

    @DeleteMapping("/delete/{ids}")
    public Message<String> delete(@PathVariable(name = "ids") List<String> ids,
                                  @CurrentUser UserInfo userInfo) {
        ProductRoles.requireWriteVoucher();
        Message<String> result = voucherService.delete(ids, userInfo.getBookId());
        auditLog("删除", ids, result, userInfo);
        return result;
    }

    @PostMapping("/submit")
    public Message<String> submit(@Validated @RequestBody VoucherChangeDto dto,
                                  @CurrentUser UserInfo userInfo) {
        ProductRoles.requireWriteVoucher();
        dto.setBookId(userInfo.getBookId());
        return voucherService.submit(dto, true);
    }

    @PostMapping("/submit/{ids}")
    public Message<String> submitBatch(@PathVariable(name = "ids") List<String> ids,
                                       @CurrentUser UserInfo userInfo) {
        ProductRoles.requireWriteVoucher();
        return voucherService.submitBatch(ids, userInfo.getBookId());
    }

    @PutMapping("/cancel/{ids}")
    public Message<Integer> cancelByIds(@PathVariable(name = "ids") List<String> ids,
                                        @CurrentUser UserInfo userInfo) {
        ProductRoles.requireWriteVoucher();
        return voucherService.cancelByIds(ids, userInfo.getBookId());
    }

    /**
     * 作废凭证：保留字号、不参与账表，可恢复。仅暂存/被拒绝且未过账可作废。
     */
    @PutMapping("/void/{id}")
    public Message<String> voidVoucher(@PathVariable(name = "id") String id,
                                       @CurrentUser UserInfo userInfo) {
        ProductRoles.requireWriteVoucher();
        Message<String> result = voucherService.voidById(id, userInfo.getBookId());
        auditLog("作废", List.of(id), result, userInfo);
        return result;
    }

    /**
     * 恢复作废：已作废凭证恢复为暂存。
     */
    @PutMapping("/unvoid/{id}")
    public Message<String> unvoidVoucher(@PathVariable(name = "id") String id,
                                         @CurrentUser UserInfo userInfo) {
        ProductRoles.requireWriteVoucher();
        Message<String> result = voucherService.unvoidById(id, userInfo.getBookId());
        auditLog("恢复作废", List.of(id), result, userInfo);
        return result;
    }

    /**
     * 红字冲销：为已过账凭证生成金额全负的冲销凭证（暂存态）。
     */
    @PostMapping("/reverse/{id}")
    public Message<String> reverseVoucher(@PathVariable(name = "id") String id,
                                          @CurrentUser UserInfo userInfo) {
        ProductRoles.requireWriteVoucher();
        Message<String> result = voucherService.reverseById(id, userInfo.getBookId());
        auditLog("红字冲销", List.of(id), result, userInfo);
        return result;
    }

    @GetMapping("/successive")
    public Message<List<VoucherSuccessiveDto>> checkSuccessive(@CurrentUser UserInfo userInfo,
                                                               VoucherSuccessiveQueryDto query) {
        query.setBookId(userInfo.getBookId());
        return voucherService.checkSuccessiveAll(userInfo.getBookId());
    }

    @PutMapping("/successive")
    public Message<Void> updateSuccessive(@CurrentUser UserInfo userInfo,
                                          @RequestBody @Validated List<VoucherSuccessiveDto> dtos) {
        for (VoucherSuccessiveDto dto : dtos) {
            dto.setBookId(userInfo.getBookId());
        }
        return voucherService.updateSuccessive(dtos);
    }

    @PutMapping("/audit/{ids}")
    public Message<Void> audit(@PathVariable(name = "ids") List<String> ids,
                               @CurrentUser UserInfo userInfo) {
        ProductRoles.requireApproveVoucher();
        Message<Void> result = voucherService.audit(ids, userInfo);
        auditLog("审核", ids, result, userInfo);
        return result;
    }

    @PutMapping("/unaudit/{ids}")
    public Message<Void> unaudit(@PathVariable(name = "ids") List<String> ids,
                                   @CurrentUser UserInfo userInfo) {
        ProductRoles.requireApproveVoucher();
        Message<Void> result = voucherService.unaudit(ids, userInfo.getBookId());
        auditLog("反审核", ids, result, userInfo);
        return result;
    }

    @PutMapping("/sender/{ids}")
    public Message<Void> sender(@PathVariable(name = "ids") List<String> ids,
                                @CurrentUser UserInfo userInfo) {
        ProductRoles.requireWriteVoucher();
        Message<Void> result = voucherService.sender(ids, userInfo);
        auditLog("过账", ids, result, userInfo);
        return result;
    }

    @PutMapping("/unsender/{ids}")
    public Message<Void> unsender(@PathVariable(name = "ids") List<String> ids,
                                  @CurrentUser UserInfo userInfo) {
        ProductRoles.requireWriteVoucher();
        Message<Void> result = voucherService.unsender(ids, userInfo.getBookId());
        auditLog("反过账", ids, result, userInfo);
        return result;
    }

    @PutMapping("/manage-audit/{ids}")
    public Message<Void> manageAudit(@PathVariable(name = "ids") List<String> ids,
                                     @CurrentUser UserInfo userInfo) {
        ProductRoles.requireApproveVoucher();
        return voucherService.manageAudit(ids, userInfo);
    }

    @GetMapping("/export")
    public void export(HttpServletResponse response,
                       VoucherPageDto dto,
                       @CurrentUser UserInfo userInfo) throws IOException {
        dto.setBookId(userInfo.getBookId());
        voucherService.export(dto, response);
    }

    @GetMapping("/import-template")
    public void importTemplate(HttpServletResponse response) throws IOException {
        voucherService.downloadImportTemplate(response);
    }

    @PostMapping("/import")
    public Message<VoucherImportResultVo> importExcel(
            @ModelAttribute("excelImportFile") ExcelImport excelImportFile,
            @RequestParam(value = "conflictMode", required = false) String conflictMode,
            @CurrentUser UserInfo userInfo) {
        ProductRoles.requireWriteVoucher();
        return voucherService.importFromExcel(userInfo.getBookId(), excelImportFile, conflictMode);
    }
}
