package com.financial.cloud.service.book.backup;

import com.financial.cloud.exception.BusinessException;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;
import java.util.zip.ZipOutputStream;

/** Shared limits keep every exported archive acceptable to the restore reader. */
final class BackupArchive {
    static final int MAX_ENTRY_BYTES = 32 * 1024 * 1024;
    static final long MAX_TOTAL_BYTES = 128L * 1024 * 1024;
    static final int MAX_ENTRIES = 10000;

    private long total;
    private int count;

    void write(ZipOutputStream zip, String name, byte[] bytes) throws IOException {
        validateName(name);
        checkLimits(bytes.length, total + bytes.length, ++count);
        total += bytes.length;
        zip.putNextEntry(new ZipEntry(name));
        zip.write(bytes);
        zip.closeEntry();
    }

    static Map<String, byte[]> read(InputStream stream) {
        Map<String, byte[]> entries = new LinkedHashMap<>();
        long total = 0;
        try (ZipInputStream zip = new ZipInputStream(stream)) {
            ZipEntry entry;
            byte[] buffer = new byte[8192];
            while ((entry = zip.getNextEntry()) != null) {
                String name = entry.getName();
                validateName(name);
                if (entry.isDirectory() || entries.containsKey(name)) {
                    throw new BusinessException(400, "备份包包含目录或重复 ZIP 项：" + name);
                }
                ByteArrayOutputStream out = new ByteArrayOutputStream();
                int len;
                while ((len = zip.read(buffer)) != -1) {
                    total += len;
                    checkLimits((long) out.size() + len, total, entries.size() + 1);
                    out.write(buffer, 0, len);
                }
                checkLimits(out.size(), total, entries.size() + 1);
                entries.put(name, out.toByteArray());
            }
        } catch (IOException e) {
            throw new BusinessException(400, "备份包读取失败，请确认上传的是 ZIP 文件：" + e.getMessage());
        }
        return entries;
    }

    private static void validateName(String name) {
        if (name == null || !(name.equals("manifest.json")
                || name.matches("data/[a-z][a-z0-9_]*\\.jsonl")
                || name.matches("files/[0-9]+\\.bin"))) {
            throw new BusinessException(400, "备份包包含无效 ZIP 路径：" + name);
        }
    }

    private static void checkLimits(long entry, long total, int count) {
        if (entry > MAX_ENTRY_BYTES || total > MAX_TOTAL_BYTES || count > MAX_ENTRIES) {
            throw new BusinessException(400, "备份包超过安全限制（单项32MiB、总计128MiB、10000项）");
        }
    }
}
