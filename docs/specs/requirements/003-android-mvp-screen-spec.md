# 003 — Android MVP Screen Spec (ShelfPlayer-Android)

**Status:** Draft  
**Owner:** Theo  
**Created:** 2026-05-07  
**Type:** Requirements Spec

**Basis:** `002-original-shelfplayer-reference.md`

---

## Problem Statement

Der aktuelle MVP-Plan ist zu high-level und reicht nicht aus, um konsistent Story-basiert zu implementieren. Es fehlt eine verbindliche, screen-nahe Spezifikation mit klaren States, Events, Navigation und Akzeptanzkriterien.

## Goal

Eine umsetzbare MVP-Spezifikation für Android, die:
- die ersten verbindlichen Screens definiert,
- pro Screen klare Zustände und Aktionen beschreibt,
- Navigation und Datenverträge festlegt,
- und direkt in Stories/Issues umgesetzt werden kann.

## Users / Personas

1. **Self-hosted Audiobookshelf Nutzer:in**
   - möchte schnell verbinden und sofort hören.
2. **Power-User mit größerer Bibliothek**
   - braucht zuverlässige Suche und progress-sichere Wiedergabe.
3. **Unterwegs-Hörer:in (MVP-light)**
   - braucht stabile Basisfunktionen, später Offline-Ausbau.

---

## MVP Scope (v0.1)

## In Scope
1. Connection Onboarding (Server + Token)
2. Library Overview (Listenansicht)
3. Search (global innerhalb aktiver Library)
4. Item Detail (Audiobook/Episode Basis)
5. Player (Play/Pause, Seek, Skip, Rate, Chapter-Liste Basis)
6. Progress Sync (lesen/schreiben)
7. Basis-Settings (Verbindung, Skip-Intervalle, Theme-Basis)

## Out of Scope (v0.1)
- Multi-Server-Management mit paralleler Nutzung
- OpenID Login Flow
- Convenience Downloads / Offline-Automation
- Widgets / Android Auto / App Shortcuts
- Collections/Playlists Editing
- PDF-Viewer

---

## Informationsarchitektur & Navigation

## Bottom Navigation (MVP)
1. **Library**
2. **Search**
3. **Player**
4. **Settings**

## Root Routing States
Analog zur iOS-Logik, vereinfacht für Android:
1. `AppLoading`
2. `NoConnection`
3. `Ready`
4. `FatalError` (mit Retry)

## Deep Link / Internal Routing (MVP)
- `item/{itemId}` → Item Detail
- `player` → Player Screen

---

## Screen Specs

## S1 — ConnectionOnboardingScreen

**Zweck:** Erste Verbindung zu Audiobookshelf herstellen.

**UI-Elemente**
- Server URL Input
- Access Token Input
- „Verbindung testen“
- „Speichern & weiter“
- Fehlerbanner/Inline-Errors

**States**
- `Idle`
- `Editing`
- `Validating`
- `ValidationError(field|global)`
- `Saving`
- `Saved`

**Events**
- `OnServerUrlChanged`
- `OnTokenChanged`
- `OnValidateClicked`
- `OnSaveClicked`

**Akzeptanzkriterien**
- URL-Validierung blockiert ungültige/malformte Eingaben.
- Remote-HTTP wird abgelehnt, lokale Dev-Ausnahmen sind erlaubt.
- Token wird nicht im Klartext geloggt.
- Erfolgreiches Speichern führt in `Ready`-State.

---

## S2 — LibraryScreen

**Zweck:** Bibliothekseinträge anzeigen und Einstieg in Detail/Playback.

**UI-Elemente**
- Listenansicht mit Cover, Titel, Autor, Fortschritt
- Pull-to-refresh
- Empty-State
- Error-State mit Retry

**States**
- `Loading`
- `Loaded(items)`
- `Empty`
- `Error(message)`
- `Refreshing`

**Events**
- `OnAppear`
- `OnRefresh`
- `OnItemClick(itemId)`
- `OnPlayClick(itemId)`

**Akzeptanzkriterien**
- Bei Erfolg mindestens Titel + Fortschritt sichtbar.
- Klick auf Item öffnet Detail.
- Klick auf Play startet Player und setzt Queue (MVP: Einzelitem reicht).

---

## S3 — SearchScreen

**Zweck:** Inhalte in aktiver Library durchsuchen.

**UI-Elemente**
- Suchfeld
- Result-List
- Loading/Empty/Error Zustände

**States**
- `Idle`
- `Typing(query)`
- `Searching(query)`
- `Results(query, items)`
- `NoResults(query)`
- `Error`

**Events**
- `OnQueryChanged`
- `OnSubmit`
- `OnResultClick(itemId)`

**Akzeptanzkriterien**
- Suchanfrage wird debounced oder explizit via Submit ausgeführt.
- Ergebnisnavigation zu Detail funktioniert.

---

## S4 — ItemDetailScreen

