$utf8 = [System.Text.UTF8Encoding]::new($false)

chcp.com 65001 | Out-Null
[Console]::InputEncoding = $utf8
[Console]::OutputEncoding = $utf8
$OutputEncoding = $utf8

function Add-EnvFlag {
    param(
        [string]$Name,
        [string]$Flag
    )

    $current = [Environment]::GetEnvironmentVariable($Name, "Process")
    if ([string]::IsNullOrWhiteSpace($current)) {
        [Environment]::SetEnvironmentVariable($Name, $Flag, "Process")
        return
    }

    if (-not $current.Contains($Flag)) {
        [Environment]::SetEnvironmentVariable($Name, "$current $Flag", "Process")
    }
}

Add-EnvFlag -Name "JAVA_TOOL_OPTIONS" -Flag "-Dfile.encoding=UTF-8"
Add-EnvFlag -Name "MAVEN_OPTS" -Flag "-Dfile.encoding=UTF-8"
[Environment]::SetEnvironmentVariable("PYTHONIOENCODING", "utf-8", "Process")

Write-Host "PowerShell UTF-8 console enabled for this session."
