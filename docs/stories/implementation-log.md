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
