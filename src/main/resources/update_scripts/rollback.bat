@echo off
setlocal enabledelayedexpansion

:: OllamaFX Rollback Script (Windows)
:: This script manually restores the previous version of OllamaFX if an update fails.
:: It lives in <OllamaFX_Root>\update\rollback.bat

cd /d "%~dp0.."

echo [OllamaFX Rollback] Checking for previous version backup...

if exist "update\old" (
    echo [OllamaFX Rollback] Restoring files from update\old\...
    xcopy /S /Y "update\old\*" ".\"
    echo [OllamaFX Rollback] Rollback completed!
    
    echo [OllamaFX Rollback] Restarting OllamaFX...
    start "" ".\OllamaFX.bat"
    exit /b 0
) else (
    echo [OllamaFX Rollback] ERROR: update\old\ directory not found. Cannot perform rollback.
    pause
    exit /b 1
)
