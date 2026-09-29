package com.financial.cloud.controller.voucher;

import com.financial.cloud.authn.annotation.CurrentUser;
import com.financial.cloud.common.Message;
import com.financial.cloud.constants.auth.ProductRoles;
import com.financial.cloud.domain.auth.FileStorage;
import com.financial.cloud.domain.idm.UserInfo;
import com.financial.cloud.domain.voucher.VoucherAttachment;
import com.financial.cloud.service.voucher.VoucherAttachmentService;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.List;

/**
 * 凭证附件（影像）。对应 docs/product/20-gap-analysis.md §3.2。
 */
@Slf4j
@RestController
@RequestMapping("/api/voucher/attachment")
@RequiredArgsConstructor
public class VoucherAttachmentController {

    private final VoucherAttachmentService voucherAttachmentService;

    @GetMapping("/list")
    public Message<List<VoucherAttachment>> list(@RequestParam String voucherId,
                                                 @CurrentUser UserInfo userInfo) {
        return new Message<>(Message.SUCCESS,
                voucherAttachmentService.listByVoucher(voucherId, userInfo.getBookId()));
    }

    @PostMapping("/upload")
    public Message<VoucherAttachment> upload(@RequestParam("file") MultipartFile file,
                                             @RequestParam String voucherId,
                                             @CurrentUser UserInfo userInfo) {
        ProductRoles.requireWriteVoucher();
        return new Message<>(Message.SUCCESS, "上传成功",
                voucherAttachmentService.upload(file, voucherId, userInfo));
    }

    @GetMapping("/download/{id}")
    public void download(@PathVariable String id,
                         @CurrentUser UserInfo userInfo,
                         HttpServletResponse response) throws IOException {
        FileStorage storage = voucherAttachmentService.download(id, userInfo.getBookId());
        response.setContentType(storage.getContentType() == null
                ? "application/octet-stream" : storage.getContentType());
        response.setContentLength(storage.getDataStored().length);
        response.setHeader("Content-Disposition", "attachment; filename*=UTF-8''"
                + URLEncoder.encode(storage.getFileName() == null ? "attachment" : storage.getFileName(),
                StandardCharsets.UTF_8).replace("+", "%20"));
        response.getOutputStream().write(storage.getDataStored());
        response.getOutputStream().flush();
    }

    @DeleteMapping("/{id}")
    public Message<String> delete(@PathVariable String id,
                                  @CurrentUser UserInfo userInfo) {
        ProductRoles.requireWriteVoucher();
        voucherAttachmentService.deleteAttachment(id, userInfo.getBookId());
        return new Message<>(Message.SUCCESS, "删除成功");
    }
}
