# Sprint 2 QA report

Date: 2026-10-22 · Tested by: Lama · Build: develop at commit f00aded


## Automated tests
| Component | Command | Result |
| --- | --- | --- |
| Backend | ./mvnw clean test | 314 passed, 0 failed |

## Acceptance Tests
| Story | Result | Notes or bug link |
|---|---|---|
| US-01 Register | Pass | Registration service unit tests (`UserServicesTest`) and authentication API integration tests (`AuthIntegrationTest`). |
| US-02 Login | Pass | Login service unit tests (`UserServicesTest`) and authentication API integration tests (`AuthIntegrationTest`). |
| US-03 Link robot | Pass | Robot service and Robot Pairing Service. |
| US-04 Start session | Pass | Session service and full-system integration testing. |


## Screenshots or Recordings

| Test Name | Screenshot |
|---|---|
| `UserServicesTest` | [View Screenshot](https://github.com/user-attachments/assets/2eb0ad14-42b1-4e6e-945d-f1e9619b2233) |
| `AuthIntegrationTest` | [View Screenshot](https://github.com/user-attachments/assets/7001b010-b087-440e-99a5-3f9c1df51271) |
| `RobotServiceTest` | [View Screenshot](https://github.com/user-attachments/assets/dfc09700-2e18-4a5b-9df4-a589e9881c88) |
| `RobotPairingServiceTest` | [View Screenshot](https://github.com/user-attachments/assets/391db21a-7208-4330-ab24-d1aa58508ec5) |
| `FocusSessionServiceTest` | [View Screenshot](https://github.com/user-attachments/assets/da373cc2-058d-4a30-958b-cdbbf1f984a4) |
