package com.financial.cloud.controller.book;

import com.financial.cloud.authn.annotation.CurrentUser;
import com.financial.cloud.common.Message;
import com.financial.cloud.domain.idm.UserInfo;
import com.financial.cloud.exception.BusinessException;
import com.financial.cloud.service.book.backup.BookBackupService;
import com.financial.cloud.service.book.backup.BookRestoreService;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.io.InputStream;

/**
 * 账套备份与恢复。对应 openspec/changes/book-backup-restore。
 */
@Slf4j
@RestController
@RequestMapping("/api/book/backup")
@RequiredArgsConstructor
public class BookBackupController {

    private final BookBackupService bookBackupService;
    private final BookRestoreService bookRestoreService;

    /** 导出当前账套业务备份包（ZIP）。 */
    @PostMapping("/export")
    public void export(@RequestParam String bookId,
                       @CurrentUser UserInfo userInfo,
                       HttpServletResponse response) throws IOException {
        BookBackupService.BackupPackage pack = bookBackupService.export(bookId, userInfo);
        response.setContentType(BookBackupService.CONTENT_TYPE);
        response.setContentLength(pack.content().length);
        response.setHeader("Content-Disposition",
                "attachment; filename*=UTF-8''" + pack.encodedFileName());
        response.getOutputStream().write(pack.content());
        response.getOutputStream().flush();
    }

    /** 上传备份包，克隆式恢复为新账套。 */
    @PostMapping("/restore")
    public Message<BookRestoreService.RestoreResult> restore(@RequestParam("file") MultipartFile file,
                                                             @CurrentUser UserInfo userInfo) {
        if (file == null || file.isEmpty()) {
            throw new BusinessException(400, "请选择要恢复的备份 ZIP 文件");
        }
        try (InputStream in = file.getInputStream()) {
            BookRestoreService.RestoreResult result = bookRestoreService.restore(in, userInfo);
            return new Message<>(Message.SUCCESS, "恢复成功", result);
        } catch (IOException e) {
            throw new BusinessException(400, "备份包读取失败：" + e.getMessage());
        }
    }
}
