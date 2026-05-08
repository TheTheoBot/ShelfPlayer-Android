# Recovery-Zeitraum: Technische Zielarchitektur & Entscheidungsprotokoll (BMAD)

## 1) Zielbild für den Recovery-Zeitraum (A+B priorisiert)

**Leitziel:** Hauptflow `Library -> Detail -> Player` deterministisch, testbar und CI-verifizierbar stabilisieren, bevor UX/Design-Feinschliff oder Feature-Ausbau erfolgt.

### Architekturprinzipien (Recovery)
1. **Stabilität vor Feature-Tiefe**: Nur Änderungen, die direkt auf A2/A3/B1/B2 einzahlen.
2. **Explizite Zustände statt impliziter Flags**: Für App-Root, Navigation, Playback und Datenladen.
3. **Ports-and-Adapters light**: Domain-/State-Logik in purem Kotlin, I/O (HTTP, MediaPlayer, SharedPreferences) über austauschbare Adapter.
4. **Single Writer je Zustandsdomäne**: Jeder zentrale State hat genau eine schreibende Stelle (Reducer/Controller), UI liest nur.
5. **CI-first als Source of Truth**: Lokale Ausführung optional, Merge-Entscheidung ausschließlich über Pipeline-Gates.
6. **Fehler sind Produktzustände**: Keine stummen Catch-Blöcke, keine Endlos-Loading-Zustände.

---

## 2) Zielarchitektur (Recovery-Scope)

## 2.1 Layer-Schnitt
- **UI (Compose)**
  - Screens: `LibraryScreen`, `ItemDetailScreen`, `ConnectionScreen`, `SettingsScreen`
  - Kennt nur UI-State + UI-Events.
- **Application/State Layer**
  - In `ShelfPlayerApp` schrittweise entkoppeln in:
    - `AppRootStateMachine` (Launch/Onboarding/Ready/Error)
    - `NavigationStateMachine` (In-App-Back-Stack)
    - `PlaybackStateMachine` (Idle/Preparing/Playing/Paused/Buffering/Error)
- **Domain/Use-Case Layer**
  - `StartPlaybackSession`, `ResumePlayback`, `SyncProgress`, `LoadLibrary`, `LoadItemDetail`
  - Pure Kotlin, keine Android-API-Abhängigkeit.
- **Infrastructure Adapter**
  - `AudiobookshelfLibraryRepository`, `PlaybackProgressRepository`, `ConnectionPersistence`
  - `MediaEngine` Adapter (kurzfristig MediaPlayer, später ExoPlayer)

## 2.2 Datenfluss (Playback Happy Path)
1. UI-Event `Play(itemId, chapter?)`
2. Use-Case `StartPlaybackSession` -> `POST /api/items/{id}/play`
3. Mapping: Session-DTO -> Domain `PlaybackSession` (`audioTracks[].contentUrl` verpflichtend)
4. `PlaybackStateMachine` Transition `Idle/Paused -> Preparing`
5. `MediaEngine.prepare(url)` erfolgreich -> `Playing`
6. Progress-Ticks/Events -> `PlaybackProgressRepository` (debounced sync)

---

## 3) Zustandsgrenzen (harte Ownership)

## 3.1 AppRoot
- **Owned by:** `AppRootStateMachine`
- **States:** `LoadingCredentials`, `OnboardingRequired`, `Ready`, `LoadError`, `FatalError`
- **Regel:** Keine Navigation/Playback-Aktion außerhalb `Ready`.

## 3.2 Navigation
- **Owned by:** `NavigationStateMachine`
- **Modell:** expliziter In-App-Stack (`Library`, `Search`, `Detail(itemId)`, `Player`)
- **Regel:** Android-Back delegiert immer zuerst an In-App-Stack; App-Exit nur bei Root-Stack.

## 3.3 Playback
- **Owned by:** `PlaybackStateMachine`
- **Minimalzustände Recovery:** `Idle`, `Preparing`, `Playing`, `Paused`, `Error`
- **Optional (wenn technisch nötig):** `Buffering` separat zu `Preparing`.
- **Regeln:**
  - Timeout auf `Preparing` (z. B. 8-10s) -> `Error(PlaybackTimeout)`
  - Kein direkter Sprung `Idle -> Playing` ohne erfolgreiches `prepare` Event
  - `Error` ist quittierbar (`Retry`, `Dismiss`)

## 3.4 Datenmapping
- **Owned by:** Repository/Mapper, nicht UI.
- **Regel:** Placeholder (`Unbekannter Titel`) nur bei echten Null-/Leerwerten nach Mapping-Fallback-Kette.
- **Fallback-Kette Titel:** `media.metadata.title -> item.title -> episode.title -> "Unbekannter Titel"`.

---

## 4) Decision Records (Recovery)

## DR-001: MediaPlayer kurzfristig beibehalten, MediaEngine abstrahieren
- **Status:** Accepted (Recovery)
- **Kontext:** Hauptproblem ist derzeit Flow-/State-Instabilität, nicht primär Codec-Featuretiefe.
- **Entscheidung:**
  - Kurzfristig `MediaPlayer` weiter nutzen.
  - Sofort Adapter-Grenze `MediaEngine` einziehen (`prepare/play/pause/seek/release + callbacks`).
