# Wave 3: journal + statement full stack migration
$ErrorActionPreference = "Stop"
$base = "C:\Users\Administrator\Projects\jinbooks\jinbooks\src\main\java\com\jinbooks"
$resBase = "C:\Users\Administrator\Projects\jinbooks\jinbooks\src\main\resources"
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
            Set-Package "$base\domain\$domain\$($_.Name)" "com.jinbooks.domain.$domain"
        }
        Get-ChildItem "$base\entity\$dir\dto\*.java","$base\entity\$dir\vo\*.java" -ErrorAction SilentlyContinue | ForEach-Object {
            Move-File $_.FullName "$base\dto\$domain\$($_.Name)"
            Set-Package "$base\dto\$domain\$($_.Name)" "com.jinbooks.dto.$domain"
        }
    }

    foreach ($c in $controllers) {
        Move-File "$base\web\$domain\controller\$c" "$base\controller\$domain\$c"
        Set-Package "$base\controller\$domain\$c" "com.jinbooks.controller.$domain"
    }

    foreach ($m in $mappers) {
        Move-File "$base\persistence\mapper\$m" "$base\repository\$domain\$m"
        Set-Package "$base\repository\$domain\$m" "com.jinbooks.repository.$domain"
    }

    foreach ($s in $services) {
        Move-File "$base\persistence\service\$s" "$base\service\$domain\$s"
        Set-Package "$base\service\$domain\$s" "com.jinbooks.service.$domain"
    }
    foreach ($i in $impls) {
        Move-File "$base\persistence\service\impl\$i" "$base\service\$domain\impl\$i"
        Set-Package "$base\service\$domain\impl\$i" "com.jinbooks.service.$domain.impl"
    }

    $xmlDst = "$resBase\com\jinbooks\repository\$domain\xml\mysql"
    Ensure-Dir $xmlDst
    foreach ($x in $xmlFiles) {
        $src = "$resBase\com\jinbooks\persistence\mapper\xml\mysql\$x"
        if (Test-Path $src) {
            Move-File $src "$xmlDst\$x"
            $c = [System.IO.File]::ReadAllText("$xmlDst\$x")
            $c = $c -replace 'com\.jinbooks\.persistence\.mapper\.', "com.jinbooks.repository.$domain."
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
    'com.jinbooks.entity.journal.dto.' = 'com.jinbooks.dto.journal.'
    'com.jinbooks.entity.journal.vo.'  = 'com.jinbooks.dto.journal.'
    'com.jinbooks.entity.journal.'     = 'com.jinbooks.domain.journal.'
    'com.jinbooks.entity.statement.dto.' = 'com.jinbooks.dto.statement.'
    'com.jinbooks.entity.statement.vo.'  = 'com.jinbooks.dto.statement.'
    'com.jinbooks.entity.statement.'     = 'com.jinbooks.domain.statement.'
    'com.jinbooks.web.journal.controller.' = 'com.jinbooks.controller.journal.'
    'com.jinbooks.web.statement.controller.' = 'com.jinbooks.controller.statement.'

    'com.jinbooks.persistence.mapper.JournalAccount' = 'com.jinbooks.repository.journal.JournalAccount'
    'com.jinbooks.persistence.mapper.JournalEntry' = 'com.jinbooks.repository.journal.JournalEntry'
    'com.jinbooks.persistence.mapper.JournalSummary' = 'com.jinbooks.repository.journal.JournalSummary'
    'com.jinbooks.persistence.mapper.StatementBalanceSheetItem' = 'com.jinbooks.repository.statement.StatementBalanceSheetItem'
    'com.jinbooks.persistence.mapper.StatementBalanceSheet' = 'com.jinbooks.repository.statement.StatementBalanceSheet'
    'com.jinbooks.persistence.mapper.StatementCashFlow' = 'com.jinbooks.repository.statement.StatementCashFlow'
    'com.jinbooks.persistence.mapper.StatementIncomeItem' = 'com.jinbooks.repository.statement.StatementIncomeItem'
    'com.jinbooks.persistence.mapper.StatementIncome' = 'com.jinbooks.repository.statement.StatementIncome'
    'com.jinbooks.persistence.mapper.StatementRules' = 'com.jinbooks.repository.statement.StatementRules'
    'com.jinbooks.persistence.mapper.StatementSubjectBalance' = 'com.jinbooks.repository.statement.StatementSubjectBalance'

    'com.jinbooks.persistence.service.impl.JournalAccount' = 'com.jinbooks.service.journal.impl.JournalAccount'
    'com.jinbooks.persistence.service.impl.JournalEntry' = 'com.jinbooks.service.journal.impl.JournalEntry'
    'com.jinbooks.persistence.service.impl.JournalSummary' = 'com.jinbooks.service.journal.impl.JournalSummary'
    'com.jinbooks.persistence.service.impl.StatementBalanceSheetConfig' = 'com.jinbooks.service.statement.impl.StatementBalanceSheetConfig'
    'com.jinbooks.persistence.service.impl.StatementBalanceSheet' = 'com.jinbooks.service.statement.impl.StatementBalanceSheet'
    'com.jinbooks.persistence.service.impl.StatementCashFlow' = 'com.jinbooks.service.statement.impl.StatementCashFlow'
    'com.jinbooks.persistence.service.impl.StatementIncomeConfig' = 'com.jinbooks.service.statement.impl.StatementIncomeConfig'
    'com.jinbooks.persistence.service.impl.StatementIncome' = 'com.jinbooks.service.statement.impl.StatementIncome'
    'com.jinbooks.persistence.service.impl.StatementReport' = 'com.jinbooks.service.statement.impl.StatementReport'
    'com.jinbooks.persistence.service.impl.StatementSubjectBalance' = 'com.jinbooks.service.statement.impl.StatementSubjectBalance'

    'com.jinbooks.persistence.service.JournalAccount' = 'com.jinbooks.service.journal.JournalAccount'
    'com.jinbooks.persistence.service.JournalEntry' = 'com.jinbooks.service.journal.JournalEntry'
    'com.jinbooks.persistence.service.JournalSummary' = 'com.jinbooks.service.journal.JournalSummary'
    'com.jinbooks.persistence.service.StatementBalanceSheetConfig' = 'com.jinbooks.service.statement.StatementBalanceSheetConfig'
    'com.jinbooks.persistence.service.StatementBalanceSheet' = 'com.jinbooks.service.statement.StatementBalanceSheet'
    'com.jinbooks.persistence.service.StatementCashFlow' = 'com.jinbooks.service.statement.StatementCashFlow'
    'com.jinbooks.persistence.service.StatementIncomeConfig' = 'com.jinbooks.service.statement.StatementIncomeConfig'
    'com.jinbooks.persistence.service.StatementIncome' = 'com.jinbooks.service.statement.StatementIncome'
    'com.jinbooks.persistence.service.StatementReport' = 'com.jinbooks.service.statement.StatementReport'
    'com.jinbooks.persistence.service.StatementSubjectBalance' = 'com.jinbooks.service.statement.StatementSubjectBalance'
}

Get-ChildItem -Path $srcRoot -Recurse -Include *.java,*.xml | ForEach-Object {
    $c = [System.IO.File]::ReadAllText($_.FullName)
    $orig = $c
    foreach ($k in $replacements.Keys) { $c = $c.Replace($k, $replacements[$k]) }
    if ($c -ne $orig) { [System.IO.File]::WriteAllText($_.FullName, $c) }
}

# Cross-domain symbol imports
$symbols = [ordered]@{
    'JournalAccountService' = 'import com.jinbooks.service.journal.JournalAccountService;'
    'JournalEntryService' = 'import com.jinbooks.service.journal.JournalEntryService;'
    'JournalSummaryService' = 'import com.jinbooks.service.journal.JournalSummaryService;'
    'JournalAccountMapper' = 'import com.jinbooks.repository.journal.JournalAccountMapper;'
    'JournalEntryMapper' = 'import com.jinbooks.repository.journal.JournalEntryMapper;'
    'JournalSummaryMapper' = 'import com.jinbooks.repository.journal.JournalSummaryMapper;'
    'StatementBalanceSheetConfigService' = 'import com.jinbooks.service.statement.StatementBalanceSheetConfigService;'
    'StatementBalanceSheetService' = 'import com.jinbooks.service.statement.StatementBalanceSheetService;'
    'StatementCashFlowService' = 'import com.jinbooks.service.statement.StatementCashFlowService;'
    'StatementIncomeConfigService' = 'import com.jinbooks.service.statement.StatementIncomeConfigService;'
    'StatementIncomeService' = 'import com.jinbooks.service.statement.StatementIncomeService;'
    'StatementReportService' = 'import com.jinbooks.service.statement.StatementReportService;'
    'StatementSubjectBalanceService' = 'import com.jinbooks.service.statement.StatementSubjectBalanceService;'
    'StatementBalanceSheetMapper' = 'import com.jinbooks.repository.statement.StatementBalanceSheetMapper;'
    'StatementBalanceSheetItemMapper' = 'import com.jinbooks.repository.statement.StatementBalanceSheetItemMapper;'
    'StatementCashFlowMapper' = 'import com.jinbooks.repository.statement.StatementCashFlowMapper;'
    'StatementIncomeMapper' = 'import com.jinbooks.repository.statement.StatementIncomeMapper;'
    'StatementIncomeItemMapper' = 'import com.jinbooks.repository.statement.StatementIncomeItemMapper;'
    'StatementRulesMapper' = 'import com.jinbooks.repository.statement.StatementRulesMapper;'
    'StatementSubjectBalanceMapper' = 'import com.jinbooks.repository.statement.StatementSubjectBalanceMapper;'
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
        Add-ImportIfMissing $f "import com.jinbooks.service.$($pair.Domain).$iface;"
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
