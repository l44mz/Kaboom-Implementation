# Sprint 6 QA report (final, Stage 4)

Date: 2026-11-23 · Prepared by: Gheed · Build: `develop` at commit `3586b91`

This is the final QA report for Stage 4. It covers the automated tests of all three parts (backend, app, robot) and the end-to-end pairing test done on Nov 23.

## Automated tests

| Component | Command | Result | Where it runs |
| --- | --- | --- | --- |
| Backend | `./mvnw test` | **397 passed**, 0 failed, 0 skipped | GitHub Actions, every push and PR |
| Flutter | `flutter test` | **39 passed**, 0 failed | GitHub Actions, every push and PR |
| Robot | `pytest` | **46 passed**, 0 failed | GitHub Actions, every push and PR (job added with this report) |
| **Total** | | **482 passed, 0 failed** | |

CI run with the backend and app results: [Tests run #45192477327](https://github.com/l44mz/Kaboom-Implementation/actions/runs/45192477327) (`develop`, commit `3586b91`, both jobs green).
All runs: [Actions → Tests](https://github.com/l44mz/Kaboom-Implementation/actions/workflows/tests.yml).

### Backend: 397 tests

Unit tests use JUnit 5 and Mockito. Integration tests start the whole app against a real MySQL (Testcontainers) and call the API over HTTP.

| Test class | Type | Tests |
| --- | --- | --- |
| `UserServicesTest` | Unit | 71 |
| `FocusSessionServiceTest` | Unit | 50 |
| `JwtServicesTest` | Unit | 33 |
| `CameraSessionServiceTest` | Unit | 26 |
| `PurchaseServiceTest` | Unit | 18 |
| `ProductImageServiceTest` | Unit | 15 |
| `RobotPairingServiceTest` | Unit | 15 |
| `ProductServiceTest` | Unit | 14 |
| `RobotServiceTest` | Unit | 7 |
| `ProductIntegrationTest` | Integration | 50 |
| `FocusSessionIntegrationTest` | Integration | 33 |
| `AuthIntegrationTest` | Integration | 26 |
| `PurchaseIntegrationTest` | Integration | 16 |
| `UserControllerIntegrationTest` | Integration | 14 |
| `SecurityIntegrationTest` | Integration | 6 |
| `RobotStompIntegrationTest` | Integration (WebSocket) | 2 |
| `KaboomApplicationTests` | App starts | 1 |
| **Total** | | **397** |

### Flutter: 39 tests

| Test file | What it checks | Tests |
| --- | --- | --- |
| `focus_session_test.dart` | Session timer and state handling | 8 |
| `rewards_progress_test.dart` | Coins and reward progress | 7 |
| `user_controller_test.dart` | User data and coin balance (GetX) | 6 |
| `session_complete_test.dart` | End-of-session summary | 5 |
| `api_error_test.dart` | API error messages | 3 |
| `models_test.dart` | JSON models | 3 |
| `robot_pairing_test.dart` | Connect Robot screen: right code, wrong code, code validation | 3 |
| `start_session_error_test.dart` | Errors when a session cannot start | 2 |
| `signup_screen_test.dart` | Sign Up form | 1 |
| `widget_test.dart` | App starts | 1 |
| **Total** | | **39** |

### Robot: 46 tests

| Test file | What it checks | Tests |
| --- | --- | --- |
| `test_state_machine.py` | FOCUSED, DISTRACTED, AWAY transitions and smoothing | 13 |
| `test_cv_improvements.py` | Detection rules (head pose, eyes, no face) | 12 |
| `test_metrics.py` | Accuracy metrics | 10 |
| `test_scoring.py` | Focus scoring | 7 |
| `test_calibration_and_config.py` | Calibration baseline and config loading | 4 |
| **Total** | | **46** |

## Critical scenarios (test plan, section 4)

| # | Scenario | Result | Covered by |
| --- | --- | --- | --- |
| 1 | Register with valid data | Pass | `AuthIntegrationTest`, `UserServicesTest` |
| 2 | Register with an existing email (409) | Pass | `AuthIntegrationTest`, `UserServicesTest` |
| 3 | Log in with valid data | Pass | `AuthIntegrationTest` |
| 4 | Expired or invalid token (401) | Pass | `SecurityIntegrationTest`, `JwtServicesTest` |
| 5 | Second active session (409) | Pass | `FocusSessionIntegrationTest`, `FocusSessionServiceTest` |
| 6 | Pause an already paused session (409) | Pass | `FocusSessionServiceTest` (`pauseSession_alreadyPaused_throwsConflict`) |
| 7 | End an already completed session (409) | Pass | `FocusSessionServiceTest` (`endSession_alreadyCompleted_throwsConflict`) |
| 8 | Open another user's session (404) | Pass | `FocusSessionIntegrationTest` |
| 9 | Invalid fields (400) | Pass | `AuthIntegrationTest`, `ProductIntegrationTest`, `UserControllerIntegrationTest` (400 Bad Request) |
| 10 | Device sends states for a robot that is not its own (403) | Partial | Device tokens are tested in `JwtServicesTest`. The robot focus-state endpoints are still open while the robot moves to device tokens. |
| 11 | Robot resends the same event (one log saved) | Pass | `FocusSessionServiceTest` (stale sequence rejected) |
| 12 | Robot expression changes with the state | Partial | State logic: robot `test_state_machine.py`, backend `FocusSessionServiceTest`. Not run on the hardware for this report. |
| 13 | Coins follow `min(30, floor(focusedSeconds / 60))` | Pass | `FocusSessionServiceTest` (`endSession_activeSession_calculatesStatsAndAwardsCoins`, `endSession_rewardIsCappedAtThirtyCoins`) |
| 14 | Two purchases at the same time | Pass | `PurchaseIntegrationTest`, `PurchaseServiceTest` (row lock, unique purchase) |
| 15 | Non-admin creates a product (403) | Pass | `ProductIntegrationTest` |
| 16 | Robot loses Wi-Fi, then reconnects | Not run | Hardware test, not run for this report. |

## Acceptance tests

| Story | Result | Notes |
| --- | --- | --- |
| US-01 Register | Pass | `AuthIntegrationTest`, `UserServicesTest`, `signup_screen_test.dart` |
| US-02 Log in | Pass | `AuthIntegrationTest`, `JwtServicesTest` |
| US-03 Link robot | Pass | `RobotPairingServiceTest` (code creation, expiry, claim), `robot_pairing_test.dart` (app screen). **Manual end-to-end test on Nov 23:** the Raspberry Pi showed a 6-digit code on Sparky's screen, the code was entered in the app (Flutter web in Chrome), the backend linked the robot to the account, and Sparky's face came back. |
| US-04 Start session | Pass | `FocusSessionIntegrationTest`, `start_session_error_test.dart` |
| US-05 Detect focus state | Partial | Robot detection logic (`test_state_machine.py`, `test_cv_improvements.py`, `test_scoring.py`) and backend state handling are tested. The camera on the Pi was not re-tested for this report. |
| US-06 Robot expression | Partial | Backend expression update is tested. Not re-tested on the hardware for this report. |
| US-07 Live status in app | Partial | `RobotStompIntegrationTest` checks the robot WebSocket. Manual test on an Android phone in Sprint 3. |
| US-08 Earn coins | Pass | `FocusSessionIntegrationTest`, `rewards_progress_test.dart` |
| US-09 End confirmation | Pass | `FocusSessionIntegrationTest`, `session_complete_test.dart` |
| US-10 Customize robot | Pass | `PurchaseIntegrationTest`, `PurchaseServiceTest` |
| US-11 Session summary | Pass | `FocusSessionServiceTest`, `session_complete_test.dart` |
| US-12 Session history | Pass | `FocusSessionIntegrationTest`, `FocusSessionServiceTest` (only the owner's finished sessions) |
| US-13 Admin dashboard | Partial | Admin-only API access is tested in `ProductIntegrationTest`. The dashboard screen is not covered by an automated test. |
| US-14 Add product | Pass | `ProductIntegrationTest`, `ProductServiceTest` |
| SH-01 Pause | Pass | `FocusSessionServiceTest` (pause, resume, paused time not counted), `focus_session_test.dart` |

## Bugs found and fixed (Nov 22 to 23)

| Bug | Severity | Fix | Status |
| --- | --- | --- | --- |
| PR #21 was merged into `master` instead of `develop`; its revert (#22) removed the app code from `develop` | Critical | Reverted the revert (commit `c98436f`) | Fixed |
| The old `pairing_codes` table had the wrong columns for the new pairing code | High | Dropped the table so the backend recreates it | Fixed |
| `flutter_local_notifications` 23 needs Android SDK 37 and broke the build | High | Pinned the package to 22.3.3 | Fixed |
| The JUnit branch had a hard-coded database password and crash logs | High | Moved the password to `DB_PASSWORD`, removed the logs (#23) | Fixed |
| `Response` import clash (Dio and GetX) in `robot_pairing_test.dart` | Low | `import 'package:get/get.dart' hide Response;` (commit `b6bae66`) | Fixed |
| The phone cannot reach the backend on the current Wi-Fi network | Medium | Under investigation (network, not app code). The app works in Chrome. | Open |

## Not tested in this report

- Hardware scenarios 12 and 16, and the detection accuracy matrix (test plan, section 7), were not run on the Raspberry Pi for this report.
- Static checks (`flutter analyze`, `ruff check .`) were not part of this report.