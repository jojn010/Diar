#!/usr/bin/env bash
set -euo pipefail
cd "$(dirname "$0")/.."
./gradlew -PdiarOffline=true assembleDebug testDebugUnitTest lintDebug --console=plain
adb_path="${ANDROID_HOME:-${ANDROID_SDK_ROOT:-$HOME/Android/Sdk}}/platform-tools/adb"
if [[ ! -x "$adb_path" ]]; then
  echo 'Build/unit/lint finished. Emulator tests skipped: adb unavailable.'
  exit 0
fi
emulator_serial="${ANDROID_SERIAL:-}"
if [[ -n "$emulator_serial" && "$emulator_serial" != emulator-* ]]; then
  echo "Refusing physical-device serial for automatic tests: $emulator_serial" >&2
  exit 1
fi
if [[ -z "$emulator_serial" ]]; then
  emulator_serial="$("$adb_path" devices | awk '$1 ~ /^emulator-/ && $2 == "device" { print $1; exit }')"
fi
if [[ -z "$emulator_serial" ]]; then
  echo 'Build/unit/lint finished. Emulator tests skipped: no running emulator.'
  exit 0
fi
ANDROID_SERIAL="$emulator_serial" ./gradlew -PdiarOffline=true connectedDebugAndroidTest -Pandroid.testInstrumentationRunnerArguments.notAnnotation=com.example.diar.LiveProjectTest --console=plain
