# Wave 6: auth + common leftovers + MyBatis cleanup
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
    Write-Host "Moved -> $(Split-Path $dst -Leaf) ($pkg)"
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
function Rename-Controller($src, $newName, $pkg) {
    if (-not (Test-Path $src)) { Write-Warning "Skip missing: $src"; return }
    $dstDir = "$base\controller\auth"
    Ensure-Dir $dstDir
    $dst = "$dstDir\$newName"
    Move-Item -Force $src $dst
    Set-Package $dst $pkg
    # rename class Endpoint -> Controller
    $c = [System.IO.File]::ReadAllText($dst)
    $oldClass = [System.IO.Path]::GetFileNameWithoutExtension($src)
    $newClass = [System.IO.Path]::GetFileNameWithoutExtension($newName)
    $c = $c -replace "\bclass\s+$oldClass\b", "class $newClass"
    $c = $c -replace "\b$oldClass\b", $newClass  # logger refs etc - careful, may over-replace
    # revert over-aggressive: only class name already done; undo string replacements that broke things
    # safer: only replace class declaration and constructors
    $c = [System.IO.File]::ReadAllText($dst)
    $c = $c -replace "package\s+[\w.]+;", "package $pkg;"
    $c = $c -replace "\b(public\s+class|class)\s+$oldClass\b", "`$1 $newClass"
    [System.IO.File]::WriteAllText($dst, $c)
    Write-Host "Renamed $oldClass -> $newClass"
}

# ========== AUTH ==========
Ensure-Dir "$base\domain\auth"; Ensure-Dir "$base\dto\auth"; Ensure-Dir "$base\controller\auth"
Ensure-Dir "$base\repository\auth"; Ensure-Dir "$base\service\auth\impl"

Move-Set "$base\entity\FileStorage.java" "$base\domain\auth\FileStorage.java" "com.financial.cloud.domain.auth"
Move-Set "$base\entity\ForgotPassword.java" "$base\dto\auth\ForgotPassword.java" "com.financial.cloud.dto.auth"
Move-Set "$base\entity\ChangePassword.java" "$base\dto\auth\ChangePassword.java" "com.financial.cloud.dto.auth"

# auth-related DTOs from entity/dto
foreach ($d in @("NewPwdDto.java","RegisterDto.java","QueryGrantedAppsDto.java","QueryAppResourceDto.java","QueryGroupMembersDto.java","QueryOrgDto.java","QueryRoleMembersDto.java")) {
    Move-Set "$base\entity\dto\$d" "$base\dto\auth\$d" "com.financial.cloud.dto.auth"
}
Move-Set "$base\entity\vo\AppResourcesVo.java" "$base\dto\auth\AppResourcesVo.java" "com.financial.cloud.dto.auth"

Move-Set "$base\persistence\mapper\LoginMapper.java" "$base\repository\auth\LoginMapper.java" "com.financial.cloud.repository.auth"
Move-Set "$base\persistence\mapper\FileStorageMapper.java" "$base\repository\auth\FileStorageMapper.java" "com.financial.cloud.repository.auth"
Move-Set "$base\persistence\service\LoginService.java" "$base\service\auth\LoginService.java" "com.financial.cloud.service.auth"
Move-Set "$base\persistence\service\FileStorageService.java" "$base\service\auth\FileStorageService.java" "com.financial.cloud.service.auth"
Move-Set "$base\persistence\service\impl\LoginServiceImpl.java" "$base\service\auth\impl\LoginServiceImpl.java" "com.financial.cloud.service.auth.impl"
Move-Set "$base\persistence\service\impl\FileStorageServiceImpl.java" "$base\service\auth\impl\FileStorageServiceImpl.java" "com.financial.cloud.service.auth.impl"
Move-Xml "LoginMapper.xml" "auth"

# Controllers: Endpoint -> Controller
$endpointMap = @{
    "$base\web\controller\LoginEndpoint.java" = "LoginController.java"
    "$base\web\controller\LogoutEndpoint.java" = "LogoutController.java"
    "$base\web\controller\ImageCaptchaEndpoint.java" = "ImageCaptchaController.java"
    "$base\web\FileStorageEndpoint.java" = "FileStorageController.java"
    "$base\web\MetadataEndpoint.java" = "MetadataController.java"
    "$base\web\ProductVersionEndpoint.java" = "ProductVersionController.java"
    "$base\web\ExceptionEndpoint.java" = "ExceptionController.java"
}
foreach ($src in $endpointMap.Keys) {
    Rename-Controller $src $endpointMap[$src] "com.financial.cloud.controller.auth"
}

