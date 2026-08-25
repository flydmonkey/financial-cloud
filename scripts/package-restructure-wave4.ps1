# Wave 4: idm + permissions + security
$ErrorActionPreference = "Stop"
$base = "C:\Users\Administrator\Projects\jinbooks\jinbooks\src\main\java\com\jinbooks"
$resBase = "C:\Users\Administrator\Projects\jinbooks\jinbooks\src\main\resources"
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
    $c = $c -replace 'com\.jinbooks\.persistence\.mapper\.', "com.jinbooks.repository.$domain."
    [System.IO.File]::WriteAllText($dst, $c)
    Write-Host "XML -> $domain/$name"
}

# ========== IDM ==========
Ensure-Dir "$base\domain\idm"; Ensure-Dir "$base\dto\idm"; Ensure-Dir "$base\controller\idm"
Ensure-Dir "$base\repository\idm"; Ensure-Dir "$base\service\idm\impl"

# entities from idm + Roles/RoleMember (controllers live under idm)
foreach ($e in @("UserInfo.java","UserInstInfo.java","Organizations.java")) {
    Move-Set "$base\entity\idm\$e" "$base\domain\idm\$e" "com.jinbooks.domain.idm"
}
Move-Set "$base\entity\permissions\Roles.java" "$base\domain\idm\Roles.java" "com.jinbooks.domain.idm"
Move-Set "$base\entity\permissions\RoleMember.java" "$base\domain\idm\RoleMember.java" "com.jinbooks.domain.idm"

Get-ChildItem "$base\entity\idm\dto\*.java" -ErrorAction SilentlyContinue | ForEach-Object {
    Move-Set $_.FullName "$base\dto\idm\$($_.Name)" "com.jinbooks.dto.idm"
}
# Roles/RoleMember DTOs currently under permissions/dto
foreach ($d in @("RolesPageDto.java","RoleMemberPageDto.java","RoleMemberDto.java","RoleMemberUserGroupsDto.java")) {
    Move-Set "$base\entity\permissions\dto\$d" "$base\dto\idm\$d" "com.jinbooks.dto.idm"
}

foreach ($c in @("UserInfoController.java","UserInstInfoController.java","OrganizationsController.java","RolesController.java","RoleMemberController.java")) {
    Move-Set "$base\web\idm\controller\$c" "$base\controller\idm\$c" "com.jinbooks.controller.idm"
}

foreach ($m in @("UserInfoMapper.java","UserInstInfoMapper.java","OrganizationsMapper.java","RolesMapper.java","RoleMemberMapper.java")) {
    Move-Set "$base\persistence\mapper\$m" "$base\repository\idm\$m" "com.jinbooks.repository.idm"
}
foreach ($s in @("UserInfoService.java","UserInfoExcelService.java","UserInstInfoService.java","OrganizationsService.java","OrganizationsExcelService.java","RolesService.java","RoleMemberService.java")) {
    Move-Set "$base\persistence\service\$s" "$base\service\idm\$s" "com.jinbooks.service.idm"
}
foreach ($i in @("UserInfoServiceImpl.java","UserInfoExcelServiceImpl.java","UserInstInfoServiceImpl.java","OrganizationsServiceImpl.java","OrganizationsExcelServiceImpl.java","RolesServiceImpl.java","RoleMemberServiceImpl.java")) {
    Move-Set "$base\persistence\service\impl\$i" "$base\service\idm\impl\$i" "com.jinbooks.service.idm.impl"
}
foreach ($x in @("UserInfoMapper.xml","UserInstInfoMapper.xml","OrganizationsMapper.xml","RolesMapper.xml","RoleMemberMapper.xml")) { Move-Xml $x "idm" }

# ========== PERMISSIONS ==========
Ensure-Dir "$base\domain\permissions"; Ensure-Dir "$base\dto\permissions"; Ensure-Dir "$base\controller\permissions"
Ensure-Dir "$base\repository\permissions"; Ensure-Dir "$base\service\permissions\impl"

