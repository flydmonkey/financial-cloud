# Fix imports after wave 0+1 package restructure
$ErrorActionPreference = "Stop"
$srcRoot = "C:\Users\Administrator\Projects\jinbooks\financial-cloud\src\main\java"

function Add-ImportIfMissing($file, $importLine) {
    $c = [System.IO.File]::ReadAllText($file)
    if ($c -match [regex]::Escape($importLine)) { return }
    if ($c -match '(?m)^package .+;\r?\n\r?\n') {
        $c = $c -replace '(?m)^(package .+;\r?\n)\r?\n', "`$1`r`n$importLine`r`n"
        [System.IO.File]::WriteAllText($file, $c)
        Write-Host "Added import to $(Split-Path $file -Leaf): $importLine"
    }
}

# 1. BaseEntity import for all extenders
Get-ChildItem -Path $srcRoot -Recurse -Filter *.java | ForEach-Object {
    $c = [System.IO.File]::ReadAllText($_.FullName)
    if ($c -match 'extends BaseEntity' -and $c -notmatch 'import com\.jinbooks\.common\.BaseEntity') {
        Add-ImportIfMissing $_.FullName 'import com.financial.cloud.common.BaseEntity;'
    }
}

# 2. PageQuery import for extenders
Get-ChildItem -Path $srcRoot -Recurse -Filter *.java | ForEach-Object {
    $c = [System.IO.File]::ReadAllText($_.FullName)
    if ($c -match 'extends PageQuery' -and $c -notmatch 'import com\.jinbooks\.common\.PageQuery') {
        Add-ImportIfMissing $_.FullName 'import com.financial.cloud.common.PageQuery;'
    }
}

# 3. Fix star imports from entity (Message was in entity root)
$starEntityFiles = @(
    "$srcRoot\com\jinbooks\authn\support\cas\HttpTrustEntryPoint.java",
    "$srcRoot\com\jinbooks\authn\support\cas\service\CasTrustLoginService.java",
    "$srcRoot\com\jinbooks\authn\support\cas\service\impl\CasTrustLoginServiceImpl.java",
    "$srcRoot\com\jinbooks\web\idm\controller\UserInfoController.java"
)
foreach ($f in $starEntityFiles) {
    if (-not (Test-Path $f)) { continue }
    $c = [System.IO.File]::ReadAllText($f)
    $c = $c -replace 'import com\.jinbooks\.entity\.\*;\r?\n', ''
    if ($c -notmatch 'import com\.jinbooks\.common\.Message') {
        $c = $c -replace '(?m)^(package .+;\r?\n)\r?\n', "`$1`r`nimport com.financial.cloud.common.Message;`r`n"
    }
    [System.IO.File]::WriteAllText($f, $c)
    Write-Host "Fixed star entity import: $(Split-Path $f -Leaf)"
}

# 4. Global string replacements for cross-domain voucher references
$replacements = [ordered]@{
    'import com.financial.cloud.persistence.mapper.Voucher' = 'import com.financial.cloud.repository.voucher.Voucher'
    'import com.financial.cloud.persistence.service.Voucher' = 'import com.financial.cloud.service.voucher.Voucher'
    'import com.financial.cloud.persistence.service.impl.Voucher' = 'import com.financial.cloud.service.voucher.impl.Voucher'
}

Get-ChildItem -Path $srcRoot -Recurse -Filter *.java | ForEach-Object {
    $c = [System.IO.File]::ReadAllText($_.FullName)
    $orig = $c
    foreach ($k in $replacements.Keys) {
        $c = $c.Replace($k, $replacements[$k])
    }
    if ($c -ne $orig) { [System.IO.File]::WriteAllText($_.FullName, $c) }
}

