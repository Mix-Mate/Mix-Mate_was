#!/bin/bash
# 부하 테스트 스크립트 공통 설정 (다른 스크립트에서 source 한다)
LT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"   # docs/loadtest
REPO_ROOT="$(cd "$LT_DIR/../.." && pwd)"
TMP_BASE="${TMPDIR:-/tmp}"; TMP_BASE="${TMP_BASE%/}"
WORK="${LT_WORK:-$TMP_BASE/mixmate-loadtest}"               # MySQL 데이터·로그·결과가 쌓이는 임시 폴더
MYSQL_PORT="${LT_MYSQL_PORT:-3307}"
REDIS_PORT="${LT_REDIS_PORT:-6380}"
APP_PORT=8080
MYSQL_PREFIX="${MYSQL_PREFIX:-$(brew --prefix mysql 2>/dev/null)}"
mkdir -p "$WORK/logs" "$WORK/results"

wait_app_ready() {
  for _ in $(seq 1 90); do
    [ "$(curl -s -o /dev/null -w '%{http_code}' http://localhost:$APP_PORT/v3/api-docs)" = "200" ] && return 0
    sleep 1
  done
  echo "앱이 90초 안에 기동하지 않았습니다. 로그: $WORK/logs" >&2; return 1
}

# 개발 앱(IntelliJ 등)이 이미 8080 을 쓰고 있으면 중단한다. 임시 앱이 아니라 그 앱을 때리는 사고를 막는다.
# (lsof 대신 TCP 접속 시도로 확인한다. 직전 앱이 내려가는 중일 수 있어 10초까지 기다린다.)
require_port_free() {
  for _ in $(seq 1 10); do
    (echo > /dev/tcp/127.0.0.1/"$1") 2>/dev/null || return 0
    sleep 1
  done
  echo "포트 $1 을(를) 다른 프로세스가 사용 중입니다. IntelliJ 등에서 띄운 앱을 먼저 종료하세요." >&2
  return 1
}

app_jar() { ls "$REPO_ROOT"/build/libs/*.jar 2>/dev/null | grep -v plain | head -1; }
