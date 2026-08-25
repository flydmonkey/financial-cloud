# Wave 0: Move BaseEntity, Message, PageQuery to common
$ErrorActionPreference = "Stop"
$base = "C:\Users\Administrator\Projects\jinbooks\jinbooks\src\main\java\com\jinbooks"
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
    $c = $c -replace 'package com\.jinbooks\.entity;', 'package com.jinbooks.common;'
    [System.IO.File]::WriteAllText($_.FullName, $c)
}

# Global import replacements
$replacements = @{
    'com.jinbooks.entity.BaseEntity' = 'com.jinbooks.common.BaseEntity'
    'com.jinbooks.entity.Message'    = 'com.jinbooks.common.Message'
    'com.jinbooks.entity.PageQuery'  = 'com.jinbooks.common.PageQuery'
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
