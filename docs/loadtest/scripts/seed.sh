#!/bin/bash
# 테스트 계정 1개와 그룹 N개(기본 10)를 만든다. 앱이 떠 있어야 하며, 결과는 $WORK/seed.env 에 남긴다.
# (JSON 은 변수에 먼저 담아 쓴다. macOS 기본 bash 3.2 는 $(...) 안의 중첩 따옴표를 잘못 파싱한다.)
source "$(dirname "$0")/common.sh"
BASE="http://localhost:$APP_PORT"
EMAIL=lt@example.com
PW=password123
H='Content-Type: application/json'
N="${SEED_GROUPS:-10}"
rm -f "$WORK/seed.env"

fail() { echo "SEED FAILED: $*" >&2; exit 1; }

# 이메일 인증 완료 상태를 임시 Redis에 직접 심는다 (메일 발송 우회)
redis-cli -p "$REDIS_PORT" SET "VERIFIED_EMAIL:$EMAIL" DONE EX 600 >/dev/null

SIGNUP_BODY=$(printf '{"email":"%s","password":"%s","userName":"loadtest"}' "$EMAIL" "$PW")
CODE=$(curl -s -o /dev/null -w '%{http_code}' -X POST "$BASE/api/v1/auth/signup" -H "$H" -d "$SIGNUP_BODY")
echo "signup: $CODE"      # 이미 가입돼 있으면 409 여도 로그인은 되므로 계속 진행한다

LOGIN_BODY=$(printf '{"email":"%s","password":"%s"}' "$EMAIL" "$PW")
TOKEN=$(curl -s -X POST "$BASE/api/v1/auth/login" -H "$H" -d "$LOGIN_BODY" \
  | python3 -c 'import sys,json; print(json.load(sys.stdin).get("accessToken",""))')
[ -n "$TOKEN" ] || fail "로그인 실패 (signup=$CODE)"

IDS=""
for i in $(seq 1 "$N"); do
  GROUP_BODY=$(printf '{"groupName":"loadtest-%s","description":"k6 load test","profile":{"displayName":"host","studentId":"20240001","position":"STAFF","major":"cs","isNew":false,"grade":"FIRST","gender":"MALE","mbti":"ENFP","age":20,"instaId":"lt","bio":"hi","visibility":"PUBLIC"}}' "$i")
  RES=$(curl -s -X POST "$BASE/api/v1/groups" -H "$H" -H "Authorization: Bearer $TOKEN" -d "$GROUP_BODY")
  ID=$(echo "$RES" | python3 -c 'import sys,json; print(json.load(sys.stdin).get("groupId",""))')
  [ -n "$ID" ] || fail "그룹 생성 실패: $RES"
  IDS="${IDS:+$IDS,}$ID"
done

{ echo "GROUP_ID=${IDS%%,*}"; echo "GROUP_IDS=$IDS"; echo "EMAIL=$EMAIL"; echo "PASSWORD=$PW"; } > "$WORK/seed.env"
echo "seeded groups: $IDS"
echo "SSE smoke: $(curl -s -N --max-time 2 -H "Authorization: Bearer $TOKEN" -H 'Accept: text/event-stream' \
  "$BASE/api/v1/groups/${IDS%%,*}/status/stream" | head -2 | tr '\n' ' ')"
