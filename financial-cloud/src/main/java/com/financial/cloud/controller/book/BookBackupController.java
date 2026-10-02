package com.financial.cloud.controller.book;

import com.financial.cloud.authn.annotation.CurrentUser;
import com.financial.cloud.common.Message;
import com.financial.cloud.constants.auth.ProductRoles;
import com.financial.cloud.domain.idm.UserInfo;
import com.financial.cloud.exception.BusinessException;
import com.financial.cloud.service.book.backup.BookBackupService;
import com.financial.cloud.service.book.backup.BookRestoreService;
import com.financial.cloud.service.book.backup.ScheduledBookBackupService;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.io.InputStream;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * 账套备份与恢复（克隆 / 覆盖）及定时备份管理。
 */
@Slf4j
@RestController
@RequestMapping("/api/book/backup")
@RequiredArgsConstructor
public class BookBackupController {

    private final BookBackupService bookBackupService;
    private final BookRestoreService bookRestoreService;
    private final ScheduledBookBackupService scheduledBookBackupService;

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

    /**
     * 覆盖式恢复到指定账套。confirmPhrase 必须为「覆盖恢复」。
     */
    @PostMapping("/restore-overwrite")
    public Message<BookRestoreService.OverwriteResult> restoreOverwrite(
            @RequestParam("file") MultipartFile file,
            @RequestParam String bookId,
            @RequestParam String confirmPhrase,
            @CurrentUser UserInfo userInfo) {
        if (file == null || file.isEmpty()) {
            throw new BusinessException(400, "请选择要恢复的备份 ZIP 文件");
        }
        try (InputStream in = file.getInputStream()) {
            BookRestoreService.OverwriteResult result =
                    bookRestoreService.overwrite(bookId, in, confirmPhrase, userInfo);
            return new Message<>(Message.SUCCESS, "覆盖恢复成功", result);
        } catch (IOException e) {
            throw new BusinessException(400, "备份包读取失败：" + e.getMessage());
        }
    }

    /** 定时备份配置与最近一次运行摘要。 */
    @GetMapping("/schedule/status")
    public Message<Map<String, Object>> scheduleStatus(@CurrentUser UserInfo userInfo) {
        ProductRoles.requireAdministrator();
        ScheduledBookBackupService.ScheduleStatus st = scheduledBookBackupService.statusForOperator(userInfo);
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("enabled", st.enabled());
        body.put("cron", st.cron());
        body.put("directory", st.directory());
        body.put("retainCount", st.retainCount());
        body.put("running", st.running());
        ScheduledBookBackupService.LastRun last = st.lastRun();
        if (last != null) {
            Map<String, Object> lastMap = new LinkedHashMap<>();
            lastMap.put("finishedAt", last.finishedAt() == null ? null : last.finishedAt().toString());
            lastMap.put("success", last.success());
            lastMap.put("booksAttempted", last.booksAttempted());
            lastMap.put("booksSucceeded", last.booksSucceeded());
            lastMap.put("booksFailed", last.booksFailed());
            lastMap.put("message", last.message());
            body.put("lastRun", lastMap);
        } else {
            body.put("lastRun", null);
        }
        return new Message<>(Message.SUCCESS, body);
    }

    /** 立即跑一轮启用账套定时备份。 */
    @PostMapping("/schedule/run")
    public Message<Map<String, Object>> scheduleRun(@CurrentUser UserInfo userInfo) {
        ProductRoles.requireAdministrator();
        boolean started = scheduledBookBackupService.runCycleForOperator(userInfo);
        if (!started) {
            throw new BusinessException(409, "定时备份正在执行中，请稍后再试");
        }
        ScheduledBookBackupService.ScheduleStatus st = scheduledBookBackupService.status();
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("started", true);
        body.put("lastRun", st.lastRun());
        return new Message<>(Message.SUCCESS, "定时备份已执行", body);
    }
}
