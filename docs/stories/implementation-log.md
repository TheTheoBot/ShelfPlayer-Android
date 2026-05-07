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
- Next step:
  - Push to `main`, monitor the GitHub Actions run, and continue expanding connection/auth coverage.
