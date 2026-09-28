/**
 * 通用报表打印：新窗口渲染简洁表格 HTML 并自动唤起打印（可另存为 PDF）。
 */
export type TablePrintOptions = {
  /** 报表标题，如「资产负债表」 */
  title: string
  /** 副标题行，如核算单位与期间 */
  subtitle?: string
  /** 完整 <thead>/<tbody> HTML */
  tableHtml: string
}

function escapeHtml(value: unknown): string {
  return String(value ?? '')
    .replace(/&/g, '&amp;')
    .replace(/</g, '&lt;')
    .replace(/>/g, '&gt;')
}

export function openTablePrintWindow(options: TablePrintOptions): void {
  const win = window.open('', '_blank')
  if (!win) {
    return
  }
  const html = `<!DOCTYPE html>
<html lang="zh-CN">
<head>
<meta charset="utf-8"/>
<title>${escapeHtml(options.title)}</title>
<style>
  body { font-family: "Songti SC", "SimSun", serif; color: #222; margin: 24px; }
  h1 { text-align: center; font-size: 18px; margin: 0 0 4px; }
  .meta { text-align: center; color: #555; font-size: 12px; margin-bottom: 12px; }
  table { width: 100%; border-collapse: collapse; font-size: 12px; }
  th, td { border: 1px solid #333; padding: 4px 6px; line-height: 1.5; }
  th { background: #f0f0f0; }
  td.r { text-align: right; }
  td.c { text-align: center; }
  .toolbar { text-align: right; margin-bottom: 8px; font-family: system-ui, sans-serif; }
  .toolbar button { padding: 4px 16px; }
  @media print {
    body { margin: 0; }
    .toolbar { display: none; }
    th { background: #f0f0f0 !important; -webkit-print-color-adjust: exact; print-color-adjust: exact; }
  }
</style>
</head>
<body>
<div class="toolbar"><button type="button" onclick="window.print()">打印</button></div>
<h1>${escapeHtml(options.title)}</h1>
<div class="meta">${escapeHtml(options.subtitle || '')}</div>
<table>${options.tableHtml}</table>
<script>setTimeout(function () { window.print() }, 200)<\/script>
</body>
</html>`
  win.document.write(html)
  win.document.close()
}
