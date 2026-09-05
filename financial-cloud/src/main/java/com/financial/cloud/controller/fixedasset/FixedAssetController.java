package com.financial.cloud.controller.fixedasset;

import com.baomidou.mybatisplus.core.toolkit.StringUtils;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.financial.cloud.authn.annotation.CurrentUser;
import com.financial.cloud.common.ExcelImport;
import com.financial.cloud.common.Message;
import com.financial.cloud.constants.auth.ProductRoles;
import com.financial.cloud.domain.idm.UserInfo;
import com.financial.cloud.dto.common.ListIdsDto;
import com.financial.cloud.dto.fixedasset.FixedAssetChangeDto;
import com.financial.cloud.dto.fixedasset.FixedAssetDisposeDto;
import com.financial.cloud.dto.fixedasset.FixedAssetDisposeResultVo;
import com.financial.cloud.dto.fixedasset.FixedAssetImportResultVo;
import com.financial.cloud.dto.fixedasset.FixedAssetPageDto;
import com.financial.cloud.dto.fixedasset.FixedAssetSaveResultVo;
import com.financial.cloud.dto.fixedasset.FixedAssetVo;
import com.financial.cloud.service.fixedasset.FixedAssetService;
import com.financial.cloud.validation.AddGroup;
import com.financial.cloud.validation.EditGroup;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.io.IOException;

@RestController
@RequestMapping("/api/fixed-asset/card")
@RequiredArgsConstructor
public class FixedAssetController {

    private final FixedAssetService fixedAssetService;

    @GetMapping("/fetch")
    public Message<Page<FixedAssetVo>> fetch(FixedAssetPageDto dto, @CurrentUser UserInfo userInfo) {
        dto.setBookId(userInfo.getBookId());
        if (StringUtils.isBlank(dto.getBookId())) {
            return Message.failed("所属账套ID不能为空");
        }
        return fixedAssetService.pageList(dto);
    }

    @GetMapping("/export")
    public void export(FixedAssetPageDto dto, @CurrentUser UserInfo userInfo, HttpServletResponse response)
            throws IOException {
        dto.setBookId(userInfo.getBookId());
        fixedAssetService.export(dto, response);
    }

    @GetMapping("/import-template")
    public void importTemplate(HttpServletResponse response) throws IOException {
        fixedAssetService.downloadImportTemplate(response);
    }

    @PostMapping("/import")
    public Message<FixedAssetImportResultVo> importExcel(
            @ModelAttribute("excelImportFile") ExcelImport excelImportFile,
            @CurrentUser UserInfo userInfo) {
        ProductRoles.requireWriteBusiness();
        return fixedAssetService.importFromExcel(userInfo.getBookId(), excelImportFile);
    }

    @GetMapping("/get/{id}")
    public Message<FixedAssetVo> getById(@PathVariable("id") String id) {
        return fixedAssetService.getById(id);
    }

    @PostMapping("/save")
    public Message<FixedAssetSaveResultVo> save(@Validated(AddGroup.class) @RequestBody FixedAssetChangeDto dto,
                                                @CurrentUser UserInfo userInfo) {
        ProductRoles.requireWriteBusiness();
        dto.setBookId(userInfo.getBookId());
        return fixedAssetService.save(dto);
    }

    @PutMapping("/update")
    public Message<String> update(@Validated(EditGroup.class) @RequestBody FixedAssetChangeDto dto,
                                  @CurrentUser UserInfo userInfo) {
        ProductRoles.requireWriteBusiness();
        dto.setBookId(userInfo.getBookId());
        return fixedAssetService.update(dto);
    }

    @PostMapping("/copy/{id}")
    public Message<String> copy(@PathVariable("id") String id, @CurrentUser UserInfo userInfo) {
        ProductRoles.requireWriteBusiness();
        return fixedAssetService.copy(id, userInfo.getBookId());
    }

    @PostMapping("/suspend/{id}")
    public Message<String> suspend(@PathVariable("id") String id, @CurrentUser UserInfo userInfo) {
        ProductRoles.requireWriteBusiness();
        return fixedAssetService.suspend(id, userInfo.getBookId());
    }

    @PostMapping("/resume/{id}")
    public Message<String> resume(@PathVariable("id") String id, @CurrentUser UserInfo userInfo) {
        ProductRoles.requireWriteBusiness();
        return fixedAssetService.resume(id, userInfo.getBookId());
    }

    @DeleteMapping("/delete")
    public Message<String> delete(@RequestBody ListIdsDto dto) {
        ProductRoles.requireWriteBusiness();
        return fixedAssetService.delete(dto);
    }

    @PostMapping("/dispose/{id}")
    public Message<FixedAssetDisposeResultVo> dispose(@PathVariable("id") String id,
                                                      @RequestBody(required = false) FixedAssetDisposeDto dto,
                                                      @CurrentUser UserInfo userInfo) {
        ProductRoles.requireWriteBusiness();
        return fixedAssetService.dispose(id, userInfo.getBookId(), dto);
    }
}
