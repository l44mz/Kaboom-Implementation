# Sprint 5 QA report

Date: 2026-11-21 · Tested by: Lama · Build: develop at commit 172b085

## Automated tests

| Component | Command | Result |
|---|---|---|
| Backend | `./mvnw clean test` | 397 passed, 0 fails |

## Acceptance tests

| Story | Result | Notes or bug link |
|---|---|---|
| US-10 – Customize robot / Purchase items | Partial | `PurchaseIntegrationTest` verifies purchases, coin deduction, insufficient balance, duplicate purchases, concurrent purchases, equipping, replacing equipped items, and ownership restrictions. Physical robot display of equipped items was not verified. |
| US-14 – Add product (Admin) | Pass | `ProductIntegrationTest` verifies admin product creation and confirms normal users receive HTTP 403 when attempting to create products. |
| Shop – Product listing | Pass | Tests verify that available products are listed and hidden products are not exposed to normal users. |
| Product images | Pass | Tests cover image upload, download, replacement, supported and unsupported formats, file-size limits, and access restrictions. |
| SH-01 – Pause and resume session | Partial | Backend tests verify pausing, resuming, rejecting invalid pause/resume operations, and accumulating paused time. Flutter timer behavior was manually tested. |

## Critical scenarios

| # | Scenario | Result | Evidence |
|---|---|---|---|
| 6 | Pause an already paused session → 409 | Pass | `shouldRejectPausingAPausedSession()` |
| 14 | Two simultaneous purchases with insufficient coins for both → only one succeeds | Pass | `shouldNotOverspendWithConcurrentPurchases()` verifies one HTTP 201, one HTTP 400, and a remaining balance of 40. |
| 15 | Non-admin creates a product → 403 | Pass | `shouldRejectProductCreationByNormalUser()` verifies HTTP 403 and no database change. |

## Additional integration test coverage

- Product creation, updating, deletion, and availability.
- Product image upload, replacement, download, and deletion.
- Purchase history and inventory ownership.
- Concurrent purchases and prevention of negative coin balances.
- Session ownership and unauthorized access prevention.
- Concurrent session creation and ending.
- Session rewards, including the 30-coin limit.
- Robot state reporting, sequence-number validation, and distraction counting.
- Session pause, resume, and ending while paused.

## Bugs found

| Bug | Severity | Status |
|---|---|---|
| No bugs documented | N/A | N/A |

## Screenshots or recordings

| Test Name | Evidence |
|---|---|
| `ProductIntegrationTest` | [View Screenshot](https://github.com/user-attachments/assets/c7e3dc83-6f81-4ff6-87dc-aee10fe8ca0d) |
| `PurchaseIntegrationTest` | [View Screenshot](https://github.com/user-attachments/assets/b0602975-e7b6-48c3-b0a3-b56755ae151a) |
| `FocusSessionIntegrationTest` | [View Screenshot](https://github.com/user-attachments/assets/436960d4-d3ee-4df3-ae16-ea1e345d34b4) |
