@echo off
setlocal
set "PROJECT_DIR=%CD%"
set "UTF8_COMMAND=$utf8 = [System.Text.UTF8Encoding]::new($false); chcp.com 65001 > $null; [Console]::InputEncoding = $utf8; [Console]::OutputEncoding = $utf8; $OutputEncoding = $utf8; $env:JAVA_TOOL_OPTIONS = '-Dfile.encoding=UTF-8'; $env:MAVEN_OPTS = '-Dfile.encoding=UTF-8'; $env:PYTHONIOENCODING = 'utf-8'; Set-Location -LiteralPath $env:PROJECT_DIR; Write-Host 'PowerShell UTF-8 console enabled.'"

if /I "%~1"=="--check" (
  powershell.exe -NoProfile -ExecutionPolicy Bypass -Command "%UTF8_COMMAND%"
  exit /b %ERRORLEVEL%
)

powershell.exe -NoExit -NoProfile -ExecutionPolicy Bypass -Command "%UTF8_COMMAND%"
