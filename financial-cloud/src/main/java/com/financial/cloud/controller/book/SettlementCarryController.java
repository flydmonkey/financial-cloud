package com.financial.cloud.controller.book;



import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.financial.cloud.authn.annotation.CurrentUser;
import com.financial.cloud.common.Message;
import com.financial.cloud.dto.book.SettlementCarryforwardVo;
import com.financial.cloud.domain.idm.UserInfo;
import com.financial.cloud.dto.voucher.GenerateVoucherDto;
import com.financial.cloud.dto.voucher.VoucherTemplatePageDto;
import com.financial.cloud.service.book.SettlementCarryService;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

@RequiredArgsConstructor
@Slf4j
@RestController
@RequestMapping("/api/settlementcarry")
public class SettlementCarryController {

    private final SettlementCarryService settlementCarryService;

    
    @GetMapping(value = { "/fetchcarry" })
    public Message<Page<SettlementCarryforwardVo>> fetchCarry(VoucherTemplatePageDto dto,@CurrentUser UserInfo userInfo) {
        log.debug("fetch {}",dto);
        dto.setRelatedId(userInfo.getBookId());
        dto.setBookId(userInfo.getBookId());
        return settlementCarryService.fetchCarry(dto);
    }
    
    @PostMapping("/generate-voucher")
    public Message<String> generateVoucher(@Validated @RequestBody GenerateVoucherDto dto, @CurrentUser UserInfo currentUser) {
        dto.setBookId(currentUser.getBookId());
        return settlementCarryService.generateVoucher(dto);
    }
    
    /**
     *
     * @param id 被删除项ID
     * @return 结果
     */
    @DeleteMapping(value = {"/delete/{voucherId}"})
    public Message<String> delete(@PathVariable("voucherId") String voucherId, @CurrentUser UserInfo currentUser) {
        return settlementCarryService.delete(currentUser.getBookId(), voucherId);
    }
}
