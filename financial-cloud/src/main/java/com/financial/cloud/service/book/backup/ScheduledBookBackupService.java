package com.financial.cloud.service.book.backup;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.financial.cloud.configuration.BookBackupScheduleProperties;
import com.financial.cloud.domain.book.Book;
import com.financial.cloud.enums.book.BookStatusEnum;
import com.financial.cloud.repository.book.BookMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.nio.file.DirectoryStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;

/**
 * 定时/立即跑一轮：对启用账套落盘备份 ZIP 并按 retainCount 清理。
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class ScheduledBookBackupService {

    private static final DateTimeFormatter FILE_TS = DateTimeFormatter.ofPattern("yyyyMMdd-HHmmss");

    private final BookBackupScheduleProperties properties;
    private final BookBackupService bookBackupService;
    private final BookMapper bookMapper;
    private final com.financial.cloud.service.book.BookService bookService;

    private final AtomicBoolean running = new AtomicBoolean(false);
    private final AtomicReference<LastRun> lastRun = new AtomicReference<>();

    public record LastRun(
            LocalDateTime finishedAt,
            boolean success,
            int booksAttempted,
            int booksSucceeded,
            int booksFailed,
            String message
    ) {
    }

    public record ScheduleStatus(
            boolean enabled,
            String cron,
            String directory,
            int retainCount,
            boolean running,
            LastRun lastRun
    ) {
    }

    public ScheduleStatus status() {
        return new ScheduleStatus(
                properties.isEnabled(),
                properties.getCron(),
                properties.getDirectory(),
                Math.max(1, properties.getRetainCount()),
                running.get(),
                lastRun.get()
        );
    }

    /**
     * @return true if this call acquired the lock and ran; false if skipped (already running)
     */
    public boolean runCycle(String trigger) {
        return runCycle(trigger, activeBooks());
    }

    public ScheduleStatus statusForOperator(com.financial.cloud.domain.idm.UserInfo operator) {
        authorizedBooks(operator);
        return status();
    }

    public boolean runCycleForOperator(com.financial.cloud.domain.idm.UserInfo operator) {
        return runCycle("manual", authorizedBooks(operator));
    }

    private List<Book> activeBooks() {
        return bookMapper.selectList(Wrappers.<Book>lambdaQuery()
                .eq(Book::getStatus, BookStatusEnum.ACTIVE.getValue()));
    }

    private List<Book> authorizedBooks(com.financial.cloud.domain.idm.UserInfo operator) {
        List<Book> books = activeBooks();
        for (Book book : books) bookService.requireBookAdministrator(operator, book.getId());
        return books;
    }

    private boolean runCycle(String trigger, List<Book> books) {
        if (!running.compareAndSet(false, true)) {
            log.info("定时备份跳过：上一轮仍在执行（trigger={}）", trigger);
            return false;
        }
        int attempted = 0;
        int succeeded = 0;
        int failed = 0;
        String summary;
        try {
            Path dir = ensureDirectory();
            attempted = books.size();
            for (Book book : books) {
                try {
                    writeBookBackup(dir, book);
                    prune(dir, book.getId(), Math.max(1, properties.getRetainCount()));
                    succeeded++;
                } catch (Exception e) {
                    failed++;
                    log.error("定时备份失败 bookId={} name={}: {}", book.getId(), book.getName(), e.getMessage(), e);
                }
            }
            summary = "trigger=" + trigger + " attempted=" + attempted
                    + " ok=" + succeeded + " fail=" + failed;
            lastRun.set(new LastRun(LocalDateTime.now(), failed == 0, attempted, succeeded, failed, summary));
            log.info("定时备份完成：{}", summary);
            return true;
        } catch (Exception e) {
            summary = "trigger=" + trigger + " fatal=" + e.getMessage();
            lastRun.set(new LastRun(LocalDateTime.now(), false, attempted, succeeded, failed, summary));
            log.error("定时备份异常终止：{}", e.getMessage(), e);
            return true;
        } finally {
            running.set(false);
        }
    }

    private Path ensureDirectory() throws IOException {
        Path dir = Paths.get(properties.getDirectory()).toAbsolutePath().normalize();
        Files.createDirectories(dir);
        return dir;
    }

    private void writeBookBackup(Path dir, Book book) throws IOException {
        BookBackupService.BackupPackage pack = bookBackupService.exportForSystem(book.getId());
        String fileName = "scheduled-" + BookBackupService.safeBookFileName(book.getName())
                + "-" + book.getId() + "-" + FILE_TS.format(LocalDateTime.now()) + ".zip";
        Path target = dir.resolve(fileName);
        Files.write(target, pack.content());
        log.info("定时备份已写入 {}", target);
    }

    void prune(Path dir, String bookId, int retainCount) throws IOException {
        java.util.regex.Pattern marker = java.util.regex.Pattern.compile(
                ".*-" + java.util.regex.Pattern.quote(bookId) + "-\\d{8}-\\d{6}\\.zip");
        List<Path> matches = new ArrayList<>();
        try (DirectoryStream<Path> stream = Files.newDirectoryStream(dir, "scheduled-*.zip")) {
            for (Path p : stream) {
                String name = p.getFileName().toString();
                if (marker.matcher(name).matches()) {
                    matches.add(p);
                }
            }
        }
        matches.sort(Comparator.comparingLong(p -> {
            try {
                return Files.getLastModifiedTime(p).toMillis();
            } catch (IOException e) {
                return 0L;
            }
        }));
        int excess = matches.size() - retainCount;
        for (int i = 0; i < excess; i++) {
            Files.deleteIfExists(matches.get(i));
        }
    }
}
