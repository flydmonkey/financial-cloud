package com.financial.cloud.util.excel;

import jakarta.servlet.http.HttpServletResponse;
import lombok.*;

@Setter
@Getter
@Builder
@AllArgsConstructor
@NoArgsConstructor
@ToString
@EqualsAndHashCode
public class ExcelParams<T> {

    /**
     * HTTP 响应对象，用于将生成的 Excel 推送至客户端浏览器
     */
    private HttpServletResponse httpResponse;

    /**
     * Excel 模板文件绝对路径或类路径（如 classpath:templates/report.xlsx）
     */
    private String templateFilePath;

    /**
     * 渲染模板所需的数据模型，支持单对象或列表
     */
    private T dataModel;

    /**
     * 渲染模式
     * 当且仅当dataModel类型为Object时有效
     */
    private ExcelDataModeEnum mode;

    /**
     * 导出文件名，建议包含扩展名，如 report.xlsx
     */
    private String outputFileName;

    /**
     * 本地文件保存目录，留空则不在服务器保存，仅通过 HTTP 响应输出
     */
    private String outputDirectory;

    /**
     * 目标工作表名称；若模板中不存在该名称则新建，否则使用已有
     */
    private String sheetName;

    /**
     * 是否启用相同值单元格合并（仅对列表渲染生效）
     */
    private boolean enableMergeCells;

    /**
     * 是否自动根据内容调整所有列宽
     */
    private boolean autoSizeColumns;

    /**
     * 是否在写入后强制重新计算公式
     */
    private boolean recalculateFormulas;

}
