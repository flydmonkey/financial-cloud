# Wave 5: config + hr + standard + history + report
$ErrorActionPreference = "Stop"
$base = "C:\Users\Administrator\Projects\jinbooks\financial-cloud\src\main\java\com\jinbooks"
$resBase = "C:\Users\Administrator\Projects\jinbooks\financial-cloud\src\main\resources"
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
    $c = $c -replace 'com\.jinbooks\.persistence\.mapper\.', "com.financial.cloud.repository.$domain."
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
    Move-Set "$base\entity\config\$e" "$base\domain\config\$e" "com.financial.cloud.domain.config"
}
Move-Set "$base\entity\Institutions.java" "$base\domain\config\Institutions.java" "com.financial.cloud.domain.config"

Get-ChildItem "$base\entity\config\dto\*.java","$base\entity\config\vo\*.java" -ErrorAction SilentlyContinue | ForEach-Object {
    Move-Set $_.FullName "$base\dto\config\$($_.Name)" "com.financial.cloud.dto.config"
}
Move-Set "$base\entity\dto\InstitutionsPageDto.java" "$base\dto\config\InstitutionsPageDto.java" "com.financial.cloud.dto.config"
Move-Set "$base\entity\dto\ExpandAttrsPageDto.java" "$base\dto\config\ExpandAttrsPageDto.java" "com.financial.cloud.dto.config" # may already be gone

foreach ($c in @("ConfigSysController.java","ConfigCashFlowBalanceController.java","ConfigInsuranceFundController.java","ConfigPersonalTaxController.java","ConfigSalaryFormulaController.java","InstitutionsController.java")) {
    Move-Set "$base\web\config\controller\$c" "$base\controller\config\$c" "com.financial.cloud.controller.config"
}

foreach ($m in @("ConfigSysMapper.java","ConfigCashFlowBalanceMapper.java","ConfigInsuranceFundMapper.java","ConfigPersonalTaxMapper.java","ConfigSalaryFormulaMapper.java","InstitutionsMapper.java")) {
    Move-Set "$base\persistence\mapper\$m" "$base\repository\config\$m" "com.financial.cloud.repository.config"
}
foreach ($s in @("ConfigSysService.java","ConfigService.java","ConfigCashFlowBalanceService.java","ConfigInsuranceFundService.java","ConfigPersonalTaxService.java","ConfigSalaryFormulaService.java","InstitutionsService.java")) {
    Move-Set "$base\persistence\service\$s" "$base\service\config\$s" "com.financial.cloud.service.config"
}
foreach ($i in @("ConfigSysServiceImpl.java","ConfigCashFlowBalanceServiceImpl.java","ConfigInsuranceFundServiceImpl.java","ConfigPersonalTaxServiceImpl.java","ConfigSalaryFormulaServiceImpl.java","InstitutionsServiceImpl.java")) {
    Move-Set "$base\persistence\service\impl\$i" "$base\service\config\impl\$i" "com.financial.cloud.service.config.impl"
}
Move-Xml "InstitutionsMapper.xml" "config"

# ========== HR ==========
Ensure-Dir "$base\domain\hr"; Ensure-Dir "$base\dto\hr"; Ensure-Dir "$base\controller\hr"
Ensure-Dir "$base\repository\hr"; Ensure-Dir "$base\service\hr\impl"