- **Konsequenzen:**
  - Schnellere Stabilisierung ohne großflächige Migration.
  - ExoPlayer-Wechsel wird nach Recovery zu Adapter-Tausch statt App-Umbau.

## DR-002: Playback Session Contract strikt vor Playback
- **Status:** Accepted
- **Entscheidung:** Nie direkte URL-Annahme aus Altpfaden; immer Session-Request vor Start.
- **Konsequenz:** Beseitigt inkonsistente Startlogik, verbessert Reproduzierbarkeit.

## DR-003: Navigation als explizite State Machine statt ad-hoc Tab/Route-Mix
- **Status:** Accepted
- **Entscheidung:** Eigener In-App-Back-Stack als Truth Source.
- **Konsequenz:** Back-Geste wird deterministisch und testbar.

## DR-004: CI-first Quality Gate wegen lokaler Java-Lücke
- **Status:** Accepted
- **Entscheidung:** Merge nur bei grünem Gate: `lint` (falls verfügbar), `testDebugUnitTest`, `assembleDebug`, optional `assembleRelease/apk`.
- **Konsequenz:** Verlässliche Qualitätsaussage trotz eingeschränkter lokaler Umgebung.

## DR-005: Fehlerklassifikation statt generischer "Fehler"-Texte
- **Status:** Accepted
- **Entscheidung:** Fehler in Klassen mappen: `Network`, `Auth`, `Server`, `Mapping`, `Playback`, `Timeout`, `Unknown`.
- **Konsequenz:** Bessere UI-Meldungen + gezielte Retry-Policies.

---

## 5) Fehlerbehandlung & Recovery-Strategie

## 5.1 Einheitliches Fehlerobjekt
`AppError(type, userMessage, technicalMessage, retryable, cause)`

## 5.2 Retry-Policy
- **Network/Timeout:** max. 2 Retries mit Backoff (z. B. 300ms, 900ms)
- **Auth (401/403):** kein Auto-Retry, stattdessen Re-Auth/Nutzeraktion
- **Mapping/Parsing:** kein Auto-Retry, Logging + Fallback UI
- **Playback prepare fail:** 1 Retry mit frischer Session, danach Error-State

## 5.3 UX-Regeln
- Jede Fehlersituation zeigt *konkrete* Aktion: `Erneut versuchen`, `Zurück`, `Verbindung prüfen`.
- Kein permanenter Spinner > Timeout-Schwelle.

## 5.4 Observability minimal
- Strukturierte Logs mit Korrelation: `itemId`, `sessionId`, `route`, `stateTransition`.
- Optional Crash/Telemetry später; im Recovery zunächst konsistente Log-Tags.

---

## 6) Teststrategie (CI-first)

## 6.1 Testpyramide (Recovery)
1. **Unit-Tests (Pflicht, schnell):**
   - State-Machines (Root/Nav/Playback)
   - Mapper (Library/Detail/PlaybackSession)
   - Retry-/Timeout-Policy
2. **Integrationstests JVM (Pflicht):**
   - Repository mit Fake HTTP Layer
   - PlaybackProgress Sync POST->PUT Fallback
3. **UI-/E2E-Tests (selektiv):**
   - 2-3 kritische Flows später ergänzen, sobald A+B stabil grün.

## 6.2 Contract-Tests Audiobookshelf
- Golden JSON fixtures für:
  - Audiobook Detail
  - Podcast/Episode Detail
  - Play-Session Response (`audioTracks`)
- Testet nur Mapping/Contract, nicht Live-Server.

## 6.3 CI Gates (blocking)
- `./gradlew testDebugUnitTest`
- `./gradlew assembleDebug`
- Optional/empfohlen: `./gradlew lintDebug`
- Artifact Upload bei jedem PR-Run (APK/Test reports)

## 6.4 Branch/PR-Regeln
- Maximal ein aktiver Recovery-Workstream.
- PR muss enthalten: Problem, Decision-Record-Bezug, Tests, Risiko/Rollback.
- Kein Merge bei roten oder fehlenden Pflichtjobs.

---

## 7) Konkreter 10-Tage Recovery-Plan (technisch)

- **Tag 1-2:** A2 Navigation State Machine + Back-Tests
- **Tag 3-4:** A3 Mapping-Härtung + Fixture-Tests
- **Tag 5-7:** B1 Session Contract + Fehlerklassifikation
- **Tag 8-9:** B2 Playback State Machine + Timeout/Retry
- **Tag 10:** Hardening, Refactor auf `MediaEngine` Interface, Doku/DoD-Abnahme

---

## 8) Exit-Kriterien Recovery
- Kein bekannter Endlos-Loading-Pfad.
- Back-Verhalten in Hauptflow vollständig deterministisch.
- Playback Start/Pause/Resume reproduzierbar auf Basis Session-Contract.
- Mapping zeigt echte Metadaten robust (inkl. Podcast-Fall).
- CI-Gates 100% grün für alle Recovery-PRs.

---

## 9) Nächste Architekturarbeit nach Recovery (nicht jetzt)
- ExoPlayer-Migration über `MediaEngine` Adapter.
- Offline-/Cache-Strategie.
- Telemetrie/Crash-Reporting.
- Design-System-Ausbau (Token + Komponentenbibliothek).