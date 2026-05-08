# BMAD QA/Test-Strategie vor Story-Ableitung (verifizierbar)

## Ziel & Scope
Diese Strategie definiert **harte, messbare Qualitätsgates**, die **vor** und **während** der Story-Ableitung gelten.
Fokus ist der Hauptjourney: **Library → Detail → Play → Back**.

---

## 1) Qualitätsprinzipien (BMAD-konform)
1. **Shift-left**: Akzeptanzkriterien werden testbar formuliert, bevor Stories geplant werden.
2. **Gate statt Bauchgefühl**: Jede Phase hat pass/fail Exit-Kriterien.
3. **Pyramide vor E2E**: Möglichst viele Fehler in Unit/Integration finden.
4. **One-in/one-out**: Ein Inkrement gilt nur als "fertig", wenn die verpflichtende Suite grün ist.
5. **Keine Story ohne DoR, kein Merge ohne DoD**.

---

## 2) Testpyramide für die App (Soll-Verteilung)
**Zielverteilung pro Inkrement** (Anzahl und Laufzeit):
- **Unit-Tests: 70%** (schnell, deterministisch, JVM)
- **Integration-Tests: 20%** (Repository/API-Mapping/State-Übergänge)
- **UI-/Journey-Tests: 10%** (Compose/UI, Hauptjourney)

**Qualitative Anforderungen je Ebene:**
- **Unit**: reine Funktionen, Routing-Logik, State-Reducer, Mapper.
- **Integration**: API-Contract (Play-Session), Fehlerpfade, Retry/Timeout-Verhalten.
- **UI/Journey**: Screen-Übergänge, Back-Verhalten, Playback-Start-Feedback.

**Verifizierbar:**
- Jede Story ordnet neue/angepasste Tests mindestens einer Ebene zu.
- PR-Template enthält Pflichtfeld „Test-Ebene(n) + Abdeckung im Journey“.

---

## 3) Minimale verpflichtende Test-Suite pro Inkrement (MUST PASS)
Ein Inkrement darf nur in den nächsten BMAD-Schritt, wenn alle folgenden Checks grün sind:

### 3.1 CI Build & Static Gates
1. `Debug Build` erfolgreich
2. `Unit Tests` erfolgreich
3. `APK Build` erfolgreich
4. `git diff --check` ohne Whitespace-/Merge-Artefakte

### 3.2 Fachliche Pflichttests (Journey-kritisch)
Mindestens:
1. **Routing/Back Unit Test**: Detail → Back führt zur Library (kein ungewollter App-Exit)
2. **Detail-Mapping Unit/Integration Test**: Titel/Autor aus API-Payload korrekt
3. **Playback Session Contract Test**: `/api/items/{id}/play` wird korrekt verarbeitet (`audioTracks[].contentUrl`)
4. **Player-State-Test**: erlaubte Transitionen `Idle→Preparing→Playing→Paused` + Fehlerpfad
5. **UI/Journey Smoke Test**: Library öffnen, Detail öffnen, Play triggern, Back navigiert korrekt

### 3.3 Laufzeit-SLOs (Smoke-Ebene)
- Play-Start-Feedback innerhalb **≤5 Sekunden** (Start oder explizite Fehlermeldung)
- Kein endloser Loading-State ohne Fehlertext > **10 Sekunden**

---

## 4) Regressions-Checklist Hauptjourney (Library → Detail → Play → Back)
Diese Checkliste ist pro Inkrement auszuführen und im Implementation Log zu dokumentieren.

### Library
- [ ] Bibliothek lädt ohne Crash
- [ ] Listeneinträge zeigen sinnvolle Primärinfos
- [ ] Tap auf Eintrag öffnet Detail

### Detail
- [ ] Titel ist nicht „Unbekannter Titel“, wenn API Daten liefert
- [ ] Autor/Metadaten sind konsistent
- [ ] Play-/Fortsetzen-CTA sichtbar und korrekt beschriftet

