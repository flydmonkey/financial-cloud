$ErrorActionPreference = "Stop"
$repoRoot = "C:\Users\Administrator\Projects\jinbooks"
$utf8NoBom = New-Object System.Text.UTF8Encoding $false
$changed = 0
$extensions = @('.java', '.xml', '.properties', '.kt', '.groovy', '.ts', '.js', '.vue', '.scss', '.css')

$excludeDirs = @('node_modules', '.git', 'target', 'dist', '.mvn', 'build')

function Should-ProcessFile([string]$fullPath) {
    foreach ($dir in $excludeDirs) {
        if ($fullPath -match "[\\/]$dir[\\/]") { return $false }
    }
    return $true
}

function Strip-LicenseHeader([string]$text) {
    $result = $text
    while ($result -match '(?s)^(/\*.*?\*/\s*)') {
        $header = $Matches[1]
        if ($header -match 'Copyright' -or $header -match 'Apache License' -or $header -match 'Licensed under') {
            $result = $result.Substring($header.Length)
            $result = $result -replace '^\s+', ''
        } else {
            break
        }
    }
    # Remove standalone ruoyi copyright line inside block comments
    $result = $result -replace '(?m)^\s*\*\s*Copyright \(c\) \d{4} ruoyi\s*\r?\n', ''
    return $result
}

Get-ChildItem -Path $repoRoot -Recurse -File | Where-Object {
    $extensions -contains $_.Extension -and (Should-ProcessFile $_.FullName)
} | ForEach-Object {
    $text = [System.IO.File]::ReadAllText($_.FullName)
    $new = Strip-LicenseHeader $text
    if ($new -ne $text) {
        [System.IO.File]::WriteAllText($_.FullName, $new, $utf8NoBom)
        $script:changed++
    }
}

Write-Host "stripped headers from $changed files"

