#!/bin/bash
# Retired. Unloads the old continuous watcher if it is still installed.
cat >/dev/null || true
launchctl bootout "gui/$(id -u)/com.disciplinescore.apk-watcher" >/dev/null 2>&1 || true
rm -f "$HOME/Library/LaunchAgents/com.disciplinescore.apk-watcher.plist"
echo '{}'
exit 0
