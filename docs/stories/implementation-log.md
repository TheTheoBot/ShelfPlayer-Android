     1|     1|     1|# Implementation Log
     2|     2|     2|
     3|     3|     3|## 2026-05-07T07:17:00Z
     4|     4|     4|- Story/Issue: #1 — Story: Harden connection onboarding URL validation
     5|     5|     5|- Implemented:
     6|     6|     6|  - Hardened `validateServerUrl` to reject missing-scheme and malformed URL inputs while keeping valid HTTPS URLs and local development HTTP URLs accepted.
     7|     7|     7|  - Added regression coverage for missing scheme and malformed URL parsing.
     8|     8|     8|  - Added GitHub Actions workflow for Android unit tests on push and pull request to `main`.
     9|     9|     9|- Verification:
    10|    10|    10|  - `./gradlew testDebugUnitTest` could not run locally because `java` is not installed in this environment.
    11|    11|    11|  - Added CI workflow to run the same Gradle unit test job on GitHub Actions.
    12|    12|    12|- Commit(s):
    13|    13|    13|  - `f413103` — feat: harden connection URL validation
    14|    14|    14|  - `13edbe2` — ci: add Android unit test workflow
    15|    15|    15|  - `5bc6a1e` — ci: rename Android unit test workflow
    16|    16|    16|  - `30fef45` — ci: fix Android unit test workflow
    17|    17|    17|  - `0e49cd2` — test: fix malformed URL regression case
    18|    18|    18|- Next step:
    19|    19|    19|  - Push to `main`, monitor the GitHub Actions run, and continue expanding connection/auth coverage.
    20|    20|    20|
    21|    21|    21|## 2026-05-07T08:23:55Z
    22|    22|    22|- Story/Issue: #1 — Story: Harden connection onboarding URL validation
    23|    23|    23|- Implemented:
    24|    24|    24|  - Introduced a shared `ConnectionSession` model so the remembered server URL can be read by multiple screens.
    25|    25|    25|  - Hoisted connection state into `ShelfPlayerApp`, surfaced the saved server in the top bar and the Library screen, and prefilled the Connect tab from the remembered server.
    26|    26|    26|  - Tightened URL handling by rejecting embedded credentials and clearing the access token after a successful save.
    27|    27|    27|  - Added regression coverage for the new connection-session status text and the credential-rejection rule.
    28|    28|    28|- Verification:
    29|    29|    29|  - `git diff --check` ✅
    30|    30|    30|  - `./gradlew testDebugUnitTest` could not run locally because `java` is not installed in this environment.
    31|    31|    31|- Commit(s):
    32|    32|    32|  - `b0a2896` — feat: share remembered connection state
    33|    33|    33|- Next step:
    34|    34|    34|  - Push the run to `main`, monitor GitHub Actions, and continue toward real connection/auth persistence.
    35|    35|    35|
    36|    36|    36|## 2026-05-07T08:32:40Z
    37|    37|    37|- Story/Issue: #1 — Story: Harden connection onboarding URL validation
    38|    38|    38|- Implemented:
    39|    39|    39|  - Fixed the Connect screen imports so the Compose state helpers resolve correctly in CI.
    40|    40|    40|- Verification:
    41|    41|    41|  - `git diff --check` ✅
    42|    42|    42|  - GitHub Actions run `25484817650` (Android CI) — success: https://github.com/TheTheoBot/ShelfPlayer-Android/actions/runs/25484817650
    43|    43|    43|  - GitHub Actions run `25484817607` (Android Debug Build) — success: https://github.com/TheTheoBot/ShelfPlayer-Android/actions/runs/25484817607
    44|    44|    44|  - GitHub Actions run `25484817608` (Android Unit Tests) — success: https://github.com/TheTheoBot/ShelfPlayer-Android/actions/runs/25484817608
    45|    45|    45|  - GitHub Actions run `25484817623` (Android APK) — success: https://github.com/TheTheoBot/ShelfPlayer-Android/actions/runs/25484817623
    46|    46|    46|- Commit(s):
    47|    47|    47|  - `88056b0` — fix: restore connection screen compose imports
    48|    48|    48|- Next step:
    49|    49|    49|  - Update the story issue with the successful run and continue toward actual auth persistence.
    50|    50|    50|
    51|    51|    51|## 2026-05-07T09:15:21Z
    52|    52|    52|- Story/Issue: #1 — Story: Harden connection onboarding URL validation
    53|    53|    53|- Implemented:
    54|    54|    54|  - Added encrypted connection credential persistence with AndroidX Security so the server URL and access token survive app restarts without storing them in cleartext.
    55|    55|    55|  - Loaded saved credentials at app startup and used the remembered server URL to initialize the app shell, top bar, and Library connection status.
    56|    56|    56|  - Split onboarding into explicit "Verbindung testen" and "Speichern & weiter" actions with a pure form-validation helper and safe success/status text helpers.
    57|    57|    57|  - Extended JVM coverage for the new validation helper and safe status text behavior.
    58|    58|    58|- Verification:
    59|    59|    59|  - `git diff --check` ✅
    60|    60|    60|  - `./gradlew testDebugUnitTest` could not run locally because `java` is not installed in this environment.
    61|    61|    61|- Next step:
    62|    62|    62|  - Run Android unit tests in CI and continue toward real Audiobookshelf auth/session wiring.
    63|    63|    63|
    64|    64|    64|## 2026-05-07T11:07:55Z
    65|    65|    65|- Story/Issue: #1 — Story: Harden connection onboarding URL validation
    66|    66|    66|- Implemented:
    67|    67|    67|  - Added a remote connection test helper and explicit connection screen/app root state phases so the onboarding flow matches the MVP spec more closely.
    68|    68|    68|  - Moved cleartext allowance into the debug manifest, keeping release manifests stricter while still allowing local development HTTP URLs.
    69|    69|    69|  - Tightened connection verification and persistence handling with cancellation-safe coroutine wrapping, normalized saved URLs, and broader unit coverage for HTTP status handling.
    70|    70|    70|  - Added a small coroutine helper for cancellation-preserving `runCatching` usage and updated the implementation log story slice.
    71|    71|    71|- Verification:
    72|    72|    72|  - `git diff --check` ✅
    73|    73|    73|  - Local Gradle build/test unavailable because `java` is not installed in this environment.
    74|    74|    74|  - Independent spec review: PASS
    75|    75|    75|  - Independent code quality review: APPROVED
    76|    76|    76|- Commit(s):
    77|    77|    77|  - `a626946` — feat: harden connection onboarding and verification
    78|    78|    78|- Next step:
    79|    79|    79|  - Commit, push, watch GitHub Actions, and update the story issue with the commit hash and CI run link.
    80|    80|    80|
    81|    81|
    82|    82|## 2026-05-07T12:12:37Z
    83|    83|- Story/Issue: #1 — Story: Harden connection onboarding URL validation
    84|    84|- Implemented:
    85|    85|  - Added explicit app-root onboarding handling so first launch now shows the connection screen instead of the tab shell.
    86|    86|  - Added retryable app-shell error states for credential-store initialization failures and load failures.
    87|    87|  - Added pure root-state helpers plus JVM coverage for loading, onboarding, load-error, ready, and fatal-error paths.
    88|    88|- Verification:
    89|    89|  - `git diff --check` ✅
    90|    90|  - Independent spec review: PASS
    91|    91|  - Independent code quality review: APPROVED
    92|    92|  - Local Gradle execution is still blocked here because `java` is not installed.
    93|    93|  - GitHub Actions verification: pending after push.
    94|    94|- Commit(s):
    95|    95|  - `c01d73d` — feat: add onboarding root-state handling
    96|    96|- Next step:
    97|    97|  - Push to `main`, watch GitHub Actions, and add the CI run link plus issue update once the workflow finishes.
    98|    98|
    99|
   100|## 2026-05-07T12:22:32Z
   101|- Story/Issue: #1 — Story: Harden connection onboarding URL validation
   102|- Implemented:
   103|  - Added explicit app-root onboarding handling so first launch now shows the connection screen instead of the tab shell.
   104|  - Added retryable app-shell error states for credential-store initialization failures and load failures.
   105|  - Added pure root-state helpers plus JVM coverage for loading, onboarding, load-error, ready, and fatal-error paths.
   106|  - Fixed the encrypted shared-preferences factory argument order so the Android build compiles again.
   107|- Verification:
   108|  - `git diff --check` ✅
   109|  - Independent spec review: PASS
   110|  - Independent code quality review: APPROVED
   111|  - GitHub Actions run `25495294375` (Android CI) — success: https://github.com/TheTheoBot/ShelfPlayer-Android/actions/runs/25495294375
   112|  - GitHub Actions run `25495294331` (Android Debug Build) — success: https://github.com/TheTheoBot/ShelfPlayer-Android/actions/runs/25495294331
   113|  - GitHub Actions run `25495294406` (Android APK) — success: https://github.com/TheTheoBot/ShelfPlayer-Android/actions/runs/25495294406
   114|  - GitHub Actions run `25495294349` (Android Unit Tests) — success: https://github.com/TheTheoBot/ShelfPlayer-Android/actions/runs/25495294349
   115|  - Local Gradle execution is still blocked here because `java` is not installed.
   116|- Commit(s):
   117|  - `c01d73d` — feat: add onboarding root-state handling
   118|  - `b09f20b` — docs: log onboarding root-state run
   119|  - `cd98568` — fix: correct encrypted shared preferences factory order
   120|- Next step:
   121|  - Update the story issue with the commit hashes and CI link, then continue toward the next MVP slice.
   122|
   123|## 2026-05-07T13:10:38Z
   124|- Story/Issue: #2 — Story: Typed library list scaffold
   125|- Implemented:
   126|  - Added typed library domain models and a repository contract for the MVP library slice.
   127|  - Replaced the hard-coded demo strings with a repository-backed Compose library screen.
   128|  - Added an in-memory repository slice plus JVM tests for model fields, defensive copying, empty content, and helper formatting.
   129|- Verification:
   130|  - `git diff --check` ✅
   131|  - Independent spec review: PASS
   132|  - Independent code quality review: APPROVED after addressing defensive-copy and coverage feedback.
   133|  - `./gradlew testDebugUnitTest` could not run locally because `java` is not installed in this environment.
   134|  - `java -version` → `command not found`
   135|- Commit(s):
   136|  - `e2943e2` — feat: scaffold typed library list
   137|- Next step:
   138|  - Push the branch, let GitHub Actions validate the Android build/tests, and then wire the typed library slice toward live Audiobookshelf data.
   139|
   140|## 2026-05-07T14:17:55Z
   141|- Story/Issue: #2 — Story: Typed library list scaffold
   142|- Implemented:
   143|  - Introduced a typed `LibraryFeedState` model with Loading, Loaded, Empty, Error, and Refreshing states plus helper copy functions.
   144|  - Upgraded the in-memory library repository to start in Loading, emit Refreshing during pull-to-refresh, and restore state safely after cancellation.
   145|  - Wired the library screen to explicit appear/refresh/item-click/play callbacks, added pull-to-refresh, retry handling, cover thumbnails, and item-level Details/Play actions.
   146|  - Routed library item/play actions in the app shell to the player tab and selected item so the controls are no longer dead ends.
   147|  - Expanded JVM coverage for loading, refresh transitions, cancellation recovery, helper formatting, and defensive copying.
   148|- Verification:
   149|  - `git diff --check` ✅
   150|  - Independent spec review: PASS
   151|  - Independent code quality review: APPROVED
   152|  - Local Android Gradle execution is still blocked because `java` is not installed in this environment.
   153|- Commit(s):
   154|  - pending
   155|- Next step:
   156|  - Commit and push this library slice, then let GitHub Actions confirm the Android build/tests and update the story issue with the new run link.
   157|
   158|## 2026-05-07T15:34:21Z
   159|- Story/Issue: pending — Story: Item detail + chapter list basis
   160|- Implemented:
   161|  - Added item-detail domain models and repository lookup support for Audiobookshelf and the in-memory slice.
   162|  - Added an item detail screen with header, expandable description, explicit Play/Pause and Ab hier abspielen actions, and chapter cards.
   163|  - Wired library item selection into item detail, and wired detail/chapter playback into the player with a one-shot playback request flow.
   164|  - Added regression tests for detail parsing, progress/formatting helpers, and in-memory detail lookup.
   165|- Verification:
   166|  - `git diff --check` ✅
   167|  - Local Gradle execution unavailable here because `java` and `kotlinc` are not installed.
   168|  - Independent spec review: PASS
   169|  - Independent code quality review: PASS
   170|- Commit(s): pending
   171|- Next step:
   172|  - Commit, push, create/update the story issue, and post the GitHub Actions run link once CI finishes.
   173|
   174|## 2026-05-07T16:59:19Z
   175|- Story/Issue: #3 — Story: Item detail + chapter list basis
   176|- Implemented:
   177|  - Added selectable chapter highlighting in the item-detail list and a human-readable chapter label in the player.
   178|  - Hoisted playback state so item detail can toggle Play/Pause and keep the active playback item stable in the player tab.
   179|  - Preserved chapter context across item-detail playback flows and cleared stale chapter state when opening a different item.
   180|  - Added helper coverage for chapter labels and playback action labels/enabled states.
   181|- Verification:
   182|  - `git diff --check` ✅
   183|  - Independent spec review: PASS
   184|  - Independent code quality review: APPROVED
   185|  - Local Gradle execution is still blocked here because `java` is not installed.
   186|- Commit(s):
   187|  - `db55804` — feat: refine item detail playback flow
   188|- Next step:
   189|  - Push to GitHub, monitor the GitHub Actions run, and update Issue #3 with the commit hash and CI link.
   190|
## 2026-05-07T18:47:12Z
- Story/Issue: #4 — Story: Player progress sync and resume
- Implemented:
  - Added a playback progress repository with local snapshot persistence and Audiobookshelf sync fallback (POST → PUT) for the MVP player slice.
  - Wired the player to resume from the most recent saved progress when no chapter-specific start position is selected, while still honoring explicit chapter starts.
  - Hardened lifecycle handling for progress sync work and added deterministic tests for progress persistence, request encoding, fallback behavior, and refresh state transitions.
- Verification:
  - `git diff --check` ✅
  - `java -version` → `command not found`
  - Local Gradle execution remains blocked in this environment because Java is unavailable.
  - Independent code quality review: APPROVED
- Commit(s):
  - pending
- Next step:
  - Commit and push this player/progress slice, then update the GitHub story issue with the commit hash and CI run link.
