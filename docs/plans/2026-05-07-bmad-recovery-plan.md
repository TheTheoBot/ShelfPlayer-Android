# ShelfPlayer Android Recovery Plan (BMAD-basiert)

> **For Hermes:** Dieses Dokument ist die verbindliche Umsetzungsgrundlage. Erst stabilisieren, dann UX/Design, dann Feature-Ausbau. Keine parallelen Baustellen ohne klare Abnahme pro Phase.

**Goal:** Einen stabilen, testbaren MVP liefern, bei dem Navigation, Detailansicht, Player-Steuerung und Basis-Design konsistent funktionieren.

**Architecture:** Wir fahren einen Recovery-Ansatz in 4 Phasen: (1) Produktbasis stabilisieren, (2) Playback-Flow korrekt nach Audiobookshelf-API, (3) Navigation/Back-Behavior reparieren, (4) UI an Referenz-Design angleichen. Jede Phase hat harte Exit-Kriterien.

**Tech Stack:** Kotlin, Jetpack Compose, MediaPlayer (kurzfristig), Audiobookshelf HTTP API, GitHub Actions (CI als Primär-Verifikation).

---

## 1) Problem Statement (aus aktuellem Ist-Zustand)

### Funktionale Defizite
- Player startet nicht zuverlässig (teilweise nur „Lädt…“).
- Playback-Steuerung ist nicht robust genug für echten Hörbetrieb.
- Detailseite zeigt inkonsistente Daten (z. B. „Unbekannter Titel").
- Android-Back-Geste beendet App statt sinnvoller In-App-Navigation.

### UX-/Design-Defizite
- Oberfläche weicht stark von der Zielvorlage ab.
- Informationshierarchie (Library → Detail → Player) ist nicht klar genug.

### Delivery-Defizite
- Häufige, überlappende Änderungen auf mehreren Pfaden (Player/Search/Settings) erhöhen Regressionsrisiko.
- CI ist zwar verfügbar, aber ohne klare Phasen-Gates wird sie zum „Fehlerdetektor statt Qualitätsgate".

---

## 2) BMAD Delivery Model für dieses Projekt

## Phase A — Stabilisierung (Must-Pass vor jeder neuen UX-Arbeit)
**Outcome:** Build/Test stabil, Navigation konsistent, keine offensichtlichen App-Killer.

### A1: Baseline einfrieren
**Objective:** Einen sauberen technischen Startpunkt erzeugen.

**Files:**
- Modify: `docs/stories/implementation-log.md`

**Steps:**
1. Aktuellen Baseline-Commit notieren.
2. Alle offenen Arbeitsstränge als „in review“ markieren.
3. Scope-Freeze definieren: nur Bugfixes bis Phase A abgeschlossen.

**Verification:**
- CI muss für den Baseline-Zweig grün sein.

---

### A2: Navigation/Back-Behavior reparieren
**Objective:** Back-Geste darf App nicht ungewollt schließen, wenn In-App-Navigation möglich ist.

**Files:**
- Modify: `app/src/main/java/com/thetheobot/shelfplayer/ShelfPlayerApp.kt`
- Modify: `app/src/main/java/com/thetheobot/shelfplayer/AppRoute.kt`
- Test: `app/src/test/java/com/thetheobot/shelfplayer/AppRouteTest.kt`

**Steps:**
1. Failing Test für erwarteten Back-Stack schreiben.
2. App-Route-Logik auf expliziten In-App-Back-Stack umbauen.
3. Hardware-Back + Gesten-Verhalten in Compose sauber abfangen.

**Verification:**
- Detailseite → Back landet in Library/Search, nicht App-Exit.
- Tests grün.

---

### A3: Detaildaten-Integrität sicherstellen
**Objective:** Keine falschen Placeholder wie „Unbekannter Titel“, wenn API-Daten vorhanden sind.

**Files:**
- Modify: `app/src/main/java/com/thetheobot/shelfplayer/AudiobookshelfLibraryRepository.kt`
- Modify: `app/src/main/java/com/thetheobot/shelfplayer/ItemDetailModels.kt`
- Test: `app/src/test/java/com/thetheobot/shelfplayer/AudiobookshelfLibraryRepositoryTest.kt`

**Steps:**
1. Mapping-Tests für echte Detailpayloads (Buch + Podcast) ergänzen.
2. Title/Author/Persistenz-Mapping robust machen.
3. Placeholder nur bei echten Nullfällen anzeigen.

**Verification:**
- Repro-Case „Unbekannter Titel“ verschwindet.

---

## Phase B — Player-Flow korrekt nach Audiobookshelf
**Outcome:** Start/Stop/Resume funktionieren reproduzierbar.

### B1: Playback Session Contract sauber implementieren
**Objective:** Immer erst Play-Session anfordern, dann `audioTracks[].contentUrl` abspielen.

**Files:**
- Modify: `app/src/main/java/com/thetheobot/shelfplayer/ShelfPlayerApp.kt`
- Modify: `app/src/main/java/com/thetheobot/shelfplayer/PlaybackProgressRepository.kt`
- Test: `app/src/test/java/com/thetheobot/shelfplayer/PlaybackProgressRepositoryTest.kt`

**Steps:**
1. Session-Request strikt nach API-Doku (POST `/api/items/{id}/play`).
2. Response robust parsen (`audioTracks`, Episode-Fall optional vorbereiten).
3. Fallback-Strategie dokumentieren (nur bei erwarteten Fehlercodes).

**Verification:**
- Titel startet innerhalb 3–5s oder zeigt konkrete Fehlermeldung mit Ursache.

---

### B2: Player State Machine härten
**Objective:** Keine „Lädt“-Deadlocks, klare Zustände.

**Files:**
- Modify: `app/src/main/java/com/thetheobot/shelfplayer/ShelfPlayerApp.kt`
- Test: `app/src/test/java/com/thetheobot/shelfplayer/LibraryRepositoryTest.kt`

**Steps:**
1. Zustände explizit modellieren: `Idle`, `Preparing`, `Playing`, `Paused`, `Error`.
2. Transition-Guards einbauen (keine ungültigen Übergänge).
3. Timeout/Retry-Policy klar trennen (UI vs Netzwerkfehler).

**Verification:**
- Kein Endlos-Preparing mehr.

---

## Phase C — UX-Fluss Library → Detail → Player
**Outcome:** Durchgängige Bedienung ohne Sackgassen.

### C1: User Journey glätten
**Objective:** Vom Listeneintrag bis Wiedergabe mit 2–3 klaren Schritten.

**Files:**
- Modify: `app/src/main/java/com/thetheobot/shelfplayer/LibraryScreen.kt`
- Modify: `app/src/main/java/com/thetheobot/shelfplayer/ItemDetailScreen.kt`
- Modify: `app/src/main/java/com/thetheobot/shelfplayer/ShelfPlayerApp.kt`

**Steps:**
1. CTA-Texte vereinheitlichen (Play, Fortsetzen, Kapitel starten).
2. Detailseite zeigt valide Metadaten + Kapitelknoten.
3. Rückwege und Kontextanzeige konsistent machen.

**Verification:**
- Journey-Test auf Gerät/Emulator + dokumentierter Ablauf in `implementation-log`.

---

## Phase D — Design-Angleichung an Vorlage
**Outcome:** Sichtbar näher an Zielbild (Yubal-inspiriert) statt Roh-MVP.

### D1: Design Token & Layout-Hierarchie
**Objective:** Grundgerüst für konsistente Optik schaffen.

**Files:**
- Modify: `app/src/main/java/com/thetheobot/shelfplayer/SettingsScreen.kt`
- Modify: `app/src/main/java/com/thetheobot/shelfplayer/LibraryScreen.kt`
- Modify: `app/src/main/res/values/themes.xml`

**Steps:**
1. Spacing, Radius, Typografie, Surface-Kontrast als Token definieren.
2. Cards/Topbar/BottomNav visuell vereinheitlichen.
3. Detail-/Player-Hero-Bereich visuell priorisieren.

**Verification:**
- Vorher/Nachher-Screenshots dokumentieren.

---

## 3) Abnahme-Kriterien (Definition of Done)

### DoD Technisch
- CI: Unit Tests + Debug Build + APK Build grün.
- Keine offenen kritischen Runtime-Fehler im Hauptflow.

### DoD Funktional
- Back-Geste funktioniert in allen Hauptscreens.
- Detailseite zeigt korrekte Titel/Autor-Daten.
- Player: Start, Pause, Resume, Stop reproduzierbar.

### DoD UX
- Kein Endlos-Ladezustand ohne Fehlerhinweis.
- Navigation ist für Nutzer nachvollziehbar.

---

## 4) Priorisierung (jetzt sofort)

1. **A2 Navigation/Back fixen** (akut, UX-kritisch)  
2. **B1/B2 Player-Flow robust machen** (MVP-Kernfunktion)  
3. **A3 Detaildaten sauber mappen** (Vertrauen in Daten)  
4. **D1 Design-Angleichung** (wichtig, aber nach Funktionsstabilität)

---

## 5) Arbeitsmodus ab jetzt

- Keine neuen Features, bis Phase A+B abgeschlossen und abgenommen sind.
- Maximal ein aktiver Implementierungsstrang gleichzeitig.
- Nach jedem abgeschlossenen Task:
  - kurzer Logeintrag in `docs/stories/implementation-log.md`
  - CI-Check
  - erst dann nächster Task

---

## 6) Nächster konkreter Schritt

**Task A2 starten:** Back-Stack & Android-Back-Geste reparieren (inkl. Tests), danach sofort CI-Lauf und Geräteprüfung.