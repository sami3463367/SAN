#!/usr/bin/env bash
set -uo pipefail
DEVICE="$1"
[[ "$DEVICE" == android10 || "$DEVICE" == android16 ]] || exit 2
mkdir -p "deliverables/evidence/$DEVICE" "deliverables/$DEVICE-results"
./gradlew connectedDebugAndroidTest --rerun-tasks --stacktrace 2>&1 | tee "deliverables/$DEVICE-test-log.txt"
RESULT=${PIPESTATUS[0]}
adb pull /data/local/tmp/san-evidence/. "deliverables/evidence/$DEVICE/" || true
adb logcat -d > "deliverables/evidence/$DEVICE-logcat.txt"
find app/build/outputs/androidTest-results/connected -name 'TEST-*.xml' -exec cp '{}' "deliverables/$DEVICE-results/" \;
printf '%s\n' "$RESULT" > "deliverables/$DEVICE-results/exit-code.txt"
exit "$RESULT"