### Play
- [ ] Play-Action erzeugt entweder Wiedergabe oder klare Fehlermeldung
- [ ] Kein permanenter „Lädt…“-Zustand
- [ ] Pause/Resume reagiert reproduzierbar

### Back
- [ ] Von Player/Detail zurück zur erwarteten In-App-Ansicht
- [ ] Back beendet App nur am Root-Screen
- [ ] Keine Navigations-Sackgassen

---

## 5) Definition of Ready (DoR) für kommende Stories
Eine Story ist **Ready**, wenn alle Punkte erfüllt sind:
1. **Business-Ziel** in 1–2 Sätzen klar
2. **Akzeptanzkriterien Given/When/Then** formuliert
3. **Betroffene Dateien/Module** benannt
4. **Teststrategie pro Story** benannt (Unit/Integration/UI)
5. **Testdaten/Mockbedarf** geklärt
6. **Abhängigkeiten/Risiken** dokumentiert
7. **Nicht-Ziele (Out of Scope)** explizit
8. **Messbare Erfolgskriterien** (z. B. Startzeit, Fehlerverhalten)

Failt ein Punkt, wird die Story **nicht** in Umsetzung gezogen.

---

## 6) Definition of Done (DoD) für kommende Stories
Eine Story ist **Done**, wenn alle Punkte erfüllt sind:
1. Alle Akzeptanzkriterien nachweislich erfüllt
2. Pflicht-Tests implementiert/angepasst und CI grün
3. Keine neuen Critical/High Defects im Hauptjourney
4. Regressions-Checklist vollständig abgehakt
5. Logging/Fehlertexte nutzerverständlich
6. `implementation-log.md` aktualisiert (inkl. Nachweis: CI-Run/Tests)
7. Review-Freigabe (Code + QA) dokumentiert

---

## 7) Exit-Kriterien je Phase A–D

## Phase A — Stabilisierung
**Exit nur wenn:**
- Navigation/Back-Verhalten im Hauptjourney korrekt (inkl. Testnachweis)
- Detaildaten-Mapping robust (kein falscher Placeholder bei vorhandenen Daten)
- CI-Baseline stabil: Build + Unit + APK grün

## Phase B — Player-Flow/API-Korrektheit
**Exit nur wenn:**
- Play-Session Contract korrekt implementiert und getestet
- Player-State-Machine hat keine Deadlock-Transitions
- Play startet reproduzierbar oder liefert konkrete Fehlerursache

## Phase C — UX-Fluss
**Exit nur wenn:**
- Journey Library→Detail→Play→Back ohne Sackgasse auf Emulator/Gerät verifiziert
- CTA-Texte, Rückwege, Kontextanzeige konsistent
- Journey-Smoketest dokumentiert

## Phase D — Design-Angleichung
**Exit nur wenn:**
- Definierte Design-Tokens angewendet (Spacing, Typo, Surface)
- Kernscreens visuell konsistent
- Vorher/Nachher-Nachweis (Screenshots) vorhanden

---

## 8) Gate-Matrix (Story-Freigabe → Merge → Phasenwechsel)
- **Gate G0 (Pre-Story):** DoR vollständig
- **Gate G1 (Pre-Merge):** Pflicht-Suite + Regressions-Checklist grün
- **Gate G2 (Phase Exit):** Phase-spezifische Exit-Kriterien erfüllt

**Entscheidungsregel:**
- Ein rotes Gate blockiert Story-Start, Merge oder Phasenwechsel.

---

## 9) Nachweisformat (verifizierbar, auditierbar)
Pro Inkrement im `implementation-log.md`:
1. Story-ID + Scope
2. Durchgeführte Tests (Unit/Integration/UI)
3. Ergebnis der Regressions-Checklist
4. CI-Run-Links/IDs
5. Offene Risiken + Entscheidung (akzeptiert/verschoben)

Damit ist Qualität vor Story-Ableitung operationalisiert und objektiv prüfbar.