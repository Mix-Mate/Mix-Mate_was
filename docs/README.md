# MixMate 백엔드 문서

코드를 기준으로 작성한 문서입니다. API 명세의 기준은 Swagger(`/swagger-ui/index.html`)이고, 여기에는 코드만 봐서는 알기 어려운 **구조 · 정책 · 결정 배경**을 정리했습니다.

| 문서 | 내용 |
| --- | --- |
| [architecture.md](architecture.md) | 배포 구조, 컨테이너 구성, CI/CD, 환경 변수, 현재 구조의 한계 |
| [domain-model.md](domain-model.md) | ERD, 엔티티·enum, 설계 결정(대리 등록 참가자, 소프트 삭제, MVP 집계) |
| [group-lifecycle.md](group-lifecycle.md) | 그룹 8단계 상태 머신, 전이 API와 조건, SSE 실시간 푸시 |
| [auth.md](auth.md) | JWT·쿠키, 소셜 로그인 정책, 비밀번호 재설정, 회원 탈퇴 |
| [assignment.md](assignment.md) | 조 편성 알고리즘, 조건, 경고, 동시성 제어 |
| [vote.md](vote.md) | MVP·2차 참여 투표 규칙, MVP 누적 횟수 집계 |
| [design-decisions.md](design-decisions.md) | 설계 결정(ADR) — 토큰 전략, 소셜 로그인 정책, 조 편성 알고리즘, 상태 머신 등 *왜 그렇게 만들었는지* |
| [troubleshooting.md](troubleshooting.md) | 장애·버그 대응 기록 (증상 → 원인 규명 → 해결과 선택 근거) |
| [loadtest/README.md](loadtest/README.md) | 부하 테스트 보고서 — OSIV 재현 실험, 용량 테스트, 재현 방법과 원본 결과 |
| [operations.md](operations.md) | 로그·모니터링, 부하 테스트, DB 수동 작업, 릴리즈 절차, 점검 필요 항목 |
| [releases.md](releases.md) | 버전별 릴리즈 노트 |

## 읽는 순서

- 서비스 구조가 궁금하다 → `architecture` → `domain-model` → `group-lifecycle`
- 설계 의도와 문제 해결 과정이 궁금하다 → `design-decisions`, `troubleshooting`
- 인증/권한을 건드린다 → `auth`
- 조 편성·투표 로직을 건드린다 → `assignment`, `vote`
- 장애가 났다 / 배포·운영 → `operations`, `troubleshooting`

## 문서 작성 원칙

- 코드가 사실의 기준입니다. 정책이나 수치(만료 시간, 최소 인원 등)를 바꾸면 해당 문서도 같이 고칩니다.
- 수치·상태명·경로는 코드에서 확인한 값만 적고, 문서에서 코드 위치를 링크로 남깁니다.
- 장애 기록에는 관련 커밋 해시를 남겨 `git show <hash>`로 근거를 바로 볼 수 있게 합니다.