Get-ChildItem "$base\entity\hr\*.java" -ErrorAction SilentlyContinue | ForEach-Object {
    Move-Set $_.FullName "$base\domain\hr\$($_.Name)" "com.financial.cloud.domain.hr"
}
Get-ChildItem "$base\entity\hr\dto\*.java","$base\entity\hr\vo\*.java" -ErrorAction SilentlyContinue | ForEach-Object {
    Move-Set $_.FullName "$base\dto\hr\$($_.Name)" "com.financial.cloud.dto.hr"
}
foreach ($c in @("EmployeeController.java","EmployeeSalaryController.java","EmployeeSalarySummaryController.java","EmployeeSalaryTempController.java","EmployeeTaxDeductionController.java")) {
    Move-Set "$base\web\hr\controller\$c" "$base\controller\hr\$c" "com.financial.cloud.controller.hr"
}
foreach ($m in @("EmployeeMapper.java","EmployeeSalaryMapper.java","EmployeeSalarySummaryMapper.java","EmployeeSalaryTempMapper.java","EmployeeTaxDeductionMapper.java")) {
    Move-Set "$base\persistence\mapper\$m" "$base\repository\hr\$m" "com.financial.cloud.repository.hr"
}
foreach ($s in @("EmployeeService.java","EmployeeSalaryService.java","EmployeeSalarySummaryService.java","EmployeeSalaryTempService.java","EmployeeTaxDeductionService.java")) {
    Move-Set "$base\persistence\service\$s" "$base\service\hr\$s" "com.financial.cloud.service.hr"
}
foreach ($i in @("EmployeeServiceImpl.java","EmployeeSalaryServiceImpl.java","EmployeeSalarySummaryServiceImpl.java","EmployeeSalaryTempServiceImpl.java","EmployeeTaxDeductionServiceImpl.java")) {
    Move-Set "$base\persistence\service\impl\$i" "$base\service\hr\impl\$i" "com.financial.cloud.service.hr.impl"
}
foreach ($x in @("EmployeeMapper.xml","EmployeeSalaryMapper.xml","EmployeeSalarySummaryMapper.xml","EmployeeSalaryTempMapper.xml","EmployeeTaxDeductionMapper.xml")) { Move-Xml $x "hr" }

# ========== STANDARD ==========
Ensure-Dir "$base\domain\standard"; Ensure-Dir "$base\dto\standard"; Ensure-Dir "$base\controller\standard"
Ensure-Dir "$base\repository\standard"; Ensure-Dir "$base\service\standard\impl"

Get-ChildItem "$base\entity\standard\*.java" -ErrorAction SilentlyContinue | ForEach-Object {
    Move-Set $_.FullName "$base\domain\standard\$($_.Name)" "com.financial.cloud.domain.standard"
}
Get-ChildItem "$base\entity\standard\dto\*.java","$base\entity\standard\vo\*.java" -ErrorAction SilentlyContinue | ForEach-Object {
    Move-Set $_.FullName "$base\dto\standard\$($_.Name)" "com.financial.cloud.dto.standard"
}
foreach ($c in @("StandardController.java","StandardSubjectController.java","StandardSubjectCashFlowController.java","StandardStatementBalanceSheetController.java","StandardStatementIncomeController.java")) {
    Move-Set "$base\web\standard\controller\$c" "$base\controller\standard\$c" "com.financial.cloud.controller.standard"
}
foreach ($m in @("StandardMapper.java","StandardSubjectMapper.java","StandardSubjectCashFlowMapper.java","StandardStatementBalanceSheetMapper.java","StandardStatementIncomeMapper.java","StandardStatementRulesMapper.java")) {
    Move-Set "$base\persistence\mapper\$m" "$base\repository\standard\$m" "com.financial.cloud.repository.standard"
}
foreach ($s in @("StandardService.java","StandardSubjectService.java","StandardSubjectCashFlowService.java","StandardStatementBalanceSheetService.java","StandardStatementIncomeService.java")) {
    Move-Set "$base\persistence\service\$s" "$base\service\standard\$s" "com.financial.cloud.service.standard"
}
foreach ($i in @("StandardServiceImpl.java","StandardSubjectServiceImpl.java","StandardSubjectCashFlowServiceImpl.java","StandardStatementBalanceSheetServiceImpl.java","StandardStatementIncomeServiceImpl.java")) {
    Move-Set "$base\persistence\service\impl\$i" "$base\service\standard\impl\$i" "com.financial.cloud.service.standard.impl"
}
foreach ($x in @("StandardMapper.xml","StandardSubjectMapper.xml","standardSubjectCashFlowMapper.xml","StandardStatementBalanceSheetMapper.xml","StandardStatementRulesMapper.xml")) { Move-Xml $x "standard" }

# ========== HISTORY ==========
Ensure-Dir "$base\domain\history"; Ensure-Dir "$base\dto\history"; Ensure-Dir "$base\controller\history"
Ensure-Dir "$base\repository\history"; Ensure-Dir "$base\service\history\impl"

