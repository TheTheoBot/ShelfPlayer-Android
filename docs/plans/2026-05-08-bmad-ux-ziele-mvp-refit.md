# BMAD UX-Zielbild: Pragmatic MVP Refit

## Kontext
Aktuelle Probleme:
- UI wirkt nicht wie die Zielvorlage.
- Informationshierarchie ist unklar.
- Navigation erzeugt Frustration (Orientierungsverlust, inkonsistentes Back-Verhalten).

Ziel dieses Dokuments: Ein **abnahmefähiges UX-Framework** für den MVP-Refit definieren, bevor visuelles Fine-Tuning startet.

---

## 1) UX-Ziele (MVP)

### Primärziele
1. **Orientierung in < 5 Sekunden**
   - Nutzer:innen erkennen sofort: *Wo bin ich? Was kann ich hier als Nächstes tun?*
2. **Happy Path ohne Sackgassen**
   - Library → Detail → Play/Resume → zurück zur Library funktioniert konsistent.
3. **Kognitive Last reduzieren**
   - Pro Screen ein klarer Hauptzweck, max. eine primäre Aktion.
4. **Vertrauen in Inhalte erhöhen**
   - Titel, Autor:in, Fortschritt und Status sind sichtbar und konsistent.

### Nicht-Ziele für diesen Refit
- Kein umfassendes Brand-Redesign.
- Keine Animation-Politur über funktional notwendiges Feedback hinaus.
- Keine Erweiterung des Feature-Scope (nur UX-Qualität im Kernflow).

---

## 2) Zentrale User Journey (North-Star-Flow)

## Journey: „Ich will in < 15s weiterhören“
1. **Entry: LibraryScreen**
   - Nutzer:in sieht zuletzt relevante Inhalte (Fortsetzen / zuletzt geöffnet / Bibliothek).
   - Primäre Aktion sichtbar: *Fortsetzen* oder *Öffnen*.
2. **Decision: ItemDetailScreen**
   - Kerninfos oben: Cover, Titel, Autor:in, Fortschritt.
   - Primäre CTA: *Abspielen* / *Fortsetzen*.
3. **Execution: PlayerScreen / MiniPlayer**
   - Sofort erkennbarer Wiedergabezustand (Preparing, Playing, Paused, Error).
   - Basissteuerung ohne Umwege: Play/Pause, Seek, Zurück.
4. **Return: Zurück-Navigation**
   - Back führt deterministisch zum vorherigen Kontext (Detail → Library), nicht zum App-Exit.

**UX-Prinzip:** Jeder Schritt muss den nächsten Schritt „ziehen“ (progressive disclosure, kein Feature-Labyrinth).

---

## 3) Priorisierung der Screens für MVP-Refit

### P0 (Blocker für Abnahme)
1. **LibraryScreen**
   - Informationshierarchie + scannbare Listenstruktur
   - klare primäre Aktion pro Card
2. **ItemDetailScreen**
   - Hero-Bereich (Titel, Autor:in, Fortschritt, CTA)
   - eindeutige CTA-Beschriftung
3. **Navigation/Back-Behavior (app-weit)**
   - konsistenter Back-Stack

### P1 (stark empfohlen vor Launch)
4. **PlayerScreen / MiniPlayer**
   - Zustandstransparenz (Lädt, Spielt, Pausiert, Fehler)
   - „Jetzt hörbar“-Feedback innerhalb kurzer Zeit
5. **Connection/Onboarding Screen**
   - verständliche Eingabeführung + Fehlermeldungen

### P2 (nach MVP-Abnahme)
6. **SettingsScreen visuell harmonisieren**
7. **Search/Filter UX verfeinern**

---

## 4) Design-Leitplanken (minimales Token-Set)

MVP-Regel: **so wenig Tokens wie möglich, so viele wie nötig**.

### 4.1 Spacing
- `space-1 = 4dp`
- `space-2 = 8dp`
- `space-3 = 12dp`
- `space-4 = 16dp` *(Default-Layoutabstand)*
- `space-6 = 24dp` *(Section-Abstand)*

### 4.2 Radius
- `radius-sm = 8dp`
- `radius-md = 12dp` *(Standard für Cards/Inputs)*
- `radius-lg = 16dp` *(Hero-Flächen sparsam)*

### 4.3 Typography (Rollen statt Pixel-Mikrosteuerung)
- `type-title` (Screen-Titel)
- `type-subtitle` (Metadaten)
- `type-body` (Standardtext)
- `type-caption` (sekundäre Hinweise)
- `type-button` (CTA)