**Zweck:** Kerninfos und Aktionen für ein Item.

**UI-Elemente**
- Header (Cover, Titel, Autor)
- Beschreibung (gekürzt + expand)
- Kapitel-Liste (Basis)
- Aktionen: Play/Pause, „Ab hier abspielen“, Progress Reset (optional)

**States**
- `Loading`
- `Loaded(item, chapters, progress)`
- `Error`

**Events**
- `OnPlay`
- `OnChapterSelected(chapterId)`
- `OnRetry`

**Akzeptanzkriterien**
- Detaildaten werden aus API geladen.
- Playback kann von Item und Kapitel gestartet werden.

---

## S5 — PlayerScreen

**Zweck:** Laufende Wiedergabe kontrollieren.

**UI-Elemente**
- Titel + Kontext
- Play/Pause
- Seekbar + Current/Remaining Time
- Skip Backward/Forward
- Playback Rate Picker
- Kapitel Quick Access (BottomSheet oder Liste)

**States**
- `NoActiveItem`
- `Buffering`
- `Playing`
- `Paused`
- `Error`

**Events**
- `OnPlayPause`
- `OnSeekTo(position)`
- `OnSkipForward`
- `OnSkipBackward`
- `OnRateChanged`

**Akzeptanzkriterien**
- Grundkontrollen reagieren ohne UI-Hänger.
- Fortschritt wird lokal aktualisiert und periodisch synchronisiert.

---

## S6 — SettingsScreen (MVP-Basis)

**Zweck:** essentielle Einstellungen verwalten.

**Bereiche**
1. Verbindung (Server anzeigen, neu authentifizieren)
2. Playback-Basis (Skip-Intervalle, Default-Rate)
3. Appearance-Basis (Theme light/dark/system)

**Akzeptanzkriterien**
- Änderungen werden persistent gespeichert.
- Änderungen wirken spätestens nach App-Neustart, idealerweise sofort.

---

## Daten- & API-Verträge (MVP)

## Domain Models (Minimum)
- `ConnectionConfig(serverUrl, token)`
- `LibraryItem(id, title, author, duration, progress, coverUrl, type)`
- `ItemDetail(id, title, description, chapters, mediaUrl)`
- `Chapter(id, title, startMs, endMs)`
- `PlaybackState(itemId, positionMs, durationMs, rate, isPlaying)`

## Repository Interfaces
- `AuthRepository`
  - `validateConnection(config)`
  - `saveConnection(config)`
- `LibraryRepository`
  - `getLibraryItems()`
  - `search(query)`
  - `getItemDetail(itemId)`
- `PlaybackRepository`
  - `start(itemId, chapterId?)`
  - `observePlaybackState()`
  - `setRate(rate)`
  - `seekTo(positionMs)`
- `ProgressRepository`
  - `syncProgress(itemId, positionMs, durationMs)`

---

## Story-Slice Backlog (direkt issue-fähig)

1. **Story A:** Hardened Connection Save + Verify
2. **Story B:** Library List from API (no demo data)
3. **Story C:** Search with result navigation
4. **Story D:** Item Detail + chapter list
5. **Story E:** Player controls + progress sync
6. **Story F:** MVP Settings (connection/playback/theme)

Jede Story soll enthalten:
- User Story Satz
- Akzeptanzkriterien (Checkboxen)
- Scope In/Out
- Test/Verifikation

---

## Non-Functional Requirements (MVP)

- Keine Secrets in Logs.
- UI bleibt bei Netzwerkfehlern bedienbar (Error + Retry).
- App darf ohne aktive Verbindung nicht crashen.
- Grundlegende Unit-Tests für Validation, Mapping und ViewModel-State-Transitions.
- CI muss Build + Unit Tests auf `main` grün halten.

---

## Definition of Done (v0.1)

MVP ist erfüllt, wenn:
1. Nutzer:in kann Server + Token speichern und validieren.
2. Library wird aus realer Quelle geladen (keine reine Demo-Liste).
3. Search → Detail Navigation funktioniert.
4. Player kann ein Item starten, pausieren, seeken und skippen.
5. Progress wird zum Server synchronisiert (mindestens periodisch + beim Pause/Stop).
6. CI läuft grün für Build + Unit Tests.

---

## Offene Entscheidungen

1. v0.1 nur Single-Server (empfohlen) oder direkt Multi-Server-Lite?
2. Welche minimalen Detailfelder sind für Podcast vs. Audiobook zwingend?
3. Welche Progress-Sync-Strategie (Intervall in Sekunden) ist akzeptiert?
4. Soll Kapitelnavigation im MVP als volle Liste oder nur Next/Prev starten?

---

## Nächster Schritt

Diese Spec in **Story-Issues A–F** aufteilen und pro Story ein kleines Implementation-Window fahren (30-Minuten-Runs), inkl. verpflichtendem Log in `docs/stories/implementation-log.md`.