Get-ChildItem "$base\entity\history\*.java" -ErrorAction SilentlyContinue | ForEach-Object {
    Move-Set $_.FullName "$base\domain\history\$($_.Name)" "com.financial.cloud.domain.history"
}
Get-ChildItem "$base\entity\history\dto\*.java" -ErrorAction SilentlyContinue | ForEach-Object {
    Move-Set $_.FullName "$base\dto\history\$($_.Name)" "com.financial.cloud.dto.history"
}
# historys web package (with s)
foreach ($c in @("LoginHistoryController.java","SystemLogsController.java","SynchronizerHistoryController.java","ConnectorHistoryController.java")) {
    Move-Set "$base\web\historys\controller\$c" "$base\controller\history\$c" "com.financial.cloud.controller.history"
}
# HistorySynchronizerPageDto under web.historys.controller.dto if exists
Get-ChildItem "$base\web\historys\controller\dto\*.java" -ErrorAction SilentlyContinue | ForEach-Object {
    Move-Set $_.FullName "$base\dto\history\$($_.Name)" "com.financial.cloud.dto.history"
}

foreach ($m in @("HistoryLoginMapper.java","HistoryLoginAppsMapper.java","HistorySystemLogsMapper.java","HistorySynchronizerMapper.java","HistoryConnectorMapper.java")) {
    Move-Set "$base\persistence\mapper\$m" "$base\repository\history\$m" "com.financial.cloud.repository.history"
}
foreach ($s in @("HistoryLoginService.java","HistorySystemLogsService.java","HistorySynchronizerService.java","HistoryConnectorService.java")) {
    Move-Set "$base\persistence\service\$s" "$base\service\history\$s" "com.financial.cloud.service.history"
}
foreach ($i in @("HistoryLoginServiceImpl.java","HistorySystemLogsServiceImpl.java","HistorySynchronizerServiceImpl.java","HistoryConnectorServiceImpl.java")) {
    Move-Set "$base\persistence\service\impl\$i" "$base\service\history\impl\$i" "com.financial.cloud.service.history.impl"
}
foreach ($x in @("HistoryLoginMapper.xml","HistoryLoginAppsMapper.xml","HistorySystemLogsMapper.xml","HistorySynchronizerMapper.xml","HistoryConnectorMapper.xml")) { Move-Xml $x "history" }

# ========== REPORT (report + fund + dashboards) ==========
Ensure-Dir "$base\domain\report"; Ensure-Dir "$base\dto\report"; Ensure-Dir "$base\controller\report"
Ensure-Dir "$base\repository\report"; Ensure-Dir "$base\service\report\impl"

Get-ChildItem "$base\entity\report\dto\*.java","$base\entity\report\vo\*.java","$base\entity\fund\*.java" -ErrorAction SilentlyContinue | ForEach-Object {
    Move-Set $_.FullName "$base\dto\report\$($_.Name)" "com.financial.cloud.dto.report"
}
# CashFlowSubjectBalanceVo under entity/vo
Move-Set "$base\entity\vo\CashFlowSubjectBalanceVo.java" "$base\dto\report\CashFlowSubjectBalanceVo.java" "com.financial.cloud.dto.report"

foreach ($c in @("DashboardController.java","FundDashboardController.java")) {
    Move-Set "$base\web\controller\$c" "$base\controller\report\$c" "com.financial.cloud.controller.report"
}
Move-Set "$base\persistence\mapper\ReportMapper.java" "$base\repository\report\ReportMapper.java" "com.financial.cloud.repository.report"
foreach ($s in @("ReportService.java","FundDashboardService.java")) {
    Move-Set "$base\persistence\service\$s" "$base\service\report\$s" "com.financial.cloud.service.report"
}
foreach ($i in @("ReportServiceImpl.java","FundDashboardServiceImpl.java")) {
    Move-Set "$base\persistence\service\impl\$i" "$base\service\report\impl\$i" "com.financial.cloud.service.report.impl"
}
Move-Xml "ReportMapper.xml" "report"

