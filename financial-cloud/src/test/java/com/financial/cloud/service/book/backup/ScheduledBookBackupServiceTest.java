package com.financial.cloud.service.book.backup;

import com.financial.cloud.configuration.BookBackupScheduleProperties;
import com.financial.cloud.repository.book.BookMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.api.io.TempDir;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ScheduledBookBackupServiceTest {

    @Mock private BookBackupService bookBackupService;
    @Mock private BookMapper bookMapper;

    @TempDir Path tempDir;

    private BookBackupScheduleProperties properties;
    private ScheduledBookBackupService service;

    @BeforeEach
    void setUp() {
        properties = new BookBackupScheduleProperties();
        properties.setEnabled(true);
        properties.setDirectory(tempDir.toString());
        properties.setRetainCount(2);
        service = new ScheduledBookBackupService(properties, bookBackupService, bookMapper);
    }

    @Test
    void prune_keepsNewestRetainCount() throws Exception {
        String bookId = "book-a";
        Path older = tempDir.resolve("scheduled-demo-" + bookId + "-20260101-010101.zip");
        Path mid = tempDir.resolve("scheduled-demo-" + bookId + "-20260102-010101.zip");
        Path newest = tempDir.resolve("scheduled-demo-" + bookId + "-20260103-010101.zip");
        Files.write(older, new byte[]{1});
        Thread.sleep(5);
        Files.write(mid, new byte[]{2});
        Thread.sleep(5);
        Files.write(newest, new byte[]{3});
        // other book should not be pruned
        Path other = tempDir.resolve("scheduled-other-book-b-20260101-010101.zip");
        Files.write(other, new byte[]{9});

        service.prune(tempDir, bookId, 2);

        assertThat(Files.exists(older)).isFalse();
        assertThat(Files.exists(mid)).isTrue();
        assertThat(Files.exists(newest)).isTrue();
        assertThat(Files.exists(other)).isTrue();
    }

    @Test
    void runCycle_writesZipAndUpdatesLastRun() throws Exception {
        com.financial.cloud.domain.book.Book book = new com.financial.cloud.domain.book.Book();
        book.setId("book-1");
        book.setName("测试账套");
        when(bookMapper.selectList(org.mockito.ArgumentMatchers.any())).thenReturn(List.of(book));
        when(bookBackupService.exportForSystem("book-1"))
                .thenReturn(new BookBackupService.BackupPackage(new byte[]{'P', 'K', 3, 4}, "x.zip"));

        assertThat(service.runCycle("test")).isTrue();
        assertThat(service.status().lastRun()).isNotNull();
        assertThat(service.status().lastRun().booksSucceeded()).isEqualTo(1);
        assertThat(Files.list(tempDir).filter(p -> p.getFileName().toString().startsWith("scheduled-")).count())
                .isEqualTo(1);
    }
}
