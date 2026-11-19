# 인증 · 계정

> 설계 배경과 트레이드오프: [설계 결정 1 · 토큰 전략](design-decisions.md#1-토큰-전략--stateless-access--서버-통제-refresh) · [설계 결정 2 · 소셜 로그인 정책](design-decisions.md#2-소셜-로그인-계정-정책--자동-연동하지-않는다) · [설계 결정 7 · 탈퇴 정책](design-decisions.md#7-탈퇴는-소프트-삭제다)

모든 경로는 `/api/v1/auth` 아래에 있습니다(마이페이지 포함). 상세 요청·응답은 Swagger를 참고하세요.

## 토큰

| 토큰 | 만료 | 서버 저장 | 비고 |
| --- | --- | --- | --- |
| Access | **1시간** | 없음 (서명·만료 검증만으로 인증) | HS256 JWT, subject는 이메일. 로그아웃·탈퇴한 토큰만 예외로 Redis `BLACKLIST:{token}`에 올려 폐기 |
| Refresh | **7일** | Redis `REFTOKEN:{userId}` | 사용자당 1개. 다시 로그인하면 값이 덮어써져 이전 기기의 refresh는 무효 |

두 토큰 모두 클라이언트는 응답 바디와 쿠키로 받아 보관합니다([토큰 전달 방식](#토큰-전달-방식)). "서버 저장"은 서버 쪽 저장소(Redis)에 두느냐를 뜻합니다.

- 만료 시간은 [JwtUtil](../src/main/java/com/mixmate/security/JwtUtil.java)에 상수로 들어 있습니다. `application.yaml`의 `jwt.access-expiration`/`refresh-expiration`은 코드에서 읽지 않는 값이라 실제 동작과 무관합니다([operations.md](operations.md#점검-필요-항목)).
- 로그아웃하면 Redis의 refresh를 지우고 access를 `BLACKLIST:{accessToken}`에 **남은 만료 시간만큼** 등록합니다.
- 재발급(`POST /reissue`)은 refresh를 쿠키에서 읽고(없으면 요청 바디), 서명·만료를 검증한 뒤 Redis에 저장된 값과 같은지 대조합니다. **access만 새로 발급**하고 refresh는 교체하지 않습니다.

## 토큰 전달 방식

- 로그인 계열(`/login`, `/oauth/kakao`, `/oauth/google`) 응답은 토큰을 **응답 바디와 쿠키 둘 다**로 내려줍니다.
- 쿠키는 `HttpOnly; Secure; SameSite=None; Path=/`이고 `Max-Age`는 실제 토큰 만료와 같습니다.
- 요청에서 access 토큰은 **`Authorization: Bearer` 헤더를 우선**, 없으면 `accessToken` 쿠키를 읽습니다([JwtUtil.resolveToken](../src/main/java/com/mixmate/security/JwtUtil.java)).
- 헤더 우선인 이유: 일부 모바일 브라우저는 크로스도메인 `SameSite=None` 쿠키를 저장하지 않아, 쿠키만 보면 로그아웃·탈퇴가 항상 `UNAUTHORIZED`로 막혔습니다([troubleshooting](troubleshooting.md#2-1-모바일에서-로그아웃회원탈퇴가-항상-실패)).

## 공개 엔드포인트

인증 없이 호출할 수 있는 경로는 아래뿐이고 나머지는 모두 인증이 필요합니다([SecurityConfig](../src/main/java/com/mixmate/config/SecurityConfig.java)).

`/signup`, `/send`, `/verify`, `/password/send`, `/password/verify`, `/password/reset`, `/login`, `/oauth/kakao`, `/oauth/google`, `/reissue` (모두 `/api/v1/auth` 하위), Swagger 관련 경로, `/error`

## 이메일 회원가입

1. `POST /send?email=` — 6자리 인증번호를 메일로 발송, Redis `EMAIL_AUTH:{email}`에 **5분**
2. `POST /verify?email=&code=` — 일치하면 Redis `VERIFIED_EMAIL:{email}`을 **10분** 동안 열어 둠(인증번호는 즉시 삭제해 재사용 방지)
3. `POST /signup` — 위 인증 완료 상태를 확인하고 가입. 비밀번호는 BCrypt로 해시해 저장

## 소셜 로그인 (카카오 · 구글)

프론트가 provider에서 받은 **인가 코드**를 `POST /oauth/kakao`·`/oauth/google`로 보내면, 서버가 provider와 코드를 교환해 사용자 정보를 얻습니다.

- 서버는 사용자 정보 API 호출에만 provider의 access token을 쓰고 **저장하지 않습니다**. DB에 남는 것은 `provider`, `providerId`(provider의 고유 회원번호), 이메일, 이름뿐이며 provider 계정에 접근할 수 있는 자격 증명은 없습니다.
- 재로그인 식별은 `(provider, providerId)`입니다. 처음 로그인하면 자동 가입되고, 이때 비밀번호는 없습니다(`password = null`).
- 이름은 provider 닉네임을 쓰되 컬럼 길이(10자)에 맞춰 자르고, 비어 있으면 `카카오사용자`/`구글사용자`로 채웁니다.
- 이메일을 받지 못하면(카카오 이메일 동의 안 함 등) `OAUTH_EMAIL_CONSENT_REQUIRED`로 거부합니다. 이메일이 JWT subject이자 유니크 키이기 때문입니다.
- **같은 이메일이 이미 다른 방식으로 가입되어 있으면 자동 연동하지 않고 `409 EMAIL_CONFLICTED`로 막습니다.** 이메일이 같다고 같은 사람이라고 가정하지 않고, 기존 방식으로 로그인하도록 유도하는 정책입니다. 일반가입으로 만든 이메일로 구글 로그인을 시도하는 경우도 같습니다.
- 카카오 쪽 "연결 해제"는 우리 DB에 반영되지 않습니다. 연결 끊기 콜백(Unlink Callback)을 구현하지 않았기 때문이며, 계정을 지우려면 탈퇴 API나 [수동 삭제 절차](operations.md#회원-데이터-수동-삭제)를 써야 합니다.

## 비밀번호 재설정 (찾기 · 변경)

로그인 상태의 "비밀번호 변경"도 별도 API 없이 같은 3단계를 씁니다.

1. `POST /password/send?email=` — 인증번호 발송 (`PW_RESET_AUTH:{email}`, 5분)
2. `POST /password/verify?email=&code=` — 검증 성공 시 `PW_RESET_VERIFIED:{email}`을 10분 동안 열어 둠
3. `POST /password/reset` — 새 비밀번호 저장. 저장 후 인증 상태를 지우고 **refresh 토큰도 삭제**해 다른 기기의 로그인을 무효화

**소셜 로그인 계정은 비밀번호가 없으므로 1단계에서 `NOT_LOCAL_ACCOUNT`(400)로 막습니다.** 3단계에도 같은 검사가 있어 이중으로 막힙니다. 탈퇴한 계정(`deleted_at` 존재)은 조회에서 제외되어 `USER_NOT_FOUND`입니다. 이 정책이 두 단계에 걸치게 된 경위는 [troubleshooting](troubleshooting.md#2-5-소셜-로그인-계정의-비밀번호-재설정--정책은-마지막이-아니라-맨-앞에서-강제한다)에 있습니다.

프론트 처리 기준: 마이페이지 응답의 `provider`가 `LOCAL`이 아니면 비밀번호 변경 진입점에서 "소셜 로그인 계정은 비밀번호가 없다"는 안내를 보여 주고, "비밀번호 찾기" 화면에서는 `/password/send`가 `NOT_LOCAL_ACCOUNT`를 반환하면 인증번호 입력 화면으로 넘어가지 않고 같은 안내를 보여 줍니다.

## 회원 탈퇴

`DELETE /withdraw` — 소프트 삭제입니다([domain-model.md](domain-model.md#탈퇴는-소프트-삭제다)).

- **로컬 계정**은 현재 비밀번호를 함께 보내 본인 확인을 합니다(틀리면 `INVALID_PASSWORD`).
- **소셜 계정**은 비밀번호가 없으므로 이미 JWT 인증을 통과했다는 사실로 본인 확인을 대신합니다. 요청 바디의 `password`는 비워도 됩니다. 프론트는 `provider`로 분기해 소셜 계정에는 비밀번호 입력 없이 확인 창만 띄우면 됩니다.
- 처리: `deleted_at` 기록 + 이메일 변조 → Redis refresh 삭제 → 현재 access를 블랙리스트에 등록(`"withdraw"`) → 쿠키 만료.
- 탈퇴한 계정의 토큰으로 요청하면 인증 실패(401)로 처리됩니다. 서명이 유효해도 계정이 사라졌기 때문입니다.

## 인증 실패 응답

모든 에러는 `{ "code": "...", "message": "..." }` 형식입니다. JWT 필터가 실패 사유(블랙리스트, 만료·파싱 실패 등)를 요청 속성에 담고, 진입점이 그 사유에 맞는 코드를 내려줍니다. 프론트는 `message`가 아니라 `code`로 분기하세요. 전체 코드는 [ErrorCode](../src/main/java/com/mixmate/exception/ErrorCode.java)를 참고하세요.

| 코드 | HTTP | 상황 |
| --- | --- | --- |
| `UNAUTHORIZED` | 401 | 토큰이 없음 (그 밖의 구체적 사유가 없을 때의 기본값) |
| `BLACKLIST_TOKEN` | 401 | 로그아웃·탈퇴로 폐기된 access 토큰 |
| `JWT_TOKEN_PARSING_ERROR` | 401 | 토큰 파싱 실패·만료, 서명은 유효하지만 계정이 탈퇴된 경우 |
| `INVALID_PASSWORD` | 401 | 로그인·탈퇴 시 비밀번호 불일치 |
| `USER_NOT_FOUND` | 404 | 없는 계정, 탈퇴한 계정 (로그인·비밀번호 재설정) |
| `EMAIL_NOT_VERIFIED` | 400 | 이메일 인증을 마치지 않고 가입·재설정 시도 |
| `EMAIL_CONFLICTED` | 409 | 가입하려는 이메일이 이미 사용 중 (일반·소셜 공통) |
| `NOT_LOCAL_ACCOUNT` | 400 | 소셜 계정으로 비밀번호 재설정 시도 |
| `OAUTH_EMAIL_CONSENT_REQUIRED` | 400 | 소셜 로그인에서 이메일을 받지 못함 |
| `OAUTH_LOGIN_FAILED` | 400 | 소셜 로그인 처리 실패 |
