# Wave 3: journal + statement full stack migration
$ErrorActionPreference = "Stop"
$base = "C:\Users\Administrator\Projects\jinbooks\financial-cloud\src\main\java\com\jinbooks"
$resBase = "C:\Users\Administrator\Projects\jinbooks\financial-cloud\src\main\resources"
$srcRoot = "C:\Users\Administrator\Projects\jinbooks\jinbooks\src"

function Ensure-Dir($p) { New-Item -ItemType Directory -Force -Path $p | Out-Null }
function Move-File($src, $dst) {
    if (-not (Test-Path $src)) { Write-Warning "Skip missing: $src"; return }
    Ensure-Dir (Split-Path $dst -Parent)
    Move-Item -Force $src $dst
    Write-Host "Moved -> $(Split-Path $dst -Leaf)"
}
function Set-Package($file, $pkg) {
    if (-not (Test-Path $file)) { return }
    $c = [System.IO.File]::ReadAllText($file)
    $c = $c -replace 'package\s+[\w.]+;', "package $pkg;"
    [System.IO.File]::WriteAllText($file, $c)
}
function Add-ImportIfMissing($file, $importLine) {
    $c = [System.IO.File]::ReadAllText($file)
    if ($c -match [regex]::Escape($importLine)) { return }
    if ($c -match '(?m)^package .+;\r?\n\r?\n') {
        $c = $c -replace '(?m)^(package .+;\r?\n)\r?\n', "`$1`r`n$importLine`r`n"
        [System.IO.File]::WriteAllText($file, $c)
    } elseif ($c -match '(?m)^package .+;\r?\n') {
        $c = $c -replace '(?m)^(package .+;\r?\n)', "`$1`r`n$importLine`r`n"
        [System.IO.File]::WriteAllText($file, $c)
    }
}

function Migrate-Domain($domain, $entityDirs, $controllers, $mappers, $services, $impls, $xmlFiles) {
    Ensure-Dir "$base\domain\$domain"
    Ensure-Dir "$base\dto\$domain"
    Ensure-Dir "$base\controller\$domain"
    Ensure-Dir "$base\repository\$domain"
    Ensure-Dir "$base\service\$domain\impl"

    foreach ($dir in $entityDirs) {
        Get-ChildItem "$base\entity\$dir\*.java" -ErrorAction SilentlyContinue | ForEach-Object {
            Move-File $_.FullName "$base\domain\$domain\$($_.Name)"
            Set-Package "$base\domain\$domain\$($_.Name)" "com.financial.cloud.domain.$domain"
        }
        Get-ChildItem "$base\entity\$dir\dto\*.java","$base\entity\$dir\vo\*.java" -ErrorAction SilentlyContinue | ForEach-Object {
            Move-File $_.FullName "$base\dto\$domain\$($_.Name)"
            Set-Package "$base\dto\$domain\$($_.Name)" "com.financial.cloud.dto.$domain"
        }
    }

    foreach ($c in $controllers) {
        Move-File "$base\web\$domain\controller\$c" "$base\controller\$domain\$c"
        Set-Package "$base\controller\$domain\$c" "com.financial.cloud.controller.$domain"
    }

    foreach ($m in $mappers) {
        Move-File "$base\persistence\mapper\$m" "$base\repository\$domain\$m"
        Set-Package "$base\repository\$domain\$m" "com.financial.cloud.repository.$domain"
    }

    foreach ($s in $services) {
        Move-File "$base\persistence\service\$s" "$base\service\$domain\$s"
        Set-Package "$base\service\$domain\$s" "com.financial.cloud.service.$domain"
    }
    foreach ($i in $impls) {
        Move-File "$base\persistence\service\impl\$i" "$base\service\$domain\impl\$i"
        Set-Package "$base\service\$domain\impl\$i" "com.financial.cloud.service.$domain.impl"
    }

    $xmlDst = "$resBase\com\jinbooks\repository\$domain\xml\mysql"
    Ensure-Dir $xmlDst
    foreach ($x in $xmlFiles) {
        $src = "$resBase\com\jinbooks\persistence\mapper\xml\mysql\$x"
        if (Test-Path $src) {
            Move-File $src "$xmlDst\$x"
            $c = [System.IO.File]::ReadAllText("$xmlDst\$x")
            $c = $c -replace 'com\.jinbooks\.persistence\.mapper\.', "com.financial.cloud.repository.$domain."
            [System.IO.File]::WriteAllText("$xmlDst\$x", $c)
        }
    }
}

