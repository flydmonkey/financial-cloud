package com.financial.cloud.controller.voucher;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.financial.cloud.authn.annotation.CurrentUser;
import com.financial.cloud.common.Message;
import com.financial.cloud.dto.common.ListIdsDto;
import com.financial.cloud.domain.idm.UserInfo;
import com.financial.cloud.domain.voucher.VoucherTemplate;
import com.financial.cloud.dto.voucher.VoucherTemplateChangeDto;
import com.financial.cloud.dto.voucher.VoucherTemplatePageDto;
import com.financial.cloud.service.voucher.VoucherTemplateService;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

/**
 * 凭证模板配置接口
 */

@RestController
@RequestMapping("/api/vouchertemplate")
@Slf4j
@RequiredArgsConstructor
public class VoucherTemplateController {

    private final VoucherTemplateService voucherTemplateService;


    
    @GetMapping(value = {"/get"})
    public Message<VoucherTemplate> get(@RequestParam("id") String id,@CurrentUser UserInfo userInfo) {
    	return voucherTemplateService.get(id);
    }
    
    /**
     *
     * @param 
     */
    @GetMapping(value = {"/fetch"})
    public Message<Page<VoucherTemplate>> fetch(VoucherTemplatePageDto dto,@CurrentUser UserInfo userInfo) {
        return voucherTemplateService.pageList(dto);
    }

    /**
     *
     * @param dto 查询参数
     * @return 结果
     */
    @PostMapping(value = {"/save"})
    public Message<String> save(@Validated @RequestBody VoucherTemplateChangeDto dto,
                                                           @CurrentUser UserInfo userInfo) {
        //dto.setBookId(userInfo.getBookId());
        return voucherTemplateService.save(dto);
    }

    /**
     *
     * @param id 被删除项ID
     * @return 结果
     */
    @DeleteMapping(value = {"/delete"})
    public Message<String> delete(@Validated @RequestBody ListIdsDto dto) {
        return voucherTemplateService.delete(dto);
    }
}
