#!/usr/bin/env python3
"""Rename JinBooks to financial-cloud / com.financial.cloud."""

from __future__ import annotations

import os
import re
import shutil
from pathlib import Path

ROOT = Path(__file__).resolve().parents[1]

SKIP_DIRS = {
    ".git",
    "node_modules",
    "target",
    ".superpowers",
    "__pycache__",
    "dist",
    ".vite",
    "deps",
    "deps_temp_8d4dbe03",
}

BINARY_SUFFIXES = {
    ".png",
    ".jpg",
    ".jpeg",
    ".gif",
    ".ico",
    ".woff",
    ".woff2",
    ".ttf",
    ".eot",
    ".jar",
    ".class",
    ".zip",
    ".pdf",
}

# Order matters: longer / more specific patterns first.
TEXT_REPLACEMENTS = [
    ("com.financial.cloud", "com.financial.cloud"),
    ("com/financial/cloud", "com/financial/cloud"),
    ("FinancialCloudApplication", "FinancialCloudApplication"),
    ("FinancialCloudMvcConfig", "FinancialCloudMvcConfig"),
    ("FinancialCloudConfig", "FinancialCloudConfig"),
    ("${financial-cloud.", "${financial-cloud."),
    ("financial-cloud-boot-", "financial-cloud-boot-"),
    ("financial-cloud-ui", "financial-cloud-ui"),
    ("logs/financial-cloud.log", "logs/financial-cloud.log"),
    ("财务云", "财务云"),
    ("<artifactId>financial-cloud</artifactId>", "<artifactId>financial-cloud</artifactId>"),
    ('"name": "financial-cloud"', '"name": "financial-cloud"'),
    ('"description": "财务云"', '"description": "财务云"'),
    ('"author": "financial-cloud"', '"author": "financial-cloud"'),
    ("VITE_APP_CONTEXT_PATH = '/financial-cloud/'", "VITE_APP_CONTEXT_PATH = '/financial-cloud/'"),
    ('"/financial-cloud/temporary/', '"/financial-cloud/temporary/'),
    ("<name>financial-cloud</name>", "<name>financial-cloud</name>"),
    ("<description>财务云</description>", "<description>财务云</description>"),
    ("  title: 财务云", "  title: 财务云"),
    ("    name: financial-cloud", "    name: financial-cloud"),
    ("财务云系统启动入口", "财务云系统启动入口"),
    ("@author financial-cloud", "@author financial-cloud"),
    ("https://www.financial-cloud.com", "https://www.financial-cloud.com"),
    ("financial-cloud.com", "financial-cloud.com"),
]

MODULE_PATH_REPLACEMENTS = [
    ("financial-cloud/src/", "financial-cloud/src/"),
    ("jinbooks\\src\\", "financial-cloud\\src\\"),
    ("financial-cloud/pom.xml", "financial-cloud/pom.xml"),
    ("jinbooks\\pom.xml", "financial-cloud\\pom.xml"),
    ("cd financial-cloud", "cd financial-cloud"),
    ('ROOT / "financial-cloud"', 'ROOT / "financial-cloud"'),
    ("ROOT / 'financial-cloud'", "ROOT / 'financial-cloud'"),
    ('/"financial-cloud"/', '/"financial-cloud"/'),
]


def should_skip_dir(name: str) -> bool:
    return name in SKIP_DIRS


def iter_files() -> list[Path]:
    files: list[Path] = []
    for dirpath, dirnames, filenames in os.walk(ROOT):
        dirnames[:] = [d for d in dirnames if not should_skip_dir(d)]
        for filename in filenames:
            path = Path(dirpath) / filename
            if path.suffix.lower() in BINARY_SUFFIXES:
                continue
            files.append(path)
    return files


def replace_in_file(path: Path) -> bool:
    try:
        text = path.read_text(encoding="utf-8")
    except (UnicodeDecodeError, OSError):
        return False
    original = text
    for old, new in TEXT_REPLACEMENTS:
        text = text.replace(old, new)
    for old, new in MODULE_PATH_REPLACEMENTS:
        text = text.replace(old, new)
    # yaml root key only at line start
    text = re.sub(r"(?m)^jinbooks:", "financial-cloud:", text)
    if text != original:
        path.write_text(text, encoding="utf-8", newline="\n")
        return True
    return False


def move_if_exists(src: Path, dst: Path) -> None:
    if not src.exists():
        return
    dst.parent.mkdir(parents=True, exist_ok=True)
    if dst.exists():
        raise RuntimeError(f"destination already exists: {dst}")
    shutil.move(str(src), str(dst))


def rename_java_tree(base: Path) -> None:
    src_pkg = base / "com" / "jinbooks"
    dst_pkg = base / "com" / "financial" / "cloud"
    if src_pkg.exists():
        move_if_exists(src_pkg, dst_pkg)
    old_com = base / "com" / "jinbooks"
    if old_com.exists() and not any(old_com.iterdir()):
        old_com.rmdir()


def rename_java_files(module: Path) -> None:
    mappings = {
        "FinancialCloudApplication.java": "FinancialCloudApplication.java",
        "FinancialCloudMvcConfig.java": "FinancialCloudMvcConfig.java",
        "FinancialCloudConfig.java": "FinancialCloudConfig.java",
    }
    for folder in [module / "src/main/java", module / "src/test/java"]:
        for old, new in mappings.items():
            for path in folder.rglob(old):
                path.rename(path.with_name(new))


def main() -> None:
    changed = 0
    for path in iter_files():
        if replace_in_file(path):
            changed += 1
    print(f"updated {changed} files")

    module = ROOT / "financial-cloud"
    if module.exists():
        rename_java_files(module)
        rename_java_tree(module / "src/main/java")
        rename_java_tree(module / "src/test/java")
        rename_java_tree(module / "src/main/resources")

    ui = ROOT / "financial-cloud-ui"
    if ui.exists() and not (ROOT / "financial-cloud-ui").exists():
        shutil.move(str(ui), str(ROOT / "financial-cloud-ui"))
        print("renamed financial-cloud-ui -> financial-cloud-ui")

    if module.exists() and not (ROOT / "financial-cloud").exists():
        shutil.move(str(module), str(ROOT / "financial-cloud"))
        print("renamed jinbooks -> financial-cloud")

    print("done")


if __name__ == "__main__":
    main()
