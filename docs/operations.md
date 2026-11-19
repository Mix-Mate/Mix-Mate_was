# 운영 가이드

- [액세스 로그 (Caddy)](#액세스-로그-caddy)
- [모니터링](#모니터링)
- [부하 테스트](#부하-테스트)
- [DB 접근](#db-접근)
- [회원 데이터 수동 삭제](#회원-데이터-수동-삭제)
- [릴리즈 절차](#릴리즈-절차)
- [점검 필요 항목](#점검-필요-항목)

---

## 액세스 로그 (Caddy)

모든 요청이 Caddy를 거치므로 "누가, 언제, 어떤 API를, 얼마나 걸려서 호출했는지"는 Caddy 액세스 로그 한 곳에서 볼 수 있습니다.

**설정** ([Caddyfile](../Caddyfile), [docker-compose.yml](../docker-compose.yml))

- JSON 형식으로 컨테이너의 `/var/log/caddy/access.log`에 기록하고, 호스트의 `./caddy_logs/`에 마운트합니다. 배포 경로가 `/home/ubuntu`이므로 EC2에서는 `/home/ubuntu/caddy_logs/access.log`입니다.
- 10MiB마다 롤오버하고 최근 5개만 보관합니다(디스크가 무한히 늘지 않음).
- 비밀번호 재설정·회원가입 인증 API가 `email`, `code`를 **쿼리스트링**으로 받기 때문에, 로그에는 그 두 파라미터를 지우도록 필터를 걸었습니다. 이 필터가 없으면 이메일과 인증번호가 로그 파일에 그대로 남습니다.
- 컨테이너가 root로 파일을 만들어서 호스트에서 읽을 때는 `sudo`가 필요할 수 있습니다.

**Caddyfile을 바꿨다면 배포 전에 문법을 검증합니다.** Caddyfile이 잘못되면 Caddy가 기동하지 못해 서비스 전체가 멈춥니다.

```bash
docker run --rm -e DOMAIN=example.com \
  -v "$PWD/Caddyfile":/etc/caddy/Caddyfile:ro \
  caddy:2-alpine caddy validate --config /etc/caddy/Caddyfile
```

**배포 후 확인**: 로그 한 줄을 열어 `email`/`code`, 토큰이 남아 있지 않은지 봅니다(Caddy는 기본적으로 `Authorization`·`Cookie` 같은 자격 증명 헤더 값을 `REDACTED`로 기록합니다).

```bash
sudo tail -n 5 /home/ubuntu/caddy_logs/access.log
```

**자주 쓰는 조회** (`sudo apt install -y jq` 필요)

```bash
cd /home/ubuntu

# 실시간
sudo tail -f caddy_logs/access.log

# 경로별 요청 수 TOP 10 (쿼리스트링 제거, 숫자 ID를 {id}로 묶음)
sudo jq -r '.request.uri | split("?")[0]' caddy_logs/access.log \
  | sed -E 's#/[0-9]+#/{id}#g' | sort | uniq -c | sort -rn | head

# 상태코드 분포
sudo jq -r '.status' caddy_logs/access.log | sort | uniq -c

# 5xx만 보기
sudo jq -c 'select(.status >= 500) | {ts, uri: .request.uri, status}' caddy_logs/access.log

# 1초 넘게 걸린 요청 (duration 단위는 초)
sudo jq -c 'select(.duration > 1) | {ts, uri: .request.uri, status, duration}' caddy_logs/access.log

# 접속 IP별 요청 수 TOP 10
sudo jq -r '.request.remote_ip' caddy_logs/access.log | sort | uniq -c | sort -rn | head
```

**읽을 때 주의**

- SSE 구독(`/status/stream`)은 연결이 **끝난 뒤** 기록되고 `duration`이 최대 30분입니다. 응답 시간 분석에서는 이 경로를 제외하세요.
- 롤오버로 오래된 로그가 지워지므로 장기 통계가 필요하면 별도로 보관해야 합니다.

---

## 모니터링

EC2 콘솔 → 인스턴스 선택 → **모니터링** 탭에서 별도 설정 없이 기본 지표(5분 간격)를 볼 수 있습니다.

| 지표 | 의미 |
| --- | --- |
| CPU 사용률 | 인스턴스 전체 CPU. 앱·MySQL·Redis·Caddy가 한 인스턴스를 나눠 쓰므로 합산값 |
| 네트워크 입력/출력(바이트) | 서버로 들어오고 나간 총량 |
| 네트워크 패킷 입력/출력 | 같은 흐름의 패킷 수 |
| CPU 크레딧 사용량/밸런스 | t 계열 버스터블 인스턴스 전용. 밸런스가 계속 줄면 지속 부하, 0이 되면 CPU가 제한됨 |

**해석 시 주의**

- 이 지표들은 **서버 단위 총량**입니다. 요청 수, API별 호출, 에러율, 응답 시간은 알 수 없으므로 [액세스 로그](#액세스-로그-caddy)와 함께 봅니다.
- 웹 서비스는 보통 응답(출력)이 요청(입력)보다 큽니다. **입력이 출력보다 수십 배 크면** 사용자 트래픽이 아니라 배포 때 받는 Docker 이미지일 가능성이 높으니 스파이크 시각을 배포 이력과 대조하세요.
- 2026-09 시점 관측: CPU 최대 17%, CPU 크레딧 밸런스가 최대치(576) 부근에서 거의 변하지 않아 실사용 트래픽으로는 여유가 컸습니다.
- 메모리·디스크 사용률은 기본 지표에 없습니다. 보려면 CloudWatch Agent를 설치해야 합니다.

---

## 부하 테스트

시나리오·스크립트·측정 결과·재현 방법은 **[loadtest/README.md](loadtest/README.md)** 에 있습니다. 여기에는 운영 원칙만 둡니다.

목적은 "어디서 무너지는지"를 숫자로 찾는 것입니다. 개선 사이클은 **측정 → 병목 하나 선택 → 개선 → 같은 조건으로 재측정**입니다.

1. **운영 서버를 바로 때리지 않습니다.** 앱·DB·Redis가 한 인스턴스에 있어서, 한계를 넘으면 실제 서비스가 느려지거나 멈춥니다. 먼저 로컬의 격리된 환경에서 재현합니다(`docs/loadtest/scripts/`가 임시 MySQL·Redis를 따로 띄웁니다).
2. 운영 서버 대상 테스트는 **별도로 날짜·시간대를 정하고** 팀에 미리 알린 뒤, 사용자가 가장 적은 시간대에 읽기 API만 낮은 강도부터 올립니다. 요청 헤더에 `User-Agent: mixmate-loadtest`를 달아 [액세스 로그](#액세스-로그-caddy)에서 실사용 트래픽과 구분하고, 테스트 계정은 끝난 뒤 [수동 삭제 절차](#회원-데이터-수동-삭제)로 지웁니다. 아직 수행하지 않았습니다.
3. **측정 전에 로깅 부하를 끕니다.** 현재 기본 설정은 `org.springframework.security: DEBUG`, `show-sql: true`입니다. 이대로 재면 로그 출력 비용이 결과에 섞이므로 환경 변수로 덮어씁니다(스크립트가 자동으로 처리).
4. 테스트 전용 계정·그룹을 쓰고, 쓰기 API는 데이터가 쌓이므로 읽기 API부터 시작합니다.
5. **로컬 결과는 "이 머신의 성능"입니다.** 부하 발생기·앱·DB가 한 머신에서 돌고, 운영과 DB 버전·사양이 다르므로 EC2의 처리량을 말해주지 않습니다. 결과 표에는 항상 환경을 함께 적습니다.

결과는 `docs/loadtest/results/<날짜>/`에 원본(k6 JSON·로그)과 함께 보관합니다.

---

## DB 접근

MySQL은 `127.0.0.1:3306`에만 열려 있어 외부에서 직접 접속할 수 없습니다. **SSH 터널**로 접속합니다.

```bash
ssh -N -L 3307:127.0.0.1:3306 -i <키.pem> ubuntu@<EC2_HOST>
```

터널을 열어 둔 채 DataGrip 같은 클라이언트에서 `127.0.0.1:3307`로 접속합니다(계정은 서버 `.env`의 `DB_USER`/`DB_PASSWORD`). 운영 DB에서 쓰기 작업을 하기 전에는 아래 백업을 먼저 합니다.

```bash
docker exec mixmate-db sh -c 'mysqldump -u root -p"$MYSQL_ROOT_PASSWORD" "$MYSQL_DATABASE"' > backup_$(date +%F).sql
```

---

## 회원 데이터 수동 삭제

정식 경로는 탈퇴 API(소프트 삭제)입니다. 테스트 계정처럼 **행 자체를 지워야 할 때만** 아래 절차를 씁니다. `user`를 바로 `DELETE`하면 `participant`·`group_ban`의 외래 키 때문에 거부됩니다.

```
Cannot delete or update a parent row: a foreign key constraint fails
(`mixmate`.`participant`, CONSTRAINT ... FOREIGN KEY (`user_id`) REFERENCES `user` (`user_id`))
```

`participant.user_id`는 NULL을 허용하고(대리 등록 참가자가 이미 NULL), `group_ban.user_id`는 NOT NULL입니다. 아래에서 `:id`는 지울 `user_id`로 바꿉니다.

```sql
START TRANSACTION;

-- 0. 영향 범위 확인
SELECT * FROM participant WHERE user_id = :id;
SELECT * FROM group_ban   WHERE user_id = :id;

-- 1. 참가 기록은 "로그인 계정 없는 참가자"로 전환 (다른 참가자의 데이터는 그대로)
UPDATE participant SET user_id = NULL WHERE user_id = :id;

-- 2. 차단 기록은 NOT NULL이라 삭제
DELETE FROM group_ban WHERE user_id = :id;

-- 3. 사용자 삭제
DELETE FROM `user` WHERE user_id = :id;

COMMIT;
```

```bash
# 4. 남아 있는 refresh 토큰 정리 (선택)
docker exec mixmate-redis redis-cli DEL REFTOKEN:<id>
```

- 그 사용자가 **HOST였던 그룹**은 관리할 계정이 사라져 아무도 상태를 전이할 수 없게 됩니다. 필요하면 그 그룹도 함께 삭제합니다.
- 지운 뒤 같은 카카오·구글 계정으로 다시 로그인하면 **새 계정으로 자동 가입**됩니다. provider 쪽 "연결 해제"만으로는 우리 DB가 지워지지 않습니다([auth.md](auth.md#소셜-로그인-카카오--구글)).
- 보관 기간이 지난 탈퇴 계정을 익명화·삭제하는 배치는 아직 없습니다. 만들게 되면 위 순서(참조 정리 → 삭제)를 그대로 따르면 됩니다.

---

## 릴리즈 절차

- 버전은 `vMAJOR.MINOR.PATCH`입니다. 새 기능이 들어가면 MINOR, 버그 수정만 있으면 PATCH를 올립니다.
- 릴리즈 노트는 앱 내 패치노트와 같은 분류(`New` / `Changed` / `Improved` / `Fixed` / `Security`)로 씁니다. 버전별 노트는 [releases.md](releases.md)에 보관합니다.
- GitHub의 릴리즈 화면 "Target" 드롭다운은 브랜치·태그만 검색되고 **커밋 SHA는 검색되지 않습니다.** 특정 커밋에 릴리즈를 걸려면 로컬에서 태그를 먼저 만들어 push한 뒤, 릴리즈 화면에서 **기존 태그**를 선택합니다.

```bash
git tag v1.3.0 <커밋>      # 커밋을 생략하면 현재 HEAD
git push origin v1.3.0
```

- "Latest" 라벨은 가장 최신 버전 하나에만 붙입니다. 과거 버전을 소급해서 올릴 때는 `None`으로 두세요.

---

## 점검 필요 항목

코드를 문서화하며 확인한 **실제 동작과 설정의 불일치, 보안·운영상 개선 후보**입니다. 급한 순서가 아니라 영역별로 정리했습니다.

| 영역 | 현황 | 영향 | 제안 |
| --- | --- | --- | --- |
| JWT 만료 설정 | `application.yaml`에 `access-expiration: 30분`, `refresh-expiration: 14일`이 있지만 코드는 읽지 않음. 실제 값은 `JwtUtil` 상수 **1시간 / 7일** | 설정을 바꿔도 동작이 바뀌지 않아 오해를 부름 | yaml 키를 지우거나 `@Value`로 연결 |
| 로그 레벨 | 모든 프로필에서 `org.springframework.security: DEBUG`, `show-sql: true`, `format_sql: true` | 로그 과다, 성능 영향. Security DEBUG 로그는 요청 URL을 찍으므로 쿼리스트링의 이메일·인증번호가 남을 수 있음 | 운영 프로필에서 `INFO` / `show-sql: false` |
| 인증번호 보안 | 인증번호를 `java.util.Random`으로 생성. 재설정 검증(`/password/verify`)에 **시도 횟수 제한이 없음**(회원가입 인증도 같은 패턴). 이메일·코드가 쿼리스트링으로 전달됨 | 6자리를 5분 안에 무차별 대입할 여지, URL·로그에 민감값 노출 | `SecureRandom`, 실패 횟수 제한(Redis 카운터), 이메일·코드를 POST 바디로 이동(프론트와 협의) |
| 계정 열거 | 로그인에서 없는 이메일은 `USER_NOT_FOUND`(404), 비밀번호 불일치는 `INVALID_PASSWORD`(401)로 갈림. 비밀번호 재설정 발송도 가입 여부를 응답으로 드러냄 | 가입된 이메일을 외부에서 확인할 수 있음 | 로그인 실패 응답을 하나로 통일(프론트의 분기 확인 필요) |
| CORS | 운영에도 `http://localhost:*`와 팀원 ngrok 주소가 허용 목록에 있음 | 불필요한 오리진 허용 | 운영 프로필에서 제외 |
| 배포 파이프라인 | 테스트 생략, `latest` 태그만 사용, `down` → `up` | [architecture.md](architecture.md#현재-구조의-한계와-개선-후보) 참고 | 〃 |
| 앱 종료 시 SSE 연결 | 종료를 시작하면 열려 있는 SSE 요청 때문에 그레이스풀 셧다운이 제한 시간(30초)을 모두 기다리고, 이후 `AsyncRequestTimeoutException`이 "처리되지 않은 예외" ERROR로 로깅됨 ([부하 테스트 보고서](loadtest/README.md#부수-발견-앱-종료-때-sse-연결이-30초를-붙잡는다)) | 배포(`docker compose down`)마다 종료가 지연되고 Docker의 기본 종료 대기 시간(10초)을 넘으면 강제 종료될 수 있음. 불필요한 ERROR 로그 | 종료 시작 시 구독 emitter를 먼저 완료 처리, 이 예외를 정상 종료로 분류 |
| 계정 삭제 | 하드 삭제 배치 없음, 카카오 연결 끊기 콜백 미구현 | 탈퇴·연결 해제한 계정의 개인정보가 DB에 남음 | 보관 기간 후 익명화·삭제 배치, 필요 시 Unlink Callback |
