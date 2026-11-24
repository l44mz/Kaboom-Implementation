# Kaboom: Stage 4 sprint reviews and retrospectives

Team: Shahd (PM, Flutter, robot CV), Noura (SCM, backend), Lama (QA, backend), Gheed (Flutter, robot hardware).
Sprints were one week each. Planning lives on Trello, one board per sprint:

| Sprint | Dates | Goal | Trello |
|---|---|---|---|
| 1 | Oct 11 to 15 | Setup and feasibility | https://trello.com/b/7qCU5S6t |
| 2 | Oct 18 to 22 | Authentication and Home page | https://trello.com/b/WEjcB4Ge |
| 3 | Oct 25 to 29 | Pairing, sessions, first detection | https://trello.com/b/NNVcvLAn |
| 4 | Nov 1 to 5 | Complete the main focus loop | https://trello.com/b/L4AXm67w |
| 5 | Nov 8 to 12 | Store, history, admin, push | https://trello.com/b/9jvaSrXs |
| 6 | Nov 15 to 19 | QA, release and demo | https://trello.com/b/0AW2QE88 |

Each file below holds the sprint review (what we planned, what we delivered, the demo) and the retrospective (what went well, what did not, what we change).

- [Sprint 1](sprint-1.md)
- [Sprint 2](sprint-2.md)
- [Sprint 3](sprint-3.md)
- [Sprint 4](sprint-4.md)
- [Sprint 5](sprint-5.md)
- [Sprint 6](sprint-6.md)

## Metrics across the stage

"Done" is the Trello Done list. Points are the story points written on each card.
Commits are counted on all GitHub branches (merge commits excluded), by commit date.

| Sprint | Cards planned | Cards done | Points planned | Points done | Completion (points) | Commits | PRs merged |
|---|---|---|---|---|---|---|---|
| 1 | 16 | 16 | 34 | 34 | 100% | 15 | 4 (#1 to #4) |
| 2 | 9 | 9 | 25 | 25 | 100% | 19 | 6 (#6 to #11) |
| 3 | 5 | 5 | 14 | 14 | 100% | 14 | 3 (#12 to #14) |
| 4 | 12 | 12 | 40 | 40 | 100% | 11 | 2 (#15, #16) |
| 5 | 18 | 18 | 42 | 42 | 100% | 9 | 0 |
| 6 | 19 | 17 | 49 | 44 | 90% | 18 | 3 (#17 to #19) |
| **Total** | **79** | **77** | **204** | **199** | **98%** | **86** | **18** |

Velocity (points done per sprint): 34, 25, 14, 40, 42, 44. Sprints 1 to 5 delivered everything they planned; Sprint 4 reached its goal, the full focus loop. Sprint 6 was the QA and release sprint: 17 cards are done and 2 are in progress (the detection accuracy evaluation with 5 users, bug fixing and the production deploy).

After Sprint 6 (Nov 22 and 23), more PRs were merged into `develop`:
- #21: Sparky's outfit on Home.
- #23 and #24: robot pairing with the code on Sparky's screen, device tokens, STOMP for the robot, the backend test suite, and CI that runs the backend and app tests on every push and pull request.
- #25: these sprint documents updated from Trello.
- #26: smarter focus detection on the robot and an accuracy test tool.
- #29: final QA report and the robot tests in CI. All three test suites now run on every push and pull request: backend 397, app 39 (the Flutter widget and logic tests), robot 46. See [the final QA report](../qa/reports/sprint-6.md).

Bugs: the Trello Bugs lists were not used during the sprints. The bugs found are logged in [docs/qa/bugs.md](../qa/bugs.md): 6 bugs, all fixed.
