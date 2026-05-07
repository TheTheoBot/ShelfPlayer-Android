# Implementation Log

## 2026-05-07T07:17:00Z
- Story/Issue: #1 — Story: Harden connection onboarding URL validation
- Implemented:
  - Hardened `validateServerUrl` to reject missing-scheme and malformed URL inputs while keeping valid HTTPS URLs and local development HTTP URLs accepted.
  - Added regression coverage for missing scheme and malformed URL parsing.
  - Added GitHub Actions workflow for Android unit tests on push and pull request to `main`.
- Verification:
  - `./gradlew testDebugUnitTest` could not run locally because `java` is not installed in this environment.
  - Added CI workflow to run the same Gradle unit test job on GitHub Actions.
- Commit(s):
  - `f413103` — feat: harden connection URL validation
  - `13edbe2` — ci: add Android unit test workflow
  - `5bc6a1e` — ci: rename Android unit test workflow
  - `30fef45` — ci: fix Android unit test workflow
  - `0e49cd2` — test: fix malformed URL regression case
- Next step:
  - Push to `main`, monitor the GitHub Actions run, and continue expanding connection/auth coverage.

## 2026-05-07T08:23:55Z
- Story/Issue: #1 — Story: Harden connection onboarding URL validation
- Implemented:
  - Introduced a shared `ConnectionSession` model so the remembered server URL can be read by multiple screens.
  - Hoisted connection state into `ShelfPlayerApp`, surfaced the saved server in the top bar and the Library screen, and prefilled the Connect tab from the remembered server.
  - Tightened URL handling by rejecting embedded credentials and clearing the access token after a successful save.
  - Added regression coverage for the new connection-session status text and the credential-rejection rule.
- Verification:
  - `git diff --check` ✅
  - `./gradlew testDebugUnitTest` could not run locally because `java` is not installed in this environment.
- Commit(s):
  - `b0a2896` — feat: share remembered connection state
- Next step:
  - Push the run to `main`, monitor GitHub Actions, and continue toward real connection/auth persistence.

## 2026-05-07T08:32:40Z
- Story/Issue: #1 — Story: Harden connection onboarding URL validation
- Implemented:
  - Fixed the Connect screen imports so the Compose state helpers resolve correctly in CI.
- Verification:
  - `git diff --check` ✅
  - GitHub Actions run `25484817650` (Android CI) — success: https://github.com/TheTheoBot/ShelfPlayer-Android/actions/runs/25484817650
  - GitHub Actions run `25484817607` (Android Debug Build) — success: https://github.com/TheTheoBot/ShelfPlayer-Android/actions/runs/25484817607
  - GitHub Actions run `25484817608` (Android Unit Tests) — success: https://github.com/TheTheoBot/ShelfPlayer-Android/actions/runs/25484817608
  - GitHub Actions run `25484817623` (Android APK) — success: https://github.com/TheTheoBot/ShelfPlayer-Android/actions/runs/25484817623
- Commit(s):
  - `88056b0` — fix: restore connection screen compose imports
- Next step:
  - Update the story issue with the successful run and continue toward actual auth persistence.

## 2026-05-07T09:15:21Z
- Story/Issue: #1 — Story: Harden connection onboarding URL validation
- Implemented:
  - Added encrypted connection credential persistence with AndroidX Security so the server URL and access token survive app restarts without storing them in cleartext.
  - Loaded saved credentials at app startup and used the remembered server URL to initialize the app shell, top bar, and Library connection status.
  - Split onboarding into explicit "Verbindung testen" and "Speichern & weiter" actions with a pure form-validation helper and safe success/status text helpers.
  - Extended JVM coverage for the new validation helper and safe status text behavior.
- Verification:
  - `git diff --check` ✅
  - `./gradlew testDebugUnitTest` could not run locally because `java` is not installed in this environment.
- Next step:
  - Run Android unit tests in CI and continue toward real Audiobookshelf auth/session wiring.

## 2026-05-07T11:07:55Z
- Story/Issue: #1 — Story: Harden connection onboarding URL validation
- Implemented:
  - Added a remote connection test helper and explicit connection screen/app root state phases so the onboarding flow matches the MVP spec more closely.
  - Moved cleartext allowance into the debug manifest, keeping release manifests stricter while still allowing local development HTTP URLs.
  - Tightened connection verification and persistence handling with cancellation-safe coroutine wrapping, normalized saved URLs, and broader unit coverage for HTTP status handling.
  - Added a small coroutine helper for cancellation-preserving `runCatching` usage and updated the implementation log story slice.
- Verification:
  - `git diff --check` ✅
  - Local Gradle build/test unavailable because `java` is not installed in this environment.
  - Independent spec review: PASS
  - Independent code quality review: APPROVED
- Commit(s):
  - `a626946` — feat: harden connection onboarding and verification
- Next step:
  - Commit, push, watch GitHub Actions, and update the story issue with the commit hash and CI run link.
