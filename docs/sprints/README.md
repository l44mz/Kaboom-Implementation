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

"Done" means the feature is built and working in the code. For Sprints 1 to 4 that is the Trello Done list. For Sprint 5 it is the work built on the integration branch (`feature/getx`), because the board was not kept up to date that week; that work is waiting for review and merge. Points are the story points written on each card.
Commits are counted on all GitHub branches (merge commits excluded), by commit date.

| Sprint | Cards planned | Cards done | Points planned | Points done | Completion (points) | Commits | PRs merged |
|---|---|---|---|---|---|---|---|
| 1 | 16 | 16 | 34 | 34 | 100% | 15 | 1 (#1) |
| 2 | 9 | 9 | 25 | 25 | 100% | 19 | 6 (#6 to #11) |
| 3 | 5 | 4 | 14 | 11 | 79% | 14 | 3 (#12 to #14) |
| 4 | 23 | 13 | 66 | 45 | 68% (goal met) | 11 | 2 (#15, #16) |
| 5 | 18 | 13 | 42 | 33 | 79% | 9 | 0 (review pending) |
| 6 |  |  |  |  |  |  |  |

Velocity (points done per sprint, Sprints 1 to 5): 34, 25, 11, 45, 33. Sprint 4 had the largest board of the stage, and its goal (the full focus loop) was met. In Sprint 5, finished work stayed on branches without pull requests; the Sprint 5 file explains this.

Bugs: 0 bug cards were logged on any board. Every board has a Bugs list with a template, but it was never used.

