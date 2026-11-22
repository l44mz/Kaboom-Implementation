# Sprint 3 QA report

Date: 2026-10-29 · Tested by: Lama · Build: develop at commit 3569e9c

## Automated tests

| Component | Command | Result |
|---|---|---|
| Backend | `./mvnw clean test` | 347 passed, 0 failed |

## Acceptance tests

| Story | Result | Notes or bug link |
|---|---|---|
| US-04 Start session | Pass | Service unit tests (`FocusSessionServiceTest`) and API integration tests (`FocusSessionIntegrationTest`) verify session creation, robot attachment, authentication, and rejection of a second active session. |
| US-05 Detect focus state | Pass | Service tests verify FOCUSED, DISTRACTED, and AWAY state handling. WebSocket integration tests verify that the robot can send focus-state updates to the backend. |
| US-06 Robot expression | Partial | `RobotStompIntegrationTest` verifies that sending a DISTRACTED state through WebSocket updates the robot expression to distressed. Other expressions and physical robot display behavior were not fully verified. |
| US-07 Live status in app | Pass | Manually tested on a real Android device connected to the application. Verified that the live focus status is reflected in the app. |

## Bugs found

| Bug | Severity | Status |
|---|---|---|
| No bugs documented | N/A | N/A |

## Screenshots or recordings

| Test Name | Evidence |
|---|---|
| `FocusSessionServiceTest` | [View Screenshot](https://github.com/user-attachments/assets/da373cc2-058d-4a30-958b-cdbbf1f984a4) |
| `FocusSessionIntegrationTest` | [View Screenshot](https://github.com/user-attachments/assets/436960d4-d3ee-4df3-ae16-ea1e345d34b4) |
| `RobotStompIntegrationTest` | [View Screenshot](https://github.com/user-attachments/assets/883a7228-639c-46cd-9a3c-89476662c8f8) |
