# Sprint 4: Complete the main focus loop (Nov 1 to 5)

## Sprint planning

**Goal:** complete US-05, US-06, US-07, US-08, US-09 and US-11. The focus session works end to end with live feedback, coins and a summary.

12 cards, 40 points, all due Nov 5.

| Card | Owner | Points | Priority |
|---|---|---|---|
| POST /sessions, session rules and GET /robots/me/active-session | Lama | 5 | Must |
| POST /robots/{robotId}/focus-states | Lama | 5 | Must |
| Pairing endpoints: pairing-codes, pair, pairing-status | Noura | 5 | Must |
| Reward calculation and coin update on session end | Noura | 5 | Must |
| GET /sessions/{id} summary and state statistics | Noura | 2 | Must |
| Focus Session screen with timer, status chip and face | Gheed | 5 | Must |
| Session Complete screen | Gheed | 3 | Must |
| End Session confirm and outcome message | Shahd | 2 | Must |
| Update coin balance after a session | Shahd | 1 | Must |
| Robot display: faces for each state | Gheed | 3 | Should |
| Session endpoint integration tests | Lama | 3 | Should |
| Sprint 4 planning, review and retrospective | Shahd | 1 | Must |

## Sprint review

**Sprint goal met:** the full focus loop worked end to end for the first time. The robot detected focus, the app showed the state live, and ending a session awarded coins.

**Done:** 12 cards, 40 points (100%).

Delivered:
- Focus Session screen with timer, status chip, robot face, pause and end, and the Session Complete summary (PR #15).
- Shared coin balance on every screen, refreshed after a session (PR #16).
- The robot checks for an active session and sends its states to the backend, the app shows them, and ending a session awards coins. This was the first time the whole loop ran end to end.
- Robot faces for each state on the display.
- Pairing endpoints, session endpoints and their integration tests, and the reward and summary statistics on the backend.

**Demo:** the full focus loop (robot, backend and app together), the Focus Session screen and the Session Complete summary.

## Retrospective

**What went well**
- The main feature worked end to end for the first time, and every card of the sprint was finished.
- The frontend kept moving thanks to mock data and a demo branch that never went into `develop`.

**What did not go well**
- Integration happened on a separate branch instead of through reviewed PRs, so `develop` stopped moving after Nov 3.
- The same work existed in two versions (two history endpoints, two pairing configs), and nobody decided which one stays.

**What we change next sprint**
- Agreed: build on `feature/full-e2e` first and merge into `develop` later, but every change still goes through a PR into that branch.
- When two people build the same thing, decide in the stand-up which version stays.
