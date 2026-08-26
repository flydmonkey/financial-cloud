package com.financial.cloud.controller.voucher;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.financial.cloud.authn.annotation.CurrentUser;
import com.financial.cloud.common.Message;
import com.financial.cloud.domain.idm.UserInfo;
import com.financial.cloud.dto.voucher.VoucherChangeDto;
import com.financial.cloud.dto.voucher.VoucherItemPageDto;
import com.financial.cloud.dto.voucher.VoucherPageDto;
import com.financial.cloud.dto.voucher.VoucherSuccessiveQueryDto;
import com.financial.cloud.dto.voucher.VoucherItemVo;
import com.financial.cloud.dto.voucher.VoucherSuccessiveDto;
import com.financial.cloud.dto.voucher.VoucherVo;
import com.financial.cloud.enums.voucher.VoucherStatusEnum;
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

    @GetMapping("/items/fetch")
    public Message<Page<VoucherItemVo>> subLedger(VoucherItemPageDto paramsDto,
                                                  @CurrentUser UserInfo userInfo) {
        paramsDto.setBookId(userInfo.getBookId());
        return voucherService.subLedger(paramsDto);
    }

    @GetMapping("/items/fetch-by-cash-flow")
    public Message<Page<VoucherItemVo>> fetchByCashFlow(VoucherItemPageDto paramsDto,
                                                        @CurrentUser UserInfo userInfo) {
        paramsDto.setBookId(userInfo.getBookId());
        return voucherService.fetchByCashFlow(paramsDto);
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
        dto.setBookId(userInfo.getBookId());
        dto.setStatus(VoucherStatusEnum.DRAFT.getValue());
        if (StringUtils.isEmpty(dto.getId())) {
            return voucherService.save(dto);
        } else {
            return voucherService.update(dto);
        }
    }

    @PutMapping("/update")
    public Message<String> update(@Validated(value = EditGroup.class) @RequestBody VoucherChangeDto dto,
                                  @CurrentUser UserInfo userInfo) {
        dto.setBookId(userInfo.getBookId());
        return voucherService.update(dto);
    }

    @DeleteMapping("/delete/{ids}")
    public Message<String> delete(@PathVariable(name = "ids") List<String> ids,
                                  @CurrentUser UserInfo userInfo) {
        return voucherService.delete(ids, userInfo.getBookId());
    }

    @PostMapping("/submit")
    public Message<String> submit(@Validated @RequestBody VoucherChangeDto dto,
                                  @CurrentUser UserInfo userInfo) {
        dto.setBookId(userInfo.getBookId());
        return voucherService.submit(dto, true);
    }

    @PostMapping("/submit/{ids}")
    public Message<String> submitBatch(@PathVariable(name = "ids") List<String> ids,
                                       @CurrentUser UserInfo userInfo) {
        return voucherService.submitBatch(ids, userInfo.getBookId());
    }

    @PutMapping("/cancel/{ids}")
    public Message<Integer> cancelByIds(@PathVariable(name = "ids") List<String> ids,
                                        @CurrentUser UserInfo userInfo) {
        return voucherService.cancelByIds(ids, userInfo.getBookId());
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
        return voucherService.audit(ids, userInfo);
    }

    @PutMapping("/sender/{ids}")
    public Message<Void> sender(@PathVariable(name = "ids") List<String> ids,
                                @CurrentUser UserInfo userInfo) {
        return voucherService.sender(ids, userInfo);
    }

    @PutMapping("/manage-audit/{ids}")
    public Message<Void> manageAudit(@PathVariable(name = "ids") List<String> ids,
                                     @CurrentUser UserInfo userInfo) {
        return voucherService.manageAudit(ids, userInfo);
    }

    @GetMapping("/export")
    public void export(HttpServletResponse response,
                       VoucherPageDto dto,
                       @CurrentUser UserInfo userInfo) throws IOException {
        dto.setBookId(userInfo.getBookId());
        voucherService.export(dto, response);
    }
}