foreach ($e in @("Permission.java","PermissionBook.java","Resources.java")) {
    Move-Set "$base\entity\permissions\$e" "$base\domain\permissions\$e" "com.jinbooks.domain.permissions"
}
Move-Set "$base\entity\access\SessionList.java" "$base\domain\permissions\SessionList.java" "com.jinbooks.domain.permissions"

Get-ChildItem "$base\entity\permissions\dto\*.java" -ErrorAction SilentlyContinue | ForEach-Object {
    Move-Set $_.FullName "$base\dto\permissions\$($_.Name)" "com.jinbooks.dto.permissions"
}
Move-Set "$base\entity\access\dto\SessionListPageDto.java" "$base\dto\permissions\SessionListPageDto.java" "com.jinbooks.dto.permissions"

foreach ($c in @("PermissionController.java","PermissionBookController.java","ResourcesController.java")) {
    Move-Set "$base\web\permissions\controller\$c" "$base\controller\permissions\$c" "com.jinbooks.controller.permissions"
}
foreach ($c in @("SessionController.java","OpenFuncListController.java")) {
    Move-Set "$base\web\access\controller\$c" "$base\controller\permissions\$c" "com.jinbooks.controller.permissions"
}

foreach ($m in @("PermissionMapper.java","PermissionBookMapper.java","ResourcesMapper.java","SessionListMapper.java","AuthzMapper.java","AuthzResourceMapper.java")) {
    Move-Set "$base\persistence\mapper\$m" "$base\repository\permissions\$m" "com.jinbooks.repository.permissions"
}
foreach ($s in @("PermissionService.java","PermissionBookService.java","ResourcesService.java","SessionListService.java","AuthzService.java","AuthzResourceService.java")) {
    Move-Set "$base\persistence\service\$s" "$base\service\permissions\$s" "com.jinbooks.service.permissions"
}
foreach ($i in @("PermissionServiceImpl.java","PermissionBookServiceImpl.java","ResourcesServiceImpl.java","SessionListServiceImpl.java","AuthzServiceImpl.java","AuthzResourceServiceImpl.java")) {
    Move-Set "$base\persistence\service\impl\$i" "$base\service\permissions\impl\$i" "com.jinbooks.service.permissions.impl"
}
foreach ($x in @("PermissionMapper.xml","PermissionBookMapper.xml","ResourcesMapper.xml","SessionListMapper.xml","AuthzMapper.xml","AuthzResourceMapper.xml")) { Move-Xml $x "permissions" }

# ========== SECURITY ==========
Ensure-Dir "$base\domain\security"; Ensure-Dir "$base\dto\security"; Ensure-Dir "$base\controller\security"
Ensure-Dir "$base\repository\security"; Ensure-Dir "$base\service\security\impl"

foreach ($e in @("SocialsProvider.java","SocialsAssociate.java","SocialsProviderLogin.java")) {
    Move-Set "$base\entity\$e" "$base\domain\security\$e" "com.jinbooks.domain.security"
}
foreach ($e in @("ConfigPasswordPolicy.java","ConfigLoginPolicy.java","ConfigEmailSenders.java","ConfigSmsProvider.java")) {
    Move-Set "$base\entity\config\$e" "$base\domain\security\$e" "com.jinbooks.domain.security"
}

# security-related DTOs from entity/dto and config/dto
foreach ($d in @("SocialsProviderPageDto.java","PasswordEncryptPageDto.java","WeakPasswordPageDto.java","BlacklistPageDto.java")) {
    $src = "$base\entity\dto\$d"
    if (-not (Test-Path $src)) { $src = "$base\entity\config\dto\$d" }
    Move-Set $src "$base\dto\security\$d" "com.jinbooks.dto.security"
}

foreach ($c in @("ConfigPasswordPolicyController.java","ConfigLoginPolicyController.java","ConfigEmailSendersController.java","ConfigSmsProviderController.java","SocialsProviderController.java")) {
    Move-Set "$base\web\security\controller\$c" "$base\controller\security\$c" "com.jinbooks.controller.security"
}

