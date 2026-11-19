#!/bin/bash
# usage: run_capacity.sh [scenarios...]   (기본: smoke baseline load spike stress breakpoint, 총 약 15분)
# 앱 1개(OSIV off / 풀 20 = 현재 운영 설정)를 띄워 두고 시나리오를 차례로 실행한다.
# 시나리오마다 k6 결과와 함께 앱·MySQL·k6 의 CPU(2초 간격), Hikari 풀 상태(JMX, 1초 간격)를 기록한다.
source "$(dirname "$0")/common.sh"
SCENARIOS="${*:-smoke baseline load spike stress breakpoint}"
OSIV="${OSIV:-false}"; POOL="${POOL:-20}"
NCPU="$(sysctl -n hw.ncpu 2>/dev/null || nproc)"

duration_of() {   # capacity.js 시나리오 길이(초). 샘플러가 이보다 조금 더 길게 돈다.
  case "$1" in smoke) echo 15;; baseline) echo 60;; load) echo 180;; spike) echo 70;; stress) echo 240;; breakpoint) echo 180;; *) echo 120;; esac
}

require_port_free "$APP_PORT" || exit 1
bash "$LT_DIR/scripts/infra.sh" start || exit 1
if [ -z "$(app_jar)" ]; then (cd "$REPO_ROOT" && ./gradlew bootJar -x test -q) || exit 1; fi

HIKARI_MBEANS=1 nohup "$LT_DIR/scripts/run_app.sh" "$OSIV" "$POOL" > "$WORK/logs/app-capacity.log" 2>&1 &
APP_PID=$!
wait_app_ready || exit 1
if [ ! -s "$WORK/seed.env" ]; then bash "$LT_DIR/scripts/seed.sh" || exit 1; fi
source "$WORK/seed.env"
cd "$LT_DIR"

for sc in $SCENARIOS; do
  OUT="$WORK/results/capacity-$sc"
  echo "▶ $sc 시작 (약 $(duration_of "$sc")초)"
  SECS=$(( $(duration_of "$sc") + 20 ))

  python3 "$LT_DIR/scripts/cpusampler.py" "$OUT.cpu.csv" "$APP_PID" "$WORK/mysql.pid" 2 &
  CPU_PID=$!
  java --add-modules jdk.attach "$LT_DIR/scripts/HikariSampler.java" "$APP_PID" "$OUT.hikari.csv" "$SECS" > "$WORK/logs/sampler-$sc.log" 2>&1 &
  HK_PID=$!
  sleep 3

  SCENARIO=$sc GROUP_IDS=$GROUP_IDS TEST_EMAIL=$EMAIL TEST_PASSWORD=$PASSWORD \
    k6 run --quiet --summary-export="$OUT.json" capacity.js > "$OUT.txt" 2>&1 || true
  python3 "$LT_DIR/scripts/scrub_token.py" "$OUT.json" >/dev/null

  kill $CPU_PID 2>/dev/null; wait $CPU_PID 2>/dev/null
  kill $HK_PID 2>/dev/null; wait $HK_PID 2>/dev/null
  echo "$NCPU" > "$OUT.ncpu"
  echo "done $sc"
  sleep 20   # 시나리오 사이 냉각
done

kill "$APP_PID" 2>/dev/null; wait "$APP_PID" 2>/dev/null
python3 "$LT_DIR/scripts/report_capacity.py" "$WORK/results" | tee "$WORK/results/capacity-table.md"
