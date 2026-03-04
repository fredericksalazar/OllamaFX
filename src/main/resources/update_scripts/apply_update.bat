@echo off
setlocal enabledelayedexpansion

:: OllamaFX Auto-Update Applier Script (Windows)
:: This script is executed by OllamaFX after downloading and preparing an update.
:: It lives in <OllamaFX_Root>\update\apply_update.bat

echo [OllamaFX Updater] Waiting 3 seconds for JVM to gracefully exit...
timeout /t 3 /nobreak >nul

:: Move to the parent directory (root of OllamaFX)
cd /d "%~dp0.."

echo [OllamaFX Updater] Applying update from update\new\...

if exist "update\new" (
    :: Copy files recursively and overwrite existing files without prompting (/Y)
    xcopy /S /Y "update\new\*" ".\"
    rmdir /s /q "update\new"
    echo [OllamaFX Updater] Update applied successfully.
) else (
    echo [OllamaFX Updater] ERROR: update\new\ directory not found. Aborting update.
    pause
    exit /b 1
)

echo [OllamaFX Updater] Restarting OllamaFX...

:: Launch the updated bat file in a new hidden/independent process
start "" ".\OllamaFX.bat"

echo [OllamaFX Updater] Finished.
exit /b 0
