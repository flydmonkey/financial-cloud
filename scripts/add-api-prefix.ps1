$ErrorActionPreference = "Stop"
$utf8NoBom = New-Object System.Text.UTF8Encoding $false

function Add-ApiPrefixToMapping([string]$content) {
    $patterns = @(
        @{ Old = '@RequestMapping("/'; New = '@RequestMapping("/api/' },
        @{ Old = '@RequestMapping(value = "/'; New = '@RequestMapping(value = "/api/' },
        @{ Old = '@RequestMapping(value={"/'; New = '@RequestMapping(value={"/api/' },
        @{ Old = '@RequestMapping(value = { "/'; New = '@RequestMapping(value = { "/api/' },
        @{ Old = '@RequestMapping(value = {"/'; New = '@RequestMapping(value = {"/api/' },
        @{ Old = '@RequestMapping({"/'; New = '@RequestMapping({"/api/' }
    )
    foreach ($p in $patterns) {
        $content = $content.Replace($p.Old, $p.New)
    }
    return $content
}

function Add-ApiPrefixToMethodMappings([string]$content) {
    $patterns = @(
        @{ Old = '@GetMapping(value = "/'; New = '@GetMapping(value = "/api/' },
        @{ Old = '@GetMapping(value={"/'; New = '@GetMapping(value={"/api/' },
        @{ Old = '@GetMapping(value = { "/'; New = '@GetMapping(value = { "/api/' },
        @{ Old = '@GetMapping({ "/'; New = '@GetMapping({ "/api/' },
        @{ Old = '@GetMapping(value={"/"}'; New = '@GetMapping(value={"/api"}' },
        @{ Old = '@PostMapping(value = "/'; New = '@PostMapping(value = "/api/' },
        @{ Old = '@PostMapping(value={"/'; New = '@PostMapping(value={"/api/' },
        @{ Old = '@PutMapping(value = "/'; New = '@PutMapping(value = "/api/' },
        @{ Old = '@PutMapping(value={"/'; New = '@PutMapping(value={"/api/' },
        @{ Old = '@DeleteMapping(value = "/'; New = '@DeleteMapping(value = "/api/' },
        @{ Old = '@DeleteMapping(value={"/'; New = '@DeleteMapping(value={"/api/' }
    )
    foreach ($p in $patterns) {
        $content = $content.Replace($p.Old, $p.New)
    }
    return $content
}

$controllerRoot = "C:\Users\Administrator\Projects\jinbooks\financial-cloud\src\main\java\com\jinbooks\controller"
$endpointRoot = "C:\Users\Administrator\Projects\jinbooks\financial-cloud\src\main\java\com\jinbooks\authn\endpoint"
$changed = 0

Get-ChildItem -Path $controllerRoot, $endpointRoot -Recurse -Filter *.java | ForEach-Object {
    $text = [System.IO.File]::ReadAllText($_.FullName)
    $original = $text
    if ($text -match '@RequestMapping') {
        $text = Add-ApiPrefixToMapping $text
    } else {
        $text = Add-ApiPrefixToMethodMappings $text
    }
    if ($text -ne $original) {
        [System.IO.File]::WriteAllText($_.FullName, $text, $utf8NoBom)
        $changed++
        Write-Host "updated $($_.Name)"
    }
}

Write-Host "updated $changed controller files"
