$ErrorActionPreference = "Stop"

function Invoke-Step {
    param(
        [Parameter(Mandatory = $true)]
        [string]$Message,
        [Parameter(Mandatory = $true)]
        [scriptblock]$Action
    )

    Write-Host ""
    Write-Host "==> $Message" -ForegroundColor Cyan
    & $Action
}

if (-not $env:JAVA_HOME) {
    $javaHome = & java -XshowSettings:properties -version 2>&1 |
        Select-String "java.home" |
        ForEach-Object { ($_ -split "=")[1].Trim() } |
        Select-Object -First 1

    if ($javaHome) {
        $env:JAVA_HOME = $javaHome
        Write-Host "JAVA_HOME not set. Using detected JDK at $javaHome" -ForegroundColor Yellow
    }
}

$repoRoot = $PSScriptRoot
$webappDir = Join-Path $repoRoot "webapp"

Push-Location $repoRoot
try {
    Invoke-Step -Message "Running mvn install from repo root" -Action {
        mvn install
    }

    Push-Location $webappDir
    try {
        Invoke-Step -Message "Starting Jetty from webapp" -Action {
            mvn jetty:run
        }
    }
    finally {
        Pop-Location
    }
}
finally {
    Pop-Location
}
