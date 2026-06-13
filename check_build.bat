@echo off
echo Checking compilation status...
echo.
type "C:\Users\palin\AppData\Local\Temp\claude\D--codex-mc-mod\1565b572-1ad8-4f31-8109-d550c8c2c641\tasks\boyihn6nx.output" | findstr /C:"BUILD SUCCESSFUL" /C:"BUILD FAILED" /C:"FAILURE" /C:"error:" /C:"错误" >nul 2>&1
if %ERRORLEVEL% EQU 0 (
    echo Compilation finished!
    type "C:\Users\palin\AppData\Local\Temp\claude\D--codex-mc-mod\1565b572-1ad8-4f31-8109-d550c8c2c641\tasks\boyihn6nx.output" | findstr /C:"BUILD"
) else (
    echo Still compiling... Last lines:
    powershell -Command "Get-Content 'C:\Users\palin\AppData\Local\Temp\claude\D--codex-mc-mod\1565b572-1ad8-4f31-8109-d550c8c2c641\tasks\boyihn6nx.output' | Select-Object -Last 5"
)
