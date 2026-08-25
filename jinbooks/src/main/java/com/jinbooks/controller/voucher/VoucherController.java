package com.jinbooks.controller.voucher;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.jinbooks.authn.annotation.CurrentUser;
import com.jinbooks.common.Message;
import com.jinbooks.domain.idm.UserInfo;
import com.jinbooks.dto.voucher.VoucherChangeDto;
import com.jinbooks.dto.voucher.VoucherItemPageDto;
import com.jinbooks.dto.voucher.VoucherPageDto;
import com.jinbooks.dto.voucher.VoucherSuccessiveQueryDto;
import com.jinbooks.dto.voucher.VoucherItemVo;
import com.jinbooks.dto.voucher.VoucherSuccessiveDto;
import com.jinbooks.dto.voucher.VoucherVo;
import com.jinbooks.enums.VoucherStatusEnum;
import com.jinbooks.service.voucher.VoucherService;
import com.jinbooks.validation.AddGroup;
import com.jinbooks.validation.EditGroup;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.io.IOException;
import java.util.List;

/**
 * å­è¯ç®¡çæ¥å£
 */

@RestController
@RequestMapping("/api/voucher")
@Slf4j
@RequiredArgsConstructor
public class VoucherController {
    private final VoucherService voucherService;

    /**
     * æç»è´¦æ¥è¯?
     *
     * @param paramsDto æ¥è¯¢åæ°
     */
    @GetMapping("/items/fetch")
    public Message<Page<VoucherItemVo>> subLedger(VoucherItemPageDto paramsDto,
                                                  @CurrentUser UserInfo userInfo) {
        paramsDto.setBookId(userInfo.getBookId());
        return voucherService.subLedger(paramsDto);
    }

    /**
     * å­è¯é¡¹ç°éæµéæ¥è¯¢æ¥è¯?
     *
     * @param paramsDto æ¥è¯¢åæ°
     */
    @GetMapping("/items/fetch-by-cash-flow")
    public Message<Page<VoucherItemVo>> fetchByCashFlow(VoucherItemPageDto paramsDto,
                                                        @CurrentUser UserInfo userInfo) {
        paramsDto.setBookId(userInfo.getBookId());
        return voucherService.fetchByCashFlow(paramsDto);
    }

    /**
     * åé¡µæ¥è¯¢
     */
    @GetMapping(value = {"/fetch"})
    public Message<Page<VoucherVo>> fetch(VoucherPageDto dto,
                                          @CurrentUser UserInfo userInfo) {
        dto.setBookId(userInfo.getBookId());
        log.debug("fetch {}", dto);
        return voucherService.pageList(dto);
    }

    /**
     * æ¥è¯¢æé¡¹æç»
     */
    @GetMapping("/get/{id}")
    public Message<VoucherVo> getById(@PathVariable(name = "id") String id) {
        return voucherService.queryById(id);
    }

    /**
     * çæä¸ä¸ªå¯ç¨å­è¯å­å?
     *
     * @param head  å­å¤´
     * @param year  å¹´ä»½
     * @param month æä»½
     * @return æ°å¯ç¨å­å?
     */
    @GetMapping("/able-word-num")
    public Message<Integer> getAbleWordNum(@RequestParam(name = "head", required = false) String head,
                                           @RequestParam(name = "year", required = false) Integer year,
                                           @RequestParam(name = "month", required = false) Integer month,
                                           @CurrentUser UserInfo userInfo) {
        return voucherService.getAbleWordNum(userInfo.getBookId(), head, year, month);
    }

    /**
     * æå­
     *
     * @param dto æ°æ®
     */
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

    /**
     * ä¿å­&æäº¤
     *
     * @param dto æ°æ®
     */
    @PostMapping("/submit")
    public Message<String> submit(@Validated @RequestBody VoucherChangeDto dto,
                                  @CurrentUser UserInfo userInfo) {
        dto.setBookId(userInfo.getBookId());
        return voucherService.submit(dto, true);
    }

    /**
     * æ¹éæäº¤
     *
     * @param ids ids
     */
    @PostMapping("/submit/{ids}")
    public Message<String> submitBatch(@PathVariable(name = "ids") List<String> ids,
                                       @CurrentUser UserInfo userInfo) {
        return voucherService.submitBatch(ids, userInfo.getBookId());
    }

    /**
     * åæ¶
     *
     * @param ids å­è¯ID
     */
    @PutMapping("/cancel/{ids}")
    public Message<Integer> cancelByIds(@PathVariable(name = "ids") List<String> ids,
                                        @CurrentUser UserInfo userInfo) {
        return voucherService.cancelByIds(ids, userInfo.getBookId());
    }

    /**
     * å­è¯å·è¿ç»­æ§æ£æ?è¿åä¸è¿ç»­çå­è¯åè¡¨
     */
    @GetMapping("/successive")
    public Message<List<VoucherSuccessiveDto>> checkSuccessive(@CurrentUser UserInfo userInfo,
                                                               VoucherSuccessiveQueryDto query) {
        query.setBookId(userInfo.getBookId());
        return voucherService.checkSuccessiveAll(userInfo.getBookId());
    }

    /**
     * å­è¯è¿ç»­æ?å­è¯å·æ´æ?
     */
    @PutMapping("/successive")
    public Message<Void> updateSuccessive(@CurrentUser UserInfo userInfo,
                                          @RequestBody @Validated List<VoucherSuccessiveDto> dtos) {
        for (VoucherSuccessiveDto dto : dtos) {
            dto.setBookId(userInfo.getBookId());
        }
        return voucherService.updateSuccessive(dtos);
    }

    /**
     * å­è¯å®¡æ ¸
     */
    @PutMapping("/audit/{ids}")
    public Message<Void> audit(@PathVariable(name = "ids") List<String> ids,
                               @CurrentUser UserInfo userInfo) {
        return voucherService.audit(ids, userInfo);
    }

    /**
     * å­è¯å®¡æ ¸-è¿è´¦
     */
    @PutMapping("/sender/{ids}")
    public Message<Void> sender(@PathVariable(name = "ids") List<String> ids,
                                @CurrentUser UserInfo userInfo) {
        return voucherService.sender(ids, userInfo);
    }

    /**
     * å­è¯å®¡æ ¸-ä¸»ç®¡
     */
    @PutMapping("/manage-audit/{ids}")
    public Message<Void> manageAudit(@PathVariable(name = "ids") List<String> ids,
                                     @CurrentUser UserInfo userInfo) {
        return voucherService.manageAudit(ids, userInfo);
    }

    /**
     * å¯¼åºåè½
     */
    @GetMapping("/export")
    public void export(HttpServletResponse response,
                       VoucherPageDto dto,
                       @CurrentUser UserInfo userInfo) throws IOException {
        dto.setBookId(userInfo.getBookId());
        voucherService.export(dto, response);
    }
}
