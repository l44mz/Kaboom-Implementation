# Sprint 2: Authentication and Home page (Oct 18 to 22)

## Sprint planning

**Goal:** complete US-01 (sign up) and US-02 (log in, log out), and the Home page (Holberton Meeting 2). Users can sign up, log in and log out across Flutter, API and database.

| Card | Owner | Points | Priority |
|---|---|---|---|
| POST /auth/register with validation and BCrypt | Lama | 3 | Must |
| Login, refresh, logout with own JWT and security filter | Lama | 5 | Must |
| GET, PUT, DELETE /users/me and global error handler | Noura | 3 | Must |
| Sign Up screen wired to API | Gheed | 3 | Must |
| Login screen, auto refresh on 401, logout | Gheed | 3 | Must |
| Home page end to end | Shahd | 3 | Must |
| CV: face presence and eye state with MediaPipe | Shahd | 3 | Should |
| Sprint 2 planning, review and retrospective | Shahd | 1 | Must |
| Sprint 2 code reviews and branch hygiene | Noura | 1 | Must |

Dependencies: both Flutter auth screens needed the register and login endpoints; Home needed GET /users/me.

## Sprint review

**Planned:** 9 cards, 25 points. **Done on Trello:** 9 cards, 25 points (100%). The cards were moved to Done on Oct 25, three days after the sprint ended.

Delivered:
- Register, login, refresh and logout with our own JWT; users can only reach protected routes with a token.
- GET /users/me and a global error format.
- Flutter Sign Up and Login screens connected to the API, with tokens in secure storage.
- Home page with the robot face, duration picker, Start Session button and side menu (PR #9).
- First computer vision step: face presence and eye state with MediaPipe.

PRs merged: #6 (QA test plan), #7 (meeting files), #8 (import fixes), #9 (Home), #10 (login), #11 (backend auth fixes: CORS, JWT filter, /users/me).
The Swagger PR (#5) was closed without merging; the backend arrived through a direct commit ("Add backend from feature/swagger", Oct 21).

**Demo:** signing up, logging in and logging out in the app against the real backend, and the Home page with the user's name and coins.

## Retrospective

**What went well**
- The full authentication flow worked end to end across three layers in one week.
- Most work went through pull requests.

**What did not go well**
- Cards were moved to Done late, so the board did not show real progress during the week.
- One large backend change went in without a PR, so it was never reviewed.
- Frontend and backend disagreed on a few details (CORS, the /users/me path) and needed a fix PR (#11).

**What we change next sprint**
- Agree the request and response shape of each endpoint before the frontend starts using it.
- Every change goes through a PR, including large backend imports.
- Update cards on the day the work finishes.

