# ShelfPlayer Android MVP – Implementierungsplan

> Für Agent-Execution: `subagent-driven-development` nutzen (taskweise, mit Review-Loops).

## Ziel
Kotlin/Compose-App als Android-Neuaufbau inspiriert von ShelfPlayer mit Fokus auf Audiobookshelf-Anbindung, Library-Ansicht, Player und Progress Sync.

## Architektur (MVP)
- Single-App-Modul (`app`) für schnellen Start, später modulare Trennung.
- Schichten: `data` (API + DTO + Repository), `domain` (UseCases), `ui` (Compose + ViewModel).
- Netzwerk: Ktor oder Retrofit + Kotlinx Serialization.
- State: ViewModel + StateFlow.
- Navigation: Navigation Compose.

## Milestones
1. **Foundation**
   - Build-Setup, Theme, Basisnavigation, CI-APK-Build.
2. **Connection & Auth**
   - Server-URL + Login (Token) + Speicherung (EncryptedSharedPreferences).
3. **Library**
   - Listenansicht (Books/Podcasts), Suche lokal/remote, Detailscreen.
4. **Player**
   - ExoPlayer, Kapitel, Skip, Geschwindigkeit, Sleep Timer (Basis).
5. **Sync & Offline (MVP-light)**
   - Fortschritts-Sync, rudimentäre Download-Verwaltung.
6. **Release**
   - Signierte Release-APK + GitHub Release Asset.

## Risiken
- API-Abweichungen zwischen ABS-Versionen.
- Umfang von Offline/Sync kann Timebox sprengen.
- Material 3 Expressive ist teilweise noch evolving; ggf. Fallback auf stabile M3-Patterns.

## Definition of Done (MVP)
- Android-App startet und kann sich mit einem Audiobookshelf-Server verbinden.
- Library + Detail + einfacher Player funktionieren.
- Debug-APK wird in GitHub Actions gebaut und als Asset verfügbar gemacht.
