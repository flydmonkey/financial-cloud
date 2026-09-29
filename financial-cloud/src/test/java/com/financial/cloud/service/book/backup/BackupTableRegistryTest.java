package com.financial.cloud.service.book.backup;

import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * schema 守卫：init SQL 中所有含 book_id 的账套级表，必须在备份清单或显式排除清单中。
 * 新增账套级表忘了登记时，本测试直接失败。
 */
class BackupTableRegistryTest {

    private static final Pattern TABLE_SPLIT =
            Pattern.compile("CREATE TABLE (?:IF NOT EXISTS )?[`\"]?(\\w+)[`\"]?", Pattern.CASE_INSENSITIVE);

    @Test
    void everyBookScopedTableIsRegisteredOrExcluded() throws IOException {
        Path ddl = Path.of("..", "sql", "financial_cloud_init.sql");
        assertThat(ddl).exists();
        String sql = Files.readString(ddl);

        Matcher matcher = TABLE_SPLIT.matcher(sql);
        Set<String> bookScoped = new HashSet<>();
        List<Integer> starts = new ArrayList<>();
        List<String> names = new ArrayList<>();
        while (matcher.find()) {
            names.add(matcher.group(1));
            starts.add(matcher.end());
        }
        for (int i = 0; i < names.size(); i++) {
            int end = i + 1 < starts.size() ? starts.get(i + 1) : sql.length();
            String body = sql.substring(starts.get(i), Math.min(end, starts.get(i) + 6000));
            if (body.contains("book_id")) {
                bookScoped.add(names.get(i));
            }
        }

        Set<String> included = BackupTableRegistry.SPECS.stream()
                .map(BackupTableSpec::table).collect(Collectors.toSet());
        Set<String> covered = new HashSet<>(included);
        covered.addAll(BackupTableRegistry.EXCLUDED_BOOK_SCOPED_TABLES);
        covered.add(BackupTableRegistry.BOOK_TABLE);

        Set<String> missing = new HashSet<>(bookScoped);
        missing.removeAll(covered);
        assertThat(missing)
                .as("含 book_id 的表必须在备份清单或排除清单中登记：%s", missing)
                .isEmpty();
    }

    @Test
    void noOverlapBetweenIncludedAndExcluded() {
        Set<String> included = BackupTableRegistry.SPECS.stream()
                .map(BackupTableSpec::table).collect(Collectors.toSet());
        Set<String> overlap = new HashSet<>(included);
        overlap.retainAll(BackupTableRegistry.EXCLUDED_BOOK_SCOPED_TABLES);
        assertThat(overlap).isEmpty();
        assertThat(included).hasSize(BackupTableRegistry.SPECS.size());
    }

    private static final Pattern CREATE_BLOCK =
            Pattern.compile("CREATE TABLE (?:IF NOT EXISTS )?[`\"](\\w+)[`\"]\\s*\\((.*?)\\)\\s*ENGINE=",
                    Pattern.CASE_INSENSITIVE | Pattern.DOTALL);

    @Test
    void hasDeletedFlagMatchesDdl() throws IOException {
        Path ddl = Path.of("..", "sql", "financial_cloud_init.sql");
        assertThat(ddl).exists();
        String sql = Files.readString(ddl);

        // 表名 → 建表列定义块（精确到右括号，不含后续补丁/种子文本）
        java.util.Map<String, String> bodies = new java.util.HashMap<>();
        Matcher matcher = CREATE_BLOCK.matcher(sql);
        while (matcher.find()) {
            bodies.put(matcher.group(1), matcher.group(2));
        }
        List<String> mismatches = new ArrayList<>();
        for (BackupTableSpec spec : BackupTableRegistry.SPECS) {
            String body = bodies.get(spec.table());
            assertThat(body).as("备份表 %s 在 init SQL 中不存在", spec.table()).isNotNull();
            boolean ddlHasDeleted = body.contains("`deleted`");
            if (ddlHasDeleted != spec.hasDeleted()) {
                mismatches.add(spec.table() + "(DDL deleted=" + ddlHasDeleted + ", spec=" + spec.hasDeleted() + ")");
            }
        }
        assertThat(mismatches)
                .as("hasDeleted 声明与 DDL 不符（不符会导致导出 SQL 报 Unknown column 'deleted'）：%s", mismatches)
                .isEmpty();
    }

    @Test
    void everyFkEdgeReferencesAnEarlierRegisteredTable() {
        Set<String> seen = new HashSet<>();
        for (BackupTableSpec spec : BackupTableRegistry.SPECS) {
            for (BackupTableSpec.FkEdge edge : spec.fkEdges()) {
                assertThat(seen.contains(edge.refTable()) || edge.refTable().equals(spec.table()))
                        .as("%s.%s 引用的 %s 必须先注册（自引用除外）",
                                spec.table(), edge.column(), edge.refTable())
                        .isTrue();
            }
            seen.add(spec.table());
        }
    }
}