### 4.4 Farbe/Semantik
- `color-bg` (App-Hintergrund)
- `color-surface` (Cards/Container)
- `color-text-primary`
- `color-text-secondary`
- `color-accent` (primäre CTA)
- `color-success` / `color-error` (Status)

### 4.5 Elevation & Divider
- Max. 2 Ebenen (`flat`, `raised`) für visuelle Ruhe.
- Divider nur zur Strukturierung langer Listen, nicht als Dekoration.

---

## 5) Informationsarchitektur & Hierarchie-Regeln

1. **One Screen = One Primary Intention**
   - Library: Finden/Weiterhören
   - Detail: Entscheiden/Starten
   - Player: Steuern
2. **Top-Zone priorisiert Kontext**
   - Titel + Status immer oberhalb optionaler Metadaten.
3. **CTA-Hierarchie strikt**
   - genau 1 primäre CTA sichtbar, sekundäre Actions dezent.
4. **Text statt Icons-only bei kritischen Aktionen**
   - z. B. „Fortsetzen“ statt unbeschrifteter Play-Icon-Variante.
5. **Fehler sind Teil der UX**
   - klare Ursache + nächste Aktion („Erneut versuchen“, „Verbindung prüfen“).

---

## 6) Anti-Patterns (müssen vermieden werden)

1. **Mehrere konkurrierende Primäraktionen** auf einem Screen.
2. **Unklarer Back-Pfad** (Back = Exit trotz gültigem In-App-Parent).
3. **Endlose Loading-Zustände ohne Timeout/Fehlermeldung**.
4. **Inkonsequente CTA-Wording** („Play“, „Start“, „Abspielen“) im selben Kontext.
5. **Platzhalter als Default, obwohl Daten vorhanden sind**.
6. **Visuelle Priorität auf Deko statt Aufgabe** (Cover riesig, CTA versteckt).
7. **Settings-/Nebenfunktionen im Hauptflow übergewichten**.
8. **Unruhige Abstände/Token-Wildwuchs** pro Screen individuell.

---

## 7) Messbare UX-Heuristiken für Abnahme (vor Visual Polish)

## 7.1 Task-Erfolg & Effizienz
- **HEU-01:** 90% der Testnutzer:innen starten Wiedergabe aus der Library in ≤ 15s.
- **HEU-02:** Median Klickpfad Library → Playback Start: **≤ 3 Interaktionen**.
- **HEU-03:** Back-Navigation-Fehlerrate (falscher Zielscreen/App-Exit) **< 5%**.

## 7.2 Orientierung & Verständlichkeit
- **HEU-04:** In 5-Sekunden-Tests benennen ≥ 80% korrekt
  - aktuellen Screen-Zweck
  - nächste sinnvolle Aktion.
- **HEU-05:** CTA-Erkennungsrate für primäre Aktion **≥ 90%**.

## 7.3 Systemzustand & Feedback
- **HEU-06:** Jeder Ladezustand > 2s zeigt sichtbares Feedback + Kontext.
- **HEU-07:** Jeder Fehlerzustand zeigt: Ursache (kurz), Auswirkung, nächste Aktion.
- **HEU-08:** Kein „silent fail“ im Playback-Start (0 toleriert).

## 7.4 Konsistenz
- **HEU-09:** Einheitliches Wording der Kern-CTAs in 100% der P0-Screens.
- **HEU-10:** Token-Compliance (Spacing/Radius/Typography) in P0-Screens ≥ 95%.

---

## 8) Abnahme-Checkliste (Go/No-Go)

**Go für visuellen Fine-Tuning-Start nur wenn alle Punkte erfüllt sind:**
- [ ] P0-Screens funktional + hierarchisch konsistent
- [ ] North-Star-Journey ohne Sackgasse lauffähig
- [ ] HEU-01 bis HEU-08 erfüllt
- [ ] Keine kritischen Anti-Patterns (Abschnitt 6)
- [ ] Dokumentierte UX-Evidenz (Kurztest/Screenrecord/Notizen)

Wenn ein Punkt rot bleibt: **kein visuelles Polishing**, zuerst UX-Fluss korrigieren.

---

## 9) Umsetzungsempfehlung in 2 Iterationen

### Iteration 1 (Struktur)
- Navigation, CTA-Hierarchie, Statusfeedback, Datenintegrität.
- Erfolgskriterium: HEU-01/02/03/06/08 grün.

### Iteration 2 (Konsistenz)
- Token-Anwendung, Wording-Konsistenz, visuelle Ruhe.
- Erfolgskriterium: HEU-04/05/09/10 grün.

So bleibt der Refit pragmatisch: **erst nutzbar und klar, dann schön.**
