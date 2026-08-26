# Wave 4: idm + permissions + security
$ErrorActionPreference = "Stop"
$base = "C:\Users\Administrator\Projects\jinbooks\financial-cloud\src\main\java\com\jinbooks"
$resBase = "C:\Users\Administrator\Projects\jinbooks\financial-cloud\src\main\resources"
$srcRoot = "C:\Users\Administrator\Projects\jinbooks\jinbooks\src"

function Ensure-Dir($p) { New-Item -ItemType Directory -Force -Path $p | Out-Null }
function Move-File($src, $dst) {
    if (-not (Test-Path $src)) { Write-Warning "Skip missing: $src"; return }
    Ensure-Dir (Split-Path $dst -Parent)
    Move-Item -Force $src $dst
    Write-Host "Moved $($_.Name) -> $(Split-Path $dst -Leaf)"
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

# ========== IDM ==========
Ensure-Dir "$base\domain\idm"; Ensure-Dir "$base\dto\idm"; Ensure-Dir "$base\controller\idm"
Ensure-Dir "$base\repository\idm"; Ensure-Dir "$base\service\idm\impl"

# entities from idm + Roles/RoleMember (controllers live under idm)
foreach ($e in @("UserInfo.java","UserInstInfo.java","Organizations.java")) {
    Move-Set "$base\entity\idm\$e" "$base\domain\idm\$e" "com.financial.cloud.domain.idm"
}
Move-Set "$base\entity\permissions\Roles.java" "$base\domain\idm\Roles.java" "com.financial.cloud.domain.idm"
Move-Set "$base\entity\permissions\RoleMember.java" "$base\domain\idm\RoleMember.java" "com.financial.cloud.domain.idm"

Get-ChildItem "$base\entity\idm\dto\*.java" -ErrorAction SilentlyContinue | ForEach-Object {
    Move-Set $_.FullName "$base\dto\idm\$($_.Name)" "com.financial.cloud.dto.idm"
}
# Roles/RoleMember DTOs currently under permissions/dto
foreach ($d in @("RolesPageDto.java","RoleMemberPageDto.java","RoleMemberDto.java","RoleMemberUserGroupsDto.java")) {
    Move-Set "$base\entity\permissions\dto\$d" "$base\dto\idm\$d" "com.financial.cloud.dto.idm"
}

foreach ($c in @("UserInfoController.java","UserInstInfoController.java","OrganizationsController.java","RolesController.java","RoleMemberController.java")) {
    Move-Set "$base\web\idm\controller\$c" "$base\controller\idm\$c" "com.financial.cloud.controller.idm"
}

foreach ($m in @("UserInfoMapper.java","UserInstInfoMapper.java","OrganizationsMapper.java","RolesMapper.java","RoleMemberMapper.java")) {
    Move-Set "$base\persistence\mapper\$m" "$base\repository\idm\$m" "com.financial.cloud.repository.idm"
}
foreach ($s in @("UserInfoService.java","UserInfoExcelService.java","UserInstInfoService.java","OrganizationsService.java","OrganizationsExcelService.java","RolesService.java","RoleMemberService.java")) {
    Move-Set "$base\persistence\service\$s" "$base\service\idm\$s" "com.financial.cloud.service.idm"
}
foreach ($i in @("UserInfoServiceImpl.java","UserInfoExcelServiceImpl.java","UserInstInfoServiceImpl.java","OrganizationsServiceImpl.java","OrganizationsExcelServiceImpl.java","RolesServiceImpl.java","RoleMemberServiceImpl.java")) {
    Move-Set "$base\persistence\service\impl\$i" "$base\service\idm\impl\$i" "com.financial.cloud.service.idm.impl"
}
foreach ($x in @("UserInfoMapper.xml","UserInstInfoMapper.xml","OrganizationsMapper.xml","RolesMapper.xml","RoleMemberMapper.xml")) { Move-Xml $x "idm" }

# ========== PERMISSIONS ==========
Ensure-Dir "$base\domain\permissions"; Ensure-Dir "$base\dto\permissions"; Ensure-Dir "$base\controller\permissions"
Ensure-Dir "$base\repository\permissions"; Ensure-Dir "$base\service\permissions\impl"

foreach ($e in @("Permission.java","PermissionBook.java","Resources.java")) {
    Move-Set "$base\entity\permissions\$e" "$base\domain\permissions\$e" "com.financial.cloud.domain.permissions"
}
Move-Set "$base\entity\access\SessionList.java" "$base\domain\permissions\SessionList.java" "com.financial.cloud.domain.permissions"

Get-ChildItem "$base\entity\permissions\dto\*.java" -ErrorAction SilentlyContinue | ForEach-Object {
    Move-Set $_.FullName "$base\dto\permissions\$($_.Name)" "com.financial.cloud.dto.permissions"
}
Move-Set "$base\entity\access\dto\SessionListPageDto.java" "$base\dto\permissions\SessionListPageDto.java" "com.financial.cloud.dto.permissions"

foreach ($c in @("PermissionController.java","PermissionBookController.java","ResourcesController.java")) {
    Move-Set "$base\web\permissions\controller\$c" "$base\controller\permissions\$c" "com.financial.cloud.controller.permissions"
}
foreach ($c in @("SessionController.java","OpenFuncListController.java")) {
    Move-Set "$base\web\access\controller\$c" "$base\controller\permissions\$c" "com.financial.cloud.controller.permissions"
}

foreach ($m in @("PermissionMapper.java","PermissionBookMapper.java","ResourcesMapper.java","SessionListMapper.java","AuthzMapper.java","AuthzResourceMapper.java")) {
    Move-Set "$base\persistence\mapper\$m" "$base\repository\permissions\$m" "com.financial.cloud.repository.permissions"
}
foreach ($s in @("PermissionService.java","PermissionBookService.java","ResourcesService.java","SessionListService.java","AuthzService.java","AuthzResourceService.java")) {
    Move-Set "$base\persistence\service\$s" "$base\service\permissions\$s" "com.financial.cloud.service.permissions"
}
foreach ($i in @("PermissionServiceImpl.java","PermissionBookServiceImpl.java","ResourcesServiceImpl.java","SessionListServiceImpl.java","AuthzServiceImpl.java","AuthzResourceServiceImpl.java")) {
    Move-Set "$base\persistence\service\impl\$i" "$base\service\permissions\impl\$i" "com.financial.cloud.service.permissions.impl"
}
foreach ($x in @("PermissionMapper.xml","PermissionBookMapper.xml","ResourcesMapper.xml","SessionListMapper.xml","AuthzMapper.xml","AuthzResourceMapper.xml")) { Move-Xml $x "permissions" }

# ========== SECURITY ==========
Ensure-Dir "$base\domain\security"; Ensure-Dir "$base\dto\security"; Ensure-Dir "$base\controller\security"
Ensure-Dir "$base\repository\security"; Ensure-Dir "$base\service\security\impl"

foreach ($e in @("SocialsProvider.java","SocialsAssociate.java","SocialsProviderLogin.java")) {
    Move-Set "$base\entity\$e" "$base\domain\security\$e" "com.financial.cloud.domain.security"
}
foreach ($e in @("ConfigPasswordPolicy.java","ConfigLoginPolicy.java","ConfigEmailSenders.java","ConfigSmsProvider.java")) {
    Move-Set "$base\entity\config\$e" "$base\domain\security\$e" "com.financial.cloud.domain.security"
}

# security-related DTOs from entity/dto and config/dto
foreach ($d in @("SocialsProviderPageDto.java","PasswordEncryptPageDto.java","WeakPasswordPageDto.java","BlacklistPageDto.java")) {
    $src = "$base\entity\dto\$d"
    if (-not (Test-Path $src)) { $src = "$base\entity\config\dto\$d" }
    Move-Set $src "$base\dto\security\$d" "com.financial.cloud.dto.security"
}

foreach ($c in @("ConfigPasswordPolicyController.java","ConfigLoginPolicyController.java","ConfigEmailSendersController.java","ConfigSmsProviderController.java","SocialsProviderController.java")) {
    Move-Set "$base\web\security\controller\$c" "$base\controller\security\$c" "com.financial.cloud.controller.security"
}

foreach ($m in @("SocialsProviderMapper.java","SocialsAssociateMapper.java","ConfigPasswordPolicyMapper.java","ConfigLoginPolicyMapper.java","ConfigEmailSendersMapper.java","ConfigSmsProviderMapper.java")) {
    Move-Set "$base\persistence\mapper\$m" "$base\repository\security\$m" "com.financial.cloud.repository.security"
}
foreach ($s in @("SocialsProviderService.java","SocialsAssociatesService.java","ConfigPasswordPolicyService.java","ConfigLoginPolicyService.java","ConfigEmailSendersService.java","ConfigSmsProviderService.java","PasswordPolicyValidatorService.java")) {
    Move-Set "$base\persistence\service\$s" "$base\service\security\$s" "com.financial.cloud.service.security"
}
foreach ($i in @("SocialsProviderServiceImpl.java","SocialsAssociatesServiceImpl.java","ConfigPasswordPolicyServiceImpl.java","ConfigLoginPolicyServiceImpl.java","ConfigEmailSendersServiceImpl.java","ConfigSmsProviderServiceImpl.java","PasswordPolicyValidatorServiceImpl.java","PasswordPolicyMessageResolver.java")) {
    Move-Set "$base\persistence\service\impl\$i" "$base\service\security\impl\$i" "com.financial.cloud.service.security.impl"
}
foreach ($x in @("SocialsProviderMapper.xml","SocialsAssociateMapper.xml","ConfigPasswordPolicyMapper.xml","ConfigLoginPolicyMapper.xml")) { Move-Xml $x "security" }

# ========== replacements ==========
$replacements = [ordered]@{
    # idm
    'com.financial.cloud.entity.idm.dto.' = 'com.financial.cloud.dto.idm.'
    'com.financial.cloud.entity.idm.' = 'com.financial.cloud.domain.idm.'
    'com.financial.cloud.entity.permissions.dto.RolesPageDto' = 'com.financial.cloud.dto.idm.RolesPageDto'
    'com.financial.cloud.entity.permissions.dto.RoleMemberPageDto' = 'com.financial.cloud.dto.idm.RoleMemberPageDto'
    'com.financial.cloud.entity.permissions.dto.RoleMemberDto' = 'com.financial.cloud.dto.idm.RoleMemberDto'
    'com.financial.cloud.entity.permissions.dto.RoleMemberUserGroupsDto' = 'com.financial.cloud.dto.idm.RoleMemberUserGroupsDto'
    'com.financial.cloud.entity.permissions.Roles' = 'com.financial.cloud.domain.idm.Roles'
    'com.financial.cloud.entity.permissions.RoleMember' = 'com.financial.cloud.domain.idm.RoleMember'
    'com.financial.cloud.web.idm.controller.' = 'com.financial.cloud.controller.idm.'

    # permissions
    'com.financial.cloud.entity.permissions.dto.' = 'com.financial.cloud.dto.permissions.'
    'com.financial.cloud.entity.permissions.' = 'com.financial.cloud.domain.permissions.'
    'com.financial.cloud.entity.access.dto.' = 'com.financial.cloud.dto.permissions.'
    'com.financial.cloud.entity.access.' = 'com.financial.cloud.domain.permissions.'
    'com.financial.cloud.web.permissions.controller.' = 'com.financial.cloud.controller.permissions.'
    'com.financial.cloud.web.access.controller.' = 'com.financial.cloud.controller.permissions.'

    # security entities
    'com.financial.cloud.entity.SocialsProviderLogin' = 'com.financial.cloud.domain.security.SocialsProviderLogin'
    'com.financial.cloud.entity.SocialsProvider' = 'com.financial.cloud.domain.security.SocialsProvider'
    'com.financial.cloud.entity.SocialsAssociate' = 'com.financial.cloud.domain.security.SocialsAssociate'
    'com.financial.cloud.entity.config.ConfigPasswordPolicy' = 'com.financial.cloud.domain.security.ConfigPasswordPolicy'
    'com.financial.cloud.entity.config.ConfigLoginPolicy' = 'com.financial.cloud.domain.security.ConfigLoginPolicy'
    'com.financial.cloud.entity.config.ConfigEmailSenders' = 'com.financial.cloud.domain.security.ConfigEmailSenders'
    'com.financial.cloud.entity.config.ConfigSmsProvider' = 'com.financial.cloud.domain.security.ConfigSmsProvider'
    'com.financial.cloud.entity.dto.SocialsProviderPageDto' = 'com.financial.cloud.dto.security.SocialsProviderPageDto'
    'com.financial.cloud.entity.config.dto.PasswordEncryptPageDto' = 'com.financial.cloud.dto.security.PasswordEncryptPageDto'
    'com.financial.cloud.entity.config.dto.WeakPasswordPageDto' = 'com.financial.cloud.dto.security.WeakPasswordPageDto'
    'com.financial.cloud.entity.config.dto.BlacklistPageDto' = 'com.financial.cloud.dto.security.BlacklistPageDto'
    'com.financial.cloud.web.security.controller.' = 'com.financial.cloud.controller.security.'

    # mappers - longest first
    'com.financial.cloud.persistence.mapper.UserInstInfo' = 'com.financial.cloud.repository.idm.UserInstInfo'
    'com.financial.cloud.persistence.mapper.UserInfo' = 'com.financial.cloud.repository.idm.UserInfo'
    'com.financial.cloud.persistence.mapper.Organizations' = 'com.financial.cloud.repository.idm.Organizations'
    'com.financial.cloud.persistence.mapper.RoleMember' = 'com.financial.cloud.repository.idm.RoleMember'
    'com.financial.cloud.persistence.mapper.Roles' = 'com.financial.cloud.repository.idm.Roles'
    'com.financial.cloud.persistence.mapper.PermissionBook' = 'com.financial.cloud.repository.permissions.PermissionBook'
    'com.financial.cloud.persistence.mapper.Permission' = 'com.financial.cloud.repository.permissions.Permission'
    'com.financial.cloud.persistence.mapper.Resources' = 'com.financial.cloud.repository.permissions.Resources'
    'com.financial.cloud.persistence.mapper.SessionList' = 'com.financial.cloud.repository.permissions.SessionList'
    'com.financial.cloud.persistence.mapper.AuthzResource' = 'com.financial.cloud.repository.permissions.AuthzResource'
    'com.financial.cloud.persistence.mapper.Authz' = 'com.financial.cloud.repository.permissions.Authz'
    'com.financial.cloud.persistence.mapper.SocialsProvider' = 'com.financial.cloud.repository.security.SocialsProvider'
    'com.financial.cloud.persistence.mapper.SocialsAssociate' = 'com.financial.cloud.repository.security.SocialsAssociate'
    'com.financial.cloud.persistence.mapper.ConfigPasswordPolicy' = 'com.financial.cloud.repository.security.ConfigPasswordPolicy'
    'com.financial.cloud.persistence.mapper.ConfigLoginPolicy' = 'com.financial.cloud.repository.security.ConfigLoginPolicy'
    'com.financial.cloud.persistence.mapper.ConfigEmailSenders' = 'com.financial.cloud.repository.security.ConfigEmailSenders'
    'com.financial.cloud.persistence.mapper.ConfigSmsProvider' = 'com.financial.cloud.repository.security.ConfigSmsProvider'

    # services - longest first
    'com.financial.cloud.persistence.service.impl.UserInstInfo' = 'com.financial.cloud.service.idm.impl.UserInstInfo'
    'com.financial.cloud.persistence.service.impl.UserInfoExcel' = 'com.financial.cloud.service.idm.impl.UserInfoExcel'
    'com.financial.cloud.persistence.service.impl.UserInfo' = 'com.financial.cloud.service.idm.impl.UserInfo'
    'com.financial.cloud.persistence.service.impl.OrganizationsExcel' = 'com.financial.cloud.service.idm.impl.OrganizationsExcel'
    'com.financial.cloud.persistence.service.impl.Organizations' = 'com.financial.cloud.service.idm.impl.Organizations'
    'com.financial.cloud.persistence.service.impl.RoleMember' = 'com.financial.cloud.service.idm.impl.RoleMember'
    'com.financial.cloud.persistence.service.impl.Roles' = 'com.financial.cloud.service.idm.impl.Roles'
    'com.financial.cloud.persistence.service.impl.PermissionBook' = 'com.financial.cloud.service.permissions.impl.PermissionBook'
    'com.financial.cloud.persistence.service.impl.Permission' = 'com.financial.cloud.service.permissions.impl.Permission'
    'com.financial.cloud.persistence.service.impl.Resources' = 'com.financial.cloud.service.permissions.impl.Resources'
    'com.financial.cloud.persistence.service.impl.SessionList' = 'com.financial.cloud.service.permissions.impl.SessionList'
    'com.financial.cloud.persistence.service.impl.AuthzResource' = 'com.financial.cloud.service.permissions.impl.AuthzResource'
    'com.financial.cloud.persistence.service.impl.Authz' = 'com.financial.cloud.service.permissions.impl.Authz'
    'com.financial.cloud.persistence.service.impl.SocialsProvider' = 'com.financial.cloud.service.security.impl.SocialsProvider'
    'com.financial.cloud.persistence.service.impl.SocialsAssociates' = 'com.financial.cloud.service.security.impl.SocialsAssociates'
    'com.financial.cloud.persistence.service.impl.ConfigPasswordPolicy' = 'com.financial.cloud.service.security.impl.ConfigPasswordPolicy'
    'com.financial.cloud.persistence.service.impl.ConfigLoginPolicy' = 'com.financial.cloud.service.security.impl.ConfigLoginPolicy'
    'com.financial.cloud.persistence.service.impl.ConfigEmailSenders' = 'com.financial.cloud.service.security.impl.ConfigEmailSenders'
    'com.financial.cloud.persistence.service.impl.ConfigSmsProvider' = 'com.financial.cloud.service.security.impl.ConfigSmsProvider'
    'com.financial.cloud.persistence.service.impl.PasswordPolicyValidator' = 'com.financial.cloud.service.security.impl.PasswordPolicyValidator'
    'com.financial.cloud.persistence.service.impl.PasswordPolicyMessageResolver' = 'com.financial.cloud.service.security.impl.PasswordPolicyMessageResolver'

    'com.financial.cloud.persistence.service.UserInstInfo' = 'com.financial.cloud.service.idm.UserInstInfo'
    'com.financial.cloud.persistence.service.UserInfoExcel' = 'com.financial.cloud.service.idm.UserInfoExcel'
    'com.financial.cloud.persistence.service.UserInfo' = 'com.financial.cloud.service.idm.UserInfo'
    'com.financial.cloud.persistence.service.OrganizationsExcel' = 'com.financial.cloud.service.idm.OrganizationsExcel'
    'com.financial.cloud.persistence.service.Organizations' = 'com.financial.cloud.service.idm.Organizations'
    'com.financial.cloud.persistence.service.RoleMember' = 'com.financial.cloud.service.idm.RoleMember'
    'com.financial.cloud.persistence.service.Roles' = 'com.financial.cloud.service.idm.Roles'
    'com.financial.cloud.persistence.service.PermissionBook' = 'com.financial.cloud.service.permissions.PermissionBook'
    'com.financial.cloud.persistence.service.Permission' = 'com.financial.cloud.service.permissions.Permission'
    'com.financial.cloud.persistence.service.Resources' = 'com.financial.cloud.service.permissions.Resources'
    'com.financial.cloud.persistence.service.SessionList' = 'com.financial.cloud.service.permissions.SessionList'
    'com.financial.cloud.persistence.service.AuthzResource' = 'com.financial.cloud.service.permissions.AuthzResource'
    'com.financial.cloud.persistence.service.Authz' = 'com.financial.cloud.service.permissions.Authz'
    'com.financial.cloud.persistence.service.SocialsProvider' = 'com.financial.cloud.service.security.SocialsProvider'
    'com.financial.cloud.persistence.service.SocialsAssociates' = 'com.financial.cloud.service.security.SocialsAssociates'
    'com.financial.cloud.persistence.service.ConfigPasswordPolicy' = 'com.financial.cloud.service.security.ConfigPasswordPolicy'
    'com.financial.cloud.persistence.service.ConfigLoginPolicy' = 'com.financial.cloud.service.security.ConfigLoginPolicy'
    'com.financial.cloud.persistence.service.ConfigEmailSenders' = 'com.financial.cloud.service.security.ConfigEmailSenders'
    'com.financial.cloud.persistence.service.ConfigSmsProvider' = 'com.financial.cloud.service.security.ConfigSmsProvider'
    'com.financial.cloud.persistence.service.PasswordPolicyValidator' = 'com.financial.cloud.service.security.PasswordPolicyValidator'
}

Get-ChildItem -Path $srcRoot -Recurse -Include *.java,*.xml | ForEach-Object {
    $c = [System.IO.File]::ReadAllText($_.FullName)
    $orig = $c
    foreach ($k in $replacements.Keys) { $c = $c.Replace($k, $replacements[$k]) }
    if ($c -ne $orig) { [System.IO.File]::WriteAllText($_.FullName, $c) }
}

# Cross-domain + own-interface imports
$symbols = [ordered]@{
    'UserInfoService'='import com.financial.cloud.service.idm.UserInfoService;'
    'UserInfoExcelService'='import com.financial.cloud.service.idm.UserInfoExcelService;'
    'UserInstInfoService'='import com.financial.cloud.service.idm.UserInstInfoService;'
    'OrganizationsService'='import com.financial.cloud.service.idm.OrganizationsService;'
    'OrganizationsExcelService'='import com.financial.cloud.service.idm.OrganizationsExcelService;'
    'RolesService'='import com.financial.cloud.service.idm.RolesService;'
    'RoleMemberService'='import com.financial.cloud.service.idm.RoleMemberService;'
    'UserInfoMapper'='import com.financial.cloud.repository.idm.UserInfoMapper;'
    'UserInstInfoMapper'='import com.financial.cloud.repository.idm.UserInstInfoMapper;'
    'OrganizationsMapper'='import com.financial.cloud.repository.idm.OrganizationsMapper;'
    'RolesMapper'='import com.financial.cloud.repository.idm.RolesMapper;'
    'RoleMemberMapper'='import com.financial.cloud.repository.idm.RoleMemberMapper;'
    'PermissionService'='import com.financial.cloud.service.permissions.PermissionService;'
    'PermissionBookService'='import com.financial.cloud.service.permissions.PermissionBookService;'
    'ResourcesService'='import com.financial.cloud.service.permissions.ResourcesService;'
    'SessionListService'='import com.financial.cloud.service.permissions.SessionListService;'
    'AuthzService'='import com.financial.cloud.service.permissions.AuthzService;'
    'AuthzResourceService'='import com.financial.cloud.service.permissions.AuthzResourceService;'
    'PermissionMapper'='import com.financial.cloud.repository.permissions.PermissionMapper;'
    'PermissionBookMapper'='import com.financial.cloud.repository.permissions.PermissionBookMapper;'
    'ResourcesMapper'='import com.financial.cloud.repository.permissions.ResourcesMapper;'
    'SessionListMapper'='import com.financial.cloud.repository.permissions.SessionListMapper;'
    'AuthzMapper'='import com.financial.cloud.repository.permissions.AuthzMapper;'
    'AuthzResourceMapper'='import com.financial.cloud.repository.permissions.AuthzResourceMapper;'
    'SocialsProviderService'='import com.financial.cloud.service.security.SocialsProviderService;'
    'SocialsAssociatesService'='import com.financial.cloud.service.security.SocialsAssociatesService;'
    'ConfigPasswordPolicyService'='import com.financial.cloud.service.security.ConfigPasswordPolicyService;'
    'ConfigLoginPolicyService'='import com.financial.cloud.service.security.ConfigLoginPolicyService;'
    'ConfigEmailSendersService'='import com.financial.cloud.service.security.ConfigEmailSendersService;'
    'ConfigSmsProviderService'='import com.financial.cloud.service.security.ConfigSmsProviderService;'
    'PasswordPolicyValidatorService'='import com.financial.cloud.service.security.PasswordPolicyValidatorService;'
    'SocialsProviderMapper'='import com.financial.cloud.repository.security.SocialsProviderMapper;'
    'SocialsAssociateMapper'='import com.financial.cloud.repository.security.SocialsAssociateMapper;'
    'ConfigPasswordPolicyMapper'='import com.financial.cloud.repository.security.ConfigPasswordPolicyMapper;'
    'ConfigLoginPolicyMapper'='import com.financial.cloud.repository.security.ConfigLoginPolicyMapper;'
    'ConfigEmailSendersMapper'='import com.financial.cloud.repository.security.ConfigEmailSendersMapper;'
    'ConfigSmsProviderMapper'='import com.financial.cloud.repository.security.ConfigSmsProviderMapper;'
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

# cleanup empty
@(
    "$base\entity\idm\dto","$base\entity\idm",
    "$base\entity\permissions\dto","$base\entity\permissions",
    "$base\entity\access\dto","$base\entity\access",
    "$base\web\idm\controller","$base\web\idm",
    "$base\web\permissions\controller","$base\web\permissions",
    "$base\web\access\controller","$base\web\access",
    "$base\web\security\controller","$base\web\security"
) | ForEach-Object {
    if ((Test-Path $_) -and -not (Get-ChildItem $_ -Force -ErrorAction SilentlyContinue)) {
        Remove-Item $_ -Force -Recurse -ErrorAction SilentlyContinue
    }
}

Write-Host "Wave 4 idm+permissions+security complete."
