package com.financial.cloud.service.book.backup;

import tools.jackson.core.JacksonException;
import tools.jackson.databind.DeserializationFeature;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.json.JsonMapper;
import tools.jackson.databind.node.ObjectNode;

import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.Base64;
import java.util.Date;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * 备份包 JSONL 编解码：JdbcTemplate 行 Map ↔ 每行一个 JSON 对象。
 * 日期时间统一 ISO 字符串，二进制转 Base64 字符串，null 显式保留。
 */
final class BackupJsonCodec {

    private static final java.time.format.DateTimeFormatter DATE_TIME =
            java.time.format.DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    private static final JsonMapper MAPPER = JsonMapper.builder()
            // 金额列往返必须保持 BigDecimal 精度，不能退化为 double
            .configure(DeserializationFeature.USE_BIG_DECIMAL_FOR_FLOATS, true)
            .build();

    private BackupJsonCodec() {
    }

    /** 行 → JSONL 字节（含换行）。 */
    static byte[] encodeRow(Map<String, Object> row) {
        ObjectNode node = MAPPER.createObjectNode();
        row.forEach((key, value) -> putValue(node, key.toLowerCase(), value));
        return (node + "\n").getBytes(StandardCharsets.UTF_8);
    }

    private static void putValue(ObjectNode node, String key, Object value) {
        if (value == null) {
            node.putNull(key);
        } else if (value instanceof BigDecimal bd) {
            node.put(key, bd);
        } else if (value instanceof Number n) {
            node.putPOJO(key, n);
        } else if (value instanceof Boolean b) {
            node.put(key, b);
        } else if (value instanceof byte[] bytes) {
            node.put(key, Base64.getEncoder().encodeToString(bytes));
        } else if (value instanceof Timestamp ts) {
            node.put(key, ts.toLocalDateTime().format(DATE_TIME));
        } else if (value instanceof java.sql.Date d) {
            node.put(key, d.toLocalDate().toString());
        } else if (value instanceof Date d) {
            node.put(key, new Timestamp(d.getTime()).toLocalDateTime().format(DATE_TIME));
        } else {
            node.put(key, value.toString());
        }
    }

    /** JSONL 字节 → 行列表（键统一小写，与 MySQL 列名大小写不敏感特性一致）。 */
    static List<Map<String, Object>> decodeRows(byte[] jsonl) {
        String text = new String(jsonl, StandardCharsets.UTF_8);
        List<Map<String, Object>> rows = new ArrayList<>();
        for (String line : text.split("\n")) {
            if (line.isBlank()) {
                continue;
            }
            try {
                JsonNode json = MAPPER.readTree(line);
                Map<String, Object> row = new LinkedHashMap<>();
                json.properties().forEach(entry ->
                        row.put(entry.getKey().toLowerCase(), fromJson(entry.getValue())));
                rows.add(row);
            } catch (JacksonException e) {
                throw new IllegalArgumentException("备份数据行解析失败：" + e.getOriginalMessage(), e);
            }
        }
        return rows;
    }

    private static Object fromJson(JsonNode node) {
        if (node == null || node.isNull()) {
            return null;
        }
        if (node.isNumber()) {
            return node.numberValue();
        }
        if (node.isBoolean()) {
            return node.booleanValue();
        }
        return node.asText();
    }
}
