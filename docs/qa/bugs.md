# Kaboom! bug log

Bugs found during Stage 4, with how they were fixed. Severity follows the QA test plan, section 8.2.

| # | Bug | Severity | Found | Status | Fix |
| --- | --- | --- | --- | --- | --- |
| 1 | Revert of PR #21 removed the app code from `develop` | Critical | Nov 22 | Fixed | Commit `c98436f` |
| 2 | Old `pairing_codes` table breaks the new robot pairing | High | Nov 22 | Fixed | Database table recreated |
| 3 | Android build fails with `flutter_local_notifications` 23 | High | Nov 2026 | Fixed | Package pinned to 22.3.3 |
| 4 | JUnit branch had a hard-coded database password and crash logs | High | Nov 22 | Fixed | PR #23 |
| 5 | `robot_pairing_test.dart` does not compile (`Response` in Dio and GetX) | Low | Nov 22 | Fixed | Commit `5bffab6` (PR #24) |


**Total:** 5 bugs, 5 fixed.

## Details

### 1. Revert of PR #21 removed the app code from `develop` (Critical, fixed)

- **What happened:** PR #21 (`feature/home-outfit`) was merged into `master` by mistake instead of `develop`. The revert, PR #22, was merged into `develop`. Because #21 had brought develop's history into master, the revert also removed a large part of the app from `develop`.
- **Expected:** `develop` keeps all the app code.
- **Fix:** reverted the revert (commit `c98436f`, "Reapply 'Feature/home outfit'"). `develop` and `master` match again.
- **Prevention:** check that a PR's **base** is `develop`, because the GitHub default branch is `master`.

### 2. Old `pairing_codes` table breaks the new robot pairing (High, fixed)

- **What happened:** PR #23 changed the `PairingCode` entity. Databases created before it still had the old `pairing_codes` table. Hibernate (`ddl-auto=update`) does not change or remove old columns, so creating a pairing code failed.
- **Steps:** use a database from before #23, start the backend and the robot. The robot asks for a code and gets a database error.
- **Fix:** run `DROP TABLE IF EXISTS pairing_codes;` once and restart the backend, which creates the table again. Pairing codes are short-lived, so no data is lost.

### 3. Android build fails with `flutter_local_notifications` 23 (High, fixed)

- **What happened:** version 23.0.1 needs Android compileSdk 37 and AGP 9.1.1. The project uses AGP 9.1.0, so `flutter build apk` failed.
- **Fix:** `flutter_local_notifications: 22.3.3` in `frontend/pubspec.yaml`. The APK builds, and the session notification with the timer works.

### 4. JUnit branch had a hard-coded database password and crash logs (High, fixed)

- **What happened:** `feature/JUnit-Testing` had a real password in `application.properties` and three JVM crash logs (`hs_err_pid*.log`) in `backend/`.
- **Fix (PR #23):** the backend reads `spring.datasource.password=${DB_PASSWORD:}` from the environment, and the crash logs were removed. Only the tests and code were brought into `develop`.
- **Note:** the password is still in the history of the `feature/JUnit-Testing` branch. It should be changed wherever it is used, and the branch can be deleted.

### 5. `robot_pairing_test.dart` does not compile (Low, fixed)

- **What happened:** the test imports both Dio and GetX, and both define `Response`, which gave the errors `'Response' isn't a function` and "defined in both libraries".
- **Fix:** `import 'package:get/get.dart' hide Response;`. All 3 tests pass in CI.