# --- JOURNAL ---
Migrate-Domain -domain "journal" `
    -entityDirs @("journal") `
    -controllers @("JournalAccountController.java","JournalEntryController.java","JournalSummaryController.java") `
    -mappers @("JournalAccountMapper.java","JournalEntryMapper.java","JournalSummaryMapper.java") `
    -services @("JournalAccountService.java","JournalEntryService.java","JournalSummaryService.java") `
    -impls @("JournalAccountServiceImpl.java","JournalEntryServiceImpl.java","JournalSummaryServiceImpl.java") `
    -xmlFiles @("JournalAccountMapper.xml","JournalEntryMapper.xml","JournalSummaryMapper.xml")

# --- STATEMENT ---
Migrate-Domain -domain "statement" `
    -entityDirs @("statement") `
    -controllers @(
        "StatementBalanceSheetController.java","StatementBalanceSheetConfigController.java",
        "StatementCashFlowController.java","StatementIncomeController.java","StatementIncomeConfigController.java",
        "StatementReportController.java","StatementRuleConfigController.java","StatementSubjectBalanceController.java"
    ) `
    -mappers @(
        "StatementBalanceSheetMapper.java","StatementBalanceSheetItemMapper.java",
        "StatementCashFlowMapper.java","StatementIncomeMapper.java","StatementIncomeItemMapper.java",
        "StatementRulesMapper.java","StatementSubjectBalanceMapper.java"
    ) `
    -services @(
        "StatementBalanceSheetService.java","StatementBalanceSheetConfigService.java",
        "StatementCashFlowService.java","StatementIncomeService.java","StatementIncomeConfigService.java",
        "StatementReportService.java","StatementSubjectBalanceService.java"
    ) `
    -impls @(
        "StatementBalanceSheetServiceImpl.java","StatementBalanceSheetConfigServiceImpl.java",
        "StatementCashFlowServiceImpl.java","StatementIncomeServiceImpl.java","StatementIncomeConfigServiceImpl.java",
        "StatementReportServiceImpl.java","StatementSubjectBalanceServiceImpl.java"
    ) `
    -xmlFiles @("StatementCashFlowMapper.xml","StatementSubjectBalanceMapper.xml")