foreach ($m in @("SocialsProviderMapper.java","SocialsAssociateMapper.java","ConfigPasswordPolicyMapper.java","ConfigLoginPolicyMapper.java","ConfigEmailSendersMapper.java","ConfigSmsProviderMapper.java")) {
    Move-Set "$base\persistence\mapper\$m" "$base\repository\security\$m" "com.jinbooks.repository.security"
}
foreach ($s in @("SocialsProviderService.java","SocialsAssociatesService.java","ConfigPasswordPolicyService.java","ConfigLoginPolicyService.java","ConfigEmailSendersService.java","ConfigSmsProviderService.java","PasswordPolicyValidatorService.java")) {
    Move-Set "$base\persistence\service\$s" "$base\service\security\$s" "com.jinbooks.service.security"
}
foreach ($i in @("SocialsProviderServiceImpl.java","SocialsAssociatesServiceImpl.java","ConfigPasswordPolicyServiceImpl.java","ConfigLoginPolicyServiceImpl.java","ConfigEmailSendersServiceImpl.java","ConfigSmsProviderServiceImpl.java","PasswordPolicyValidatorServiceImpl.java","PasswordPolicyMessageResolver.java")) {
    Move-Set "$base\persistence\service\impl\$i" "$base\service\security\impl\$i" "com.jinbooks.service.security.impl"
}
foreach ($x in @("SocialsProviderMapper.xml","SocialsAssociateMapper.xml","ConfigPasswordPolicyMapper.xml","ConfigLoginPolicyMapper.xml")) { Move-Xml $x "security" }

