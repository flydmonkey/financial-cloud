# Wave 6: auth + common leftovers + MyBatis cleanup
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
    Write-Host "Moved -> $(Split-Path $dst -Leaf) ($pkg)"
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

Move-Set "$base\entity\FileStorage.java" "$base\domain\auth\FileStorage.java" "com.jinbooks.domain.auth"
Move-Set "$base\entity\ForgotPassword.java" "$base\dto\auth\ForgotPassword.java" "com.jinbooks.dto.auth"
Move-Set "$base\entity\ChangePassword.java" "$base\dto\auth\ChangePassword.java" "com.jinbooks.dto.auth"

# auth-related DTOs from entity/dto
foreach ($d in @("NewPwdDto.java","RegisterDto.java","QueryGrantedAppsDto.java","QueryAppResourceDto.java","QueryGroupMembersDto.java","QueryOrgDto.java","QueryRoleMembersDto.java")) {
    Move-Set "$base\entity\dto\$d" "$base\dto\auth\$d" "com.jinbooks.dto.auth"
}
Move-Set "$base\entity\vo\AppResourcesVo.java" "$base\dto\auth\AppResourcesVo.java" "com.jinbooks.dto.auth"

Move-Set "$base\persistence\mapper\LoginMapper.java" "$base\repository\auth\LoginMapper.java" "com.jinbooks.repository.auth"
Move-Set "$base\persistence\mapper\FileStorageMapper.java" "$base\repository\auth\FileStorageMapper.java" "com.jinbooks.repository.auth"
Move-Set "$base\persistence\service\LoginService.java" "$base\service\auth\LoginService.java" "com.jinbooks.service.auth"
Move-Set "$base\persistence\service\FileStorageService.java" "$base\service\auth\FileStorageService.java" "com.jinbooks.service.auth"
Move-Set "$base\persistence\service\impl\LoginServiceImpl.java" "$base\service\auth\impl\LoginServiceImpl.java" "com.jinbooks.service.auth.impl"
Move-Set "$base\persistence\service\impl\FileStorageServiceImpl.java" "$base\service\auth\impl\FileStorageServiceImpl.java" "com.jinbooks.service.auth.impl"
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
    Rename-Controller $src $endpointMap[$src] "com.jinbooks.controller.auth"
}

# ========== APPROVAL -> book ==========
Move-Set "$base\entity\approval\ApprovalRecord.java" "$base\domain\book\ApprovalRecord.java" "com.jinbooks.domain.book"
Move-Set "$base\persistence\mapper\ApprovalRecordMapper.java" "$base\repository\book\ApprovalRecordMapper.java" "com.jinbooks.repository.book"

# ========== COMMON leftovers ==========
foreach ($e in @("BaseSubject.java","SubjectAuxiliary.java","TreeNode.java","TreeAttributes.java","ExtraAttr.java","ExtraAttrs.java","PeriodStr.java","DbTableColumn.java","DbTableMetaData.java","ExcelImport.java")) {
    Move-Set "$base\entity\$e" "$base\common\$e" "com.jinbooks.common"
}
# shared DTOs
foreach ($d in @("ListIdsDto.java","ChangeStatusDto.java","BookQueryDto.java","NoticesPageDto.java","TimeBasedDto.java")) {
    Move-Set "$base\entity\dto\$d" "$base\dto\common\$d" "com.jinbooks.dto.common"
}
# client helpers -> common.client
Get-ChildItem "$base\entity\client\*.java" -ErrorAction SilentlyContinue | ForEach-Object {
    Move-Set $_.FullName "$base\common\client\$($_.Name)" "com.jinbooks.common.client"
}

