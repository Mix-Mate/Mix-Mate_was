#!/bin/bash
# usage: run_timeseries.sh <name> <open-in-view> <pool> <sse-holders>
# run_case.sh 와 같은 조건에 더해, k6 요청별 시계열(csv)과 Hikari 풀 상태(JMX, 1초 간격)를 남긴다. (그래프용)
NAME=$1; OSIV=$2; POOL=$3; HOLDERS=$4
source "$(dirname "$0")/common.sh"
[ -s "$WORK/seed.env" ] && source "$WORK/seed.env" || { echo "seed.env 가 없습니다. seed.sh 를 먼저 실행하세요." >&2; exit 1; }
OUT="$WORK/results/ts-$NAME"
cd "$LT_DIR"
require_port_free "$APP_PORT" || exit 1
echo "▶ ts-$NAME 시작 (OSIV=$OSIV, 풀=$POOL, SSE 구독자=$HOLDERS) — 약 1분 10초"

HIKARI_MBEANS=1 nohup "$LT_DIR/scripts/run_app.sh" "$OSIV" "$POOL" > "$WORK/logs/app-ts-$NAME.log" 2>&1 &
PID=$!
wait_app_ready || exit 1

java --add-modules jdk.attach "$LT_DIR/scripts/HikariSampler.java" "$PID" "$OUT.hikari.csv" 55 > "$WORK/logs/sampler-$NAME.log" 2>&1 &
SAMPLER=$!
sleep 3   # 샘플러가 붙을 시간

SSE_HOLDERS=$HOLDERS GROUP_ID=$GROUP_ID TEST_EMAIL=$EMAIL TEST_PASSWORD=$PASSWORD \
  k6 run --quiet --out csv="$OUT.k6.csv" --summary-export="$OUT.json" osiv.js > "$OUT.txt" 2>&1 || true
python3 "$LT_DIR/scripts/scrub_token.py" "$OUT.json" >/dev/null
wait $SAMPLER 2>/dev/null

kill "$PID" 2>/dev/null; wait "$PID" 2>/dev/null
sleep 2
echo "done ts-$NAME (osiv=$OSIV pool=$POOL sse=$HOLDERS) hikari rows: $(wc -l < "$OUT.hikari.csv" 2>/dev/null)"
