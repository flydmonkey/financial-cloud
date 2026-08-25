# Wave 1: voucher domain full stack migration
$ErrorActionPreference = "Stop"
$base = "C:\Users\Administrator\Projects\jinbooks\jinbooks\src\main\java\com\jinbooks"
$resBase = "C:\Users\Administrator\Projects\jinbooks\jinbooks\src\main\resources"
$srcRoot = "C:\Users\Administrator\Projects\jinbooks\jinbooks\src"

function Ensure-Dir($p) { New-Item -ItemType Directory -Force -Path $p | Out-Null }

function Move-File($src, $dst) {
    if (-not (Test-Path $src)) { Write-Warning "Skip missing: $src"; return }
    Ensure-Dir (Split-Path $dst -Parent)
    Move-Item -Force $src $dst
    Write-Host "Moved -> $dst"
}

function Set-Package($file, $pkg) {
    $c = [System.IO.File]::ReadAllText($file)
    $c = $c -replace 'package\s+[\w.]+;', "package $pkg;"
    [System.IO.File]::WriteAllText($file, $c)
}

# --- domain entities ---
Ensure-Dir "$base\domain\voucher"
$entities = @(
    "Voucher.java","VoucherItem.java","VoucherAuxiliary.java","VoucherItemCashFlow.java",
    "VoucherTemplate.java","VoucherTemplateItem.java","VoucherWord.java"
)
foreach ($e in $entities) {
    Move-File "$base\entity\voucher\$e" "$base\domain\voucher\$e"
    Set-Package "$base\domain\voucher\$e" "com.jinbooks.domain.voucher"
}

# --- dto (dto + vo merged) ---
Ensure-Dir "$base\dto\voucher"
Get-ChildItem "$base\entity\voucher\dto\*.java" -ErrorAction SilentlyContinue | ForEach-Object {
    Move-File $_.FullName "$base\dto\voucher\$($_.Name)"
    Set-Package "$base\dto\voucher\$($_.Name)" "com.jinbooks.dto.voucher"
}
Get-ChildItem "$base\entity\voucher\vo\*.java" -ErrorAction SilentlyContinue | ForEach-Object {
    Move-File $_.FullName "$base\dto\voucher\$($_.Name)"
    Set-Package "$base\dto\voucher\$($_.Name)" "com.jinbooks.dto.voucher"
}

# --- controllers ---
Ensure-Dir "$base\controller\voucher"
Get-ChildItem "$base\web\voucher\controller\*.java" -ErrorAction SilentlyContinue | ForEach-Object {
    Move-File $_.FullName "$base\controller\voucher\$($_.Name)"
    Set-Package "$base\controller\voucher\$($_.Name)" "com.jinbooks.controller.voucher"
}

# --- repository mappers ---
Ensure-Dir "$base\repository\voucher"
$mappers = @(
    "VoucherMapper.java","VoucherItemMapper.java","VoucherItemAuxiliaryMapper.java",
    "VoucherItemCashFlowMapper.java","VoucherTemplateMapper.java","VoucherTemplateItemMapper.java","VoucherWordMapper.java"
)
foreach ($m in $mappers) {
    Move-File "$base\persistence\mapper\$m" "$base\repository\voucher\$m"
    Set-Package "$base\repository\voucher\$m" "com.jinbooks.repository.voucher"
}

# --- services ---
Ensure-Dir "$base\service\voucher\impl"
$services = @(
    "VoucherService.java","VoucherTemplateService.java","VoucherTemplateItemService.java","VoucherItemCashFlowService.java"
)
foreach ($s in $services) {
    Move-File "$base\persistence\service\$s" "$base\service\voucher\$s"
    Set-Package "$base\service\voucher\$s" "com.jinbooks.service.voucher"
}
Get-ChildItem "$base\persistence\service\impl\Voucher*.java" -ErrorAction SilentlyContinue | ForEach-Object {
    Move-File $_.FullName "$base\service\voucher\impl\$($_.Name)"
    Set-Package "$base\service\voucher\impl\$($_.Name)" "com.jinbooks.service.voucher.impl"
}

# --- XML ---
$xmlDst = "$resBase\com\jinbooks\repository\voucher\xml\mysql"
Ensure-Dir $xmlDst
$xmlFiles = @("VoucherItemMapper.xml","VoucherItemCashFlowMapper.xml","VoucherTemplateMapper.xml")
foreach ($x in $xmlFiles) {
    $src = "$resBase\com\jinbooks\persistence\mapper\xml\mysql\$x"
    if (Test-Path $src) {
        Move-File $src "$xmlDst\$x"
        $c = [System.IO.File]::ReadAllText("$xmlDst\$x")
        $c = $c -replace 'com\.jinbooks\.persistence\.mapper\.', 'com.jinbooks.repository.voucher.'
        [System.IO.File]::WriteAllText("$xmlDst\$x", $c)
    }
}

# --- global import replacements (voucher-specific only) ---
$replacements = [ordered]@{
    'com.jinbooks.entity.voucher.dto.'       = 'com.jinbooks.dto.voucher.'
    'com.jinbooks.entity.voucher.vo.'        = 'com.jinbooks.dto.voucher.'
    'com.jinbooks.entity.voucher.'           = 'com.jinbooks.domain.voucher.'
    'com.jinbooks.web.voucher.controller.'   = 'com.jinbooks.controller.voucher.'
    'com.jinbooks.persistence.mapper.Voucher' = 'com.jinbooks.repository.voucher.Voucher'
    'com.jinbooks.persistence.service.impl.Voucher' = 'com.jinbooks.service.voucher.impl.Voucher'
    'com.jinbooks.persistence.service.Voucher' = 'com.jinbooks.service.voucher.Voucher'
}

Get-ChildItem -Path $srcRoot -Recurse -Include *.java,*.xml | ForEach-Object {
    $c = [System.IO.File]::ReadAllText($_.FullName)
    $orig = $c
    foreach ($k in $replacements.Keys) {
        $c = $c.Replace($k, $replacements[$k])
    }
    if ($c -ne $orig) {
        [System.IO.File]::WriteAllText($_.FullName, $c)
    }
}

# Cleanup empty dirs
@(
    "$base\entity\voucher\dto",
    "$base\entity\voucher\vo",
    "$base\entity\voucher",
    "$base\web\voucher\controller",
    "$base\web\voucher"
) | ForEach-Object {
    if ((Test-Path $_) -and -not (Get-ChildItem $_ -Recurse -ErrorAction SilentlyContinue)) {
        Remove-Item $_ -Force -Recurse -ErrorAction SilentlyContinue
    }
}

Write-Host "Wave 1 voucher migration complete."
