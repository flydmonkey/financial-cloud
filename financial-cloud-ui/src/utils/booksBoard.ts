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

const CLOSE_PRIORITY: Record<string, number> = {
  BEHIND: 0,
  OPEN: 1,
  UNKNOWN: 2,
  CLOSED: 3,
};

/** 紧急度：落后 > 未结 > 未知 > 已结，同档按账套名。 */
export function sortBooksBoardRows<T extends { closeStatus?: string; bookName?: string }>(
  rows: T[] | null | undefined,
): T[] {
  return [...(rows || [])].sort((a, b) => {
    const pa = CLOSE_PRIORITY[a.closeStatus || ""] ?? 9;
    const pb = CLOSE_PRIORITY[b.closeStatus || ""] ?? 9;
    if (pa !== pb) return pa - pb;
    return String(a.bookName || "").localeCompare(String(b.bookName || ""), "zh");
  });
}

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

export const BOOKS_BOARD_FOCUS_KEY = "fc.booksBoard.focusPeriod";

export function readStoredFocusPeriod(fallback: string): string {
  try {
    const stored = sessionStorage.getItem(BOOKS_BOARD_FOCUS_KEY);
    if (stored && /^\d{4}-(0[1-9]|1[0-2])$/.test(stored)) {
      return stored;
    }
  } catch {
    // ignore
  }
  return fallback;
}

export function writeStoredFocusPeriod(period: string): void {
  try {
    if (/^\d{4}-(0[1-9]|1[0-2])$/.test(period)) {
      sessionStorage.setItem(BOOKS_BOARD_FOCUS_KEY, period);
    }
  } catch {
    // ignore
  }
}
