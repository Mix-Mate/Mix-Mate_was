#!/bin/bash
# usage: run_case.sh <name> <open-in-view> <pool> <sse-holders>
# 앱을 새로 띄우고 → k6(osiv.js) 실행(+부하 중 Hikari 대기 스레드 수 채취) → 앱 종료
NAME=$1; OSIV=$2; POOL=$3; HOLDERS=$4
source "$(dirname "$0")/common.sh"
[ -s "$WORK/seed.env" ] && source "$WORK/seed.env" || { echo "seed.env 가 없습니다. seed.sh 를 먼저 실행하세요." >&2; exit 1; }
[ -n "$GROUP_ID" ] || { echo "seed.env 에 GROUP_ID 가 없습니다." >&2; exit 1; }
OUT="$WORK/results/$NAME"
cd "$LT_DIR"
require_port_free "$APP_PORT" || exit 1
echo "▶ $NAME 시작 (OSIV=$OSIV, 풀=$POOL, SSE 구독자=$HOLDERS) — 앱 기동 후 k6 약 45초, 케이스당 약 1분 10초"

nohup "$LT_DIR/scripts/run_app.sh" "$OSIV" "$POOL" > "$WORK/logs/app-$NAME.log" 2>&1 &
PID=$!
wait_app_ready || exit 1
echo "  앱 기동 완료 → k6 실행 중..."

# 측정 구간 중간(k6 시작 후 ~30s)에 Hikari 커넥션 대기 스레드 수를 센다
( sleep 30; echo "waiting_threads=$(jcmd "$PID" Thread.print 2>/dev/null \
    | awk 'BEGIN{RS="\n\n"} /HikariPool.getConnection/ {n++} END{print n+0}')" > "$OUT.threads" ) &
SAMPLER=$!

SSE_HOLDERS=$HOLDERS GROUP_ID=$GROUP_ID TEST_EMAIL=$EMAIL TEST_PASSWORD=$PASSWORD \
  k6 run --quiet --summary-export="$OUT.json" osiv.js > "$OUT.txt" 2>&1 || true
python3 "$LT_DIR/scripts/scrub_token.py" "$OUT.json" >/dev/null
wait $SAMPLER 2>/dev/null

kill "$PID" 2>/dev/null; wait "$PID" 2>/dev/null
sleep 2
echo "done $NAME (osiv=$OSIV pool=$POOL sse=$HOLDERS) $(cat "$OUT.threads" 2>/dev/null)"
