# Sprint 1: Setup and feasibility (Oct 11 to 15)

## Sprint planning

**Goal:** every project created, running for every member, and deployed (Holberton Meeting 1). Each component runs locally and on staging, the board and roles are in place, and hardware and computer vision feasibility is confirmed.

**Roles agreed:** Shahd PM, Noura SCM, Lama QA.

| Card | Owner | Points | Priority |
|---|---|---|---|
| Spring Boot project, base structure and Swagger | Lama | 3 | Must |
| Docker Compose MySQL and .env.example | Noura | 2 | Must |
| Flyway migrations for the full Stage 3 schema | Noura | 3 | Must |
| Deploy backend and MySQL to staging | Noura | 3 | Must |
| GitHub Actions CI | Lama | 3 | Should |
| Flutter project with theme, fonts and routing | Shahd | 3 | Must |
| Flutter API client, config and secure token storage | Shahd | 3 | Must |
| Sign Up page MVP for the Sprint 1 meeting | Gheed | 2 | Should |
| Robot Python project | Shahd | 2 | Must |
| Confirm hardware and start 3D model | Gheed | 2 | Must |
| GitHub develop branch, protection rules, PR template, Trello board | Shahd | 2 | Must |
| Assign Stage 4 roles and create docs/stage4 | Shahd | 1 | Must |
| Stage 4 QA test plan and bug workflow | Shahd | 2 | Must |
| Sprint 1 planning, review and retrospective | Shahd | 1 | Must |
| Sprint 1 QA pass | Lama | 1 | Should |
| Sprint 1 code reviews and branch hygiene | Noura | 1 | Must |

Dependencies: the Flutter API client needed the backend base URL; staging deploy needed the Spring Boot project and MySQL.

## Sprint review

**Planned:** 16 cards, 34 points. **Done on Trello:** 16 cards, 34 points (100%).

Delivered:
- Monorepo with `backend/`, `frontend/`, `robot/`, `docs/` (PR #1).
- Spring Boot backend with Swagger, MySQL through Docker Compose.
- Flutter app with the Figma theme, fonts (Readex Pro, JetBrains Mono), routes and a first Sign Up page.
- Robot Python project set up; hardware confirmed (Raspberry Pi 4, camera, display).
- Stage 4 QA test plan, definition of done and bug card template (`docs/qa/test-plan.md`, merged in PR #6).

**Gaps found while writing this review:**
- The CI card is in Done, but `.github/workflows/` only holds a `.gitkeep`; no workflow file exists on any branch.
- The Flyway card is in Done, but the backend uses `spring.jpa.hibernate.ddl-auto=update` and has no migration files.
- The staging deploy card was moved to Done on Oct 25, ten days after the sprint, and the staging URL is not written anywhere in the repo.

**Demo:** the first Sign Up page in the Flutter app, the backend running with Swagger, and the robot hardware.

## Retrospective

**What went well**
- All six sprint boards were planned on day one with owners, points and due dates, so everyone knew the whole plan.
- Roles were clear from the start.
- The test plan and bug template were ready before any feature work.

**What did not go well**
- Some cards were marked Done without the deliverable existing in the repo (CI, Flyway).
- Setup work like the staging deploy ran past the sprint end.

**What we change next sprint**
- A card moves to Done only when its code is merged into `develop` (or its document is in the repo).
- Write deployment URLs and environment notes in the README.

