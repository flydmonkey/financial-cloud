# Wave 0: Move BaseEntity, Message, PageQuery to common
$ErrorActionPreference = "Stop"
$base = "C:\Users\Administrator\Projects\jinbooks\financial-cloud\src\main\java\com\jinbooks"
$srcRoot = "C:\Users\Administrator\Projects\jinbooks\jinbooks\src"

New-Item -ItemType Directory -Force -Path "$base\common" | Out-Null

$moves = @(
    "BaseEntity.java",
    "Message.java",
    "PageQuery.java"
)
foreach ($f in $moves) {
    $src = Join-Path "$base\entity" $f
    $dst = Join-Path "$base\common" $f
    if (Test-Path $src) {
        Move-Item -Force $src $dst
        Write-Host "Moved $f -> common/"
    }
}

# Update package in moved files
Get-ChildItem "$base\common\*.java" | ForEach-Object {
    $c = [System.IO.File]::ReadAllText($_.FullName)
    $c = $c -replace 'package com\.jinbooks\.entity;', 'package com.financial.cloud.common;'
    [System.IO.File]::WriteAllText($_.FullName, $c)
}

# Global import replacements
$replacements = @{
    'com.financial.cloud.entity.BaseEntity' = 'com.financial.cloud.common.BaseEntity'
    'com.financial.cloud.entity.Message'    = 'com.financial.cloud.common.Message'
    'com.financial.cloud.entity.PageQuery'  = 'com.financial.cloud.common.PageQuery'
}
Get-ChildItem -Path $srcRoot -Recurse -Filter *.java | ForEach-Object {
    $c = [System.IO.File]::ReadAllText($_.FullName)
    $orig = $c
    foreach ($k in $replacements.Keys) {
        $c = $c.Replace($k, $replacements[$k])
    }
    if ($c -ne $orig) {
        [System.IO.File]::WriteAllText($_.FullName, $c)
        Write-Host "Updated imports: $($_.Name)"
    }
}

Write-Host "Wave 0 file moves complete."