# ========== replacements ==========
$replacements = [ordered]@{
    'com.financial.cloud.entity.config.dto.' = 'com.financial.cloud.dto.config.'
    'com.financial.cloud.entity.config.vo.' = 'com.financial.cloud.dto.config.'
    'com.financial.cloud.entity.config.' = 'com.financial.cloud.domain.config.'
    'com.financial.cloud.entity.Institutions' = 'com.financial.cloud.domain.config.Institutions'
    'com.financial.cloud.entity.dto.InstitutionsPageDto' = 'com.financial.cloud.dto.config.InstitutionsPageDto'
    'com.financial.cloud.web.config.controller.' = 'com.financial.cloud.controller.config.'

    'com.financial.cloud.entity.hr.dto.' = 'com.financial.cloud.dto.hr.'
    'com.financial.cloud.entity.hr.vo.' = 'com.financial.cloud.dto.hr.'
    'com.financial.cloud.entity.hr.' = 'com.financial.cloud.domain.hr.'
    'com.financial.cloud.web.hr.controller.' = 'com.financial.cloud.controller.hr.'

    'com.financial.cloud.entity.standard.dto.' = 'com.financial.cloud.dto.standard.'
    'com.financial.cloud.entity.standard.vo.' = 'com.financial.cloud.dto.standard.'
    'com.financial.cloud.entity.standard.' = 'com.financial.cloud.domain.standard.'
    'com.financial.cloud.web.standard.controller.' = 'com.financial.cloud.controller.standard.'

    'com.financial.cloud.entity.history.dto.' = 'com.financial.cloud.dto.history.'
    'com.financial.cloud.entity.history.' = 'com.financial.cloud.domain.history.'
    'com.financial.cloud.web.historys.controller.dto.' = 'com.financial.cloud.dto.history.'
    'com.financial.cloud.web.historys.controller.' = 'com.financial.cloud.controller.history.'

    'com.financial.cloud.entity.report.dto.' = 'com.financial.cloud.dto.report.'
    'com.financial.cloud.entity.report.vo.' = 'com.financial.cloud.dto.report.'
    'com.financial.cloud.entity.fund.' = 'com.financial.cloud.dto.report.'
    'com.financial.cloud.entity.vo.CashFlowSubjectBalanceVo' = 'com.financial.cloud.dto.report.CashFlowSubjectBalanceVo'
    'com.financial.cloud.web.controller.DashboardController' = 'com.financial.cloud.controller.report.DashboardController'
    'com.financial.cloud.web.controller.FundDashboardController' = 'com.financial.cloud.controller.report.FundDashboardController'

    # mappers longest first
    'com.financial.cloud.persistence.mapper.ConfigCashFlowBalance' = 'com.financial.cloud.repository.config.ConfigCashFlowBalance'
    'com.financial.cloud.persistence.mapper.ConfigInsuranceFund' = 'com.financial.cloud.repository.config.ConfigInsuranceFund'
    'com.financial.cloud.persistence.mapper.ConfigPersonalTax' = 'com.financial.cloud.repository.config.ConfigPersonalTax'
    'com.financial.cloud.persistence.mapper.ConfigSalaryFormula' = 'com.financial.cloud.repository.config.ConfigSalaryFormula'
    'com.financial.cloud.persistence.mapper.ConfigSys' = 'com.financial.cloud.repository.config.ConfigSys'
    'com.financial.cloud.persistence.mapper.Institutions' = 'com.financial.cloud.repository.config.Institutions'
    'com.financial.cloud.persistence.mapper.EmployeeSalarySummary' = 'com.financial.cloud.repository.hr.EmployeeSalarySummary'
    'com.financial.cloud.persistence.mapper.EmployeeSalaryTemp' = 'com.financial.cloud.repository.hr.EmployeeSalaryTemp'
    'com.financial.cloud.persistence.mapper.EmployeeTaxDeduction' = 'com.financial.cloud.repository.hr.EmployeeTaxDeduction'
    'com.financial.cloud.persistence.mapper.EmployeeSalary' = 'com.financial.cloud.repository.hr.EmployeeSalary'
    'com.financial.cloud.persistence.mapper.Employee' = 'com.financial.cloud.repository.hr.Employee'
    'com.financial.cloud.persistence.mapper.StandardSubjectCashFlow' = 'com.financial.cloud.repository.standard.StandardSubjectCashFlow'
    'com.financial.cloud.persistence.mapper.StandardSubject' = 'com.financial.cloud.repository.standard.StandardSubject'
    'com.financial.cloud.persistence.mapper.StandardStatementBalanceSheet' = 'com.financial.cloud.repository.standard.StandardStatementBalanceSheet'
    'com.financial.cloud.persistence.mapper.StandardStatementIncome' = 'com.financial.cloud.repository.standard.StandardStatementIncome'
    'com.financial.cloud.persistence.mapper.StandardStatementRules' = 'com.financial.cloud.repository.standard.StandardStatementRules'
    'com.financial.cloud.persistence.mapper.Standard' = 'com.financial.cloud.repository.standard.Standard'
    'com.financial.cloud.persistence.mapper.HistoryLoginApps' = 'com.financial.cloud.repository.history.HistoryLoginApps'
    'com.financial.cloud.persistence.mapper.HistoryLogin' = 'com.financial.cloud.repository.history.HistoryLogin'
    'com.financial.cloud.persistence.mapper.HistorySystemLogs' = 'com.financial.cloud.repository.history.HistorySystemLogs'
    'com.financial.cloud.persistence.mapper.HistorySynchronizer' = 'com.financial.cloud.repository.history.HistorySynchronizer'
    'com.financial.cloud.persistence.mapper.HistoryConnector' = 'com.financial.cloud.repository.history.HistoryConnector'
    'com.financial.cloud.persistence.mapper.Report' = 'com.financial.cloud.repository.report.Report'

    # services
    'com.financial.cloud.persistence.service.impl.ConfigCashFlowBalance' = 'com.financial.cloud.service.config.impl.ConfigCashFlowBalance'
    'com.financial.cloud.persistence.service.impl.ConfigInsuranceFund' = 'com.financial.cloud.service.config.impl.ConfigInsuranceFund'
    'com.financial.cloud.persistence.service.impl.ConfigPersonalTax' = 'com.financial.cloud.service.config.impl.ConfigPersonalTax'
    'com.financial.cloud.persistence.service.impl.ConfigSalaryFormula' = 'com.financial.cloud.service.config.impl.ConfigSalaryFormula'
    'com.financial.cloud.persistence.service.impl.ConfigSys' = 'com.financial.cloud.service.config.impl.ConfigSys'
    'com.financial.cloud.persistence.service.impl.Institutions' = 'com.financial.cloud.service.config.impl.Institutions'
    'com.financial.cloud.persistence.service.impl.EmployeeSalarySummary' = 'com.financial.cloud.service.hr.impl.EmployeeSalarySummary'
    'com.financial.cloud.persistence.service.impl.EmployeeSalaryTemp' = 'com.financial.cloud.service.hr.impl.EmployeeSalaryTemp'
    'com.financial.cloud.persistence.service.impl.EmployeeTaxDeduction' = 'com.financial.cloud.service.hr.impl.EmployeeTaxDeduction'
    'com.financial.cloud.persistence.service.impl.EmployeeSalary' = 'com.financial.cloud.service.hr.impl.EmployeeSalary'
    'com.financial.cloud.persistence.service.impl.Employee' = 'com.financial.cloud.service.hr.impl.Employee'
    'com.financial.cloud.persistence.service.impl.StandardSubjectCashFlow' = 'com.financial.cloud.service.standard.impl.StandardSubjectCashFlow'
    'com.financial.cloud.persistence.service.impl.StandardSubject' = 'com.financial.cloud.service.standard.impl.StandardSubject'
    'com.financial.cloud.persistence.service.impl.StandardStatementBalanceSheet' = 'com.financial.cloud.service.standard.impl.StandardStatementBalanceSheet'
    'com.financial.cloud.persistence.service.impl.StandardStatementIncome' = 'com.financial.cloud.service.standard.impl.StandardStatementIncome'
    'com.financial.cloud.persistence.service.impl.Standard' = 'com.financial.cloud.service.standard.impl.Standard'
    'com.financial.cloud.persistence.service.impl.HistoryLogin' = 'com.financial.cloud.service.history.impl.HistoryLogin'
    'com.financial.cloud.persistence.service.impl.HistorySystemLogs' = 'com.financial.cloud.service.history.impl.HistorySystemLogs'
    'com.financial.cloud.persistence.service.impl.HistorySynchronizer' = 'com.financial.cloud.service.history.impl.HistorySynchronizer'
    'com.financial.cloud.persistence.service.impl.HistoryConnector' = 'com.financial.cloud.service.history.impl.HistoryConnector'
    'com.financial.cloud.persistence.service.impl.FundDashboard' = 'com.financial.cloud.service.report.impl.FundDashboard'
    'com.financial.cloud.persistence.service.impl.Report' = 'com.financial.cloud.service.report.impl.Report'

    'com.financial.cloud.persistence.service.ConfigCashFlowBalance' = 'com.financial.cloud.service.config.ConfigCashFlowBalance'
    'com.financial.cloud.persistence.service.ConfigInsuranceFund' = 'com.financial.cloud.service.config.ConfigInsuranceFund'
    'com.financial.cloud.persistence.service.ConfigPersonalTax' = 'com.financial.cloud.service.config.ConfigPersonalTax'
    'com.financial.cloud.persistence.service.ConfigSalaryFormula' = 'com.financial.cloud.service.config.ConfigSalaryFormula'
    'com.financial.cloud.persistence.service.ConfigSys' = 'com.financial.cloud.service.config.ConfigSys'
    'com.financial.cloud.persistence.service.ConfigService' = 'com.financial.cloud.service.config.ConfigService'
    'com.financial.cloud.persistence.service.Institutions' = 'com.financial.cloud.service.config.Institutions'
    'com.financial.cloud.persistence.service.EmployeeSalarySummary' = 'com.financial.cloud.service.hr.EmployeeSalarySummary'
    'com.financial.cloud.persistence.service.EmployeeSalaryTemp' = 'com.financial.cloud.service.hr.EmployeeSalaryTemp'
    'com.financial.cloud.persistence.service.EmployeeTaxDeduction' = 'com.financial.cloud.service.hr.EmployeeTaxDeduction'
    'com.financial.cloud.persistence.service.EmployeeSalary' = 'com.financial.cloud.service.hr.EmployeeSalary'
    'com.financial.cloud.persistence.service.Employee' = 'com.financial.cloud.service.hr.Employee'
    'com.financial.cloud.persistence.service.StandardSubjectCashFlow' = 'com.financial.cloud.service.standard.StandardSubjectCashFlow'
    'com.financial.cloud.persistence.service.StandardSubject' = 'com.financial.cloud.service.standard.StandardSubject'
    'com.financial.cloud.persistence.service.StandardStatementBalanceSheet' = 'com.financial.cloud.service.standard.StandardStatementBalanceSheet'
    'com.financial.cloud.persistence.service.StandardStatementIncome' = 'com.financial.cloud.service.standard.StandardStatementIncome'
    'com.financial.cloud.persistence.service.Standard' = 'com.financial.cloud.service.standard.Standard'
    'com.financial.cloud.persistence.service.HistoryLogin' = 'com.financial.cloud.service.history.HistoryLogin'
    'com.financial.cloud.persistence.service.HistorySystemLogs' = 'com.financial.cloud.service.history.HistorySystemLogs'
    'com.financial.cloud.persistence.service.HistorySynchronizer' = 'com.financial.cloud.service.history.HistorySynchronizer'
    'com.financial.cloud.persistence.service.HistoryConnector' = 'com.financial.cloud.service.history.HistoryConnector'
    'com.financial.cloud.persistence.service.FundDashboard' = 'com.financial.cloud.service.report.FundDashboard'
    'com.financial.cloud.persistence.service.Report' = 'com.financial.cloud.service.report.Report'
}

