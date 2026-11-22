# Sprint 4: Complete the main focus loop (Nov 1 to 5)

## Sprint planning

**Goal:** complete US-05, US-06, US-07, US-08, US-09 and US-11. The focus session works end to end with live feedback, coins and a summary.

The board held 23 cards, 66 points, all due Nov 5.

Main cards: session endpoints and active-session for the robot (Lama, 5), focus-state endpoint (Lama, 5), live updates over STOMP (Lama, 5, and Gheed, 3), pairing endpoints (Noura, 5) and Connect screen (Gheed, 3), reward on session end (Noura, 5), session summary statistics (Noura, 2), Focus Session screen (Gheed, 5), End Session confirm (Shahd, 2), Session Complete screen (Gheed, 3), coin balance after a session (Shahd, 1), robot faces on the display (Gheed, 3), robot reporting loop (Shahd, 5), plus QA, SCM and PM cards.

## Sprint review

**Sprint goal met:** the full focus loop worked end to end for the first time. The robot detected focus, the app showed the state live, and ending a session awarded coins.

**Done:** 13 cards, 45 points of 66 (68%).

The robot reporting loop (Shahd, 5 points) is counted as done: in this sprint the robot checked for an active session, sent its states and sent a heartbeat. Its last part, the offline buffer that resends states after a network drop, is still open.

Delivered:
- Focus Session screen with timer, status chip, robot face, pause and end, and the Session Complete summary (PR #15).
- Shared coin balance on every screen, refreshed after a session (PR #16).
- On the integration branch `feature/full-e2e`: the robot checks for an active session and sends its states to the backend, the app shows them, and ending a session awards coins. This was the first time the whole loop ran end to end.
- Robot faces for each state on the display.
- Pairing endpoints, session endpoints and their integration tests, and the reward and summary statistics on the backend.

Not delivered (10 cards, 21 points): STOMP live updates (the app polls the backend every few seconds instead), the Connect screen, auth unit tests, the outcome on session end, and the QA and SCM passes.

Note: five of the backend cards marked Done were finished on branches (`feature/full-e2e`, `feature/reward-state-history`) and not merged into `develop`.

**Demo:** the full focus loop (robot, backend and app together), the Focus Session screen and the Session Complete summary.

## Retrospective

**What went well**
- The main feature worked end to end for the first time.
- The frontend kept moving thanks to mock data and a demo branch that never went into `develop`.

**What did not go well**
- The board had more cards than the team could finish in one week.
- Integration happened on a separate branch instead of through reviewed PRs, so `develop` stopped moving after Nov 3.
- The same work existed in two versions (two history endpoints, two pairing configs), and nobody decided which one stays.
- STOMP was planned as a must but was not started this sprint; polling replaced it quietly.

**What we change next sprint**
- Agreed: build on `feature/full-e2e` first and merge into `develop` later, but every change still goes through a PR into that branch.
- When two people build the same thing, decide in the stand-up which version stays.
- Re-plan honestly when a must-have will not make it, and write the decision on the card.
- Plan a sprint total the team can realistically finish in one week.

