/**
 * 代账工作台：按阻塞摘要决定「进入处理」跳转路径。
 */
export function booksBoardBlockerPath(blocker: string | undefined | null): string {
  switch (blocker) {
    case "AUDIT":
    case "POST":
      return "/voucher/voucher-index";
    case "DEPRECIATION":
      return "/fixed-asset/depreciation";
    case "READY_CLOSE":
    case "BEHIND":
      return "/settlement/settle-period";
    case "READY_PACK":
      return "/settlement/settle-list";
    default:
      return "/index";
  }
}

export type CloseStatusFilter = "ALL" | "OPEN" | "CLOSED" | "BEHIND" | "UNKNOWN";

export function filterBooksBoardRows<T extends { closeStatus?: string }>(
  rows: T[] | null | undefined,
  closeStatusFilter: CloseStatusFilter,
): T[] {
  const list = rows || [];
  if (!closeStatusFilter || closeStatusFilter === "ALL") {
    return list;
  }
  return list.filter((row) => row.closeStatus === closeStatusFilter);
}

export function summarizeBooksBoardRows(
  rows: Array<{ closeStatus?: string; pendingAuditCount?: number; pendingPostCount?: number; depreciationPending?: boolean }> | null | undefined,
): { total: number; open: number; closed: number; behind: number; withTodo: number } {
  const list = rows || [];
  let open = 0;
  let closed = 0;
  let behind = 0;
  let withTodo = 0;
  for (const row of list) {
    if (row.closeStatus === "OPEN") open += 1;
    else if (row.closeStatus === "CLOSED") closed += 1;
    else if (row.closeStatus === "BEHIND") behind += 1;
    const todo =
      (row.pendingAuditCount || 0) > 0 ||
      (row.pendingPostCount || 0) > 0 ||
      !!row.depreciationPending ||
      row.closeStatus === "OPEN" ||
      row.closeStatus === "BEHIND";
    if (todo) withTodo += 1;
  }
  return { total: list.length, open, closed, behind, withTodo };
}
