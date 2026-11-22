# Kaboom! Stage 4 QA Test Plan

This plan defines how the team tests Kaboom! during Stage 4 (Oct 11 to Nov 19, 2026): what is tested, with which tool, when, how a card counts as done, and how bugs are reported and fixed.

| Item | Value |
| --- | --- |
| QA lead | Lama |
| Plan owner | Shahd |
| Based on | Stage 3 Technical Documentation, sections 6.2 to 6.5 |
| Applies to | `frontend/` (Flutter), `backend/` (Spring Boot), `robot/` (Raspberry Pi, Python) |

## 1. Test levels

### 1.1 Unit tests

Test one piece of code on its own, with no network, database, or camera.

| Component | Tool | Location | Command | What to cover |
| --- | --- | --- | --- | --- |
| Flutter app | `flutter_test` | `frontend/test/` | `flutter test` | Form validation, API error messages, timer logic, widgets render the right state |
| Spring Boot | JUnit 5, Mockito | `backend/src/test/java/` | `./mvnw test` | Reward calculation, session rules (pause, resume, end), pairing code expiry, authorization checks |
| Robot | `pytest` | `robot/tests/` | `pytest` | State mapping (FOCUSED, DISTRACTED, AWAY), state smoothing buffer, heartbeat timing, sequence numbers, config loading |

Rule: every pull request that adds logic adds or updates at least one unit test for it.

### 1.2 Integration tests

Test that two parts work together for real.

| Integration | How | Owner |
| --- | --- | --- |
| Backend and MySQL | Spring Boot Test against the Docker MySQL from `docker-compose.yml` | Backend (Noura, Lama) |
| API contracts | Swagger UI: call each endpoint with valid, invalid, and unauthorized input; compare with Stage 3 section 5.4 | Backend (Noura, Lama) |
| Flutter and backend | Run the app against the local backend; check each screen's real request and response | Frontend (Shahd, Gheed) |
| Robot and backend | Pi sends focus states to `POST /robots/{robotId}/focus-states`; check the state log and the robot expression | Robot (Shahd, Gheed) |

### 1.3 Acceptance tests

A person follows the steps for each user story and confirms the expected result. A story moves to Done only when its acceptance test passes (see section 3). The full list is in section 5.

### 1.4 Static checks

| Component | Command | Must show |
| --- | --- | --- |
| Flutter | `flutter analyze` | `No issues found!` |
| Robot | `ruff check .` | `All checks passed!` |
| Backend | `./mvnw verify` | `BUILD SUCCESS` |

## 2. Testing per sprint

### 2.1 Rhythm inside each sprint

1. **During the sprint:** the developer runs unit tests and static checks before opening a pull request.
2. **Pull request review:** the reviewer checks that tests exist and pass. GitHub Actions runs them automatically once CI is set up.
3. **After merge into `develop`:** the card moves to **QA**. Lama runs the acceptance test for that story on `develop`.
4. **Last day of the sprint (Thursday):** Lama runs a QA pass on everything merged that week, records results in the sprint QA report (section 6), and reports open bugs in the sprint review.

### 2.2 What is tested in which sprint

| Sprint | Dates | Stories under test | Extra QA focus |
| --- | --- | --- | --- |
| 1 | Oct 11 to 15 | Setup only | Every component builds and runs on every member's laptop; camera and MediaPipe run on the Pi |
| 2 | Oct 18 to 22 | US-01, US-02, Home page | Auth scenarios 1 to 4 and 9 in section 4 |
| 3 | Oct 25 to 29 | US-03, US-04, first version of US-05 and US-07 | Pairing flow; session start rules |
| 4 | Nov 1 to 5 | US-05, US-06, US-07, US-08, US-09, US-11 | Detection accuracy matrix (section 7); reward calculation |
| 5 | Nov 8 to 12 | US-10, US-12, US-13, US-14, SH-01 | Purchases, admin permissions, push notifications |
| 6 | Nov 15 to 19 | Regression: all stories; SH-02 if time allows | Full end-to-end run on the deployed version; no open Critical or High bugs |

## 3. Definition of Done

A card moves to **Done** only when all of these are true:

