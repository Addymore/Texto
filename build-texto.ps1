$ErrorActionPreference = 'Stop'
Push-Location $PSScriptRoot
$previousJavaOptions = $env:JAVA_TOOL_OPTIONS
try {
    New-Item -ItemType Directory -Force '.tmp' | Out-Null
    # Short path avoids the Windows AF_UNIX socket-length limit in JDK 17.
    $socketDir = (Join-Path $PSScriptRoot '.tmp').Replace('\', '/')
    $env:JAVA_TOOL_OPTIONS = "$previousJavaOptions -Djdk.net.unixdomain.tmpdir=`"$socketDir`""
    & .\gradlew.bat :presentation:assembleDebug :common:testDebugUnitTest --no-daemon --console=plain
    if ($LASTEXITCODE -ne 0) { throw "Texto build failed with exit code $LASTEXITCODE" }
} finally {
    $env:JAVA_TOOL_OPTIONS = $previousJavaOptions
    Pop-Location
}
