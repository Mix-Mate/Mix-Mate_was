#!/bin/bash
# OSIV 실험: 4개 설정 × SSE 구독자 3단계 = 12회, 케이스당 약 1분 10초 (총 15분 내외)
#   A: OSIV on / 풀 10   (장애 당시 설정)
#   B: OSIV on / 풀 20   (1차 완화: 풀 증설)
#   C: OSIV off / 풀 20  (현재 설정)
#   D: OSIV off / 풀 10  (A와 OSIV만 다름 — 가장 깨끗한 비교)
# 환경 변수: CASES="A D"  HOLDERS="0 15"  로 일부만 실행할 수 있다.
source "$(dirname "$0")/common.sh"
require_port_free "$APP_PORT" || exit 1
bash "$LT_DIR/scripts/infra.sh" start || exit 1

if [ -z "$(app_jar)" ]; then (cd "$REPO_ROOT" && ./gradlew bootJar -x test -q) || exit 1; fi

if [ ! -f "$WORK/seed.env" ]; then
  nohup "$LT_DIR/scripts/run_app.sh" false 20 > "$WORK/logs/app-seed.log" 2>&1 &
  SEED_PID=$!
  wait_app_ready || exit 1
  bash "$LT_DIR/scripts/seed.sh" || { kill "$SEED_PID"; exit 1; }
  kill "$SEED_PID"; wait "$SEED_PID" 2>/dev/null; sleep 2
fi

cfg_of() {   # macOS 기본 bash(3.2)는 연관 배열을 지원하지 않아 case 로 푼다
  case "$1" in A) echo "true 10";; B) echo "true 20";; C) echo "false 20";; D) echo "false 10";; esac
}
echo "케이스 $(( $(echo ${CASES:-A B C D} | wc -w) * $(echo ${HOLDERS:-0 15 30} | wc -w) ))개 실행 — 케이스당 약 1분 10초, 진행 상황은 케이스마다 출력됩니다."
RAN=""
for c in ${CASES:-A B C D}; do
  for h in ${HOLDERS:-0 15 30}; do
    bash "$LT_DIR/scripts/run_case.sh" "$c-h$h" $(cfg_of "$c") "$h"
    RAN="$RAN $c-h$h"
  done
done

python3 "$LT_DIR/scripts/report.py" "$WORK/results" $RAN | tee "$WORK/results/table.md"
echo "결과 폴더: $WORK/results"
echo "임시 MySQL·Redis 는 계속 떠 있습니다. 끝났으면: docs/loadtest/scripts/infra.sh stop"
