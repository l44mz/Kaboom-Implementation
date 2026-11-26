# Sprint 6: QA, release and demo (Nov 15 to 19)

## Sprint planning

**Goal:** make Kaboom! ready to hand in. Test every user story again (regression), add the missing tests, release `develop` to `master`, deploy to production, and prepare the demo. Exit rule from the QA test plan: no open Critical or High bugs.

19 cards, 49 points. Main work:
- Testing: backend unit and integration tests (Lama), Flutter widget and logic tests, the detection accuracy evaluation with 5 users, and the sprint QA reports.
- Release: integrate all the work built since Nov 3 into `develop` through pull requests, release to `master`, and deploy to production.
- App: move state management from Provider to GetX (Gheed), and camera mode, which runs a session with a laptop webcam instead of the robot (Noura).
- Documents: sprint reviews and retrospectives, and the QA reports.

## Sprint review

**Planned:** 19 cards, 49 points. **Done:** 17 cards, 44 points (90%).

Still in progress (2 cards):
- The detection accuracy evaluation with 5 users (test plan, section 7).
- Bug fixing and the production deploy. The bugs are fixed and logged in [docs/qa/bugs.md](../qa/bugs.md). The deploy was finished after the sprint, on Nov 25: the backend, the web app and the MySQL database run on Railway (https://kaboom-implementation-production.up.railway.app).

Delivered during the sprint:
- Camera mode: a focus session can run with a laptop webcam instead of the robot (Noura, Nov 16).
- The whole app moved from Provider to GetX (Gheed, Nov 17).
- Backend integration tests that start the app against a real MySQL and call the API over HTTP, plus more service unit tests (Lama, Nov 18 to 21).
- Sprint reviews, retrospectives and QA reports for Sprints 1 to 5 (Shahd, Lama).

Merged into `develop` (Nov 21):
- #17: sprint reviews and retrospectives.
- #18: QA reports for Sprints 2 to 4.
- #19: the integrated app (robot loop, shop, history, admin, UI polish, explosion, camera mode). This was the first time `develop` held the full app.

Released: PR #20 (`develop` into `master`, "Stage 4 MVP") on Nov 22.

Finished right after the sprint (Nov 22 to 24):
- Robot pairing with a 6-digit code on Sparky's screen, device tokens, and the robot sending focus states over a WebSocket with an HTTP backup (#23, #24).
- Sparky's equipped outfit and the coin balance on Home (#21).
- Smarter focus detection on the robot and an accuracy test tool (#26).
- CI on GitHub Actions: backend, app and robot tests run on every push and pull request (#23, #29).
- Flutter widget and logic tests: 39 tests, all passing in CI.
- The final QA report: 482 automated tests, 0 failed (backend 397, app 39, robot 46). See [docs/qa/reports/sprint-6.md](../qa/reports/sprint-6.md).
- The bug log, with 5 bugs, all fixed (#30).

**Demo:** the full flow on the real hardware. Sparky shows a pairing code, the user enters it in the app, starts a session, and Sparky's face follows the user's focus. Then the session summary with coins, the shop, and the CI test results.

## Retrospective

**What went well**
- The project reached a complete, tested state: 482 automated tests across the backend, the app and the robot, and they run automatically on every pull request.
- The GetX migration and the backend integration tests were finished inside the sprint.
- Every feature from the plan, plus pairing with a code and WebSocket for the robot, works end to end on the real Raspberry Pi.

**What did not go well**
- The integration PRs (#17 to #19) were merged on Nov 21, after the sprint ended, so `develop` was not testable during most of the sprint.
- A pull request was merged into `master` by mistake (#21), and its revert (#22) removed the app from `develop` for a few hours.
- The production deploy and the accuracy evaluation with users were left to the end. The deploy was only finished on Nov 25, after the sprint, and the evaluation is not finished.
- The Trello Bugs list was not used; bugs were only logged after the sprint.

**What we change next time**
- Check that every pull request's base is `develop`, and turn on branch protection for `develop` and `master`.
- Deploy early, in the first sprint, and redeploy every week, instead of deploying at the end.
- Log every bug when it is found, with its severity, and link it to the fix.
- Merge into `develop` at least once a week, so the QA pass at the end of each sprint runs on real code.
