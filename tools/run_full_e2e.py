"""Run every Playwright spec, isolating suites that require a blank book."""
import os
from pathlib import Path
import shutil
import subprocess
import sys

root = Path(__file__).resolve().parents[1]
ui = root / "financial-cloud-ui"
env = os.environ.copy()
if not env.get("FC_DB_NAME", "").startswith("financial_cloud_e2e_"):
    sys.exit("FC_DB_NAME must name an isolated financial_cloud_e2e_ database")
env["E2E_RESET_BOOK"] = "1"
env["E2E_ENABLE_UI"] = "1"
independent = [
    "independent-accounting-reference.spec.ts",
    "fixed-asset-accounting-reference.spec.ts",
    "04-balance-sheet-golden-dataset.spec.ts",
    "05-income-statement-golden-dataset.spec.ts",
    "carry-forward-year-end.spec.ts",
]
specs = sorted((ui / "e2e").glob("*.spec.ts"))
groups = [[f"e2e/{p.name}" for p in specs if p.name not in independent]]
groups.extend([[f"e2e/{name}"] for name in independent])
npx = shutil.which("npx.cmd" if os.name == "nt" else "npx")
if not npx:
    sys.exit("npx is unavailable")
failed = False
for index, group in enumerate(groups, 1):
    print(f"Full E2E group {index}/{len(groups)}: {len(group)} spec files", flush=True)
    result = subprocess.run([npx, "playwright", "test", *group], cwd=ui, env=env)
    failed = failed or result.returncode != 0
sys.exit(1 if failed else 0)
