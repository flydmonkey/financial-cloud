package com.financial.cloud.controller.fixedasset;

import com.baomidou.mybatisplus.core.toolkit.StringUtils;
import com.financial.cloud.authn.annotation.CurrentUser;
import com.financial.cloud.common.Message;
import com.financial.cloud.constants.auth.ProductRoles;
import com.financial.cloud.domain.idm.UserInfo;
import com.financial.cloud.dto.fixedasset.FixedAssetAccrueDto;
import com.financial.cloud.dto.fixedasset.FixedAssetAccrueResultVo;
import com.financial.cloud.dto.fixedasset.FixedAssetDepreciationParamsDto;
import com.financial.cloud.dto.fixedasset.FixedAssetDepreciationStatusVo;
import com.financial.cloud.dto.fixedasset.FixedAssetWorkItemDto;
import com.financial.cloud.service.fixedasset.FixedAssetDepreciationService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/fixed-asset/depreciation")
@RequiredArgsConstructor
public class FixedAssetDepreciationController {

    private final FixedAssetDepreciationService depreciationService;

    @GetMapping("/status")
    public Message<FixedAssetDepreciationStatusVo> status(@RequestParam(required = false) String yearPeriod,
                                                         @CurrentUser UserInfo userInfo) {
        if (StringUtils.isBlank(userInfo.getBookId())) {
            return Message.failed("所属账套ID不能为空");
        }
        return depreciationService.status(userInfo.getBookId(), yearPeriod);
    }

    @GetMapping("/params")
    public Message<FixedAssetDepreciationParamsDto> getParams(@RequestParam(required = false) String yearPeriod,
                                                             @CurrentUser UserInfo userInfo) {
        return depreciationService.getParams(userInfo.getBookId(), yearPeriod);
    }

    @PutMapping("/params")
    public Message<FixedAssetDepreciationParamsDto> saveParams(@RequestBody FixedAssetDepreciationParamsDto dto,
                                                              @CurrentUser UserInfo userInfo) {
        ProductRoles.requireWriteBusiness();
        return depreciationService.saveParams(userInfo.getBookId(), dto);
    }

    @GetMapping("/work")
    public Message<List<FixedAssetWorkItemDto>> listWork(@RequestParam(required = false) String yearPeriod,
                                                         @CurrentUser UserInfo userInfo) {
        return depreciationService.listWork(userInfo.getBookId(), yearPeriod);
    }

    @PutMapping("/work")
    public Message<String> saveWork(@RequestParam(required = false) String yearPeriod,
                                    @RequestBody List<FixedAssetWorkItemDto> items,
                                    @CurrentUser UserInfo userInfo) {
        ProductRoles.requireWriteBusiness();
        return depreciationService.saveWork(userInfo.getBookId(), yearPeriod, items);
    }

    @PostMapping("/accrue")
    public Message<FixedAssetAccrueResultVo> accrue(@RequestBody(required = false) FixedAssetAccrueDto dto,
                                                    @CurrentUser UserInfo userInfo) {
        ProductRoles.requireWriteBusiness();
        return depreciationService.accrue(userInfo.getBookId(), dto);
    }
}
