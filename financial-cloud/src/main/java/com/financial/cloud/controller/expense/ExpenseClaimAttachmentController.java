package com.financial.cloud.controller.expense;

import com.financial.cloud.authn.annotation.CurrentUser;
import com.financial.cloud.common.Message;
import com.financial.cloud.domain.auth.FileStorage;
import com.financial.cloud.domain.expense.ExpenseClaimAttachment;
import com.financial.cloud.domain.idm.UserInfo;
import com.financial.cloud.service.expense.ExpenseClaimAttachmentService;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.List;

/**
 * 报销单票据附件：上传（暂存/已拒绝可传）、列表、下载、删除。
 */
@RestController
@RequestMapping("/api/expense/claim/attachment")
@RequiredArgsConstructor
public class ExpenseClaimAttachmentController {

    private final ExpenseClaimAttachmentService attachmentService;

    @GetMapping("/list")
    public Message<List<ExpenseClaimAttachment>> list(@RequestParam String claimId,
                                                      @CurrentUser UserInfo userInfo) {
        return Message.ok(attachmentService.listByClaim(claimId, userInfo.getBookId()));
    }

    @PostMapping("/upload")
    public Message<ExpenseClaimAttachment> upload(@RequestParam("file") MultipartFile file,
                                                  @RequestParam String claimId,
                                                  @CurrentUser UserInfo userInfo) {
        return Message.ok(attachmentService.upload(file, claimId, userInfo));
    }

    @GetMapping("/download/{id}")
    public void download(@PathVariable String id,
                         @CurrentUser UserInfo userInfo,
                         HttpServletResponse response) throws IOException {
        FileStorage storage = attachmentService.download(id, userInfo.getBookId());
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
        attachmentService.deleteAttachment(id, userInfo.getBookId());
        return Message.ok(id);
    }
}