# ========== replacements ==========
$replacements = [ordered]@{
    # idm
    'com.jinbooks.entity.idm.dto.' = 'com.jinbooks.dto.idm.'
    'com.jinbooks.entity.idm.' = 'com.jinbooks.domain.idm.'
    'com.jinbooks.entity.permissions.dto.RolesPageDto' = 'com.jinbooks.dto.idm.RolesPageDto'
    'com.jinbooks.entity.permissions.dto.RoleMemberPageDto' = 'com.jinbooks.dto.idm.RoleMemberPageDto'
    'com.jinbooks.entity.permissions.dto.RoleMemberDto' = 'com.jinbooks.dto.idm.RoleMemberDto'
    'com.jinbooks.entity.permissions.dto.RoleMemberUserGroupsDto' = 'com.jinbooks.dto.idm.RoleMemberUserGroupsDto'
    'com.jinbooks.entity.permissions.Roles' = 'com.jinbooks.domain.idm.Roles'
    'com.jinbooks.entity.permissions.RoleMember' = 'com.jinbooks.domain.idm.RoleMember'
    'com.jinbooks.web.idm.controller.' = 'com.jinbooks.controller.idm.'

    # permissions
    'com.jinbooks.entity.permissions.dto.' = 'com.jinbooks.dto.permissions.'
    'com.jinbooks.entity.permissions.' = 'com.jinbooks.domain.permissions.'
    'com.jinbooks.entity.access.dto.' = 'com.jinbooks.dto.permissions.'
    'com.jinbooks.entity.access.' = 'com.jinbooks.domain.permissions.'
    'com.jinbooks.web.permissions.controller.' = 'com.jinbooks.controller.permissions.'
    'com.jinbooks.web.access.controller.' = 'com.jinbooks.controller.permissions.'

    # security entities
    'com.jinbooks.entity.SocialsProviderLogin' = 'com.jinbooks.domain.security.SocialsProviderLogin'
    'com.jinbooks.entity.SocialsProvider' = 'com.jinbooks.domain.security.SocialsProvider'
    'com.jinbooks.entity.SocialsAssociate' = 'com.jinbooks.domain.security.SocialsAssociate'
    'com.jinbooks.entity.config.ConfigPasswordPolicy' = 'com.jinbooks.domain.security.ConfigPasswordPolicy'
    'com.jinbooks.entity.config.ConfigLoginPolicy' = 'com.jinbooks.domain.security.ConfigLoginPolicy'
    'com.jinbooks.entity.config.ConfigEmailSenders' = 'com.jinbooks.domain.security.ConfigEmailSenders'
    'com.jinbooks.entity.config.ConfigSmsProvider' = 'com.jinbooks.domain.security.ConfigSmsProvider'
    'com.jinbooks.entity.dto.SocialsProviderPageDto' = 'com.jinbooks.dto.security.SocialsProviderPageDto'
    'com.jinbooks.entity.config.dto.PasswordEncryptPageDto' = 'com.jinbooks.dto.security.PasswordEncryptPageDto'
    'com.jinbooks.entity.config.dto.WeakPasswordPageDto' = 'com.jinbooks.dto.security.WeakPasswordPageDto'
    'com.jinbooks.entity.config.dto.BlacklistPageDto' = 'com.jinbooks.dto.security.BlacklistPageDto'
    'com.jinbooks.web.security.controller.' = 'com.jinbooks.controller.security.'

    # mappers - longest first
    'com.jinbooks.persistence.mapper.UserInstInfo' = 'com.jinbooks.repository.idm.UserInstInfo'
    'com.jinbooks.persistence.mapper.UserInfo' = 'com.jinbooks.repository.idm.UserInfo'
    'com.jinbooks.persistence.mapper.Organizations' = 'com.jinbooks.repository.idm.Organizations'
    'com.jinbooks.persistence.mapper.RoleMember' = 'com.jinbooks.repository.idm.RoleMember'
    'com.jinbooks.persistence.mapper.Roles' = 'com.jinbooks.repository.idm.Roles'
    'com.jinbooks.persistence.mapper.PermissionBook' = 'com.jinbooks.repository.permissions.PermissionBook'
    'com.jinbooks.persistence.mapper.Permission' = 'com.jinbooks.repository.permissions.Permission'
    'com.jinbooks.persistence.mapper.Resources' = 'com.jinbooks.repository.permissions.Resources'
    'com.jinbooks.persistence.mapper.SessionList' = 'com.jinbooks.repository.permissions.SessionList'
    'com.jinbooks.persistence.mapper.AuthzResource' = 'com.jinbooks.repository.permissions.AuthzResource'
    'com.jinbooks.persistence.mapper.Authz' = 'com.jinbooks.repository.permissions.Authz'
    'com.jinbooks.persistence.mapper.SocialsProvider' = 'com.jinbooks.repository.security.SocialsProvider'
    'com.jinbooks.persistence.mapper.SocialsAssociate' = 'com.jinbooks.repository.security.SocialsAssociate'
    'com.jinbooks.persistence.mapper.ConfigPasswordPolicy' = 'com.jinbooks.repository.security.ConfigPasswordPolicy'
    'com.jinbooks.persistence.mapper.ConfigLoginPolicy' = 'com.jinbooks.repository.security.ConfigLoginPolicy'
    'com.jinbooks.persistence.mapper.ConfigEmailSenders' = 'com.jinbooks.repository.security.ConfigEmailSenders'
    'com.jinbooks.persistence.mapper.ConfigSmsProvider' = 'com.jinbooks.repository.security.ConfigSmsProvider'

    # services - longest first
    'com.jinbooks.persistence.service.impl.UserInstInfo' = 'com.jinbooks.service.idm.impl.UserInstInfo'
    'com.jinbooks.persistence.service.impl.UserInfoExcel' = 'com.jinbooks.service.idm.impl.UserInfoExcel'
    'com.jinbooks.persistence.service.impl.UserInfo' = 'com.jinbooks.service.idm.impl.UserInfo'
    'com.jinbooks.persistence.service.impl.OrganizationsExcel' = 'com.jinbooks.service.idm.impl.OrganizationsExcel'
    'com.jinbooks.persistence.service.impl.Organizations' = 'com.jinbooks.service.idm.impl.Organizations'
    'com.jinbooks.persistence.service.impl.RoleMember' = 'com.jinbooks.service.idm.impl.RoleMember'
    'com.jinbooks.persistence.service.impl.Roles' = 'com.jinbooks.service.idm.impl.Roles'
    'com.jinbooks.persistence.service.impl.PermissionBook' = 'com.jinbooks.service.permissions.impl.PermissionBook'
    'com.jinbooks.persistence.service.impl.Permission' = 'com.jinbooks.service.permissions.impl.Permission'
    'com.jinbooks.persistence.service.impl.Resources' = 'com.jinbooks.service.permissions.impl.Resources'
    'com.jinbooks.persistence.service.impl.SessionList' = 'com.jinbooks.service.permissions.impl.SessionList'
    'com.jinbooks.persistence.service.impl.AuthzResource' = 'com.jinbooks.service.permissions.impl.AuthzResource'
    'com.jinbooks.persistence.service.impl.Authz' = 'com.jinbooks.service.permissions.impl.Authz'
    'com.jinbooks.persistence.service.impl.SocialsProvider' = 'com.jinbooks.service.security.impl.SocialsProvider'
    'com.jinbooks.persistence.service.impl.SocialsAssociates' = 'com.jinbooks.service.security.impl.SocialsAssociates'
    'com.jinbooks.persistence.service.impl.ConfigPasswordPolicy' = 'com.jinbooks.service.security.impl.ConfigPasswordPolicy'
    'com.jinbooks.persistence.service.impl.ConfigLoginPolicy' = 'com.jinbooks.service.security.impl.ConfigLoginPolicy'
    'com.jinbooks.persistence.service.impl.ConfigEmailSenders' = 'com.jinbooks.service.security.impl.ConfigEmailSenders'
    'com.jinbooks.persistence.service.impl.ConfigSmsProvider' = 'com.jinbooks.service.security.impl.ConfigSmsProvider'
    'com.jinbooks.persistence.service.impl.PasswordPolicyValidator' = 'com.jinbooks.service.security.impl.PasswordPolicyValidator'
    'com.jinbooks.persistence.service.impl.PasswordPolicyMessageResolver' = 'com.jinbooks.service.security.impl.PasswordPolicyMessageResolver'

    'com.jinbooks.persistence.service.UserInstInfo' = 'com.jinbooks.service.idm.UserInstInfo'
    'com.jinbooks.persistence.service.UserInfoExcel' = 'com.jinbooks.service.idm.UserInfoExcel'
    'com.jinbooks.persistence.service.UserInfo' = 'com.jinbooks.service.idm.UserInfo'
    'com.jinbooks.persistence.service.OrganizationsExcel' = 'com.jinbooks.service.idm.OrganizationsExcel'
    'com.jinbooks.persistence.service.Organizations' = 'com.jinbooks.service.idm.Organizations'
    'com.jinbooks.persistence.service.RoleMember' = 'com.jinbooks.service.idm.RoleMember'
    'com.jinbooks.persistence.service.Roles' = 'com.jinbooks.service.idm.Roles'
    'com.jinbooks.persistence.service.PermissionBook' = 'com.jinbooks.service.permissions.PermissionBook'
    'com.jinbooks.persistence.service.Permission' = 'com.jinbooks.service.permissions.Permission'
    'com.jinbooks.persistence.service.Resources' = 'com.jinbooks.service.permissions.Resources'
    'com.jinbooks.persistence.service.SessionList' = 'com.jinbooks.service.permissions.SessionList'
    'com.jinbooks.persistence.service.AuthzResource' = 'com.jinbooks.service.permissions.AuthzResource'
    'com.jinbooks.persistence.service.Authz' = 'com.jinbooks.service.permissions.Authz'
    'com.jinbooks.persistence.service.SocialsProvider' = 'com.jinbooks.service.security.SocialsProvider'
    'com.jinbooks.persistence.service.SocialsAssociates' = 'com.jinbooks.service.security.SocialsAssociates'
    'com.jinbooks.persistence.service.ConfigPasswordPolicy' = 'com.jinbooks.service.security.ConfigPasswordPolicy'
    'com.jinbooks.persistence.service.ConfigLoginPolicy' = 'com.jinbooks.service.security.ConfigLoginPolicy'
    'com.jinbooks.persistence.service.ConfigEmailSenders' = 'com.jinbooks.service.security.ConfigEmailSenders'
    'com.jinbooks.persistence.service.ConfigSmsProvider' = 'com.jinbooks.service.security.ConfigSmsProvider'
    'com.jinbooks.persistence.service.PasswordPolicyValidator' = 'com.jinbooks.service.security.PasswordPolicyValidator'
}

