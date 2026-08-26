# Maven wrapper for Windows PowerShell (works when mvnw.cmd fails with spaced JAVA_HOME paths).
param(
    [Parameter(ValueFromRemainingArguments = $true)]
    [string[]]$MavenArgs
)

$ErrorActionPreference = "Stop"
$ProjectDir = Split-Path -Parent $MyInvocation.MyCommand.Path
$JavaHome = if ($env:JAVA_HOME) { $env:JAVA_HOME } else { "C:\Program Files\Java\jdk-17" }
$JavaExe = Join-Path $JavaHome "bin\java.exe"
$WrapperJar = Join-Path $ProjectDir ".mvn\wrapper\maven-wrapper.jar"

if (-not (Test-Path $JavaExe)) {
    throw "Java not found at $JavaExe. Set JAVA_HOME to a valid JDK 17 installation."
}
if (-not (Test-Path $WrapperJar)) {
    throw "Maven wrapper jar not found at $WrapperJar"
}

$arguments = @(
    "-classpath", $WrapperJar,
    "-Dmaven.multiModuleProjectDirectory=$ProjectDir",
    "org.apache.maven.wrapper.MavenWrapperMain"
) + $MavenArgs

& $JavaExe @arguments
exit $LASTEXITCODE
