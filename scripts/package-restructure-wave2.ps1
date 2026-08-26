# Wave 2: book (+ base) full stack migration
$ErrorActionPreference = "Stop"
$base = "C:\Users\Administrator\Projects\jinbooks\financial-cloud\src\main\java\com\jinbooks"
$resBase = "C:\Users\Administrator\Projects\jinbooks\financial-cloud\src\main\resources"
$srcRoot = "C:\Users\Administrator\Projects\jinbooks\jinbooks\src"

function Ensure-Dir($p) { New-Item -ItemType Directory -Force -Path $p | Out-Null }
function Move-File($src, $dst) {
    if (-not (Test-Path $src)) { Write-Warning "Skip missing: $src"; return }
    Ensure-Dir (Split-Path $dst -Parent)
    Move-Item -Force $src $dst
    Write-Host "Moved -> $dst"
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

# --- domain entities (book + base) ---
Ensure-Dir "$base\domain\book"
$entities = @(
    @{ Src = "$base\entity\book\Book.java"; Name = "Book.java" },
    @{ Src = "$base\entity\book\BookSubject.java"; Name = "BookSubject.java" },
    @{ Src = "$base\entity\book\Settlement.java"; Name = "Settlement.java" },
    @{ Src = "$base\entity\book\SettlementCarryforward.java"; Name = "SettlementCarryforward.java" },
    @{ Src = "$base\entity\base\AssistAcc.java"; Name = "AssistAcc.java" },
    @{ Src = "$base\entity\base\BookInitBalance.java"; Name = "BookInitBalance.java" }
)
foreach ($e in $entities) {
    Move-File $e.Src "$base\domain\book\$($e.Name)"
    Set-Package "$base\domain\book\$($e.Name)" "com.financial.cloud.domain.book"
}

# --- dto (book dto/vo + base dto/vo) ---
Ensure-Dir "$base\dto\book"
Get-ChildItem "$base\entity\book\dto\*.java","$base\entity\book\vo\*.java","$base\entity\base\dto\*.java","$base\entity\base\vo\*.java" -ErrorAction SilentlyContinue | ForEach-Object {
    Move-File $_.FullName "$base\dto\book\$($_.Name)"
    Set-Package "$base\dto\book\$($_.Name)" "com.financial.cloud.dto.book"
}

# --- controllers ---
Ensure-Dir "$base\controller\book"
Get-ChildItem "$base\web\book\controller\*.java" -ErrorAction SilentlyContinue | ForEach-Object {
    Move-File $_.FullName "$base\controller\book\$($_.Name)"
    Set-Package "$base\controller\book\$($_.Name)" "com.financial.cloud.controller.book"
}
# AssistAcc / BookInitBalance live under web.config but belong to book domain
foreach ($c in @("AssistAccController.java","BookInitBalanceController.java")) {
    Move-File "$base\web\config\controller\$c" "$base\controller\book\$c"
    Set-Package "$base\controller\book\$c" "com.financial.cloud.controller.book"
}

# --- repository mappers ---
Ensure-Dir "$base\repository\book"
$mappers = @(
    "BookMapper.java","BookSubjectMapper.java","SettlementMapper.java",
    "SettlementCarryforwardMapper.java","AssistAccMapper.java","BookInitBalanceMapper.java"
)
foreach ($m in $mappers) {
    Move-File "$base\persistence\mapper\$m" "$base\repository\book\$m"
    Set-Package "$base\repository\book\$m" "com.financial.cloud.repository.book"
}

# --- services ---
Ensure-Dir "$base\service\book\impl"
$services = @(
    "BookService.java","BookSubjectService.java","SettlementService.java",
    "SettlementCarryService.java","AssistAccService.java","BookInitBalanceService.java"
)
foreach ($s in $services) {
    Move-File "$base\persistence\service\$s" "$base\service\book\$s"
    Set-Package "$base\service\book\$s" "com.financial.cloud.service.book"
}
$impls = @(
    "BookServiceImpl.java","BookSubjectServiceImpl.java","SettlementServiceImpl.java",
    "SettlementCarryServiceImpl.java","AssistAccServiceImpl.java","BookInitBalanceServiceImpl.java"
)
foreach ($i in $impls) {
    Move-File "$base\persistence\service\impl\$i" "$base\service\book\impl\$i"
    Set-Package "$base\service\book\impl\$i" "com.financial.cloud.service.book.impl"
}

# --- XML ---
$xmlDst = "$resBase\com\jinbooks\repository\book\xml\mysql"
Ensure-Dir $xmlDst
$xmlFiles = @("BookMapper.xml","BookSubjectMapper.xml","SettlementMapper.xml","SettlementCarryforwardMapper.xml","AssistAccMapper.xml","BookInitBalanceMapper.xml")
foreach ($x in $xmlFiles) {
    $src = "$resBase\com\jinbooks\persistence\mapper\xml\mysql\$x"
    if (Test-Path $src) {
        Move-File $src "$xmlDst\$x"
        $c = [System.IO.File]::ReadAllText("$xmlDst\$x")
        $c = $c -replace 'com\.jinbooks\.persistence\.mapper\.', 'com.financial.cloud.repository.book.'
        [System.IO.File]::WriteAllText("$xmlDst\$x", $c)
    }
}

# --- global import replacements (longest / most specific first) ---
$replacements = [ordered]@{
    'com.financial.cloud.entity.book.dto.' = 'com.financial.cloud.dto.book.'
    'com.financial.cloud.entity.book.vo.'  = 'com.financial.cloud.dto.book.'
    'com.financial.cloud.entity.base.dto.' = 'com.financial.cloud.dto.book.'
    'com.financial.cloud.entity.base.vo.'  = 'com.financial.cloud.dto.book.'
    'com.financial.cloud.entity.book.'     = 'com.financial.cloud.domain.book.'
    'com.financial.cloud.entity.base.'     = 'com.financial.cloud.domain.book.'
    'com.financial.cloud.web.book.controller.' = 'com.financial.cloud.controller.book.'
    'com.financial.cloud.web.config.controller.AssistAccController' = 'com.financial.cloud.controller.book.AssistAccController'
    'com.financial.cloud.web.config.controller.BookInitBalanceController' = 'com.financial.cloud.controller.book.BookInitBalanceController'
    'com.financial.cloud.persistence.mapper.BookSubject' = 'com.financial.cloud.repository.book.BookSubject'
    'com.financial.cloud.persistence.mapper.BookInitBalance' = 'com.financial.cloud.repository.book.BookInitBalance'
    'com.financial.cloud.persistence.mapper.Book' = 'com.financial.cloud.repository.book.Book'
    'com.financial.cloud.persistence.mapper.SettlementCarryforward' = 'com.financial.cloud.repository.book.SettlementCarryforward'
    'com.financial.cloud.persistence.mapper.Settlement' = 'com.financial.cloud.repository.book.Settlement'
    'com.financial.cloud.persistence.mapper.AssistAcc' = 'com.financial.cloud.repository.book.AssistAcc'
    'com.financial.cloud.persistence.service.impl.BookSubject' = 'com.financial.cloud.service.book.impl.BookSubject'
    'com.financial.cloud.persistence.service.impl.BookInitBalance' = 'com.financial.cloud.service.book.impl.BookInitBalance'
    'com.financial.cloud.persistence.service.impl.Book' = 'com.financial.cloud.service.book.impl.Book'
    'com.financial.cloud.persistence.service.impl.SettlementCarry' = 'com.financial.cloud.service.book.impl.SettlementCarry'
    'com.financial.cloud.persistence.service.impl.Settlement' = 'com.financial.cloud.service.book.impl.Settlement'
    'com.financial.cloud.persistence.service.impl.AssistAcc' = 'com.financial.cloud.service.book.impl.AssistAcc'
    'com.financial.cloud.persistence.service.BookSubject' = 'com.financial.cloud.service.book.BookSubject'
    'com.financial.cloud.persistence.service.BookInitBalance' = 'com.financial.cloud.service.book.BookInitBalance'
    'com.financial.cloud.persistence.service.Book' = 'com.financial.cloud.service.book.Book'
    'com.financial.cloud.persistence.service.SettlementCarry' = 'com.financial.cloud.service.book.SettlementCarry'
    'com.financial.cloud.persistence.service.Settlement' = 'com.financial.cloud.service.book.Settlement'
    'com.financial.cloud.persistence.service.AssistAcc' = 'com.financial.cloud.service.book.AssistAcc'
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

# Cross-domain: add explicit imports where wildcard persistence.service.* was used
$symbols = [ordered]@{
    'BookService' = 'import com.financial.cloud.service.book.BookService;'
    'BookSubjectService' = 'import com.financial.cloud.service.book.BookSubjectService;'
    'SettlementService' = 'import com.financial.cloud.service.book.SettlementService;'
    'SettlementCarryService' = 'import com.financial.cloud.service.book.SettlementCarryService;'
    'AssistAccService' = 'import com.financial.cloud.service.book.AssistAccService;'
    'BookInitBalanceService' = 'import com.financial.cloud.service.book.BookInitBalanceService;'
    'BookMapper' = 'import com.financial.cloud.repository.book.BookMapper;'
    'BookSubjectMapper' = 'import com.financial.cloud.repository.book.BookSubjectMapper;'
    'SettlementMapper' = 'import com.financial.cloud.repository.book.SettlementMapper;'
    'SettlementCarryforwardMapper' = 'import com.financial.cloud.repository.book.SettlementCarryforwardMapper;'
    'AssistAccMapper' = 'import com.financial.cloud.repository.book.AssistAccMapper;'
    'BookInitBalanceMapper' = 'import com.financial.cloud.repository.book.BookInitBalanceMapper;'
}

Get-ChildItem -Path "$srcRoot\main\java" -Recurse -Filter *.java | ForEach-Object {
    # skip files already in service.book / repository.book / domain.book / dto.book / controller.book
    $rel = $_.FullName
    if ($rel -match '\\(service|repository|domain|dto|controller)\\book\\') { return }
    $c = [System.IO.File]::ReadAllText($_.FullName)
    $changed = $false
    foreach ($sym in $symbols.Keys) {
        if ($c -match "\b$sym\b" -and $c -notmatch [regex]::Escape($symbols[$sym])) {
            Add-ImportIfMissing $_.FullName $symbols[$sym]
            $c = [System.IO.File]::ReadAllText($_.FullName)
            $changed = $true
        }
    }
}

# Cleanup empty dirs
@(
    "$base\entity\book\dto","$base\entity\book\vo","$base\entity\book",
    "$base\entity\base\dto","$base\entity\base\vo","$base\entity\base",
    "$base\web\book\controller","$base\web\book"
) | ForEach-Object {
    if ((Test-Path $_) -and -not (Get-ChildItem $_ -Force -ErrorAction SilentlyContinue | Where-Object { $_.Name -ne '.' -and $_.Name -ne '..' })) {
        Remove-Item $_ -Force -Recurse -ErrorAction SilentlyContinue
        Write-Host "Removed empty $_"
    }
}

Write-Host "Wave 2 book migration complete."
