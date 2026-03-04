#!/usr/bin/env bash

# OllamaFX Rollback Script (Mac)
# This script manually restores the previous version of OllamaFX if an update fails.
# It lives in <OllamaFX_Root>/update/rollback.command

cd "$(dirname "$0")" || exit
cd .. || exit

echo "[OllamaFX Rollback] Checking for previous version backup..."

if [ -d "update/old" ]; then
    echo "[OllamaFX Rollback] Restoring files from update/old/..."
    cp -Rv update/old/* ./
    echo "[OllamaFX Rollback] Rollback completed!"
    
    echo "[OllamaFX Rollback] Restarting OllamaFX..."
    chmod +x ./OllamaFX.command
    open -a Terminal.app ./OllamaFX.command
    exit 0
else
    echo "[OllamaFX Rollback] ERROR: update/old/ directory not found. Cannot perform rollback."
    exit 1
fi
