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

## 주요 기능
<img width="100%" alt="README • Key Features 4" src="https://github.com/user-attachments/assets/7559697d-76df-4c69-8dce-54f6f5faf7ba" />
<img width="100%" alt="README • Key Features 5" src="https://github.com/user-attachments/assets/362da093-c4c1-484f-8182-33ea175edec1" />

## 목차

- [Architecture](#architecture)
- [ERD](#erd)
- [핵심 기능](#핵심-기능)
- [기술 스택](#tech-stack)
- [팀 구성](#팀-구성)
- [브랜치 · 커밋 컨벤션](#브랜치--커밋-컨벤션)


---

## 🏗️ Architecture
=== 아키텍처 사진 추가 예정 ===


- **Docker / Docker Compose**: 로컬(`docker-compose.local.yml`)과 운영(`docker-compose.yml`) 구성을 분리해, "내 컴퓨터에서는 되는데" 문제 없이 앱·DB·캐시·프록시를 하나의 정의로 재현합니다.
- **Caddy**: Nginx 대비 최소 설정으로 Let's Encrypt 인증서 발급·갱신을 자동 처리합니다.
- **MySQL 포트 제한**: 3306은 `127.0.0.1`에만 바인딩해 외부에서 직접 접근할 수 없고, DB 확인은 SSH 터널을 통해서만 가능합니다.
- **CI/CD**: `dev` 브랜치 push 시 GitHub Actions가 이미지를 빌드해 Docker Hub에 올리고, SSH로 EC2에 접속해 최신 이미지로 재기동합니다. 수동 배포로 인한 실수(옛날 이미지 재기동, 설정 누락)를 없애기 위한 선택이었습니다.

---

## 🗂️ ERD

<img width="1090" height="689" alt="Image" src="https://github.com/user-attachments/assets/b2cf8cce-3ec2-4ff7-97c9-c20ec313ca14" />

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
| 인프라 | AWS, Docker, Docker Compose, Caddy(리버스 프록시), GitHub Actions(CI/CD) |

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
