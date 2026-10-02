package com.financial.cloud.service.book.backup;

import org.junit.jupiter.api.Test;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.nio.charset.StandardCharsets;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class BackupArchiveTest {
    @Test
    void unsafePathIsRejected() throws Exception {
        assertThatThrownBy(() -> BackupArchive.read(new ByteArrayInputStream(zip("../manifest.json", new byte[]{1}))))
                .hasMessageContaining("无效 ZIP 路径");
    }

    @Test
    void duplicateEntriesAreRejected() throws Exception {
        var out = new ByteArrayOutputStream();
        try (var zip = new ZipOutputStream(out)) {
            zip.putNextEntry(new ZipEntry("files/0.bin")); zip.write(1); zip.closeEntry();
            zip.putNextEntry(new ZipEntry("files/1.bin")); zip.write(2); zip.closeEntry();
        }
        // Rename the second entry in local and central headers; valid duplicate-name archive.
        byte[] bytes = out.toByteArray();
        byte[] name = "files/1.bin".getBytes(StandardCharsets.UTF_8);
        for (int i = 0; i <= bytes.length - name.length; i++) {
            boolean match = true;
            for (int j = 0; j < name.length; j++) if (bytes[i+j] != name[j]) match = false;
            if (match) bytes[i+6] = '0';
        }
        assertThatThrownBy(() -> BackupArchive.read(new ByteArrayInputStream(bytes))).hasMessageContaining("重复 ZIP 项");
    }

    @Test
    void compressedExpansionLimitIsEnforcedDuringRead() throws Exception {
        byte[] compressed = zip("files/0.bin", new byte[BackupArchive.MAX_ENTRY_BYTES + 1]);
        assertThatThrownBy(() -> BackupArchive.read(new ByteArrayInputStream(compressed))).hasMessageContaining("超过安全限制");
    }

    @Test
    void exportAlsoRejectsOversizedEntries() {
        assertThatThrownBy(() -> new BackupArchive().write(new ZipOutputStream(new ByteArrayOutputStream()),
                "files/0.bin", new byte[BackupArchive.MAX_ENTRY_BYTES + 1])).hasMessageContaining("超过安全限制");
    }

    @Test
    void totalSizeLimitAppliesAcrossEntries() throws Exception {
        var archive = new BackupArchive();
        var zip = new ZipOutputStream(new ByteArrayOutputStream());
        byte[] bytes = new byte[BackupArchive.MAX_ENTRY_BYTES];
        for (int i = 0; i < 4; i++) archive.write(zip, "files/" + i + ".bin", bytes);
        assertThatThrownBy(() -> archive.write(zip, "files/4.bin", new byte[]{1})).hasMessageContaining("超过安全限制");
        zip.close();
    }

    @Test
    void entryCountLimitAppliesEvenToEmptyEntries() throws Exception {
        var archive = new BackupArchive();
        var zip = new ZipOutputStream(new ByteArrayOutputStream());
        for (int i = 0; i < BackupArchive.MAX_ENTRIES; i++) archive.write(zip, "files/" + i + ".bin", new byte[0]);
        assertThatThrownBy(() -> archive.write(zip, "files/10000.bin", new byte[0])).hasMessageContaining("超过安全限制");
        zip.close();
    }

    private byte[] zip(String name, byte[] bytes) throws Exception {
        var out = new ByteArrayOutputStream();
        try (var zip = new ZipOutputStream(out)) {
            zip.putNextEntry(new ZipEntry(name)); zip.write(bytes); zip.closeEntry();
        }
        return out.toByteArray();
    }
}
