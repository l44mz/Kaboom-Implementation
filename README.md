# Kaboom!

**Sparky is a little desk robot that keeps you focused.** Start a focus session in the app, and Sparky watches with its camera. When you look away, close your eyes or leave your desk, its face changes and the app shows it live. Stay focused and you earn coins to buy Sparky hats, screens and effects. Lose focus too often and Sparky goes **KABOOM**.

Holberton School portfolio project by Shahd, Gheed, Lama and Noura.

<!-- Add here once available: production URL, demo video, screenshots. -->

## How it works

```text
 ┌──────────────┐   focus states    ┌──────────────┐   session, coins   ┌──────────────┐
 │    Robot     │ ────────────────► │   Backend    │ ◄────────────────► │     App      │
 │ Raspberry Pi │ ◄──────────────── │ Spring Boot  │                    │   Flutter    │
 │ camera + CV  │  active session   │   + MySQL    │                    │ iOS/Android  │
 └──────────────┘                   └──────────────┘                    └──────────────┘
```

1. The user starts a session in the **app**.
2. The **robot** asks the backend for the active session, then watches the user with MediaPipe (face, eyes, head pose) and decides **Focused**, **Distracted** or **Away**.
3. It sends each state (and a heartbeat) to the **backend**, which keeps the time in each state.
4. The **app** shows the state, the timer and Sparky's face. At the end, the backend awards coins for the focused time and the app shows the summary.

## Features

- Sign up, log in, log out (JWT with refresh tokens)
- Focus sessions: choose a duration, start, pause, resume, end early
- Live focus state from the robot: Focused, Distracted, Away
- Sparky explodes when the user goes away twice (or is distracted four times) in a session
- Camera mode: run a session with a laptop webcam instead of the robot
- Coins for focused time, session summary, and session history
- Shop: buy and equip items for Sparky
- Admin: dashboard stats, add and edit products, show or hide them in the shop
- Session notifications with the timer
- Animations: confetti, coin shine, purchase celebration

## Tech stack

| Part | Technology |
|---|---|
| App | Flutter (Dart), GetX, go_router, Dio, Lottie |
| Backend | Java 21, Spring Boot 4, Spring Security (JWT), Spring Data JPA, springdoc OpenAPI |
| Database | MySQL 8 |
| Robot | Python 3.11, OpenCV, MediaPipe, Raspberry Pi 4 with camera and display |
| Planning | Trello, GitHub pull requests |

## Repository layout

```text
backend/    Spring Boot API
frontend/   Flutter app
robot/      Python program for the Raspberry Pi (computer vision and robot face)
docs/       sprint reviews and retrospectives, QA test plan and reports
Dockerfile  builds the backend for deployment
docker-compose.yml  local MySQL
```

## Run it locally

### 1. Database and backend

Needs Docker and Java 21.

```bash
# start MySQL (set any password)
MYSQL_ROOT_PASSWORD=change-me docker compose up -d

# start the API
cd backend
DB_PASSWORD=change-me ./mvnw spring-boot:run
```

The API runs on `http://localhost:8080/api/v1`. Swagger UI: `http://localhost:8080/swagger-ui/index.html`.

Settings come from environment variables: `DB_URL`, `DB_USERNAME`, `DB_PASSWORD`, `PORT`.

### 2. App

Needs Flutter.

```bash
cd frontend
flutter pub get
flutter run --dart-define=API_BASE_URL=http://localhost:8080/api/v1
```

On a phone, use your computer's IP address instead of `localhost`.

### 3. Robot

See [robot/README.md](robot/README.md) for the full setup on a laptop or a Raspberry Pi. In short:

```bash
cd robot
python3 -m venv .venv && source .venv/bin/activate
pip install -r requirements.txt
python -m kaboom_robot.vision.download_model
cp config.example.yaml config.yaml   # set api.base_url and api.robot_id
python -m kaboom_robot.main
```

## Tests

```bash
cd backend && ./mvnw test          # backend unit and integration tests
cd frontend && flutter test        # app widget and logic tests
cd robot && pytest                 # computer vision state machine
```

## Project management

- **Sprint planning, reviews and retrospectives:** [docs/sprints](docs/sprints)
- **QA test plan and reports:** [docs/qa](docs/qa)
- **Trello:** one board per sprint, linked from [docs/sprints/README.md](docs/sprints/README.md)

We work on feature branches and merge into `develop` through pull requests. `develop` is released to `master`.

## Team

| Name | Role |
|---|---|
| Shahd | Project Manager, Flutter, robot computer vision |
| Noura | Source Control Manager, backend |
| Lama | Quality Assurance, backend |
| Gheed | Flutter, robot hardware |
