# 🍻 MixMate — 처음 보는 사람들과도 자연스럽게 섞이는 술자리 매칭 서비스

<img width="100%" alt="표지" src="https://github.com/user-attachments/assets/5e707b57-3ea8-41de-937f-35d1ed19365b" />

 [📎MixMate URL](https://mix-mate-web.vercel.app/
)

> MixMate는 참가자 모집과 자동 조 편성, 술게임·대화 주제 추천, MVP·2차 참여 투표를 하나의 흐름으로 연결하는 모임 운영 서비스입니다.   
운영자는 모임의 진행을 관리하고, 참가자는 자신의 조와 다음 활동을 확인하며 자연스럽게 어울릴 수 있습니다.  
 <br/>

현재 학생들을 상대로 실사용 운영중입니다.

실사용 운영 및 유지보수 : 2026.09.05 ~

[![Java](https://img.shields.io/badge/Java-17-orange?logo=openjdk)](.)
[![Spring Boot](https://img.shields.io/badge/Spring%20Boot-3.5.3-6DB33F?logo=springboot)](.)
[![MySQL](https://img.shields.io/badge/MySQL-8.0-4479A1?logo=mysql&logoColor=white)](.)
[![Redis](https://img.shields.io/badge/Redis-DC382D?logo=redis&logoColor=white)](.)
[![Docker](https://img.shields.io/badge/Docker%20Compose-2496ED?logo=docker&logoColor=white)](.)
[![AWS](https://img.shields.io/badge/AWS-EC2-FF9900?logo=amazonaws&logoColor=white)](.)
[![GitHub Actions](https://img.shields.io/badge/GitHub%20Actions-CI%2FCD-2088FF?logo=githubactions&logoColor=white)](.)


---

## 서비스 소개
<img width="100%" alt="README • About MixMate" src="https://github.com/user-attachments/assets/b3058c84-402f-4b8e-ab49-9d6b75bba6d2" />

## 서비스 이용 흐름

<img width="100%" alt="README • User Flow" src="https://github.com/user-attachments/assets/c56e5577-086c-436c-bed2-85ccefae39cf" />

---

## 백엔드 문서

코드를 기준으로 작성한 문서입니다. API 명세의 기준은 Swagger(`/swagger-ui/index.html`)이고, 여기에는 코드만 봐서는 알기 어려운 **구조 · 정책 · 결정 배경**을 정리했습니다.

| 문서 | 내용 |
| --- | --- |
| [architecture.md](docs/architecture.md) | 배포 구조, 컨테이너 구성, CI/CD, 환경 변수, 현재 구조의 한계 |
| [domain-model.md](docs/domain-model.md) | ERD, 엔티티·enum, 설계 결정(대리 등록 참가자, 소프트 삭제, MVP 집계) |
| [group-lifecycle.md](docs/group-lifecycle.md) | 그룹 8단계 상태 머신, 전이 API와 조건, SSE 실시간 푸시 |
| [auth.md](docs/auth.md) | JWT·쿠키, 소셜 로그인 정책, 비밀번호 재설정, 회원 탈퇴 |
| [assignment.md](docs/assignment.md) | 조 편성 알고리즘, 조건, 경고, 동시성 제어 |
| [vote.md](docs/vote.md) | MVP·2차 참여 투표 규칙, MVP 누적 횟수 집계 |
| [design-decisions.md](docs/design-decisions.md) | 설계 결정(ADR) — 토큰 전략, 소셜 로그인 정책, 조 편성 알고리즘, 상태 머신 등 *왜 그렇게 만들었는지* |
| [troubleshooting.md](docs/troubleshooting.md) | 장애·버그 대응 기록 (증상 → 원인 규명 → 해결과 선택 근거) |
| [loadtest/README.md](docs/loadtest/README.md) | 부하 테스트 보고서 — OSIV 재현 실험, 용량 테스트, 재현 방법과 원본 결과 |
| [operations.md](docs/operations.md) | 로그·모니터링, 부하 테스트, DB 수동 작업, 릴리즈 절차, 점검 필요 항목 |
| [releases.md](docs/releases.md) | 버전별 릴리즈 노트 |

### 읽는 순서

- 서비스 구조가 궁금하다 → `architecture` → `domain-model` → `group-lifecycle`
- 설계 의도와 문제 해결 과정이 궁금하다 → `design-decisions`, `troubleshooting`
- 인증/권한을 건드린다 → `auth`
- 조 편성·투표 로직을 건드린다 → `assignment`, `vote`
- 장애가 났다 / 배포·운영 → `operations`, `troubleshooting`

### 문서 작성 원칙

- 코드가 사실의 기준입니다. 정책이나 수치(만료 시간, 최소 인원 등)를 바꾸면 해당 문서도 같이 고칩니다.
- 수치·상태명·경로는 코드에서 확인한 값만 적고, 문서에서 코드 위치를 링크로 남깁니다.
- 장애 기록에는 관련 커밋 해시를 남겨 `git show <hash>`로 근거를 바로 볼 수 있게 합니다.

---

## 🧑‍🤝‍🧑 팀 구성

| FE | FE | FE | FE | BE | BE |
| :---: | :---: | :---: | :---: | :---: | :---: |
| <img src="https://avatars.githubusercontent.com/Koo134o" width="90" height="90"/><br>[고민경](https://github.com/Koo134o) | <img src="https://avatars.githubusercontent.com/moonchanju" width="90" height="90"/><br>[문찬주](https://github.com/moonchanju) | <img src="https://avatars.githubusercontent.com/pdar124" width="90" height="90"/><br>[박다래](https://github.com/pdar124) | <img src="https://avatars.githubusercontent.com/BaekSeungBin" width="90" height="90"/><br>[백승빈](https://github.com/BaekSeungBin) | <img src="https://avatars.githubusercontent.com/KDWorld81" width="90" height="90"/><br>👑[곽동욱](https://github.com/KDWorld81) 👑 | <img src="https://avatars.githubusercontent.com/meoooogus" width="90" height="90"/><br>[김대현](https://github.com/meoooogus) |
