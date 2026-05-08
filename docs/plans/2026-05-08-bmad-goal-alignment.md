# BMAD Goal Alignment — ShelfPlayer-Android Recovery

**Status:** Draft for Review  
**Owner:** Theo (PM)  
**Date:** 2026-05-08  
**Horizont:** 2 Wochen Recovery-Sprint

---

## 1) Problem Statement

Der MVP-Kernflow ist aktuell nicht zuverlässig genug:

1. **Playback Reliability Gap** — Player startet nicht deterministisch.
2. **Data Trust Gap** — Detailseiten zeigen teils falsche/Placeholder-Daten (z. B. „Unbekannter Titel“ trotz API-Daten).
3. **Navigation Safety Gap** — Back-Geste beendet App statt sinnvoller In-App-Navigation.
4. **UX Consistency Gap** — UI-Hierarchie wirkt inkonsistent zur Zielvorlage.

**Folge:** Vertrauensverlust im Hauptflow `Library → Detail → Play`, hohe Reibung, erhöhtes Regressionsrisiko.

---

## 2) Zielbild (Recovery)

### Business Outcome
- MVP wieder **stabil demo-/pilotfähig** machen.
- Regressionskosten durch klare Gates und Scope-Disziplin senken.

### User Outcome
- Nutzer kann **zuverlässig starten, pausieren, fortsetzen**.
- Back-Navigation ist erwartbar und sicher.
- Metadaten wirken konsistent und vertrauenswürdig.

---

## 3) Scope / Non-Scope

### In Scope (2 Wochen)
- **Phase A + B zuerst:**
  - Back-Stack/Back-Geste reparieren
  - Detaildaten-Mapping robust machen
  - Playback Session Contract + State Machine stabilisieren
- **Phase C gezielt:** Journey glätten (Library → Detail → Player)
- **Phase D minimal:** visuelle Grundkonsistenz (Tokens/Layout), kein Full Redesign

### Out of Scope
- Neue Features (Search-Ausbau, Downloads, Recommendations etc.)
- Große Architektur-Migrationen ohne Blocker-Relevanz
- Pixel-perfekte Designpolitur
- Plattformübergreifende Erweiterungen außerhalb Android

---

## 4) Messbare Success Metrics (2 Wochen)

1. **Playback Start Reliability:** ≥ 90% erfolgreiche Starts (Top-10 Titel, je 3 Runs)
2. **Time-to-Playback (Median):** ≤ 5 Sekunden
3. **Back Navigation Correctness:** 100% im Hauptjourney
4. **Metadata Correctness:** ≥ 95% korrekte Titel/Autor-Anzeige in Stichprobe
5. **Loading Deadlock Rate:** ≤ 2% (`Lädt…` ohne Auflösung > 10s)
6. **CI Gate Reliability:** 100% grüne Recovery-Merges

---

## 5) Produkt-Akzeptanzkriterien (vor Story-Ableitung verbindlich)

Recovery gilt als abnahmefähig, wenn:

1. `Library → Detail → Play → Pause/Resume → Back` reproduzierbar funktioniert.
2. Fehlerzustände klar kommuniziert werden (kein stilles Hängen).
3. Placeholder nur bei echten Nullfällen erscheinen.
4. Back nur am Root zur App-Beendigung führt.
5. Kernscreens eine erkennbare visuelle Mindestkonsistenz haben.
6. CI-Gates und Nachweise dokumentiert sind.

---

## 6) Hauptrisiken & Gegenmaßnahmen

1. **API-Edge-Cases (Play Session / audioTracks)**  
   → Contract-Tests + klare Fallback-Regeln.

2. **State-Management-Komplexität**  
   → Explizite State Machine + Transition Guards + Timeouts.

3. **Scope Creep während Recovery**  
   → Harte Scope-Freeze-Regel bis A+B abgenommen.

4. **CI grün, Device UX rot**  
   → Pflicht-Smoke auf Emulator + 1 physischem Gerät pro Phase-Gate.

5. **Design-Diskussion blockiert Delivery**  
   → Prinzip: *functional reliability before visual perfection*.

---

## 7) Entscheidung für den Startpunkt

**Start mit A2 (Back-Behavior), dann B1/B2 (Playback), dann A3 (Daten), dann UX/Design.**

Diese Reihenfolge bleibt fix, bis harte Abnahmekriterien erreicht sind.