- [ ] The feature meets its user story and its acceptance test in section 5 passes.
- [ ] Unit tests for the new logic exist and pass.
- [ ] Static checks pass (`flutter analyze`, `ruff check .`, or `./mvnw verify`).
- [ ] The code is on a `feature/` or `fix/` branch, merged into `develop` through a pull request with at least one approval.
- [ ] Important errors and edge cases are handled, or written down as known limitations.
- [ ] Lama (or another member who did not write the code) tested it on `develop`.
- [ ] No open Critical or High bug is linked to the card.
- [ ] The Trello card and any affected README are up to date.

## 4. Critical test scenarios

From Stage 3, section 6.4. These must pass before the final demo.

| # | Scenario | Expected result | Level | Sprint |
| --- | --- | --- | --- | --- |
| 1 | Register with valid data | Account created | Integration, acceptance | 2 |
| 2 | Register with an existing email | `409 Conflict`; app shows "This email is already registered." | Integration, acceptance | 2 |
| 3 | Log in with valid data | Access and refresh tokens returned | Integration | 2 |
| 4 | Call an endpoint with an expired or invalid token | `401 Unauthorized` | Integration | 2 |
| 5 | Start a second session while one is active | `409 Conflict` | Unit, integration | 3 |
| 6 | Pause an already paused session | `409 Conflict` | Unit | 5 |
| 7 | End an already completed session | `409 Conflict` | Unit | 4 |
| 8 | Open another user's session | `404 Not Found` | Integration | 4 |
| 9 | Send invalid fields | `400 Bad Request` with `fieldErrors` | Integration | 2 |
| 10 | Device sends states for a robot that is not its own | `403 Forbidden` | Integration | 3 |
| 11 | Robot resends the same buffered event | Only one state log saved | Unit, integration | 4 |
| 12 | User is focused, distracted, then away | Robot expression changes each time | Hardware, acceptance | 4 |
| 13 | End a 30 minute session at about 88% focus | About 26 coins (rule: `min(30, floor(focusedSeconds / 60))`) | Unit | 4 |
| 14 | Two purchases at the same time with coins for only one | Only one succeeds; balance never goes below 0 | Integration | 5 |
| 15 | Non-admin tries to create a product | `403 Forbidden` | Integration | 5 |
| 16 | Robot loses Wi-Fi during a session, then reconnects | Buffered states are resent; none lost or duplicated | Hardware | 4 |

## 5. Acceptance tests per user story

Format: **Given** (starting point) · **When** (action) · **Then** (expected result).

| Story | Given | When | Then |
| --- | --- | --- | --- |
| **US-01** Register | The app is on the Sign Up page | The user enters a name, a new email, and an 8+ character password, and taps Sign Up | A success message shows and the app goes to Login. Empty fields show an error under each field. An existing email shows "This email is already registered." |
| **US-02** Log in | The user has an account | The user enters the right email and password | The app opens Home. A wrong password shows an error and stays on Login. Closing and reopening the app keeps the user logged in. |
| **US-03** Link robot | The robot shows a 6-digit code; the user is logged in | The user types the code on the Connect screen | The app shows the robot as connected within a few seconds. A wrong or expired code shows an error. |
| **US-04** Start a session | The user is logged in with a linked robot | The user picks a duration and taps Start | The timer starts in the app and the robot starts monitoring. Starting a second session is refused. |
| **US-05** Detect focus state | A session is running and the user sits in front of the robot | The user works, looks away for 5+ seconds, then leaves the desk | The state goes FOCUSED, then DISTRACTED, then AWAY. A quick glance or blink does not change the state. |
| **US-06** Robot expression | A session is running | The state changes | The robot face changes to match (focused, distracted, away). |
| **US-07** Live status in app | A session is running | The state changes on the robot | The app status chip shows the same state within a few seconds. |
| **US-08** Earn coins | A session has 1+ focused minutes | The session ends normally | Coins are added by the server rule (max 30). The new balance shows in the app. A session under 60 seconds earns 0. |
| **US-09** End confirmation | A session is running | The timer finishes, or the user taps End early | The app clearly says completed or ended early. |
| **US-10** Customize robot | The user has enough coins | The user buys an item in the Shop and equips it | Coins go down by the price, the item shows "Equipped", and the robot shows it. Not enough coins: the purchase is refused. |
| **US-11** Session summary | A session just ended | The summary screen opens | It shows time focused, time distracted, focus percentage, and coins earned, matching the session. |
| **US-12** Session history | The user has finished sessions | The user opens History | Past sessions show, newest first, with duration, focus percentage, and coins. |
| **US-13** Admin dashboard | An admin account exists | The admin logs in | The admin dashboard opens. A normal user cannot open it. |
| **US-14** Add product | The admin is logged in | The admin fills in the Add Product form and saves | The product appears in the Shop for users. A normal user calling the API gets `403 Forbidden`. |
| **SH-01** Pause | A session is running | The user taps Pause, waits, then Resume | The timer stops and continues. Paused time does not count toward focus or coins. |
| **SH-02** Expression themes | The user owns a theme | The user selects it | The robot face uses the new theme. |

