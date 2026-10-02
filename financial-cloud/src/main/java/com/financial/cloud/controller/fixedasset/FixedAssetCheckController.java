package com.financial.cloud.controller.fixedasset;

import com.financial.cloud.service.book.BookOwnershipGuard;
import com.baomidou.mybatisplus.core.toolkit.StringUtils;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.financial.cloud.authn.annotation.CurrentUser;
import com.financial.cloud.common.Message;
import com.financial.cloud.constants.auth.ProductRoles;
import com.financial.cloud.domain.fixedasset.FixedAssetCheck;
import com.financial.cloud.domain.fixedasset.FixedAssetCheckItem;
import com.financial.cloud.domain.idm.UserInfo;
import com.financial.cloud.dto.fixedasset.FixedAssetCheckDtos;
import com.financial.cloud.service.fixedasset.FixedAssetCheckService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/fixed-asset/check")
@RequiredArgsConstructor
public class FixedAssetCheckController {
    private final BookOwnershipGuard bookOwnershipGuard;

    private final FixedAssetCheckService fixedAssetCheckService;

    @GetMapping("/fetch")
    public Message<Page<FixedAssetCheck>> fetch(FixedAssetCheckDtos.PageDto dto, @CurrentUser UserInfo userInfo) {
        bookOwnershipGuard.checkRequest("fixed_asset_check", dto);
        dto.setBookId(userInfo.getBookId());
        if (StringUtils.isBlank(dto.getBookId())) {
            return Message.failed("所属账套ID不能为空");
        }
        return Message.ok(fixedAssetCheckService.page(dto));
    }

    @GetMapping("/get/{id}")
    public Message<FixedAssetCheckDtos.DetailVo> getById(@PathVariable("id") String id,
                                                       @CurrentUser UserInfo userInfo) {
        bookOwnershipGuard.checkReference("fixed_asset_check", "id", id);
        return Message.ok(fixedAssetCheckService.detail(id, userInfo.getBookId()));
    }

    @PostMapping("/create")
    public Message<FixedAssetCheck> create(@RequestBody FixedAssetCheckDtos.CreateDto dto,
                                           @CurrentUser UserInfo userInfo) {
        bookOwnershipGuard.checkRequest("fixed_asset_check", dto);
        ProductRoles.requireWriteBusiness();
        return Message.ok(fixedAssetCheckService.create(dto, userInfo.getBookId()));
    }

    @PutMapping("/item")
    public Message<FixedAssetCheckItem> updateItem(@RequestBody FixedAssetCheckDtos.ItemDto dto,
                                                   @CurrentUser UserInfo userInfo) {
        bookOwnershipGuard.checkRequest("fixed_asset_check", dto);
        ProductRoles.requireWriteBusiness();
        return Message.ok(fixedAssetCheckService.updateItem(dto, userInfo.getBookId()));
    }

    @PutMapping("/complete/{id}")
    public Message<FixedAssetCheck> complete(@PathVariable("id") String id, @CurrentUser UserInfo userInfo) {
        bookOwnershipGuard.checkReference("fixed_asset_check", "id", id);
        ProductRoles.requireWriteBusiness();
        return Message.ok(fixedAssetCheckService.complete(id, userInfo.getBookId()));
    }

    /**
     * 盘亏一键下账：整件盘亏资产走清理流程生成凭证；返回处理与跳过明细。
     */
    @PutMapping("/dispose-deficit/{id}")
    public Message<FixedAssetCheckDtos.DeficitDisposeVo> disposeDeficit(@PathVariable("id") String id,
                                                                        @CurrentUser UserInfo userInfo) {
        bookOwnershipGuard.checkReference("fixed_asset_check", "id", id);
        ProductRoles.requireWriteBusiness();
        return Message.ok(fixedAssetCheckService.disposeDeficit(id, userInfo.getBookId()));
    }

    @GetMapping("/surplus-preview/{id}")
    public Message<FixedAssetCheckDtos.SurplusPreviewVo> surplusPreview(@PathVariable("id") String id,
                                                                        @CurrentUser UserInfo userInfo) {
        bookOwnershipGuard.checkReference("fixed_asset_check", "id", id);
        return Message.ok(fixedAssetCheckService.surplusPreview(id, userInfo.getBookId()));
    }

    /**
     * 盘盈入账：逐项生成盘盈草稿凭证（新增卡片或原卡加数量）；返回处理与跳过明细。
     */
    @PutMapping("/book-surplus/{id}")
    public Message<FixedAssetCheckDtos.SurplusBookVo> bookSurplus(
            @PathVariable("id") String id,
            @RequestBody java.util.List<FixedAssetCheckDtos.SurplusBookItemDto> items,
            @CurrentUser UserInfo userInfo) {
        bookOwnershipGuard.checkReference("fixed_asset_check", "id", id);
        ProductRoles.requireWriteBusiness();
        return Message.ok(fixedAssetCheckService.bookSurplus(id, userInfo.getBookId(), items));
    }

    @DeleteMapping("/{id}")
    public Message<String> delete(@PathVariable("id") String id, @CurrentUser UserInfo userInfo) {
        bookOwnershipGuard.checkReference("fixed_asset_check", "id", id);
        ProductRoles.requireWriteBusiness();
        fixedAssetCheckService.delete(id, userInfo.getBookId());
        return Message.ok("删除成功");
    }
}
