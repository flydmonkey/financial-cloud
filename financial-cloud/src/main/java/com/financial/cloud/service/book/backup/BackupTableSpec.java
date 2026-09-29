package com.financial.cloud.service.book.backup;

import java.util.List;

/**
 * 账套备份表规格（声明式）。新增账套级表时必须在此登记，
 * 由 BackupTableRegistryTest 的 schema 守卫强制约束。
 *
 * @param table         表名
 * @param scope         数据范围过滤方式
 * @param pk            主键列名（多数为 id，config 为 config_id）
 * @param hasDeleted    是否有 deleted 逻辑删除列（导出时过滤 deleted='n'）
 * @param fkEdges       恢复时需重映射的外键边（列 → 引用表）
 * @param nullOnRestore 恢复时置空的列（人员/部门/实例级引用）
 * @param extraCondition 额外导出过滤条件（SQL 片段，如仅备份非模板行）
 */
public record BackupTableSpec(
        String table,
        Scope scope,
        String pk,
        boolean hasDeleted,
        List<FkEdge> fkEdges,
        List<String> nullOnRestore,
        String extraCondition) {

    public enum Scope {
        /** 账套主表自身（单行） */
        BOOK_ROW,
        /** WHERE book_id = ? */
        BOOK_ID,
        /** WHERE voucher_id IN (SELECT id FROM voucher WHERE book_id = ?) */
        VIA_VOUCHER
    }

    /** 外键边。soft=true 时找不到映射则置空（如 statement_subject_balance.source_id 多态引用）。 */
    public record FkEdge(String column, String refTable, boolean soft) {
        public static FkEdge of(String column, String refTable) {
            return new FkEdge(column, refTable, false);
        }

        public static FkEdge soft(String column, String refTable) {
            return new FkEdge(column, refTable, true);
        }
    }

    public static BackupTableSpec of(String table) {
        return new BackupTableSpec(table, Scope.BOOK_ID, "id", true, List.of(), List.of(), null);
    }

    public BackupTableSpec withPk(String pkColumn) {
        return new BackupTableSpec(table, scope, pkColumn, hasDeleted, fkEdges, nullOnRestore, extraCondition);
    }

    public BackupTableSpec noDeleted() {
        return new BackupTableSpec(table, scope, pk, false, fkEdges, nullOnRestore, extraCondition);
    }

    public BackupTableSpec fks(FkEdge... edges) {
        return new BackupTableSpec(table, scope, pk, hasDeleted, List.of(edges), nullOnRestore, extraCondition);
    }

    public BackupTableSpec nulls(String... columns) {
        return new BackupTableSpec(table, scope, pk, hasDeleted, fkEdges, List.of(columns), extraCondition);
    }

    public BackupTableSpec viaVoucher() {
        return new BackupTableSpec(table, Scope.VIA_VOUCHER, pk, hasDeleted, fkEdges, nullOnRestore, extraCondition);
    }

    public BackupTableSpec where(String condition) {
        return new BackupTableSpec(table, scope, pk, hasDeleted, fkEdges, nullOnRestore, condition);
    }
}