## 6. Testing evidence

Holberton asks for testing evidence. Keep it in the repo so it is easy to link in the deliverables.

```text
docs/qa/
├── test-plan.md                  this file
└── reports/
    ├── sprint-1.md
    ├── sprint-2.md
    └── ...
```

Each sprint report uses this template:

```markdown
# Sprint N QA report

Date: YYYY-MM-DD · Tested by: Name · Build: develop at commit <hash>

## Automated tests
| Component | Command | Result |
| --- | --- | --- |
| Flutter | flutter test | X passed, Y failed |
| Backend | ./mvnw test | X passed, Y failed |
| Robot | pytest | X passed, Y failed |

## Acceptance tests
| Story | Result | Notes or bug link |
| --- | --- | --- |
| US-01 | Pass / Fail | |

## Bugs found
| Bug | Severity | Status |
| --- | --- | --- |

## Screenshots or recordings
(attach or link)
```

## 7. Detection accuracy evaluation

From Stage 3, section 6.3. Stage 2 goal: at least **80% correct** across **5 users**. Run in Sprint 4 and repeat in Sprint 6.

Each condition is tested at least 3 times per user. For each try, write down the real behavior and the state the robot reported.

| Condition | Cases |
| --- | --- |
| Lighting | Bright indoor, dim, side light |
| Glasses | With and without |
| Head angle | Forward, left, right, down |
| Distance | Near, normal desk distance, far |
| Focus state | Focused, distracted, away |

Accuracy = correct tries / all tries. Record results in that sprint's QA report.

## 8. Bug workflow

### 8.1 Where bugs live

Bugs are Trello cards in a **Bugs** list on the current sprint board. Every bug card title starts with `[BUG]`.

### 8.2 Severity

| Severity | Meaning | Example | Fix by |
| --- | --- | --- | --- |
| **Critical** | Crash, data loss, or the main flow is blocked | Cannot start a focus session | Immediately, before other work |
| **High** | A feature works wrongly, with no workaround | Coins are not added after a completed session | This sprint |
| **Medium** | Wrong, but there is a workaround | Shop filter shows the wrong items | This sprint or next |
| **Low** | Cosmetic | Text cut off on small phones | When time allows |

### 8.3 Bug card template

```markdown
**Severity:** Critical / High / Medium / Low
**Story:** US-XX
**Component:** Flutter / Backend / Robot
**Found in:** develop at commit <hash>, on <device or browser>

**Steps to reproduce**
1.
2.
3.

**Expected result**

**Actual result**

**Screenshot, video, or error log**
```

### 8.4 Bug life cycle

1. **Report:** anyone who finds a bug creates the card in **Bugs** with the template and a severity.
2. **Triage:** Lama confirms the severity and assigns an owner. Critical bugs are announced in the team chat right away.
3. **Fix:** the owner fixes it on a branch named `fix/<short-name>` and opens a pull request into `develop` that mentions the bug card.
4. **Verify:** after merge, the bug card moves to **QA**. The person who reported it (or Lama) repeats the steps.
5. **Close:** if it passes, the card moves to **Done**. If not, it goes back to the owner with a comment.

A sprint cannot be closed with an open **Critical** bug. The final demo cannot ship with an open **Critical** or **High** bug.
