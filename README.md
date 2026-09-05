# 🍻 MixMate — 처음 보는 사람들과도 자연스럽게 섞이는 술자리 매칭 서비스

<img width="100%" alt="Frame 34" src="https://github.com/user-attachments/assets/67491883-0ec6-4086-ac86-89e6e02397f9" />

같은 학교 학생들이 초대코드로 모여 소규모 그룹을 만들고, 성별·MBTI·학년 조건에 맞춰 자동으로 조를 편성해주는 서비스입니다. 1차 모임이 끝나면 MVP 투표와 2차 참여 여부 투표를 진행해 다음 조 편성까지 이어집니다.

현재 학생들을 상대로 실사용 운영중입니다.

실사용 운영 및 유지보수 : 2026.09.05 ~

[![Java](https://img.shields.io/badge/Java-17-orange?logo=openjdk)](.)
[![Spring Boot](https://img.shields.io/badge/Spring%20Boot-3.5.3-6DB33F?logo=springboot)](.)
[![MySQL](https://img.shields.io/badge/MySQL-8.0-4479A1?logo=mysql&logoColor=white)](.)
[![Redis](https://img.shields.io/badge/Redis-DC382D?logo=redis&logoColor=white)](.)
[![Docker](https://img.shields.io/badge/Docker%20Compose-2496ED?logo=docker&logoColor=white)](.)

---

## 목차

- [기획 배경](#기획-배경)
- [핵심 기능](#핵심-기능)
- [기술 스택](#tech-stack)
- [아키텍처](#architecture)
- [ERD](#erd)
- [팀 구성](#팀-구성)
- [브랜치 · 커밋 컨벤션](#브랜치--커밋-컨벤션)

---

## 💡 기획 배경

대학생들을 대상으로 불편한 점을 조사하던 중, 동아리·학생회에서 술자리 조를 짤 때 다음과 같은 어려움이 반복된다는 걸 발견했습니다.

- 성별·MBTI·학년 등 여러 조건을 고려해 조원을 수기로 배정하는 게 번거롭다
- 참가자 명단을 엑셀·카톡으로 따로 관리하다 보니 누락·중복이 잦다
- 1차가 끝난 뒤 2차 참여 여부를 다시 취합하고, 조를 새로 짜야 할 때 처음부터 반복해야 한다

MixMate는 이 과정을 초대코드 참가 → 조건 기반 자동 조 편성 → 투표 기반 다음 라운드 진행까지 하나의 플로우로 묶어, 운영진이 수작업으로 하던 매칭·명단 관리를 자동화하기 위해 만들었습니다.

---

## ✨ 핵심 기능

**Auth — 인증/회원**
- 이메일 인증 기반 회원가입, 비밀번호 재설정
- JWT Access/Refresh 발급 및 재발급(reissue), Redis 기반 로그아웃 블랙리스트

**Group — 그룹**
- 초대코드 생성·검증·참가, 관리자의 오프라인 참가자 대리등록
- 사용자 차단(ban), 8단계 상태 머신으로 진행 상태 관리
- SSE 기반 그룹 진행 상태 실시간 push

**Assignment — 조 편성**
- 성별·MBTI·학년 균형 등 조건을 조합한 자동 조 편성 알고리즘
- 특정 인원 고정 배치, 관리자 재편성

**Vote — 투표**
- 1차 종료 후 MVP 투표
- 2차 참여 여부 투표 및 본인/관리자에 의한 정정(대리 등록 참가자 포함), 관리자 강제 종료

---

## 🛠️ tech stack

| 구분 | 사용 기술 |
| --- | --- |
| Language / Framework | Java 17, Spring Boot 3.5.3, Spring Security, Spring Data JPA (Hibernate) |
| Database | MySQL 8.0, Redis (이메일 인증코드 · 리프레시 토큰 · 로그아웃 블랙리스트) |
| 인증 | JWT (jjwt), Redis 기반 Refresh Token 재발급 / 블랙리스트 |
| 실시간 | SSE(Server-Sent Events) — 그룹 진행 상태 실시간 push |
| 문서화 | Swagger (springdoc-openapi) |
| 인프라 | Docker, Docker Compose, Caddy(리버스 프록시), GitHub Actions(CI/CD) |

---

## 🏗️ Architecture
=== 아키텍처 사진 추가 예정 ===


- **Docker / Docker Compose**: 로컬(`docker-compose.local.yml`)과 운영(`docker-compose.yml`) 구성을 분리해, "내 컴퓨터에서는 되는데" 문제 없이 앱·DB·캐시·프록시를 하나의 정의로 재현합니다.
- **Caddy**: Nginx 대비 최소 설정으로 Let's Encrypt 인증서 발급·갱신을 자동 처리합니다.
- **MySQL 포트 제한**: 3306은 `127.0.0.1`에만 바인딩해 외부에서 직접 접근할 수 없고, DB 확인은 SSH 터널을 통해서만 가능합니다.
- **CI/CD**: `dev` 브랜치 push 시 GitHub Actions가 이미지를 빌드해 Docker Hub에 올리고, SSH로 EC2에 접속해 최신 이미지로 재기동합니다. 수동 배포로 인한 실수(옛날 이미지 재기동, 설정 누락)를 없애기 위한 선택이었습니다.

---

## 🗂️ ERD

<img width="1590" height="837" alt="Image" src="https://github.com/user-attachments/assets/207d7af8-76cd-4e3e-8c13-ca14d8fe57f0" />

---

## 🧑‍🤝‍🧑 팀 구성

| FE | FE | FE | FE | BE | BE |
| :---: | :---: | :---: | :---: | :---: | :---: |
| <img src="https://avatars.githubusercontent.com/Koo134o" width="90" height="90"/><br>[고민경](https://github.com/Koo134o) | <img src="https://avatars.githubusercontent.com/moonchanju" width="90" height="90"/><br>[문찬주](https://github.com/moonchanju) | <img src="https://avatars.githubusercontent.com/pdar124" width="90" height="90"/><br>[박다래](https://github.com/pdar124) | <img src="https://avatars.githubusercontent.com/BaekSeungBin" width="90" height="90"/><br>[백승빈](https://github.com/BaekSeungBin) | <img src="https://avatars.githubusercontent.com/KDWorld81" width="90" height="90"/><br>[곽동욱](https://github.com/KDWorld81) 👑 | <img src="https://avatars.githubusercontent.com/meoooogus" width="90" height="90"/><br>[김대현](https://github.com/meoooogus) |

## 📝 브랜치 · 커밋 컨벤션

| **메시지 타입** | **설명**                                                    |
| --------------- | ----------------------------------------------------------- |
| **feat**        | ✨ 새로운 기능 추가 및 기존 기능 수정                       |
| **fix**         | 🐛 버그 수정                                                |
| **docs**        | 📚 문서 및 주석 수정                                        |
| **style**       | 🎨 코드 스타일 및 포맷팅 수정                               |
| **refact**      | ♻️ 기능 변화 없는 코드 리팩터링                             |
| **test**        | ✅ 테스트 코드 추가/수정                                    |
| **chore**       | 🔧 패키지 매니저 수정 및 기타 잡다한 변경(ex: `.gitignore`) |
| **merge**       | 🔀 브랜치 병합                                              |