Get-ChildItem -Path $srcRoot -Recurse -Include *.java,*.xml | ForEach-Object {
    $c = [System.IO.File]::ReadAllText($_.FullName)
    $orig = $c
    foreach ($k in $replacements.Keys) { $c = $c.Replace($k, $replacements[$k]) }
    if ($c -ne $orig) { [System.IO.File]::WriteAllText($_.FullName, $c) }
}

# Cross-domain + own-interface imports
$symbols = [ordered]@{
    'UserInfoService'='import com.jinbooks.service.idm.UserInfoService;'
    'UserInfoExcelService'='import com.jinbooks.service.idm.UserInfoExcelService;'
    'UserInstInfoService'='import com.jinbooks.service.idm.UserInstInfoService;'
    'OrganizationsService'='import com.jinbooks.service.idm.OrganizationsService;'
    'OrganizationsExcelService'='import com.jinbooks.service.idm.OrganizationsExcelService;'
    'RolesService'='import com.jinbooks.service.idm.RolesService;'
    'RoleMemberService'='import com.jinbooks.service.idm.RoleMemberService;'
    'UserInfoMapper'='import com.jinbooks.repository.idm.UserInfoMapper;'
    'UserInstInfoMapper'='import com.jinbooks.repository.idm.UserInstInfoMapper;'
    'OrganizationsMapper'='import com.jinbooks.repository.idm.OrganizationsMapper;'
    'RolesMapper'='import com.jinbooks.repository.idm.RolesMapper;'
    'RoleMemberMapper'='import com.jinbooks.repository.idm.RoleMemberMapper;'
    'PermissionService'='import com.jinbooks.service.permissions.PermissionService;'
    'PermissionBookService'='import com.jinbooks.service.permissions.PermissionBookService;'
    'ResourcesService'='import com.jinbooks.service.permissions.ResourcesService;'
    'SessionListService'='import com.jinbooks.service.permissions.SessionListService;'
    'AuthzService'='import com.jinbooks.service.permissions.AuthzService;'
    'AuthzResourceService'='import com.jinbooks.service.permissions.AuthzResourceService;'
    'PermissionMapper'='import com.jinbooks.repository.permissions.PermissionMapper;'
    'PermissionBookMapper'='import com.jinbooks.repository.permissions.PermissionBookMapper;'
    'ResourcesMapper'='import com.jinbooks.repository.permissions.ResourcesMapper;'
    'SessionListMapper'='import com.jinbooks.repository.permissions.SessionListMapper;'
    'AuthzMapper'='import com.jinbooks.repository.permissions.AuthzMapper;'
    'AuthzResourceMapper'='import com.jinbooks.repository.permissions.AuthzResourceMapper;'
    'SocialsProviderService'='import com.jinbooks.service.security.SocialsProviderService;'
    'SocialsAssociatesService'='import com.jinbooks.service.security.SocialsAssociatesService;'
    'ConfigPasswordPolicyService'='import com.jinbooks.service.security.ConfigPasswordPolicyService;'
    'ConfigLoginPolicyService'='import com.jinbooks.service.security.ConfigLoginPolicyService;'
    'ConfigEmailSendersService'='import com.jinbooks.service.security.ConfigEmailSendersService;'
    'ConfigSmsProviderService'='import com.jinbooks.service.security.ConfigSmsProviderService;'
    'PasswordPolicyValidatorService'='import com.jinbooks.service.security.PasswordPolicyValidatorService;'
    'SocialsProviderMapper'='import com.jinbooks.repository.security.SocialsProviderMapper;'
    'SocialsAssociateMapper'='import com.jinbooks.repository.security.SocialsAssociateMapper;'
    'ConfigPasswordPolicyMapper'='import com.jinbooks.repository.security.ConfigPasswordPolicyMapper;'
    'ConfigLoginPolicyMapper'='import com.jinbooks.repository.security.ConfigLoginPolicyMapper;'
    'ConfigEmailSendersMapper'='import com.jinbooks.repository.security.ConfigEmailSendersMapper;'
    'ConfigSmsProviderMapper'='import com.jinbooks.repository.security.ConfigSmsProviderMapper;'
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
