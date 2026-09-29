package com.financial.cloud.util.pdf;

import com.lowagie.text.Document;
import com.lowagie.text.DocumentException;
import com.lowagie.text.Element;
import com.lowagie.text.Font;
import com.lowagie.text.PageSize;
import com.lowagie.text.Paragraph;
import com.lowagie.text.Phrase;
import com.lowagie.text.pdf.BaseFont;
import com.lowagie.text.pdf.PdfPCell;
import com.lowagie.text.pdf.PdfPTable;
import com.lowagie.text.pdf.PdfWriter;
import jakarta.servlet.http.HttpServletResponse;
import org.apache.commons.lang3.StringUtils;

import java.awt.Color;
import java.io.IOException;
import java.io.OutputStream;
import java.math.BigDecimal;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.text.DecimalFormat;
import java.util.List;

/**
 * 表格型财务报表 PDF 写出器（OpenPDF）。
 */
public final class PdfTableExporter {

    public static final String APPLICATION_PDF = "application/pdf; charset=UTF-8";

    private static final DecimalFormat AMOUNT_FMT = new DecimalFormat("#,##0.00");

    private PdfTableExporter() {
    }

    public record PdfTableRequest(
            String title,
            String subtitle,
            String[] headers,
            List<String[]> rows,
            boolean landscape,
            String fileName
    ) {
    }

    public static void write(PdfTableRequest request, HttpServletResponse response) throws IOException {
        response.setContentType(APPLICATION_PDF);
        response.setHeader("Content-Disposition", "attachment; filename="
                + URLEncoder.encode(request.fileName(), StandardCharsets.UTF_8));
        write(request, response.getOutputStream());
        response.getOutputStream().flush();
    }

    public static void write(PdfTableRequest request, OutputStream out) throws IOException {
        BaseFont baseFont = resolveChineseBaseFont();
        Font titleFont = new Font(baseFont, 16, Font.BOLD);
        Font subFont = new Font(baseFont, 10, Font.NORMAL);
        Font headerFont = new Font(baseFont, 9, Font.BOLD);
        Font cellFont = new Font(baseFont, 8, Font.NORMAL);

        Document document = new Document(request.landscape() ? PageSize.A4.rotate() : PageSize.A4, 36, 36, 36, 36);
        try {
            PdfWriter.getInstance(document, out);
            document.open();
            Paragraph title = new Paragraph(StringUtils.defaultString(request.title()), titleFont);
            title.setAlignment(Element.ALIGN_CENTER);
            title.setSpacingAfter(8f);
            document.add(title);
            if (StringUtils.isNotBlank(request.subtitle())) {
                Paragraph sub = new Paragraph(request.subtitle(), subFont);
                sub.setAlignment(Element.ALIGN_CENTER);
                sub.setSpacingAfter(12f);
                document.add(sub);
            }

            String[] headers = request.headers();
            PdfPTable table = new PdfPTable(headers.length);
            table.setWidthPercentage(100f);
            table.setHeaderRows(1);
            for (String h : headers) {
                PdfPCell cell = new PdfPCell(new Phrase(StringUtils.defaultString(h), headerFont));
                cell.setHorizontalAlignment(Element.ALIGN_CENTER);
                cell.setBackgroundColor(new Color(240, 240, 240));
                cell.setPadding(4f);
                table.addCell(cell);
            }
            if (request.rows() != null) {
                for (String[] row : request.rows()) {
                    for (int i = 0; i < headers.length; i++) {
                        String text = row != null && i < row.length ? StringUtils.defaultString(row[i]) : "";
                        PdfPCell cell = new PdfPCell(new Phrase(text, cellFont));
                        cell.setPadding(3f);
                        boolean amountLike = i > 0 && looksLikeAmount(text);
                        cell.setHorizontalAlignment(amountLike ? Element.ALIGN_RIGHT : Element.ALIGN_LEFT);
                        table.addCell(cell);
                    }
                }
            }
            document.add(table);
        } catch (DocumentException e) {
            throw new IOException("生成 PDF 失败: " + e.getMessage(), e);
        } finally {
            document.close();
        }
    }

    public static String formatAmount(BigDecimal value) {
        if (value == null) {
            return "";
        }
        return AMOUNT_FMT.format(value);
    }

    public static String nz(Object value) {
        return value == null ? "" : String.valueOf(value);
    }

    static BaseFont resolveChineseBaseFont() throws IOException {
        String env = System.getenv("FINANCIAL_CLOUD_PDF_FONT");
        if (StringUtils.isNotBlank(env)) {
            return loadFont(env.trim());
        }
        String[] candidates = {
                "C:\\Windows\\Fonts\\msyh.ttc",
                "C:\\Windows\\Fonts\\msyh.ttf",
                "C:\\Windows\\Fonts\\simhei.ttf",
                "C:\\Windows\\Fonts\\simsun.ttc",
                "C:\\Windows\\Fonts\\simsun.ttf",
                "/usr/share/fonts/opentype/noto/NotoSansCJK-Regular.ttc",
                "/usr/share/fonts/truetype/noto/NotoSansCJK-Regular.ttc",
                "/usr/share/fonts/truetype/wqy/wqy-microhei.ttc",
                "/System/Library/Fonts/PingFang.ttc",
        };
        for (String path : candidates) {
            Path p = Paths.get(path.contains(",") ? path.substring(0, path.indexOf(',')) : path);
            if (Files.isRegularFile(p)) {
                try {
                    return loadFont(path.endsWith(".ttc") && !path.contains(",") ? path + ",0" : path);
                } catch (IOException ignored) {
                    // try next
                }
            }
        }
        throw new IOException("未找到可用中文字体。请设置环境变量 FINANCIAL_CLOUD_PDF_FONT 为 TTF/TTC 路径，"
                + "或在主机安装微软雅黑/黑体/Noto Sans CJK。");
    }

    private static BaseFont loadFont(String path) throws IOException {
        try {
            return BaseFont.createFont(path, BaseFont.IDENTITY_H, BaseFont.EMBEDDED);
        } catch (DocumentException e) {
            throw new IOException("加载字体失败: " + path + " — " + e.getMessage(), e);
        }
    }

    private static boolean looksLikeAmount(String text) {
        if (StringUtils.isBlank(text)) {
            return false;
        }
        String t = text.replace(",", "").replace(" ", "");
        return t.matches("-?\\d+(\\.\\d+)?");
    }
}
