package com.financial.cloud.controller.fixedasset;

import com.baomidou.mybatisplus.core.toolkit.StringUtils;
import com.financial.cloud.authn.annotation.CurrentUser;
import com.financial.cloud.common.Message;
import com.financial.cloud.domain.idm.UserInfo;
import com.financial.cloud.dto.fixedasset.FixedAssetDepreciationReportQuery;
import com.financial.cloud.dto.fixedasset.FixedAssetDepreciationReportVo;
import com.financial.cloud.service.fixedasset.FixedAssetDepreciationReportService;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.io.IOException;

@RestController
@RequestMapping("/api/fixed-asset/report")
@RequiredArgsConstructor
public class FixedAssetReportController {

    private final FixedAssetDepreciationReportService reportService;

    @GetMapping("/depreciation-detail")
    public Message<FixedAssetDepreciationReportVo> detail(FixedAssetDepreciationReportQuery query,
                                                         @CurrentUser UserInfo userInfo) {
        if (StringUtils.isBlank(userInfo.getBookId())) {
            return Message.failed("所属账套ID不能为空");
        }
        query.setBookId(userInfo.getBookId());
        return reportService.detail(query);
    }

    @GetMapping("/depreciation-summary")
    public Message<FixedAssetDepreciationReportVo> summary(FixedAssetDepreciationReportQuery query,
                                                          @CurrentUser UserInfo userInfo) {
        if (StringUtils.isBlank(userInfo.getBookId())) {
            return Message.failed("所属账套ID不能为空");
        }
        query.setBookId(userInfo.getBookId());
        return reportService.summary(query);
    }

    @GetMapping("/depreciation-detail/export")
    public void exportDetail(FixedAssetDepreciationReportQuery query,
                             @CurrentUser UserInfo userInfo,
                             HttpServletResponse response) throws IOException {
        query.setBookId(userInfo.getBookId());
        reportService.exportDetail(query, response);
    }

    @GetMapping("/depreciation-summary/export")
    public void exportSummary(FixedAssetDepreciationReportQuery query,
                              @CurrentUser UserInfo userInfo,
                              HttpServletResponse response) throws IOException {
        query.setBookId(userInfo.getBookId());
        reportService.exportSummary(query, response);
    }
}