# --- global replacements (specific first) ---
$replacements = [ordered]@{
    'com.financial.cloud.entity.journal.dto.' = 'com.financial.cloud.dto.journal.'
    'com.financial.cloud.entity.journal.vo.'  = 'com.financial.cloud.dto.journal.'
    'com.financial.cloud.entity.journal.'     = 'com.financial.cloud.domain.journal.'
    'com.financial.cloud.entity.statement.dto.' = 'com.financial.cloud.dto.statement.'
    'com.financial.cloud.entity.statement.vo.'  = 'com.financial.cloud.dto.statement.'
    'com.financial.cloud.entity.statement.'     = 'com.financial.cloud.domain.statement.'
    'com.financial.cloud.web.journal.controller.' = 'com.financial.cloud.controller.journal.'
    'com.financial.cloud.web.statement.controller.' = 'com.financial.cloud.controller.statement.'

    'com.financial.cloud.persistence.mapper.JournalAccount' = 'com.financial.cloud.repository.journal.JournalAccount'
    'com.financial.cloud.persistence.mapper.JournalEntry' = 'com.financial.cloud.repository.journal.JournalEntry'
    'com.financial.cloud.persistence.mapper.JournalSummary' = 'com.financial.cloud.repository.journal.JournalSummary'
    'com.financial.cloud.persistence.mapper.StatementBalanceSheetItem' = 'com.financial.cloud.repository.statement.StatementBalanceSheetItem'
    'com.financial.cloud.persistence.mapper.StatementBalanceSheet' = 'com.financial.cloud.repository.statement.StatementBalanceSheet'
    'com.financial.cloud.persistence.mapper.StatementCashFlow' = 'com.financial.cloud.repository.statement.StatementCashFlow'
    'com.financial.cloud.persistence.mapper.StatementIncomeItem' = 'com.financial.cloud.repository.statement.StatementIncomeItem'
    'com.financial.cloud.persistence.mapper.StatementIncome' = 'com.financial.cloud.repository.statement.StatementIncome'
    'com.financial.cloud.persistence.mapper.StatementRules' = 'com.financial.cloud.repository.statement.StatementRules'
    'com.financial.cloud.persistence.mapper.StatementSubjectBalance' = 'com.financial.cloud.repository.statement.StatementSubjectBalance'

    'com.financial.cloud.persistence.service.impl.JournalAccount' = 'com.financial.cloud.service.journal.impl.JournalAccount'
    'com.financial.cloud.persistence.service.impl.JournalEntry' = 'com.financial.cloud.service.journal.impl.JournalEntry'
    'com.financial.cloud.persistence.service.impl.JournalSummary' = 'com.financial.cloud.service.journal.impl.JournalSummary'
    'com.financial.cloud.persistence.service.impl.StatementBalanceSheetConfig' = 'com.financial.cloud.service.statement.impl.StatementBalanceSheetConfig'
    'com.financial.cloud.persistence.service.impl.StatementBalanceSheet' = 'com.financial.cloud.service.statement.impl.StatementBalanceSheet'
    'com.financial.cloud.persistence.service.impl.StatementCashFlow' = 'com.financial.cloud.service.statement.impl.StatementCashFlow'
    'com.financial.cloud.persistence.service.impl.StatementIncomeConfig' = 'com.financial.cloud.service.statement.impl.StatementIncomeConfig'
    'com.financial.cloud.persistence.service.impl.StatementIncome' = 'com.financial.cloud.service.statement.impl.StatementIncome'
    'com.financial.cloud.persistence.service.impl.StatementReport' = 'com.financial.cloud.service.statement.impl.StatementReport'
    'com.financial.cloud.persistence.service.impl.StatementSubjectBalance' = 'com.financial.cloud.service.statement.impl.StatementSubjectBalance'

    'com.financial.cloud.persistence.service.JournalAccount' = 'com.financial.cloud.service.journal.JournalAccount'
    'com.financial.cloud.persistence.service.JournalEntry' = 'com.financial.cloud.service.journal.JournalEntry'
    'com.financial.cloud.persistence.service.JournalSummary' = 'com.financial.cloud.service.journal.JournalSummary'
    'com.financial.cloud.persistence.service.StatementBalanceSheetConfig' = 'com.financial.cloud.service.statement.StatementBalanceSheetConfig'
    'com.financial.cloud.persistence.service.StatementBalanceSheet' = 'com.financial.cloud.service.statement.StatementBalanceSheet'
    'com.financial.cloud.persistence.service.StatementCashFlow' = 'com.financial.cloud.service.statement.StatementCashFlow'
    'com.financial.cloud.persistence.service.StatementIncomeConfig' = 'com.financial.cloud.service.statement.StatementIncomeConfig'
    'com.financial.cloud.persistence.service.StatementIncome' = 'com.financial.cloud.service.statement.StatementIncome'
    'com.financial.cloud.persistence.service.StatementReport' = 'com.financial.cloud.service.statement.StatementReport'
    'com.financial.cloud.persistence.service.StatementSubjectBalance' = 'com.financial.cloud.service.statement.StatementSubjectBalance'
}

Get-ChildItem -Path $srcRoot -Recurse -Include *.java,*.xml | ForEach-Object {
    $c = [System.IO.File]::ReadAllText($_.FullName)
    $orig = $c
    foreach ($k in $replacements.Keys) { $c = $c.Replace($k, $replacements[$k]) }
    if ($c -ne $orig) { [System.IO.File]::WriteAllText($_.FullName, $c) }
}

# Cross-domain symbol imports
$symbols = [ordered]@{
    'JournalAccountService' = 'import com.financial.cloud.service.journal.JournalAccountService;'
    'JournalEntryService' = 'import com.financial.cloud.service.journal.JournalEntryService;'
    'JournalSummaryService' = 'import com.financial.cloud.service.journal.JournalSummaryService;'
    'JournalAccountMapper' = 'import com.financial.cloud.repository.journal.JournalAccountMapper;'
    'JournalEntryMapper' = 'import com.financial.cloud.repository.journal.JournalEntryMapper;'
    'JournalSummaryMapper' = 'import com.financial.cloud.repository.journal.JournalSummaryMapper;'
    'StatementBalanceSheetConfigService' = 'import com.financial.cloud.service.statement.StatementBalanceSheetConfigService;'
    'StatementBalanceSheetService' = 'import com.financial.cloud.service.statement.StatementBalanceSheetService;'
    'StatementCashFlowService' = 'import com.financial.cloud.service.statement.StatementCashFlowService;'
    'StatementIncomeConfigService' = 'import com.financial.cloud.service.statement.StatementIncomeConfigService;'
    'StatementIncomeService' = 'import com.financial.cloud.service.statement.StatementIncomeService;'
    'StatementReportService' = 'import com.financial.cloud.service.statement.StatementReportService;'
    'StatementSubjectBalanceService' = 'import com.financial.cloud.service.statement.StatementSubjectBalanceService;'
    'StatementBalanceSheetMapper' = 'import com.financial.cloud.repository.statement.StatementBalanceSheetMapper;'
    'StatementBalanceSheetItemMapper' = 'import com.financial.cloud.repository.statement.StatementBalanceSheetItemMapper;'
    'StatementCashFlowMapper' = 'import com.financial.cloud.repository.statement.StatementCashFlowMapper;'
    'StatementIncomeMapper' = 'import com.financial.cloud.repository.statement.StatementIncomeMapper;'
    'StatementIncomeItemMapper' = 'import com.financial.cloud.repository.statement.StatementIncomeItemMapper;'
    'StatementRulesMapper' = 'import com.financial.cloud.repository.statement.StatementRulesMapper;'
    'StatementSubjectBalanceMapper' = 'import com.financial.cloud.repository.statement.StatementSubjectBalanceMapper;'
}