# ========== APPROVAL -> book ==========
Move-Set "$base\entity\approval\ApprovalRecord.java" "$base\domain\book\ApprovalRecord.java" "com.financial.cloud.domain.book"
Move-Set "$base\persistence\mapper\ApprovalRecordMapper.java" "$base\repository\book\ApprovalRecordMapper.java" "com.financial.cloud.repository.book"

# ========== COMMON leftovers ==========
foreach ($e in @("BaseSubject.java","SubjectAuxiliary.java","TreeNode.java","TreeAttributes.java","ExtraAttr.java","ExtraAttrs.java","PeriodStr.java","DbTableColumn.java","DbTableMetaData.java","ExcelImport.java")) {
    Move-Set "$base\entity\$e" "$base\common\$e" "com.financial.cloud.common"
}
# shared DTOs
foreach ($d in @("ListIdsDto.java","ChangeStatusDto.java","BookQueryDto.java","NoticesPageDto.java","TimeBasedDto.java")) {
    Move-Set "$base\entity\dto\$d" "$base\dto\common\$d" "com.financial.cloud.dto.common"
}
# client helpers -> common.client
Get-ChildItem "$base\entity\client\*.java" -ErrorAction SilentlyContinue | ForEach-Object {
    Move-Set $_.FullName "$base\common\client\$($_.Name)" "com.financial.cloud.common.client"
}

# ========== replacements ==========
$replacements = [ordered]@{
    'com.financial.cloud.entity.FileStorage' = 'com.financial.cloud.domain.auth.FileStorage'
    'com.financial.cloud.entity.ForgotPassword' = 'com.financial.cloud.dto.auth.ForgotPassword'
    'com.financial.cloud.entity.ChangePassword' = 'com.financial.cloud.dto.auth.ChangePassword'
    'com.financial.cloud.entity.dto.NewPwdDto' = 'com.financial.cloud.dto.auth.NewPwdDto'
    'com.financial.cloud.entity.dto.RegisterDto' = 'com.financial.cloud.dto.auth.RegisterDto'
    'com.financial.cloud.entity.dto.QueryGrantedAppsDto' = 'com.financial.cloud.dto.auth.QueryGrantedAppsDto'
    'com.financial.cloud.entity.dto.QueryAppResourceDto' = 'com.financial.cloud.dto.auth.QueryAppResourceDto'
    'com.financial.cloud.entity.dto.QueryGroupMembersDto' = 'com.financial.cloud.dto.auth.QueryGroupMembersDto'
    'com.financial.cloud.entity.dto.QueryOrgDto' = 'com.financial.cloud.dto.auth.QueryOrgDto'
    'com.financial.cloud.entity.dto.QueryRoleMembersDto' = 'com.financial.cloud.dto.auth.QueryRoleMembersDto'
    'com.financial.cloud.entity.vo.AppResourcesVo' = 'com.financial.cloud.dto.auth.AppResourcesVo'
    'com.financial.cloud.entity.approval.ApprovalRecord' = 'com.financial.cloud.domain.book.ApprovalRecord'
    'com.financial.cloud.entity.BaseSubject' = 'com.financial.cloud.common.BaseSubject'
    'com.financial.cloud.entity.SubjectAuxiliary' = 'com.financial.cloud.common.SubjectAuxiliary'
    'com.financial.cloud.entity.TreeAttributes' = 'com.financial.cloud.common.TreeAttributes'
    'com.financial.cloud.entity.TreeNode' = 'com.financial.cloud.common.TreeNode'
    'com.financial.cloud.entity.ExtraAttrs' = 'com.financial.cloud.common.ExtraAttrs'
    'com.financial.cloud.entity.ExtraAttr' = 'com.financial.cloud.common.ExtraAttr'
    'com.financial.cloud.entity.PeriodStr' = 'com.financial.cloud.common.PeriodStr'
    'com.financial.cloud.entity.DbTableMetaData' = 'com.financial.cloud.common.DbTableMetaData'
    'com.financial.cloud.entity.DbTableColumn' = 'com.financial.cloud.common.DbTableColumn'
    'com.financial.cloud.entity.ExcelImport' = 'com.financial.cloud.common.ExcelImport'
    'com.financial.cloud.entity.dto.ListIdsDto' = 'com.financial.cloud.dto.common.ListIdsDto'
    'com.financial.cloud.entity.dto.ChangeStatusDto' = 'com.financial.cloud.dto.common.ChangeStatusDto'
    'com.financial.cloud.entity.dto.BookQueryDto' = 'com.financial.cloud.dto.common.BookQueryDto'
    'com.financial.cloud.entity.dto.NoticesPageDto' = 'com.financial.cloud.dto.common.NoticesPageDto'
    'com.financial.cloud.entity.dto.TimeBasedDto' = 'com.financial.cloud.dto.common.TimeBasedDto'
    'com.financial.cloud.entity.client.' = 'com.financial.cloud.common.client.'

    'com.financial.cloud.web.controller.LoginEndpoint' = 'com.financial.cloud.controller.auth.LoginController'
    'com.financial.cloud.web.controller.LogoutEndpoint' = 'com.financial.cloud.controller.auth.LogoutController'
    'com.financial.cloud.web.controller.ImageCaptchaEndpoint' = 'com.financial.cloud.controller.auth.ImageCaptchaController'
    'com.financial.cloud.web.FileStorageEndpoint' = 'com.financial.cloud.controller.auth.FileStorageController'
    'com.financial.cloud.web.MetadataEndpoint' = 'com.financial.cloud.controller.auth.MetadataController'
    'com.financial.cloud.web.ProductVersionEndpoint' = 'com.financial.cloud.controller.auth.ProductVersionController'
    'com.financial.cloud.web.ExceptionEndpoint' = 'com.financial.cloud.controller.auth.ExceptionController'

    'com.financial.cloud.persistence.mapper.Login' = 'com.financial.cloud.repository.auth.Login'
    'com.financial.cloud.persistence.mapper.FileStorage' = 'com.financial.cloud.repository.auth.FileStorage'
    'com.financial.cloud.persistence.mapper.ApprovalRecord' = 'com.financial.cloud.repository.book.ApprovalRecord'
    'com.financial.cloud.persistence.service.impl.Login' = 'com.financial.cloud.service.auth.impl.Login'
    'com.financial.cloud.persistence.service.impl.FileStorage' = 'com.financial.cloud.service.auth.impl.FileStorage'
    'com.financial.cloud.persistence.service.Login' = 'com.financial.cloud.service.auth.Login'
    'com.financial.cloud.persistence.service.FileStorage' = 'com.financial.cloud.service.auth.FileStorage'
}

