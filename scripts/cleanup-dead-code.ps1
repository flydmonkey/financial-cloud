$ErrorActionPreference = "Stop"
$root = "C:\Users\Administrator\Projects\jinbooks\jinbooks\src"

$filesToDelete = @(
    # authn leftovers
    "main\java\com\jinbooks\authn\handler\SessionSecurityContextHolderStrategy.java",
    "main\java\com\jinbooks\authn\realm\IAuthenticationServer.java",
    "main\java\com\jinbooks\authn\session\SessionDto.java",
    # configuration
    "main\java\com\jinbooks\configuration\EmailConfig.java",
    # unused constants
    "main\java\com\jinbooks\constants\ConstsBoolean.java",
    "main\java\com\jinbooks\constants\ConstsConfig.java",
    "main\java\com\jinbooks\constants\ConstsProperties.java",
    "main\java\com\jinbooks\constants\ConstsSalaryItem.java",
    "main\java\com\jinbooks\constants\ConstsStrategy.java",
    "main\java\com\jinbooks\constants\ConstsSymbols.java",
    "main\java\com\jinbooks\constants\ConstsTimeInterval.java",
    # unused dtos
    "main\java\com\jinbooks\dto\auth\ForgotPassword.java",
    "main\java\com\jinbooks\dto\auth\NewPwdDto.java",
    "main\java\com\jinbooks\dto\auth\QueryGrantedAppsDto.java",
    "main\java\com\jinbooks\dto\auth\QueryOrgDto.java",
    "main\java\com\jinbooks\dto\auth\QueryRoleMembersDto.java",
    "main\java\com\jinbooks\dto\auth\RegisterDto.java",
    "main\java\com\jinbooks\dto\book\StandardSubjectVo.java",
    "main\java\com\jinbooks\dto\book\SubjectJoinSetDto.java",
    "main\java\com\jinbooks\dto\common\ChangeStatusDto.java",
    "main\java\com\jinbooks\dto\common\NoticesPageDto.java",
    "main\java\com\jinbooks\dto\common\TimeBasedDto.java",
    "main\java\com\jinbooks\dto\config\ExpandAttrsPageDto.java",
    "main\java\com\jinbooks\dto\history\HistoryLoginAppsPageDto.java",
    "main\java\com\jinbooks\dto\hr\EmployeeVo.java",
    "main\java\com\jinbooks\dto\idm\OrgUserPageDto.java",
    "main\java\com\jinbooks\dto\idm\PostsPageDto.java",
    "main\java\com\jinbooks\dto\security\BlacklistPageDto.java",
    "main\java\com\jinbooks\dto\security\PasswordEncryptPageDto.java",
    "main\java\com\jinbooks\dto\security\WeakPasswordPageDto.java",
    "main\java\com\jinbooks\dto\standard\StandardSubjectDto.java",
    "main\java\com\jinbooks\dto\statement\StatementCashFlowExport.java",
    "main\java\com\jinbooks\dto\voucher\VoucherWordPageDto.java",
    # unused enums/exceptions/utils
    "main\java\com\jinbooks\enums\AccountBalanceTypeEnum.java",
    "main\java\com\jinbooks\enums\SalaryVoucherTemplateEnum.java",
    "main\java\com\jinbooks\exception\NameException.java",
    "main\java\com\jinbooks\exception\OperaterSqlException.java",
    "main\java\com\jinbooks\util\MethodInvoke.java",
    "main\java\com\jinbooks\util\ObjectTransformer.java",
    "main\java\com\jinbooks\util\RequestTokenUtils.java",
    "main\java\com\jinbooks\validate\QueryGroup.java",
    # orphan feature clusters
    "main\java\com\jinbooks\domain\history\HistoryLoginApps.java",
    "main\java\com\jinbooks\repository\history\HistoryLoginAppsMapper.java",
    "main\resources\com\jinbooks\repository\history\HistoryLoginAppsMapper.xml",
    "main\java\com\jinbooks\domain\book\ApprovalRecord.java",
    "main\java\com\jinbooks\repository\book\ApprovalRecordMapper.java",
    "main\java\com\jinbooks\service\voucher\VoucherTemplateItemService.java",
    "main\java\com\jinbooks\service\security\SocialsAssociatesService.java",
    "main\java\com\jinbooks\domain\security\SocialsAssociate.java",
    "main\java\com\jinbooks\repository\security\SocialsAssociateMapper.java",
    "main\resources\com\jinbooks\repository\security\SocialsAssociateMapper.xml",
    "main\java\com\jinbooks\controller\idm\UserInstInfoController.java",
    "main\java\com\jinbooks\service\idm\UserInstInfoService.java",
    "main\java\com\jinbooks\domain\idm\UserInstInfo.java",
    "main\java\com\jinbooks\repository\idm\UserInstInfoMapper.java",
    "main\resources\com\jinbooks\repository\idm\UserInstInfoMapper.xml",
    # scratch / duplicate tests
    "test\java\com\jinbooks\Demo.java"
)

$deleted = 0
foreach ($rel in $filesToDelete) {
    $path = Join-Path $root $rel
    if (Test-Path $path) {
        Remove-Item $path -Force
        $deleted++
        Write-Host "deleted $rel"
    }
}

# remove legacy test trees
$dirsToDelete = @(
    (Join-Path $root "test\java\org\maxkey"),
    (Join-Path $root "test\java\com\jinbooks\com")
)
foreach ($dir in $dirsToDelete) {
    if (Test-Path $dir) {
        Remove-Item $dir -Recurse -Force
        Write-Host "deleted dir $dir"
    }
}

# remove empty directories bottom-up (source only, skip target)
$emptyRemoved = 0
do {
    $empties = Get-ChildItem -Path $root -Recurse -Directory |
        Where-Object { $_.FullName -notmatch '\\target\\' } |
        Where-Object { @(Get-ChildItem $_.FullName -Force -ErrorAction SilentlyContinue).Count -eq 0 }
    foreach ($d in $empties) {
        Remove-Item $d.FullName -Force
        $emptyRemoved++
    }
} while ($empties.Count -gt 0)

Write-Host "deleted $deleted files, removed $emptyRemoved empty directories"