# ========== replacements ==========
$replacements = [ordered]@{
    'com.jinbooks.entity.FileStorage' = 'com.jinbooks.domain.auth.FileStorage'
    'com.jinbooks.entity.ForgotPassword' = 'com.jinbooks.dto.auth.ForgotPassword'
    'com.jinbooks.entity.ChangePassword' = 'com.jinbooks.dto.auth.ChangePassword'
    'com.jinbooks.entity.dto.NewPwdDto' = 'com.jinbooks.dto.auth.NewPwdDto'
    'com.jinbooks.entity.dto.RegisterDto' = 'com.jinbooks.dto.auth.RegisterDto'
    'com.jinbooks.entity.dto.QueryGrantedAppsDto' = 'com.jinbooks.dto.auth.QueryGrantedAppsDto'
    'com.jinbooks.entity.dto.QueryAppResourceDto' = 'com.jinbooks.dto.auth.QueryAppResourceDto'
    'com.jinbooks.entity.dto.QueryGroupMembersDto' = 'com.jinbooks.dto.auth.QueryGroupMembersDto'
    'com.jinbooks.entity.dto.QueryOrgDto' = 'com.jinbooks.dto.auth.QueryOrgDto'
    'com.jinbooks.entity.dto.QueryRoleMembersDto' = 'com.jinbooks.dto.auth.QueryRoleMembersDto'
    'com.jinbooks.entity.vo.AppResourcesVo' = 'com.jinbooks.dto.auth.AppResourcesVo'
    'com.jinbooks.entity.approval.ApprovalRecord' = 'com.jinbooks.domain.book.ApprovalRecord'
    'com.jinbooks.entity.BaseSubject' = 'com.jinbooks.common.BaseSubject'
    'com.jinbooks.entity.SubjectAuxiliary' = 'com.jinbooks.common.SubjectAuxiliary'
    'com.jinbooks.entity.TreeAttributes' = 'com.jinbooks.common.TreeAttributes'
    'com.jinbooks.entity.TreeNode' = 'com.jinbooks.common.TreeNode'
    'com.jinbooks.entity.ExtraAttrs' = 'com.jinbooks.common.ExtraAttrs'
    'com.jinbooks.entity.ExtraAttr' = 'com.jinbooks.common.ExtraAttr'
    'com.jinbooks.entity.PeriodStr' = 'com.jinbooks.common.PeriodStr'
    'com.jinbooks.entity.DbTableMetaData' = 'com.jinbooks.common.DbTableMetaData'
    'com.jinbooks.entity.DbTableColumn' = 'com.jinbooks.common.DbTableColumn'
    'com.jinbooks.entity.ExcelImport' = 'com.jinbooks.common.ExcelImport'
    'com.jinbooks.entity.dto.ListIdsDto' = 'com.jinbooks.dto.common.ListIdsDto'
    'com.jinbooks.entity.dto.ChangeStatusDto' = 'com.jinbooks.dto.common.ChangeStatusDto'
    'com.jinbooks.entity.dto.BookQueryDto' = 'com.jinbooks.dto.common.BookQueryDto'
    'com.jinbooks.entity.dto.NoticesPageDto' = 'com.jinbooks.dto.common.NoticesPageDto'
    'com.jinbooks.entity.dto.TimeBasedDto' = 'com.jinbooks.dto.common.TimeBasedDto'
    'com.jinbooks.entity.client.' = 'com.jinbooks.common.client.'

    'com.jinbooks.web.controller.LoginEndpoint' = 'com.jinbooks.controller.auth.LoginController'
    'com.jinbooks.web.controller.LogoutEndpoint' = 'com.jinbooks.controller.auth.LogoutController'
    'com.jinbooks.web.controller.ImageCaptchaEndpoint' = 'com.jinbooks.controller.auth.ImageCaptchaController'
    'com.jinbooks.web.FileStorageEndpoint' = 'com.jinbooks.controller.auth.FileStorageController'
    'com.jinbooks.web.MetadataEndpoint' = 'com.jinbooks.controller.auth.MetadataController'
    'com.jinbooks.web.ProductVersionEndpoint' = 'com.jinbooks.controller.auth.ProductVersionController'
    'com.jinbooks.web.ExceptionEndpoint' = 'com.jinbooks.controller.auth.ExceptionController'

    'com.jinbooks.persistence.mapper.Login' = 'com.jinbooks.repository.auth.Login'
    'com.jinbooks.persistence.mapper.FileStorage' = 'com.jinbooks.repository.auth.FileStorage'
    'com.jinbooks.persistence.mapper.ApprovalRecord' = 'com.jinbooks.repository.book.ApprovalRecord'
    'com.jinbooks.persistence.service.impl.Login' = 'com.jinbooks.service.auth.impl.Login'
    'com.jinbooks.persistence.service.impl.FileStorage' = 'com.jinbooks.service.auth.impl.FileStorage'
    'com.jinbooks.persistence.service.Login' = 'com.jinbooks.service.auth.Login'
    'com.jinbooks.persistence.service.FileStorage' = 'com.jinbooks.service.auth.FileStorage'
}

Get-ChildItem -Path $srcRoot -Recurse -Include *.java,*.xml | ForEach-Object {
    $c = [System.IO.File]::ReadAllText($_.FullName)
    $orig = $c
    foreach ($k in $replacements.Keys) { $c = $c.Replace($k, $replacements[$k]) }
    if ($c -ne $orig) { [System.IO.File]::WriteAllText($_.FullName, $c) }
}

$symbols = [ordered]@{
    'LoginService'='import com.jinbooks.service.auth.LoginService;'
    'FileStorageService'='import com.jinbooks.service.auth.FileStorageService;'
    'FileStorageServiceImpl'='import com.jinbooks.service.auth.impl.FileStorageServiceImpl;'
    'LoginMapper'='import com.jinbooks.repository.auth.LoginMapper;'
    'FileStorageMapper'='import com.jinbooks.repository.auth.FileStorageMapper;'
    'ApprovalRecordMapper'='import com.jinbooks.repository.book.ApprovalRecordMapper;'
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