Get-ChildItem -Path $srcRoot -Recurse -Include *.java,*.xml | ForEach-Object {
    $c = [System.IO.File]::ReadAllText($_.FullName)
    $orig = $c
    foreach ($k in $replacements.Keys) { $c = $c.Replace($k, $replacements[$k]) }
    if ($c -ne $orig) { [System.IO.File]::WriteAllText($_.FullName, $c) }
}

$symbols = [ordered]@{
    'LoginService'='import com.financial.cloud.service.auth.LoginService;'
    'FileStorageService'='import com.financial.cloud.service.auth.FileStorageService;'
    'FileStorageServiceImpl'='import com.financial.cloud.service.auth.impl.FileStorageServiceImpl;'
    'LoginMapper'='import com.financial.cloud.repository.auth.LoginMapper;'
    'FileStorageMapper'='import com.financial.cloud.repository.auth.FileStorageMapper;'
    'ApprovalRecordMapper'='import com.financial.cloud.repository.book.ApprovalRecordMapper;'
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

# cleanup empty dirs
@(
    "$base\entity\approval","$base\entity\client","$base\entity\dto","$base\entity\vo","$base\entity",
    "$base\web\controller",
    "$base\persistence\mapper",
    "$base\persistence\service\impl","$base\persistence\service"
) | ForEach-Object {
    if ((Test-Path $_) -and -not (Get-ChildItem $_ -Force -Recurse -ErrorAction SilentlyContinue | Where-Object { -not $_.PSIsContainer })) {
        Remove-Item $_ -Force -Recurse -ErrorAction SilentlyContinue
        Write-Host "Removed empty $_"
    }
}

Write-Host "Wave 6 auth+cleanup complete."
