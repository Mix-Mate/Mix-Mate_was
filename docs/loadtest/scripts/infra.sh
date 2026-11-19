#!/bin/bash
# usage: infra.sh start|stop
# 운영·개발 DB와 완전히 분리된 임시 MySQL(기본 3307)·Redis(기본 6380)를 띄운다. 데이터는 $WORK 아래에만 쌓인다.
source "$(dirname "$0")/common.sh"
SOCK="$TMP_BASE/mmlt.sock"   # macOS 소켓 경로 길이 제한(103자) 때문에 짧은 경로를 쓴다

case "$1" in
start)
  [ -x "$MYSQL_PREFIX/bin/mysqld" ] || { echo "mysqld 를 찾을 수 없습니다. 'brew install mysql' 하거나 MYSQL_PREFIX 를 지정하세요." >&2; exit 1; }
  if [ ! -d "$WORK/mysql-data/mysql" ]; then
    "$MYSQL_PREFIX/bin/mysqld" --no-defaults --initialize-insecure \
      --datadir="$WORK/mysql-data" --basedir="$MYSQL_PREFIX" --log-error="$WORK/logs/mysql-init.err"
  fi
  nohup "$MYSQL_PREFIX/bin/mysqld" --no-defaults --datadir="$WORK/mysql-data" --basedir="$MYSQL_PREFIX" \
    --port="$MYSQL_PORT" --bind-address=127.0.0.1 --socket="$SOCK" --mysqlx=OFF \
    --pid-file="$WORK/mysql.pid" --log-error="$WORK/logs/mysql.err" >/dev/null 2>&1 &
  for _ in $(seq 1 40); do
    "$MYSQL_PREFIX/bin/mysqladmin" --no-defaults -h127.0.0.1 -P"$MYSQL_PORT" -uroot ping >/dev/null 2>&1 && break
    sleep 1
  done
  "$MYSQL_PREFIX/bin/mysql" --no-defaults -h127.0.0.1 -P"$MYSQL_PORT" -uroot \
    -e "CREATE DATABASE IF NOT EXISTS loadtest CHARACTER SET utf8mb4"
  nohup redis-server --port "$REDIS_PORT" --bind 127.0.0.1 --save "" --appendonly no >"$WORK/logs/redis.log" 2>&1 &
  sleep 1
  echo "mysql: $("$MYSQL_PREFIX/bin/mysqld" --version | awk '{print $3}')  redis: $(redis-cli -p "$REDIS_PORT" ping)"
  ;;
stop)
  "$MYSQL_PREFIX/bin/mysqladmin" --no-defaults -h127.0.0.1 -P"$MYSQL_PORT" -uroot shutdown 2>/dev/null
  redis-cli -p "$REDIS_PORT" shutdown nosave 2>/dev/null
  echo "stopped"
  ;;
*) echo "usage: $0 start|stop" >&2; exit 1 ;;
esac
