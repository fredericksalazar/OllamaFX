#!/usr/bin/env bash

# OllamaFX Auto-Update Applier Script (Mac)
# This script is executed by OllamaFX after downloading and preparing an update.
# It lives in <OllamaFX_Root>/update/apply_update.command

# Move to the script's directory and then one level up to the root
cd "$(dirname "$0")" || exit
cd .. || exit

echo "[OllamaFX Updater] Waiting 3 seconds for JVM to gracefully exit..."
sleep 3

echo "[OllamaFX Updater] Applying update from update/new/..."

# Overwrite everything recursively from update/new to the root directory
if [ -d "update/new" ]; then
    cp -Rv update/new/* ./
    rm -rf update/new
    echo "[OllamaFX Updater] Update applied successfully."
else
    echo "[OllamaFX Updater] ERROR: update/new/ directory not found. Aborting update."
    exit 1
fi

echo "[OllamaFX Updater] Restarting OllamaFX..."

# Ensure the main launcher is executable
chmod +x ./OllamaFX.command

# In MacOS, the .command script is heavily preferred for UI execution
open -a Terminal.app ./OllamaFX.command
echo "[OllamaFX Updater] Finished."
exit 0
