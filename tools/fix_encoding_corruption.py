#!/usr/bin/env python3
"""Repair UTF-8 corruption patterns in financial-cloud-ui source files."""
from __future__ import annotations

import re
from pathlib import Path

ROOT = Path(__file__).resolve().parents[1] / "financial-cloud-ui" / "src"

LITERALS: list[tuple[str, str]] = [
    ("增值税及附?/span>", "增值税及附加</span>"),
    ("净利润?/span>", "净利润率</span>"),
    ("短期应收?/span>", "短期应收款</span>"),
    ("短期应付?/span>", "短期应付款</span>"),
    ("公司名称?/span>", "公司名称：</span>"),
    ("凭证编号?/span>", "凭证编号：</span>"),
    ("会计主管?/span>", "会计主管：</span>"),
    ("过账?/span>", "过账人：</span>"),
    ("复核?/span>", "复核人：</span>"),
    ("制单?/span>", "制单人：</span>"),
    ("确认撤回该凭证的审核申请?)", "确认撤回该凭证的审核申请？')"),
    ('proxy.$modal.msgSuccess("已取?)', 'proxy.$modal.msgSuccess("已取消")'),
    ("没有可以提交的凭证项?)", '没有可以提交的凭证项。")'),
    ("没有可以审核的凭证项?)", '没有可以审核的凭证项。")'),
    ("没有可以反审核的凭证项（需为已审核且未过账）?)", '没有可以反审核的凭证项（需为已审核且未过账）。")'),
    ("确认反审?", "确认反审核 "),
    ("没有可以过账的凭证项（需为已审核且未过账）?)", '没有可以过账的凭证项（需为已审核且未过账）。")'),
    ("没有可以反过账的凭证项（需为已过账且所在期间未结账）?)", '没有可以反过账的凭证项（需为已过账且所在期间未结账）。")'),
    ("确认反过?", "确认反过账 "),
    ("没有可以主管复核的凭证项（需为已完成状态）?)", '没有可以主管复核的凭证项（需为已完成状态）。")'),
    ("没有可以删除的凭证项（仅暂存且当期及以后凭证可删）?)", '没有可以删除的凭证项（仅暂存且当期及以后凭证可删）。")'),
    ("请选择起始月份和结束月?)", '请选择起始月份和结束月份")'),
    ("未修改任何科目余?)", '未修改任何科目余额")'),
    ("请保持试算平?)", '请保持试算平衡")'),
    ("是否确认删除员工信息编号?'", "是否确认删除员工信息编号为'\""),
    ("的数据项?)", '的数据项？")'),
    ("是否确认删除这些数据?)", '是否确认删除这些数据？")'),
    ("至少需要两条分?)", '至少需要两条分录")'),
    ("请至少输入一项摘?)", '请至少输入一项摘要")'),
    ("借贷不平衡，请检查分录金?)", '借贷不平衡，请检查分录金额")'),
    ("name: '金额/万?", "name: '金额/万',"),
    ("return h('p', '?)", "return h('p', '年')"),
    (".join('?)", ".join('至')"),
    ("periodType === '?)", "periodType === 'quarter')"),
]

REGEX: list[tuple[re.Pattern[str], str]] = [
    (re.compile(r"([^<\"']{1,30})\?/span>"), r"\1</span>"),
    (re.compile(r"name: '([^'\n]*?)\?\s*\n"), r"name: '金额/万',\n"),
    (re.compile(r"data: \['([^'\]]*?)', '([^'\]]*?)\?\],"), r"data: ['\1', '\2'],"),
    (re.compile(r"name: '([^'\n]*?)\?\s*\n(\s+type: 'line')"), r"name: '\1',\n\2"),
    (re.compile(r"(\$\w+\.msg(?:Error|Success|Warning)\(\"[^\"]*?)\?\)"), r'\1。")'),
    (re.compile(r"(\$\w+\.confirm\('[^']*?)\?\)"), r"\1？')"),
    (re.compile(r"(modal\.confirm\(\"[^\"]*?)\?\)"), r'\1？")'),
]


def read_text(path: Path) -> str:
    raw = path.read_bytes()
    for enc in ("utf-8", "utf-8-sig", "gb18030", "latin-1"):
        try:
            return raw.decode(enc)
        except UnicodeDecodeError:
            continue
    return raw.decode("utf-8", errors="replace")


def fix_text(text: str) -> str:
    for old, new in LITERALS:
        text = text.replace(old, new)
    for pattern, repl in REGEX:
        text = pattern.sub(repl, text)
    return text


def main() -> None:
    changed: list[str] = []
    for path in sorted(ROOT.rglob("*")):
        if path.suffix not in {".vue", ".ts", ".js"}:
            continue
        original = read_text(path)
        fixed = fix_text(original)
        if fixed != original:
            path.write_text(fixed, encoding="utf-8", newline="\n")
            changed.append(str(path.relative_to(ROOT.parent)))
    print(f"Fixed {len(changed)} files")
    for name in changed:
        print(f"  {name}")


if __name__ == "__main__":
    main()
