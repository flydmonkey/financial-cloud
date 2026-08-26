param(
    [string]$BaseUrl = "http://127.0.0.1:2154",
    [string]$Username = "admin",
    [string]$Password = "maxkey",
    [string]$BookId = "1925740920238481409",
    [string]$Captcha = ""
)

$ErrorActionPreference = "Stop"

function Invoke-Json {
    param(
        [string]$Method,
        [string]$Url,
        [hashtable]$Headers = @{},
        $Body = $null
    )
    $params = @{
        Method      = $Method
        Uri         = $Url
        Headers     = $Headers
        ContentType = "application/json"
    }
    if ($null -ne $Body) {
        $params.Body = ($Body | ConvertTo-Json -Depth 10)
    }
    return Invoke-RestMethod @params
}

Write-Host "1) login init"
$loginInit = Invoke-Json GET "$BaseUrl/api/login/get"
$state = $loginInit.data.state
$captchaType = $loginInit.data.captcha
if (-not $state) { throw "login/get missing state" }

if ($captchaType -ne "NONE" -and [string]::IsNullOrWhiteSpace($Captcha)) {
    $type = $captchaType.ToLower()
    $captcha = Invoke-Json GET "$BaseUrl/api/captcha?state=$state&captcha=$type"
    Write-Host "Captcha required ($captchaType). Open login page or decode image, then rerun with -Captcha <answer>."
    Write-Host "state=$state"
    if ($captcha.data.image) {
        $imgPath = Join-Path $env:TEMP "jinbooks-smoke-captcha.png"
        [IO.File]::WriteAllBytes($imgPath, [Convert]::FromBase64String(($captcha.data.image -replace '^data:image/[^;]+;base64,', '')))
        Write-Host "captcha image: $imgPath"
    }
    exit 2
}

Write-Host "2) signin"
$signinBody = @{
    username = $Username
    password = $Password
    state    = $state
    authType = "normal"
}
if ($captchaType -ne "NONE") { $signinBody.captcha = $Captcha }
$signin = Invoke-Json POST "$BaseUrl/api/login/signin" -Body $signinBody
if ($signin.code -ne 0) { throw "signin failed: $($signin.message)" }
$token = $signin.data.token
$headers = @{ Authorization = "Bearer $token" }

Write-Host "3) subject tree"
$tree = Invoke-Json GET "$BaseUrl/api/booksubject/tree/$BookId" -Headers $headers
if ($tree.code -ne 0) { throw "subject tree failed: $($tree.message)" }
$nodeCount = @($tree.data).Count
Write-Host "   nodes: $nodeCount"

Write-Host "4) resources tree"
$resources = Invoke-Json GET "$BaseUrl/api/permissions/resources/tree" -Headers $headers
if ($resources.code -ne 0) { throw "resources tree failed: $($resources.message)" }
Write-Host "   resource nodes: $($resources.data.nodeCount)"

Write-Host "SMOKE PASS"