Get-ChildItem -Path "$srcRoot\main\java" -Recurse -Filter *.java | ForEach-Object {
    if ($_.FullName -match '\\(service|repository|domain|dto|controller)\\(journal|statement)\\') { return }
    $c = [System.IO.File]::ReadAllText($_.FullName)
    foreach ($sym in $symbols.Keys) {
        if ($c -match "\b$sym\b" -and $c -notmatch [regex]::Escape($symbols[$sym])) {
            Add-ImportIfMissing $_.FullName $symbols[$sym]
            $c = [System.IO.File]::ReadAllText($_.FullName)
        }
    }
}

# Add own-interface imports for impl classes in same domain
foreach ($pair in @(
    @{ Impl='JournalAccountServiceImpl.java'; Domain='journal'; Ifaces=@('JournalAccountService') },
    @{ Impl='JournalEntryServiceImpl.java'; Domain='journal'; Ifaces=@('JournalEntryService') },
    @{ Impl='JournalSummaryServiceImpl.java'; Domain='journal'; Ifaces=@('JournalSummaryService') },
    @{ Impl='StatementBalanceSheetServiceImpl.java'; Domain='statement'; Ifaces=@('StatementBalanceSheetService') },
    @{ Impl='StatementBalanceSheetConfigServiceImpl.java'; Domain='statement'; Ifaces=@('StatementBalanceSheetConfigService') },
    @{ Impl='StatementCashFlowServiceImpl.java'; Domain='statement'; Ifaces=@('StatementCashFlowService') },
    @{ Impl='StatementIncomeServiceImpl.java'; Domain='statement'; Ifaces=@('StatementIncomeService') },
    @{ Impl='StatementIncomeConfigServiceImpl.java'; Domain='statement'; Ifaces=@('StatementIncomeConfigService') },
    @{ Impl='StatementReportServiceImpl.java'; Domain='statement'; Ifaces=@('StatementReportService') },
    @{ Impl='StatementSubjectBalanceServiceImpl.java'; Domain='statement'; Ifaces=@('StatementSubjectBalanceService') }
)) {
    $f = "$base\service\$($pair.Domain)\impl\$($pair.Impl)"
    if (-not (Test-Path $f)) { continue }
    foreach ($iface in $pair.Ifaces) {
        Add-ImportIfMissing $f "import com.financial.cloud.service.$($pair.Domain).$iface;"
    }
}

# Also add sibling service imports within statement/journal impls that use wildcards
Get-ChildItem "$base\service\journal\impl\*.java","$base\service\statement\impl\*.java" | ForEach-Object {
    $c = [System.IO.File]::ReadAllText($_.FullName)
    foreach ($sym in $symbols.Keys) {
        if ($c -match "\b$sym\b" -and $c -notmatch [regex]::Escape($symbols[$sym])) {
            Add-ImportIfMissing $_.FullName $symbols[$sym]
        }
    }
}

# Cleanup empty dirs
@(
    "$base\entity\journal\dto","$base\entity\journal\vo","$base\entity\journal",
    "$base\entity\statement\dto","$base\entity\statement\vo","$base\entity\statement",
    "$base\web\journal\controller","$base\web\journal",
    "$base\web\statement\controller","$base\web\statement"
) | ForEach-Object {
    if ((Test-Path $_) -and -not (Get-ChildItem $_ -Force -ErrorAction SilentlyContinue)) {
        Remove-Item $_ -Force -Recurse -ErrorAction SilentlyContinue
    }
}

Write-Host "Wave 3 journal+statement migration complete."
