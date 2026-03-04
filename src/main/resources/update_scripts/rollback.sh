#!/usr/bin/env bash

# OllamaFX Rollback Script (Linux)
# This script manually restores the previous version of OllamaFX if an update fails.
# It lives in <OllamaFX_Root>/update/rollback.sh

cd "$(dirname "$0")" || exit
cd .. || exit

echo "[OllamaFX Rollback] Checking for previous version backup..."

if [ -d "update/old" ]; then
    echo "[OllamaFX Rollback] Restoring files from update/old/..."
    cp -rv update/old/* ./
    echo "[OllamaFX Rollback] Rollback completed!"
    
    echo "[OllamaFX Rollback] Restarting OllamaFX..."
    chmod +x ./OllamaFX
    nohup ./OllamaFX > /dev/null 2>&1 &
    exit 0
else
    echo "[OllamaFX Rollback] ERROR: update/old/ directory not found. Cannot perform rollback."
    exit 1
fi
