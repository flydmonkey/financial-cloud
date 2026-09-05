package com.financial.cloud.controller.fixedasset;

import com.baomidou.mybatisplus.core.toolkit.StringUtils;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.financial.cloud.authn.annotation.CurrentUser;
import com.financial.cloud.common.Message;
import com.financial.cloud.constants.auth.ProductRoles;
import com.financial.cloud.domain.idm.UserInfo;
import com.financial.cloud.dto.common.ListIdsDto;
import com.financial.cloud.dto.fixedasset.AssetCategoryChangeDto;
import com.financial.cloud.dto.fixedasset.AssetCategoryPageDto;
import com.financial.cloud.dto.fixedasset.AssetCategoryVo;
import com.financial.cloud.service.fixedasset.AssetCategoryService;
import com.financial.cloud.validation.AddGroup;
import com.financial.cloud.validation.EditGroup;
import lombok.RequiredArgsConstructor;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/fixed-asset/category")
@RequiredArgsConstructor
public class AssetCategoryController {

    private final AssetCategoryService assetCategoryService;

    @GetMapping("/fetch")
    public Message<Page<AssetCategoryVo>> fetch(AssetCategoryPageDto dto, @CurrentUser UserInfo userInfo) {
        dto.setBookId(userInfo.getBookId());
        if (StringUtils.isBlank(dto.getBookId())) {
            return Message.failed("所属账套ID不能为空");
        }
        return assetCategoryService.pageList(dto);
    }

    @GetMapping("/list")
    public Message<List<AssetCategoryVo>> list(@CurrentUser UserInfo userInfo) {
        return assetCategoryService.listAll(userInfo.getBookId());
    }

    @GetMapping("/get/{id}")
    public Message<AssetCategoryVo> getById(@PathVariable("id") String id) {
        return assetCategoryService.getById(id);
    }

    @PostMapping("/save")
    public Message<String> save(@Validated(AddGroup.class) @RequestBody AssetCategoryChangeDto dto,
                                @CurrentUser UserInfo userInfo) {
        ProductRoles.requireWriteBusiness();
        dto.setBookId(userInfo.getBookId());
        return assetCategoryService.save(dto);
    }

    @PutMapping("/update")
    public Message<String> update(@Validated(EditGroup.class) @RequestBody AssetCategoryChangeDto dto,
                                  @CurrentUser UserInfo userInfo) {
        ProductRoles.requireWriteBusiness();
        dto.setBookId(userInfo.getBookId());
        return assetCategoryService.update(dto);
    }

    @DeleteMapping("/delete")
    public Message<String> delete(@RequestBody ListIdsDto dto) {
        ProductRoles.requireWriteBusiness();
        return assetCategoryService.delete(dto);
    }
}
