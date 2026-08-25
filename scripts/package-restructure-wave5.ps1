# Wave 5: config + hr + standard + history + report
$ErrorActionPreference = "Stop"
$base = "C:\Users\Administrator\Projects\jinbooks\jinbooks\src\main\java\com\jinbooks"
$resBase = "C:\Users\Administrator\Projects\jinbooks\jinbooks\src\main\resources"
$srcRoot = "C:\Users\Administrator\Projects\jinbooks\jinbooks\src"

function Ensure-Dir($p) { New-Item -ItemType Directory -Force -Path $p | Out-Null }
function Set-Package($file, $pkg) {
    if (-not (Test-Path $file)) { return }
    $c = [System.IO.File]::ReadAllText($file)
    $c = $c -replace 'package\s+[\w.]+;', "package $pkg;"
    [System.IO.File]::WriteAllText($file, $c)
}
function Move-Set($src, $dst, $pkg) {
    if (-not (Test-Path $src)) { Write-Warning "Skip missing: $src"; return }
    Ensure-Dir (Split-Path $dst -Parent)
    Move-Item -Force $src $dst
    Set-Package $dst $pkg
    Write-Host "Moved -> $(Split-Path $dst -Leaf)"
}
function Move-Xml($name, $domain) {
    $src = "$resBase\com\jinbooks\persistence\mapper\xml\mysql\$name"
    if (-not (Test-Path $src)) { return }
    $dst = "$resBase\com\jinbooks\repository\$domain\xml\mysql\$name"
    Ensure-Dir (Split-Path $dst -Parent)
    Move-Item -Force $src $dst
    $c = [System.IO.File]::ReadAllText($dst)
    $c = $c -replace 'com\.jinbooks\.persistence\.mapper\.', "com.jinbooks.repository.$domain."
    [System.IO.File]::WriteAllText($dst, $c)
    Write-Host "XML -> $domain/$name"
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

# ========== CONFIG ==========
Ensure-Dir "$base\domain\config"; Ensure-Dir "$base\dto\config"; Ensure-Dir "$base\controller\config"
Ensure-Dir "$base\repository\config"; Ensure-Dir "$base\service\config\impl"

foreach ($e in @("ConfigSys.java","ConfigCashFlowBalance.java","ConfigInsuranceFund.java","ConfigPersonalTax.java","ConfigSalaryFormula.java")) {
    Move-Set "$base\entity\config\$e" "$base\domain\config\$e" "com.jinbooks.domain.config"
}
Move-Set "$base\entity\Institutions.java" "$base\domain\config\Institutions.java" "com.jinbooks.domain.config"

Get-ChildItem "$base\entity\config\dto\*.java","$base\entity\config\vo\*.java" -ErrorAction SilentlyContinue | ForEach-Object {
    Move-Set $_.FullName "$base\dto\config\$($_.Name)" "com.jinbooks.dto.config"
}
Move-Set "$base\entity\dto\InstitutionsPageDto.java" "$base\dto\config\InstitutionsPageDto.java" "com.jinbooks.dto.config"
Move-Set "$base\entity\dto\ExpandAttrsPageDto.java" "$base\dto\config\ExpandAttrsPageDto.java" "com.jinbooks.dto.config" # may already be gone

foreach ($c in @("ConfigSysController.java","ConfigCashFlowBalanceController.java","ConfigInsuranceFundController.java","ConfigPersonalTaxController.java","ConfigSalaryFormulaController.java","InstitutionsController.java")) {
    Move-Set "$base\web\config\controller\$c" "$base\controller\config\$c" "com.jinbooks.controller.config"
}

foreach ($m in @("ConfigSysMapper.java","ConfigCashFlowBalanceMapper.java","ConfigInsuranceFundMapper.java","ConfigPersonalTaxMapper.java","ConfigSalaryFormulaMapper.java","InstitutionsMapper.java")) {
    Move-Set "$base\persistence\mapper\$m" "$base\repository\config\$m" "com.jinbooks.repository.config"
}
foreach ($s in @("ConfigSysService.java","ConfigService.java","ConfigCashFlowBalanceService.java","ConfigInsuranceFundService.java","ConfigPersonalTaxService.java","ConfigSalaryFormulaService.java","InstitutionsService.java")) {
    Move-Set "$base\persistence\service\$s" "$base\service\config\$s" "com.jinbooks.service.config"
}
foreach ($i in @("ConfigSysServiceImpl.java","ConfigCashFlowBalanceServiceImpl.java","ConfigInsuranceFundServiceImpl.java","ConfigPersonalTaxServiceImpl.java","ConfigSalaryFormulaServiceImpl.java","InstitutionsServiceImpl.java")) {
    Move-Set "$base\persistence\service\impl\$i" "$base\service\config\impl\$i" "com.jinbooks.service.config.impl"
}
Move-Xml "InstitutionsMapper.xml" "config"

# ========== HR ==========
Ensure-Dir "$base\domain\hr"; Ensure-Dir "$base\dto\hr"; Ensure-Dir "$base\controller\hr"
Ensure-Dir "$base\repository\hr"; Ensure-Dir "$base\service\hr\impl"

Get-ChildItem "$base\entity\hr\*.java" -ErrorAction SilentlyContinue | ForEach-Object {
    Move-Set $_.FullName "$base\domain\hr\$($_.Name)" "com.jinbooks.domain.hr"
}
Get-ChildItem "$base\entity\hr\dto\*.java","$base\entity\hr\vo\*.java" -ErrorAction SilentlyContinue | ForEach-Object {
    Move-Set $_.FullName "$base\dto\hr\$($_.Name)" "com.jinbooks.dto.hr"
}
foreach ($c in @("EmployeeController.java","EmployeeSalaryController.java","EmployeeSalarySummaryController.java","EmployeeSalaryTempController.java","EmployeeTaxDeductionController.java")) {
    Move-Set "$base\web\hr\controller\$c" "$base\controller\hr\$c" "com.jinbooks.controller.hr"
}
foreach ($m in @("EmployeeMapper.java","EmployeeSalaryMapper.java","EmployeeSalarySummaryMapper.java","EmployeeSalaryTempMapper.java","EmployeeTaxDeductionMapper.java")) {
    Move-Set "$base\persistence\mapper\$m" "$base\repository\hr\$m" "com.jinbooks.repository.hr"
}
foreach ($s in @("EmployeeService.java","EmployeeSalaryService.java","EmployeeSalarySummaryService.java","EmployeeSalaryTempService.java","EmployeeTaxDeductionService.java")) {
    Move-Set "$base\persistence\service\$s" "$base\service\hr\$s" "com.jinbooks.service.hr"
}
foreach ($i in @("EmployeeServiceImpl.java","EmployeeSalaryServiceImpl.java","EmployeeSalarySummaryServiceImpl.java","EmployeeSalaryTempServiceImpl.java","EmployeeTaxDeductionServiceImpl.java")) {
    Move-Set "$base\persistence\service\impl\$i" "$base\service\hr\impl\$i" "com.jinbooks.service.hr.impl"
}
foreach ($x in @("EmployeeMapper.xml","EmployeeSalaryMapper.xml","EmployeeSalarySummaryMapper.xml","EmployeeSalaryTempMapper.xml","EmployeeTaxDeductionMapper.xml")) { Move-Xml $x "hr" }

# ========== STANDARD ==========
Ensure-Dir "$base\domain\standard"; Ensure-Dir "$base\dto\standard"; Ensure-Dir "$base\controller\standard"
Ensure-Dir "$base\repository\standard"; Ensure-Dir "$base\service\standard\impl"

Get-ChildItem "$base\entity\standard\*.java" -ErrorAction SilentlyContinue | ForEach-Object {
    Move-Set $_.FullName "$base\domain\standard\$($_.Name)" "com.jinbooks.domain.standard"
}
Get-ChildItem "$base\entity\standard\dto\*.java","$base\entity\standard\vo\*.java" -ErrorAction SilentlyContinue | ForEach-Object {
    Move-Set $_.FullName "$base\dto\standard\$($_.Name)" "com.jinbooks.dto.standard"
}
foreach ($c in @("StandardController.java","StandardSubjectController.java","StandardSubjectCashFlowController.java","StandardStatementBalanceSheetController.java","StandardStatementIncomeController.java")) {
    Move-Set "$base\web\standard\controller\$c" "$base\controller\standard\$c" "com.jinbooks.controller.standard"
}
foreach ($m in @("StandardMapper.java","StandardSubjectMapper.java","StandardSubjectCashFlowMapper.java","StandardStatementBalanceSheetMapper.java","StandardStatementIncomeMapper.java","StandardStatementRulesMapper.java")) {
    Move-Set "$base\persistence\mapper\$m" "$base\repository\standard\$m" "com.jinbooks.repository.standard"
}
foreach ($s in @("StandardService.java","StandardSubjectService.java","StandardSubjectCashFlowService.java","StandardStatementBalanceSheetService.java","StandardStatementIncomeService.java")) {
    Move-Set "$base\persistence\service\$s" "$base\service\standard\$s" "com.jinbooks.service.standard"
}
foreach ($i in @("StandardServiceImpl.java","StandardSubjectServiceImpl.java","StandardSubjectCashFlowServiceImpl.java","StandardStatementBalanceSheetServiceImpl.java","StandardStatementIncomeServiceImpl.java")) {
    Move-Set "$base\persistence\service\impl\$i" "$base\service\standard\impl\$i" "com.jinbooks.service.standard.impl"
}
foreach ($x in @("StandardMapper.xml","StandardSubjectMapper.xml","standardSubjectCashFlowMapper.xml","StandardStatementBalanceSheetMapper.xml","StandardStatementRulesMapper.xml")) { Move-Xml $x "standard" }

# ========== HISTORY ==========
Ensure-Dir "$base\domain\history"; Ensure-Dir "$base\dto\history"; Ensure-Dir "$base\controller\history"
Ensure-Dir "$base\repository\history"; Ensure-Dir "$base\service\history\impl"

Get-ChildItem "$base\entity\history\*.java" -ErrorAction SilentlyContinue | ForEach-Object {
    Move-Set $_.FullName "$base\domain\history\$($_.Name)" "com.jinbooks.domain.history"
}
Get-ChildItem "$base\entity\history\dto\*.java" -ErrorAction SilentlyContinue | ForEach-Object {
    Move-Set $_.FullName "$base\dto\history\$($_.Name)" "com.jinbooks.dto.history"
}
# historys web package (with s)
foreach ($c in @("LoginHistoryController.java","SystemLogsController.java","SynchronizerHistoryController.java","ConnectorHistoryController.java")) {
    Move-Set "$base\web\historys\controller\$c" "$base\controller\history\$c" "com.jinbooks.controller.history"
}
# HistorySynchronizerPageDto under web.historys.controller.dto if exists
Get-ChildItem "$base\web\historys\controller\dto\*.java" -ErrorAction SilentlyContinue | ForEach-Object {
    Move-Set $_.FullName "$base\dto\history\$($_.Name)" "com.jinbooks.dto.history"
}

foreach ($m in @("HistoryLoginMapper.java","HistoryLoginAppsMapper.java","HistorySystemLogsMapper.java","HistorySynchronizerMapper.java","HistoryConnectorMapper.java")) {
    Move-Set "$base\persistence\mapper\$m" "$base\repository\history\$m" "com.jinbooks.repository.history"
}
foreach ($s in @("HistoryLoginService.java","HistorySystemLogsService.java","HistorySynchronizerService.java","HistoryConnectorService.java")) {
    Move-Set "$base\persistence\service\$s" "$base\service\history\$s" "com.jinbooks.service.history"
}
foreach ($i in @("HistoryLoginServiceImpl.java","HistorySystemLogsServiceImpl.java","HistorySynchronizerServiceImpl.java","HistoryConnectorServiceImpl.java")) {
    Move-Set "$base\persistence\service\impl\$i" "$base\service\history\impl\$i" "com.jinbooks.service.history.impl"
}
foreach ($x in @("HistoryLoginMapper.xml","HistoryLoginAppsMapper.xml","HistorySystemLogsMapper.xml","HistorySynchronizerMapper.xml","HistoryConnectorMapper.xml")) { Move-Xml $x "history" }

# ========== REPORT (report + fund + dashboards) ==========
Ensure-Dir "$base\domain\report"; Ensure-Dir "$base\dto\report"; Ensure-Dir "$base\controller\report"
Ensure-Dir "$base\repository\report"; Ensure-Dir "$base\service\report\impl"

Get-ChildItem "$base\entity\report\dto\*.java","$base\entity\report\vo\*.java","$base\entity\fund\*.java" -ErrorAction SilentlyContinue | ForEach-Object {
    Move-Set $_.FullName "$base\dto\report\$($_.Name)" "com.jinbooks.dto.report"
}
# CashFlowSubjectBalanceVo under entity/vo
Move-Set "$base\entity\vo\CashFlowSubjectBalanceVo.java" "$base\dto\report\CashFlowSubjectBalanceVo.java" "com.jinbooks.dto.report"

foreach ($c in @("DashboardController.java","FundDashboardController.java")) {
    Move-Set "$base\web\controller\$c" "$base\controller\report\$c" "com.jinbooks.controller.report"
}
Move-Set "$base\persistence\mapper\ReportMapper.java" "$base\repository\report\ReportMapper.java" "com.jinbooks.repository.report"
foreach ($s in @("ReportService.java","FundDashboardService.java")) {
    Move-Set "$base\persistence\service\$s" "$base\service\report\$s" "com.jinbooks.service.report"
}
foreach ($i in @("ReportServiceImpl.java","FundDashboardServiceImpl.java")) {
    Move-Set "$base\persistence\service\impl\$i" "$base\service\report\impl\$i" "com.jinbooks.service.report.impl"
}
Move-Xml "ReportMapper.xml" "report"

# ========== replacements ==========
$replacements = [ordered]@{
    'com.jinbooks.entity.config.dto.' = 'com.jinbooks.dto.config.'
    'com.jinbooks.entity.config.vo.' = 'com.jinbooks.dto.config.'
    'com.jinbooks.entity.config.' = 'com.jinbooks.domain.config.'
    'com.jinbooks.entity.Institutions' = 'com.jinbooks.domain.config.Institutions'
    'com.jinbooks.entity.dto.InstitutionsPageDto' = 'com.jinbooks.dto.config.InstitutionsPageDto'
    'com.jinbooks.web.config.controller.' = 'com.jinbooks.controller.config.'

    'com.jinbooks.entity.hr.dto.' = 'com.jinbooks.dto.hr.'
    'com.jinbooks.entity.hr.vo.' = 'com.jinbooks.dto.hr.'
    'com.jinbooks.entity.hr.' = 'com.jinbooks.domain.hr.'
    'com.jinbooks.web.hr.controller.' = 'com.jinbooks.controller.hr.'

    'com.jinbooks.entity.standard.dto.' = 'com.jinbooks.dto.standard.'
    'com.jinbooks.entity.standard.vo.' = 'com.jinbooks.dto.standard.'
    'com.jinbooks.entity.standard.' = 'com.jinbooks.domain.standard.'
    'com.jinbooks.web.standard.controller.' = 'com.jinbooks.controller.standard.'

    'com.jinbooks.entity.history.dto.' = 'com.jinbooks.dto.history.'
    'com.jinbooks.entity.history.' = 'com.jinbooks.domain.history.'
    'com.jinbooks.web.historys.controller.dto.' = 'com.jinbooks.dto.history.'
    'com.jinbooks.web.historys.controller.' = 'com.jinbooks.controller.history.'

    'com.jinbooks.entity.report.dto.' = 'com.jinbooks.dto.report.'
    'com.jinbooks.entity.report.vo.' = 'com.jinbooks.dto.report.'
    'com.jinbooks.entity.fund.' = 'com.jinbooks.dto.report.'
    'com.jinbooks.entity.vo.CashFlowSubjectBalanceVo' = 'com.jinbooks.dto.report.CashFlowSubjectBalanceVo'
    'com.jinbooks.web.controller.DashboardController' = 'com.jinbooks.controller.report.DashboardController'
    'com.jinbooks.web.controller.FundDashboardController' = 'com.jinbooks.controller.report.FundDashboardController'

    # mappers longest first
    'com.jinbooks.persistence.mapper.ConfigCashFlowBalance' = 'com.jinbooks.repository.config.ConfigCashFlowBalance'
    'com.jinbooks.persistence.mapper.ConfigInsuranceFund' = 'com.jinbooks.repository.config.ConfigInsuranceFund'
    'com.jinbooks.persistence.mapper.ConfigPersonalTax' = 'com.jinbooks.repository.config.ConfigPersonalTax'
    'com.jinbooks.persistence.mapper.ConfigSalaryFormula' = 'com.jinbooks.repository.config.ConfigSalaryFormula'
    'com.jinbooks.persistence.mapper.ConfigSys' = 'com.jinbooks.repository.config.ConfigSys'
    'com.jinbooks.persistence.mapper.Institutions' = 'com.jinbooks.repository.config.Institutions'
    'com.jinbooks.persistence.mapper.EmployeeSalarySummary' = 'com.jinbooks.repository.hr.EmployeeSalarySummary'
    'com.jinbooks.persistence.mapper.EmployeeSalaryTemp' = 'com.jinbooks.repository.hr.EmployeeSalaryTemp'
    'com.jinbooks.persistence.mapper.EmployeeTaxDeduction' = 'com.jinbooks.repository.hr.EmployeeTaxDeduction'
    'com.jinbooks.persistence.mapper.EmployeeSalary' = 'com.jinbooks.repository.hr.EmployeeSalary'
    'com.jinbooks.persistence.mapper.Employee' = 'com.jinbooks.repository.hr.Employee'
    'com.jinbooks.persistence.mapper.StandardSubjectCashFlow' = 'com.jinbooks.repository.standard.StandardSubjectCashFlow'
    'com.jinbooks.persistence.mapper.StandardSubject' = 'com.jinbooks.repository.standard.StandardSubject'
    'com.jinbooks.persistence.mapper.StandardStatementBalanceSheet' = 'com.jinbooks.repository.standard.StandardStatementBalanceSheet'
    'com.jinbooks.persistence.mapper.StandardStatementIncome' = 'com.jinbooks.repository.standard.StandardStatementIncome'
    'com.jinbooks.persistence.mapper.StandardStatementRules' = 'com.jinbooks.repository.standard.StandardStatementRules'
    'com.jinbooks.persistence.mapper.Standard' = 'com.jinbooks.repository.standard.Standard'
    'com.jinbooks.persistence.mapper.HistoryLoginApps' = 'com.jinbooks.repository.history.HistoryLoginApps'
    'com.jinbooks.persistence.mapper.HistoryLogin' = 'com.jinbooks.repository.history.HistoryLogin'
    'com.jinbooks.persistence.mapper.HistorySystemLogs' = 'com.jinbooks.repository.history.HistorySystemLogs'
    'com.jinbooks.persistence.mapper.HistorySynchronizer' = 'com.jinbooks.repository.history.HistorySynchronizer'
    'com.jinbooks.persistence.mapper.HistoryConnector' = 'com.jinbooks.repository.history.HistoryConnector'
    'com.jinbooks.persistence.mapper.Report' = 'com.jinbooks.repository.report.Report'

    # services
    'com.jinbooks.persistence.service.impl.ConfigCashFlowBalance' = 'com.jinbooks.service.config.impl.ConfigCashFlowBalance'
    'com.jinbooks.persistence.service.impl.ConfigInsuranceFund' = 'com.jinbooks.service.config.impl.ConfigInsuranceFund'
    'com.jinbooks.persistence.service.impl.ConfigPersonalTax' = 'com.jinbooks.service.config.impl.ConfigPersonalTax'
    'com.jinbooks.persistence.service.impl.ConfigSalaryFormula' = 'com.jinbooks.service.config.impl.ConfigSalaryFormula'
    'com.jinbooks.persistence.service.impl.ConfigSys' = 'com.jinbooks.service.config.impl.ConfigSys'
    'com.jinbooks.persistence.service.impl.Institutions' = 'com.jinbooks.service.config.impl.Institutions'
    'com.jinbooks.persistence.service.impl.EmployeeSalarySummary' = 'com.jinbooks.service.hr.impl.EmployeeSalarySummary'
    'com.jinbooks.persistence.service.impl.EmployeeSalaryTemp' = 'com.jinbooks.service.hr.impl.EmployeeSalaryTemp'
    'com.jinbooks.persistence.service.impl.EmployeeTaxDeduction' = 'com.jinbooks.service.hr.impl.EmployeeTaxDeduction'
    'com.jinbooks.persistence.service.impl.EmployeeSalary' = 'com.jinbooks.service.hr.impl.EmployeeSalary'
    'com.jinbooks.persistence.service.impl.Employee' = 'com.jinbooks.service.hr.impl.Employee'
    'com.jinbooks.persistence.service.impl.StandardSubjectCashFlow' = 'com.jinbooks.service.standard.impl.StandardSubjectCashFlow'
    'com.jinbooks.persistence.service.impl.StandardSubject' = 'com.jinbooks.service.standard.impl.StandardSubject'
    'com.jinbooks.persistence.service.impl.StandardStatementBalanceSheet' = 'com.jinbooks.service.standard.impl.StandardStatementBalanceSheet'
    'com.jinbooks.persistence.service.impl.StandardStatementIncome' = 'com.jinbooks.service.standard.impl.StandardStatementIncome'
    'com.jinbooks.persistence.service.impl.Standard' = 'com.jinbooks.service.standard.impl.Standard'
    'com.jinbooks.persistence.service.impl.HistoryLogin' = 'com.jinbooks.service.history.impl.HistoryLogin'
    'com.jinbooks.persistence.service.impl.HistorySystemLogs' = 'com.jinbooks.service.history.impl.HistorySystemLogs'
    'com.jinbooks.persistence.service.impl.HistorySynchronizer' = 'com.jinbooks.service.history.impl.HistorySynchronizer'
    'com.jinbooks.persistence.service.impl.HistoryConnector' = 'com.jinbooks.service.history.impl.HistoryConnector'
    'com.jinbooks.persistence.service.impl.FundDashboard' = 'com.jinbooks.service.report.impl.FundDashboard'
    'com.jinbooks.persistence.service.impl.Report' = 'com.jinbooks.service.report.impl.Report'

    'com.jinbooks.persistence.service.ConfigCashFlowBalance' = 'com.jinbooks.service.config.ConfigCashFlowBalance'
    'com.jinbooks.persistence.service.ConfigInsuranceFund' = 'com.jinbooks.service.config.ConfigInsuranceFund'
    'com.jinbooks.persistence.service.ConfigPersonalTax' = 'com.jinbooks.service.config.ConfigPersonalTax'
    'com.jinbooks.persistence.service.ConfigSalaryFormula' = 'com.jinbooks.service.config.ConfigSalaryFormula'
    'com.jinbooks.persistence.service.ConfigSys' = 'com.jinbooks.service.config.ConfigSys'
    'com.jinbooks.persistence.service.ConfigService' = 'com.jinbooks.service.config.ConfigService'
    'com.jinbooks.persistence.service.Institutions' = 'com.jinbooks.service.config.Institutions'
    'com.jinbooks.persistence.service.EmployeeSalarySummary' = 'com.jinbooks.service.hr.EmployeeSalarySummary'
    'com.jinbooks.persistence.service.EmployeeSalaryTemp' = 'com.jinbooks.service.hr.EmployeeSalaryTemp'
    'com.jinbooks.persistence.service.EmployeeTaxDeduction' = 'com.jinbooks.service.hr.EmployeeTaxDeduction'
    'com.jinbooks.persistence.service.EmployeeSalary' = 'com.jinbooks.service.hr.EmployeeSalary'
    'com.jinbooks.persistence.service.Employee' = 'com.jinbooks.service.hr.Employee'
    'com.jinbooks.persistence.service.StandardSubjectCashFlow' = 'com.jinbooks.service.standard.StandardSubjectCashFlow'
    'com.jinbooks.persistence.service.StandardSubject' = 'com.jinbooks.service.standard.StandardSubject'
    'com.jinbooks.persistence.service.StandardStatementBalanceSheet' = 'com.jinbooks.service.standard.StandardStatementBalanceSheet'
    'com.jinbooks.persistence.service.StandardStatementIncome' = 'com.jinbooks.service.standard.StandardStatementIncome'
    'com.jinbooks.persistence.service.Standard' = 'com.jinbooks.service.standard.Standard'
    'com.jinbooks.persistence.service.HistoryLogin' = 'com.jinbooks.service.history.HistoryLogin'
    'com.jinbooks.persistence.service.HistorySystemLogs' = 'com.jinbooks.service.history.HistorySystemLogs'
    'com.jinbooks.persistence.service.HistorySynchronizer' = 'com.jinbooks.service.history.HistorySynchronizer'
    'com.jinbooks.persistence.service.HistoryConnector' = 'com.jinbooks.service.history.HistoryConnector'
    'com.jinbooks.persistence.service.FundDashboard' = 'com.jinbooks.service.report.FundDashboard'
    'com.jinbooks.persistence.service.Report' = 'com.jinbooks.service.report.Report'
}

Get-ChildItem -Path $srcRoot -Recurse -Include *.java,*.xml | ForEach-Object {
    $c = [System.IO.File]::ReadAllText($_.FullName)
    $orig = $c
    foreach ($k in $replacements.Keys) { $c = $c.Replace($k, $replacements[$k]) }
    if ($c -ne $orig) { [System.IO.File]::WriteAllText($_.FullName, $c) }
}

$symbols = [ordered]@{
    'ConfigSysService'='import com.jinbooks.service.config.ConfigSysService;'
    'ConfigService'='import com.jinbooks.service.config.ConfigService;'
    'ConfigCashFlowBalanceService'='import com.jinbooks.service.config.ConfigCashFlowBalanceService;'
    'ConfigInsuranceFundService'='import com.jinbooks.service.config.ConfigInsuranceFundService;'
    'ConfigPersonalTaxService'='import com.jinbooks.service.config.ConfigPersonalTaxService;'
    'ConfigSalaryFormulaService'='import com.jinbooks.service.config.ConfigSalaryFormulaService;'
    'InstitutionsService'='import com.jinbooks.service.config.InstitutionsService;'
    'InstitutionsServiceImpl'='import com.jinbooks.service.config.impl.InstitutionsServiceImpl;'
    'ConfigSysMapper'='import com.jinbooks.repository.config.ConfigSysMapper;'
    'ConfigCashFlowBalanceMapper'='import com.jinbooks.repository.config.ConfigCashFlowBalanceMapper;'
    'ConfigInsuranceFundMapper'='import com.jinbooks.repository.config.ConfigInsuranceFundMapper;'
    'ConfigPersonalTaxMapper'='import com.jinbooks.repository.config.ConfigPersonalTaxMapper;'
    'ConfigSalaryFormulaMapper'='import com.jinbooks.repository.config.ConfigSalaryFormulaMapper;'
    'InstitutionsMapper'='import com.jinbooks.repository.config.InstitutionsMapper;'
    'EmployeeService'='import com.jinbooks.service.hr.EmployeeService;'
    'EmployeeSalaryService'='import com.jinbooks.service.hr.EmployeeSalaryService;'
    'EmployeeSalarySummaryService'='import com.jinbooks.service.hr.EmployeeSalarySummaryService;'
    'EmployeeSalaryTempService'='import com.jinbooks.service.hr.EmployeeSalaryTempService;'
    'EmployeeTaxDeductionService'='import com.jinbooks.service.hr.EmployeeTaxDeductionService;'
    'EmployeeMapper'='import com.jinbooks.repository.hr.EmployeeMapper;'
    'EmployeeSalaryMapper'='import com.jinbooks.repository.hr.EmployeeSalaryMapper;'
    'EmployeeSalarySummaryMapper'='import com.jinbooks.repository.hr.EmployeeSalarySummaryMapper;'
    'EmployeeSalaryTempMapper'='import com.jinbooks.repository.hr.EmployeeSalaryTempMapper;'
    'EmployeeTaxDeductionMapper'='import com.jinbooks.repository.hr.EmployeeTaxDeductionMapper;'
    'StandardService'='import com.jinbooks.service.standard.StandardService;'
    'StandardSubjectService'='import com.jinbooks.service.standard.StandardSubjectService;'
    'StandardSubjectCashFlowService'='import com.jinbooks.service.standard.StandardSubjectCashFlowService;'
    'StandardStatementBalanceSheetService'='import com.jinbooks.service.standard.StandardStatementBalanceSheetService;'
    'StandardStatementIncomeService'='import com.jinbooks.service.standard.StandardStatementIncomeService;'
    'StandardMapper'='import com.jinbooks.repository.standard.StandardMapper;'
    'StandardSubjectMapper'='import com.jinbooks.repository.standard.StandardSubjectMapper;'
    'StandardSubjectCashFlowMapper'='import com.jinbooks.repository.standard.StandardSubjectCashFlowMapper;'
    'StandardStatementBalanceSheetMapper'='import com.jinbooks.repository.standard.StandardStatementBalanceSheetMapper;'
    'StandardStatementIncomeMapper'='import com.jinbooks.repository.standard.StandardStatementIncomeMapper;'
    'StandardStatementRulesMapper'='import com.jinbooks.repository.standard.StandardStatementRulesMapper;'
    'HistoryLoginService'='import com.jinbooks.service.history.HistoryLoginService;'
    'HistorySystemLogsService'='import com.jinbooks.service.history.HistorySystemLogsService;'
    'HistorySynchronizerService'='import com.jinbooks.service.history.HistorySynchronizerService;'
    'HistoryConnectorService'='import com.jinbooks.service.history.HistoryConnectorService;'
    'HistoryLoginMapper'='import com.jinbooks.repository.history.HistoryLoginMapper;'
    'HistoryLoginAppsMapper'='import com.jinbooks.repository.history.HistoryLoginAppsMapper;'
    'HistorySystemLogsMapper'='import com.jinbooks.repository.history.HistorySystemLogsMapper;'
    'HistorySynchronizerMapper'='import com.jinbooks.repository.history.HistorySynchronizerMapper;'
    'HistoryConnectorMapper'='import com.jinbooks.repository.history.HistoryConnectorMapper;'
    'ReportService'='import com.jinbooks.service.report.ReportService;'
    'FundDashboardService'='import com.jinbooks.service.report.FundDashboardService;'
    'ReportMapper'='import com.jinbooks.repository.report.ReportMapper;'
}

Get-ChildItem -Path "$srcRoot\main\java" -Recurse -Filter *.java | ForEach-Object {
    $c = [System.IO.File]::ReadAllText($_.FullName)
    foreach ($sym in $symbols.Keys) {
        if ($c -match "\b$sym\b" -and $c -notmatch [regex]::Escape($symbols[$sym])) {
            Add-ImportIfMissing $_.FullName $symbols[$sym]
            $c = [System.IO.File]::ReadAllText($_.FullName)
        }
    }
}

# cleanup empties
@(
    "$base\entity\config\dto","$base\entity\config\vo","$base\entity\config",
    "$base\entity\hr\dto","$base\entity\hr\vo","$base\entity\hr",
    "$base\entity\standard\dto","$base\entity\standard\vo","$base\entity\standard",
    "$base\entity\history\dto","$base\entity\history",
    "$base\entity\report\dto","$base\entity\report\vo","$base\entity\report","$base\entity\fund",
    "$base\web\config\controller","$base\web\config",
    "$base\web\hr\controller","$base\web\hr",
    "$base\web\standard\controller","$base\web\standard",
    "$base\web\historys\controller\dto","$base\web\historys\controller","$base\web\historys"
) | ForEach-Object {
    if ((Test-Path $_) -and -not (Get-ChildItem $_ -Force -ErrorAction SilentlyContinue)) {
        Remove-Item $_ -Force -Recurse -ErrorAction SilentlyContinue
    }
}

Write-Host "Wave 5 complete."