# 5. Voucher service impl: replace wildcard mapper import with repository.voucher
$voucherImplDir = "$srcRoot\com\jinbooks\service\voucher"
if (Test-Path $voucherImplDir) {
    Get-ChildItem -Path $voucherImplDir -Recurse -Filter *.java | ForEach-Object {
        $c = [System.IO.File]::ReadAllText($_.FullName)
        if ($c -match 'import com\.jinbooks\.persistence\.mapper\.\*;') {
            $c = $c -replace 'import com\.jinbooks\.persistence\.mapper\.\*;\r?\n', ''
            $mapperImports = @(
                'import com.financial.cloud.repository.voucher.VoucherMapper;',
                'import com.financial.cloud.repository.voucher.VoucherItemMapper;',
                'import com.financial.cloud.repository.voucher.VoucherWordMapper;',
                'import com.financial.cloud.repository.voucher.VoucherItemAuxiliaryMapper;',
                'import com.financial.cloud.repository.voucher.VoucherItemCashFlowMapper;',
                'import com.financial.cloud.repository.voucher.VoucherTemplateMapper;',
                'import com.financial.cloud.repository.voucher.VoucherTemplateItemMapper;'
            )
            $needed = $mapperImports | Where-Object {
                $name = $_ -replace '.*\.(\w+);', '$1'
                $c -match "\b$name\b"
            }
            if ($needed.Count -gt 0) {
                $block = ($needed -join "`r`n") + "`r`n"
                $c = $c -replace '(?m)^(package .+;\r?\n)\r?\n', "`$1`r`n$block"
            }
            # Keep non-voucher mappers from persistence.mapper individually
            $otherMappers = @('UserInfoMapper','BookMapper','StandardSubjectCashFlowMapper','EmployeeSalarySummaryMapper')
            foreach ($m in $otherMappers) {
                if ($c -match "\b$m\b" -and $c -notmatch "import com\.jinbooks\.persistence\.mapper\.$m") {
                    Add-ImportIfMissing $_.FullName "import com.financial.cloud.persistence.mapper.$m;"
                    $c = [System.IO.File]::ReadAllText($_.FullName)
                }
            }
            [System.IO.File]::WriteAllText($_.FullName, $c)
            Write-Host "Fixed mapper imports in $($_.Name)"
        }
    }
}

# 6. Cross-domain files using Voucher* via persistence.service.* wildcard
$crossDomainFiles = @(
    "$srcRoot\com\jinbooks\persistence\service\impl\BookServiceImpl.java",
    "$srcRoot\com\jinbooks\persistence\service\impl\SettlementServiceImpl.java",
    "$srcRoot\com\jinbooks\persistence\service\impl\SettlementCarryServiceImpl.java",
    "$srcRoot\com\jinbooks\persistence\service\impl\EmployeeSalaryServiceImpl.java",
    "$srcRoot\com\jinbooks\persistence\service\impl\EmployeeSalarySummaryServiceImpl.java",
    "$srcRoot\com\jinbooks\persistence\service\impl\StatementIncomeServiceImpl.java",
    "$srcRoot\com\jinbooks\persistence\service\impl\StatementReportServiceImpl.java",
    "$srcRoot\com\jinbooks\persistence\service\impl\JournalEntryServiceImpl.java",
    "$srcRoot\com\jinbooks\persistence\service\impl\StatementSubjectBalanceServiceImpl.java",
    "$srcRoot\com\jinbooks\persistence\service\SettlementCarryService.java"
)
$crossImports = @{
    'VoucherService' = 'import com.financial.cloud.service.voucher.VoucherService;'
    'VoucherTemplateService' = 'import com.financial.cloud.service.voucher.VoucherTemplateService;'
    'VoucherTemplateItemService' = 'import com.financial.cloud.service.voucher.VoucherTemplateItemService;'
    'VoucherItemCashFlowService' = 'import com.financial.cloud.service.voucher.VoucherItemCashFlowService;'
    'VoucherMapper' = 'import com.financial.cloud.repository.voucher.VoucherMapper;'
    'VoucherItemMapper' = 'import com.financial.cloud.repository.voucher.VoucherItemMapper;'
    'VoucherTemplateMapper' = 'import com.financial.cloud.repository.voucher.VoucherTemplateMapper;'
    'VoucherTemplateItemMapper' = 'import com.financial.cloud.repository.voucher.VoucherTemplateItemMapper;'
    'VoucherWordMapper' = 'import com.financial.cloud.repository.voucher.VoucherWordMapper;'
    'VoucherItemCashFlowMapper' = 'import com.financial.cloud.repository.voucher.VoucherItemCashFlowMapper;'
}
foreach ($f in $crossDomainFiles) {
    if (-not (Test-Path $f)) { continue }
    $c = [System.IO.File]::ReadAllText($f)
    foreach ($sym in $crossImports.Keys) {
        if ($c -match "\b$sym\b" -and $c -notmatch [regex]::Escape($crossImports[$sym])) {
            Add-ImportIfMissing $f $crossImports[$sym]
            $c = [System.IO.File]::ReadAllText($f)
        }
    }
}

Write-Host "Import fix complete."
