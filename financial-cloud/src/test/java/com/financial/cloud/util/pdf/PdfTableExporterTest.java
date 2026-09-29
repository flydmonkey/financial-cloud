package com.financial.cloud.util.pdf;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Assumptions;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class PdfTableExporterTest {

    @Test
    void formatAmount_formatsNullAndScale() {
        assertEquals("", PdfTableExporter.formatAmount(null));
        assertEquals("1,234.50", PdfTableExporter.formatAmount(new BigDecimal("1234.5")));
    }

    @Test
    void write_producesPdfBytes_whenCjkFontAvailable() throws Exception {
        try {
            PdfTableExporter.resolveChineseBaseFont();
        } catch (IOException e) {
            Assumptions.assumeTrue(false, "CJK font not available on this host: " + e.getMessage());
        }
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        List<String[]> rows = new ArrayList<>();
        rows.add(new String[]{"货币资金", "1", "1,000.00"});
        PdfTableExporter.write(new PdfTableExporter.PdfTableRequest(
                "资产负债表",
                "编制单位：测试公司　期间：2026-09",
                new String[]{"项目", "行次", "期末余额"},
                rows,
                false,
                "资产负债表2026-09.pdf"
        ), out);
        byte[] bytes = out.toByteArray();
        assertTrue(bytes.length > 100, "PDF should be non-empty");
        assertEquals('%', (char) bytes[0]);
        assertEquals('P', (char) bytes[1]);
        assertEquals('D', (char) bytes[2]);
        assertEquals('F', (char) bytes[3]);
    }
}
