# 투표

> 설계 배경과 트레이드오프: [설계 결정 4 · MVP 누적 횟수](design-decisions.md#4-mvp-누적-횟수--카운터-대신-파생-집계)

1차가 끝나면 두 가지 투표가 진행됩니다. 로직은 [VoteService](../src/main/java/com/mixmate/domain/vote/service/VoteService.java)에 있고, 모든 투표 API는 `/api/v1/groups/{groupId}/votes/...` 아래입니다.

| 투표 | 대상 | 저장 |
| --- | --- | --- |
| MVP 투표 | 같은 조원 중 1명 | `mvp_vote` (voter 유니크) |
| 2차 참여 여부 | 본인의 `PARTICIPATE` / `NOT_PARTICIPATE` | `round2_participation_vote` (voter 유니크) |

두 투표 모두 그룹 상태가 **`VOTING`일 때만** 가능합니다(아니면 `VOTE_NOT_IN_PROGRESS`).

## MVP 투표

`POST /votes/mvp`

- 본인에게는 투표할 수 없습니다.
- **1차 조 편성 기준 같은 조원**에게만 투표할 수 있습니다.
- 한 사람이 한 번만 투표합니다(`ALREADY_VOTED`). 정정 API는 없습니다.

## 2차 참여 여부 투표

| 동작 | API |
| --- | --- |
| 투표 | `POST /votes/second-round` |
| 정정 | `PATCH /votes/second-round` (이미 투표한 건만 가능) |
| 관리자가 대신 투표 | `POST /votes/second-round/admin` |
| 관리자가 대신한 투표 정정 | `PATCH /votes/second-round/admin` |
| 현황 확인 | `GET /votes/second-round/status` |

- **관리자 대리 투표는 계정이 없는(대리 등록된) 참가자에게만** 쓸 수 있습니다. 계정이 있는 참가자는 본인만 투표할 수 있어서 이 경로로는 `NOT_MANUAL_PARTICIPANT`입니다.
- 관리자가 대신 투표 처리한 참가자는 현황의 미투표 목록에서 빠집니다.
- 참가자의 `RoundParticipation` 기본값은 `FIRST_ONLY`입니다. **참여를 선택하면 `FIRST_AND_SECOND`로 바뀌고**, 정정으로 불참으로 바꾸면 다시 `FIRST_ONLY`가 됩니다. 이 값이 2차 편성 대상과 2차 진행 결정(**참여 8명 이상**)의 기준입니다([group-lifecycle.md](group-lifecycle.md)).

## 투표 종료

`POST /votes/finish` (관리자). 지금 코드에서 **투표가 끝나는 유일한 경로**이고, 그룹이 `VOTING` → `VOTE_CLOSED`로 넘어갑니다. 한 번의 호출에서 다음이 일어납니다.

1. 아직 투표하지 않은 참가자는 모두 **2차 불참**으로 자동 처리합니다.
2. MVP 당선자를 확정해 `participant.is_mvp = true`로 기록합니다.
3. 그룹 상태를 `VOTE_CLOSED`로 바꾸고 상태 변경 이벤트를 발행합니다(SSE로 참가자에게 전달).

## 결과 조회

`GET /votes/result` — 투표가 종료된 뒤(`isVoteFinished`)에만 조회할 수 있습니다. MVP 당선자 목록과 각자의 1차 조 번호를 내려줍니다.

### MVP 당선 규칙

**그 모임에서 가장 많은 표를 받은 사람(들)** 이 MVP입니다. 조별이 아니라 **모임 전체**에서 최다 득표 수를 구하고, 그 표를 받은 참가자를 모두 뽑습니다. 동점이면 **공동 MVP**이고, 투표가 하나도 없으면 당선자는 없습니다. 이 계산은 결과 조회와 투표 종료 시 확정이 같은 메서드(`findMvpWinners`)를 써서 서로 어긋나지 않습니다.

## MVP 누적 횟수

마이페이지(`GET /api/v1/auth/me`)와 참가자 프로필 조회 응답에 `mvpCount`가 들어갑니다.

- 값은 **계정 기준 누적**입니다: 그 계정의 `participant` 행 중 `is_mvp = true`인 개수(`countByUserAndIsMvpTrue`).
- `User`에 카운터를 두지 않고 매번 집계하는 이유: 증가 로직을 빼먹었을 때 실제 데이터와 어긋나는 일을 구조적으로 막기 위해서입니다. 대신 조회 때마다 COUNT 쿼리가 한 번 나갑니다.
- 대리 등록 참가자(계정 없음)는 집계할 계정이 없어 항상 `0`입니다.
- 프로필에 표시되는 메달 배지는 이 누적 횟수를 프론트에서 등급으로 변환한 것입니다.
