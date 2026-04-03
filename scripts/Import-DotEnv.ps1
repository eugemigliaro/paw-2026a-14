param(
    [string]$Path = ".env"
)

$ErrorActionPreference = "Stop"

$resolvedPath = Resolve-Path -LiteralPath $Path -ErrorAction Stop
$loadedVariables = 0

Get-Content -LiteralPath $resolvedPath | ForEach-Object {
    $line = $_.Trim()

    if (-not $line -or $line.StartsWith("#")) {
        return
    }

    if ($line -notmatch '^(?:export\s+)?([A-Za-z_][A-Za-z0-9_]*)=(.*)$') {
        throw "Unsupported .env entry: $line"
    }

    $name = $matches[1]
    $value = $matches[2].Trim()

    if (
        ($value.StartsWith('"') -and $value.EndsWith('"')) -or
        ($value.StartsWith("'") -and $value.EndsWith("'"))
    ) {
        $value = $value.Substring(1, $value.Length - 2)
    }

    Set-Item -Path "Env:$name" -Value $value
    $loadedVariables++
}

Write-Host "Loaded $loadedVariables environment variable(s) from $resolvedPath." -ForegroundColor Green