Get-ChildItem -Path $srcRoot -Recurse -Include *.java,*.xml | ForEach-Object {
    $c = [System.IO.File]::ReadAllText($_.FullName)
    $orig = $c
    foreach ($k in $replacements.Keys) { $c = $c.Replace($k, $replacements[$k]) }
    if ($c -ne $orig) { [System.IO.File]::WriteAllText($_.FullName, $c) }
}

$symbols = [ordered]@{
    'ConfigSysService'='import com.financial.cloud.service.config.ConfigSysService;'
    'ConfigService'='import com.financial.cloud.service.config.ConfigService;'
    'ConfigCashFlowBalanceService'='import com.financial.cloud.service.config.ConfigCashFlowBalanceService;'
    'ConfigInsuranceFundService'='import com.financial.cloud.service.config.ConfigInsuranceFundService;'
    'ConfigPersonalTaxService'='import com.financial.cloud.service.config.ConfigPersonalTaxService;'
    'ConfigSalaryFormulaService'='import com.financial.cloud.service.config.ConfigSalaryFormulaService;'
    'InstitutionsService'='import com.financial.cloud.service.config.InstitutionsService;'
    'InstitutionsServiceImpl'='import com.financial.cloud.service.config.impl.InstitutionsServiceImpl;'
    'ConfigSysMapper'='import com.financial.cloud.repository.config.ConfigSysMapper;'
    'ConfigCashFlowBalanceMapper'='import com.financial.cloud.repository.config.ConfigCashFlowBalanceMapper;'
    'ConfigInsuranceFundMapper'='import com.financial.cloud.repository.config.ConfigInsuranceFundMapper;'
    'ConfigPersonalTaxMapper'='import com.financial.cloud.repository.config.ConfigPersonalTaxMapper;'
    'ConfigSalaryFormulaMapper'='import com.financial.cloud.repository.config.ConfigSalaryFormulaMapper;'
    'InstitutionsMapper'='import com.financial.cloud.repository.config.InstitutionsMapper;'
    'EmployeeService'='import com.financial.cloud.service.hr.EmployeeService;'
    'EmployeeSalaryService'='import com.financial.cloud.service.hr.EmployeeSalaryService;'
    'EmployeeSalarySummaryService'='import com.financial.cloud.service.hr.EmployeeSalarySummaryService;'
    'EmployeeSalaryTempService'='import com.financial.cloud.service.hr.EmployeeSalaryTempService;'
    'EmployeeTaxDeductionService'='import com.financial.cloud.service.hr.EmployeeTaxDeductionService;'
    'EmployeeMapper'='import com.financial.cloud.repository.hr.EmployeeMapper;'
    'EmployeeSalaryMapper'='import com.financial.cloud.repository.hr.EmployeeSalaryMapper;'
    'EmployeeSalarySummaryMapper'='import com.financial.cloud.repository.hr.EmployeeSalarySummaryMapper;'
    'EmployeeSalaryTempMapper'='import com.financial.cloud.repository.hr.EmployeeSalaryTempMapper;'
    'EmployeeTaxDeductionMapper'='import com.financial.cloud.repository.hr.EmployeeTaxDeductionMapper;'
    'StandardService'='import com.financial.cloud.service.standard.StandardService;'
    'StandardSubjectService'='import com.financial.cloud.service.standard.StandardSubjectService;'
    'StandardSubjectCashFlowService'='import com.financial.cloud.service.standard.StandardSubjectCashFlowService;'
    'StandardStatementBalanceSheetService'='import com.financial.cloud.service.standard.StandardStatementBalanceSheetService;'
    'StandardStatementIncomeService'='import com.financial.cloud.service.standard.StandardStatementIncomeService;'
    'StandardMapper'='import com.financial.cloud.repository.standard.StandardMapper;'
    'StandardSubjectMapper'='import com.financial.cloud.repository.standard.StandardSubjectMapper;'
    'StandardSubjectCashFlowMapper'='import com.financial.cloud.repository.standard.StandardSubjectCashFlowMapper;'
    'StandardStatementBalanceSheetMapper'='import com.financial.cloud.repository.standard.StandardStatementBalanceSheetMapper;'
    'StandardStatementIncomeMapper'='import com.financial.cloud.repository.standard.StandardStatementIncomeMapper;'
    'StandardStatementRulesMapper'='import com.financial.cloud.repository.standard.StandardStatementRulesMapper;'
    'HistoryLoginService'='import com.financial.cloud.service.history.HistoryLoginService;'
    'HistorySystemLogsService'='import com.financial.cloud.service.history.HistorySystemLogsService;'
    'HistorySynchronizerService'='import com.financial.cloud.service.history.HistorySynchronizerService;'
    'HistoryConnectorService'='import com.financial.cloud.service.history.HistoryConnectorService;'
    'HistoryLoginMapper'='import com.financial.cloud.repository.history.HistoryLoginMapper;'
    'HistoryLoginAppsMapper'='import com.financial.cloud.repository.history.HistoryLoginAppsMapper;'
    'HistorySystemLogsMapper'='import com.financial.cloud.repository.history.HistorySystemLogsMapper;'
    'HistorySynchronizerMapper'='import com.financial.cloud.repository.history.HistorySynchronizerMapper;'
    'HistoryConnectorMapper'='import com.financial.cloud.repository.history.HistoryConnectorMapper;'
    'ReportService'='import com.financial.cloud.service.report.ReportService;'
    'FundDashboardService'='import com.financial.cloud.service.report.FundDashboardService;'
    'ReportMapper'='import com.financial.cloud.repository.report.ReportMapper;'
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
