$ErrorActionPreference = "Stop"
$srcRoot = "C:\Users\Administrator\Projects\jinbooks\jinbooks\src\main\java"
$serviceRoot = Join-Path $srcRoot "com\jinbooks\service"

# 1) Transform *ServiceImpl.java -> parent/*Service.java
Get-ChildItem -Path $serviceRoot -Recurse -Filter "*ServiceImpl.java" | ForEach-Object {
    $implFile = $_
    $parentDir = $implFile.Directory.Parent.FullName
    $oldClass = $implFile.BaseName
    $newClass = $oldClass -replace 'Impl$',''
    $moduleRel = $parentDir.Substring($serviceRoot.Length + 1).Replace('\', '.')
    $newPackage = "com.jinbooks.service.$moduleRel"

    $content = Get-Content -LiteralPath $implFile.FullName -Raw -Encoding UTF8
    $content = $content -replace 'package com\.jinbooks\.service\.[^;]+\.impl;', "package $newPackage;"
    $content = $content -replace "public class $oldClass(\s+extends\s+[^{]+)?\s+implements\s+[^{]+\{", "public class $newClass`$1{"
    $content = $content -replace [regex]::Escape($oldClass), $newClass
    $content = $content -replace '(?m)^import com\.jinbooks\.service\.[^;]+\.' + [regex]::Escape($newClass) + ';\r?\n', ''
    $content = $content -replace "(?m)^import com\.jinbooks\.service\.[^;]+\.impl\.[^;]+;\r?\n", ''

    $target = Join-Path $parentDir "$newClass.java"
    Set-Content -LiteralPath $target -Value $content -Encoding UTF8 -NoNewline
    Write-Host "created $target"
}

# 2) Move PasswordPolicyMessageResolver out of impl
$resolverImpl = Join-Path $serviceRoot "security\impl\PasswordPolicyMessageResolver.java"
$resolverTarget = Join-Path $serviceRoot "security\PasswordPolicyMessageResolver.java"
if (Test-Path $resolverImpl) {
    $content = Get-Content -LiteralPath $resolverImpl -Raw -Encoding UTF8
    $content = $content -replace 'package com\.jinbooks\.service\.security\.impl;', 'package com.jinbooks.service.security;'
    Set-Content -LiteralPath $resolverTarget -Value $content -Encoding UTF8 -NoNewline
    Remove-Item -LiteralPath $resolverImpl -Force
    Write-Host "moved PasswordPolicyMessageResolver"
}

# 3) Delete service interfaces (only files that are still interfaces)
Get-ChildItem -Path $serviceRoot -Recurse -Filter "*Service.java" | Where-Object {
    $_.DirectoryName -notmatch '\\impl$' -and
    (Select-String -Path $_.FullName -Pattern '^\s*public interface ' -Quiet)
} | ForEach-Object {
    Remove-Item -LiteralPath $_.FullName -Force
    Write-Host "deleted interface $($_.Name)"
}

# Delete ConfigService marker interface if still present
$configService = Join-Path $serviceRoot "config\ConfigService.java"
if (Test-Path $configService) {
    $isInterface = Select-String -Path $configService -Pattern '^\s*public interface ' -Quiet
    if ($isInterface) {
        Remove-Item -LiteralPath $configService -Force
        Write-Host "deleted ConfigService interface"
    }
}

# 4) Remove impl directories
Get-ChildItem -Path $serviceRoot -Recurse -Directory -Filter "impl" | Sort-Object FullName -Descending | ForEach-Object {
    Remove-Item -LiteralPath $_.FullName -Recurse -Force
    Write-Host "removed dir $($_.FullName)"
}

# 5) Update references across source tree
$javaFiles = Get-ChildItem -Path (Join-Path $srcRoot "com\jinbooks") -Recurse -Filter "*.java"
$testRoot = "C:\Users\Administrator\Projects\jinbooks\jinbooks\src\test\java"
if (Test-Path $testRoot) {
    $javaFiles += Get-ChildItem -Path $testRoot -Recurse -Filter "*.java"
}

foreach ($file in $javaFiles) {
    $content = Get-Content -LiteralPath $file.FullName -Raw -Encoding UTF8
    $original = $content
    $content = $content -replace 'com\.jinbooks\.service\.([^.]+)\.impl\.(\w+)ServiceImpl', 'com.jinbooks.service.$1.$2Service'
    $content = $content -replace 'com\.jinbooks\.service\.security\.impl\.PasswordPolicyMessageResolver', 'com.jinbooks.service.security.PasswordPolicyMessageResolver'
    $content = $content -replace '(?m)^import com\.jinbooks\.service\.config\.ConfigService;\r?\n', ''
    $content = $content -replace 'ConfigSysServiceImpl', 'ConfigSysService'
    $content = $content -replace 'PasswordPolicyValidatorServiceImpl', 'PasswordPolicyValidatorService'
    $content = $content -replace 'FileStorageServiceImpl', 'FileStorageService'
    $content = $content -replace 'InstitutionsServiceImpl', 'InstitutionsService'
    if ($content -ne $original) {
        Set-Content -LiteralPath $file.FullName -Value $content -Encoding UTF8 -NoNewline
        Write-Host "updated refs in $($file.Name)"
    }
}

Write-Host "done"
