#!/bin/bash
# usage: run_app.sh <open-in-view: true|false> <hikari-pool-size>
# 임시 MySQL·Redis만 바라보도록 환경 변수로 덮어쓴 채 앱을 포그라운드로 띄운다. (실제 비밀값은 사용하지 않는다)
source "$(dirname "$0")/common.sh"
JAR="$(app_jar)"
[ -n "$JAR" ] || { echo "jar 가 없습니다. (cd $REPO_ROOT && ./gradlew bootJar -x test)" >&2; exit 1; }

export SPRING_DATASOURCE_URL="jdbc:mysql://127.0.0.1:$MYSQL_PORT/loadtest?serverTimezone=Asia/Seoul&characterEncoding=UTF-8&allowPublicKeyRetrieval=true&useSSL=false"
export SPRING_DATASOURCE_USERNAME=root
export SPRING_DATASOURCE_PASSWORD=
export DB_NAME=loadtest DB_USER=root DB_PASSWORD=unused
export REDIS_HOST=127.0.0.1 REDIS_PORT="$REDIS_PORT"
export JWT_SECRET=loadtest-only-secret-key-loadtest-only-secret-key
export JWT_ISSUER=loadtest
export SMTP_USERNAME=dummy@example.com SMTP_PASSWORD=dummy
export KAKAO_CLIENT_ID=dummy KAKAO_REDIRECT_URI=http://localhost/cb
export GOOGLE_CLIENT_ID=dummy GOOGLE_CLIENT_SECRET=dummy GOOGLE_REDIRECT_URI=http://localhost/cb

# 실험 변수
export SPRING_JPA_OPEN_IN_VIEW="${1:-false}"
export SPRING_DATASOURCE_HIKARI_MAXIMUM_POOL_SIZE="${2:-20}"

# 측정에 로깅 비용이 섞이지 않도록 끈다. (HIKARI_DEBUG=1 이면 30초마다 풀 상태를 로그로 남긴다)
export LOGGING_LEVEL_ORG_SPRINGFRAMEWORK_SECURITY=INFO
export SPRING_JPA_SHOW_SQL=false
export SPRING_JPA_PROPERTIES_HIBERNATE_FORMAT_SQL=false
[ "${HIKARI_DEBUG:-0}" = "1" ] && export LOGGING_LEVEL_COM_ZAXXER_HIKARI=DEBUG
# HIKARI_MBEANS=1 이면 풀 상태를 JMX 로 노출한다 (HikariSampler.java 가 읽는다)
[ "${HIKARI_MBEANS:-0}" = "1" ] && export SPRING_DATASOURCE_HIKARI_REGISTER_MBEANS=true

exec java -Xms512m -Xmx1g -jar "$JAR"
