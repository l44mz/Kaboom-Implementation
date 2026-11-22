# Sprint 4 QA report

Date: 2026-11-5 · Tested by: Lama · Build: develop at commit 3569e9c

## Automated tests

| Component | Command | Result |
|---|---|---|
| Backend | `./mvnw clean test` | 347 passed, 0 failed |

## Acceptance tests

| Story | Result | Notes |
|---|---|---|
| US-04 – Start focus session | Pass | Verified session creation through HTTP integration tests, including authentication, robot ownership, rejection of a second active session, and starting a new session after ending the previous one. |
| US-05 – Detect focus state | Partial | Tests verify FOCUSED, DISTRACTED, and AWAY state handling, distraction counts, stale sequence rejection, and ignored reports while paused. Physical detection is not verified by these tests. |
| US-06 – Robot expression | Partial | Service tests verify robot expression updates for focus states, but physical robot expression behavior is not verified. |
| US-08 – Earn coins | Pass | Integration tests verify reward calculation, coin balance updates, the 30-coin cap, zero rewards when no focused time is recorded, and prevention of duplicate rewards. |
| US-09 – End confirmation | Partial | Session completion is verified through HTTP integration tests. The app's end-of-session confirmation message is not verified. |
| US-11 – Session summary | Partial | Backend session statistics and reward calculations are tested, but the summary screen display is not verified. |
| US-12 – Session history | Partial | Integration tests verify that finished sessions appear only in their owner's history. App display and ordering are not verified. |

## Bugs found

| Bug | Severity | Status |
|---|---|---|
| No bugs documented | N/A | N/A |

## Screenshots or recordings

| Test Name | Evidence |
|---|---|
| `FocusSessionServiceTest` | [View Screenshot](https://github.com/user-attachments/assets/da373cc2-058d-4a30-958b-cdbbf1f984a4) |
| `FocusSessionIntegrationTest` | [View Screenshot](https://github.com/user-attachments/assets/436960d4-d3ee-4df3-ae16-ea1e345d34b4) |
