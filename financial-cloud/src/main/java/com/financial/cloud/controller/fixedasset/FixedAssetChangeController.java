package com.financial.cloud.controller.fixedasset;

import com.financial.cloud.service.book.BookOwnershipGuard;
import com.baomidou.mybatisplus.core.toolkit.StringUtils;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.financial.cloud.authn.annotation.CurrentUser;
import com.financial.cloud.common.Message;
import com.financial.cloud.constants.auth.ProductRoles;
import com.financial.cloud.domain.idm.UserInfo;
import com.financial.cloud.dto.fixedasset.FixedAssetChangePageDto;
import com.financial.cloud.dto.fixedasset.FixedAssetChangeSaveDto;
import com.financial.cloud.dto.fixedasset.FixedAssetChangeVo;
import com.financial.cloud.service.fixedasset.FixedAssetChangeService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/fixed-asset/change")
@RequiredArgsConstructor
public class FixedAssetChangeController {
    private final BookOwnershipGuard bookOwnershipGuard;

    private final FixedAssetChangeService changeService;

    @GetMapping("/fetch")
    public Message<Page<FixedAssetChangeVo>> fetch(FixedAssetChangePageDto dto, @CurrentUser UserInfo userInfo) {
        bookOwnershipGuard.checkRequest("fixed_asset_change", dto);
        dto.setBookId(userInfo.getBookId());
        if (StringUtils.isBlank(dto.getBookId())) {
            return Message.failed("所属账套ID不能为空");
        }
        return changeService.pageList(dto);
    }

    @PostMapping("/save")
    public Message<String> save(@RequestBody FixedAssetChangeSaveDto dto, @CurrentUser UserInfo userInfo) {
        bookOwnershipGuard.checkRequest("fixed_asset_change", dto);
        ProductRoles.requireWriteBusiness();
        dto.setBookId(userInfo.getBookId());
        return changeService.saveChange(dto);
    }
}
