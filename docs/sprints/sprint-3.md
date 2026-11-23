# Sprint 3: Pairing, sessions, first detection (Oct 25 to 29)

## Sprint planning

**Goal:** complete US-03 (pair the robot) and US-04 (start a session), and the first working version of US-05 (detect focus) and US-07 (live session screen). A user can link the robot and start a session, and the robot produces focus states.

Cards that stayed on the Sprint 3 board:

| Card | Owner | Points | Priority |
|---|---|---|---|
| CV: head pose and time based state machine | Shahd | 5 | Must |
| Start session flow into the session screen | Shahd | 2 | Must |
| Print robot body and mount Pi, camera, display | Gheed | 3 | Should |
| Robot pairing: request code, show it, poll status, store tokens | Shahd | 3 | Must |
| Sprint 3 planning, review and retrospective | Shahd | 1 | Must |

The rest of the Sprint 3 scope (pairing endpoints and screen, session endpoints, robot state reporting, the Focus Session screen, and two QA cards) was not finished in this sprint.

## Sprint review

**Planned:** 5 cards, 14 points. **Done:** 5 cards, 14 points (100%). Four were done within the sprint, and the robot pairing card was finished on Nov 4.

Delivered:
- Robot focus detection: face, eyes, head pose and a time based state machine (Focused, Distracted, Away) with tests (PR #12).
- Start Session on Home calls the backend and opens the session screen, with a clear message when a session is already running (PR #13).
- Admin dashboard stats on the backend (PR #14).
- The robot body was printed and the hardware mounted.

Not delivered: robot pairing end to end, the live session updates, and the robot sending its states to the backend.

**Demo:** the computer vision demo (camera with live Focused, Distracted, Away), and starting a session from Home.

## Retrospective

**What went well**
- The hardest technical risk, focus detection, worked early.
- A mock version of the session screen let the frontend move on without waiting for the backend.

**What did not go well**
- The goal depended on pairing, and pairing depended on an endpoint contract that was never agreed, so it blocked three people.
- PR #14 was merged by its author with no reviewer, and three commits were pushed straight to `develop` on Oct 31.
- Feature branches fell far behind `develop` (20 or more commits), which made later merges hard.

**What we change next sprint**
- Write the pairing and session API contract first and share it before coding.
- Nobody merges their own PR; the SCM reviews within 24 hours.
- Bring `develop` into long branches at least twice a week.

