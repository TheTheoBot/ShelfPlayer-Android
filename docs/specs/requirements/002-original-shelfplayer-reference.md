# 002 — Original ShelfPlayer Referenzanalyse (iOS)

**Status:** Draft  
**Owner:** Theo  
**Created:** 2026-05-07  
**Type:** Requirements Reference / Discovery

---

## Ziel dieses Dokuments

Diese Doku beschreibt die *Original-App* (iOS, `rasmuslos/ShelfPlayer`) als belastbare Grundlage für die Android-Implementierung.

Sie beantwortet:
- Welche Screens/Flows existieren?
- Welche Features existieren?
- Wie ist die App strukturiert (UI, Domain, Netzwerk, Persistenz, Integrationen)?
- Was sollte für Android-MVP priorisiert werden?

> Quelle: statische Repo-Analyse unter `/home/robin/projects/ShelfPlayer-upstream` (README + Code-Struktur + zentrale Dateien wie `ContentView`, `TabRouter`, `SettingsView`, `ShelfPlayer.swift`).

---

## 1) Produktumfang der Original-App (Feature-Landschaft)

Aus README + Code ergibt sich folgender Scope:

### Kernfunktionalität
- Audiobooks + Podcasts in einer App
- Multi-Server / Multi-Library Support
- Auth: Username/Password + OpenID
- Custom HTTP Headers pro Verbindung
- Listen Now / Continue Listening
- Globale & Library-spezifische Suche
- Queue + Up Next Queue
- Kapitelsteuerung, Playback Speed, konfigurierbare Skip-Intervalle
- Sleep Timer (zeitbasiert + kapitelbasiert)
- Bookmarks inkl. Notizen
- Progress Sync / Session Sync
- Listening History / Listened-Today Tracking
- Offline Mode + Downloads
- Convenience Downloads (automatisiert im Hintergrund)
- Collections + Playlists
- PDF Viewer

### Integrationen
- AirPlay / System Media Controls
- Widgets (Start, Listen Now, Listened Today)
- Live Activities + Dynamic Island (Sleep Timer)
- Siri / App Intents / Shortcuts
- CarPlay
- Spotlight Indexing + Deep Links
- Home Screen Quick Actions

---

## 2) Wichtige User-Flows

## 2.1 App-Entry / Routing (`App/Navigation/ContentView.swift`)

Top-Level Zustände:
1. **Migration läuft** → `MigrationView`
2. **ConnectionStore lädt** → `LoadingView`
3. **Keine Verbindung vorhanden** → `WelcomeView`
4. **Offline Mode aktiv** → `OfflineView`
5. **Normalbetrieb** → `TabRouter`

Zusätzlich zentral gesteuert:
- globale Sheets (z. B. Add/Edit Connection, Settings, Listen Now, Customize)
- globale Warning-Alerts
- URL-Scheme Handling
- Spotlight-Weiterleitungen (Search/Item Navigation)

## 2.2 Hauptnavigation (`App/Navigation/TabRouter.swift`)

- Navigation basiert auf `TabView` + dynamischer Tab-Konfiguration.
- Verhalten unterscheidet kompakt vs. regular (iPhone/iPad Sidebar).
- Tabs sind library- und connection-abhängig.
- Eigener Search-Tab (`SearchPanel`).
- Sichtbarkeit/Verhalten der Now-Playing-Bar ist zustandsabhängig.

### Zentrale Panel-Screens im TabRouter
- `AudiobookHomePanel`
- `AudiobookLibraryPanel`
- `AudiobookSeriesPanel`
- `AudiobookAuthorsPanel`
- `AudiobookNarratorsPanel`
- `AudiobookGenresPanel`
- `AudiobookTagsPanel`
- `AudiobookBookmarksPanel`
- `PodcastHomePanel`
- `PodcastLibraryPanel`
- `PodcastLatestPanel`
- `CollectionsPanel` (Collections/Playlists)
- `DownloadedPanel`
- `MultiLibraryHomePanel`
- `SearchPanel`

## 2.3 Connection-Onboarding

Wichtige Dateien:
- `App/Connection/WelcomeView.swift`
- `ConnectionAddSheet.swift`
- `ConnectionEditSheet.swift`
- `ReauthorizeConnectionSheet.swift`
- `ConnectionManageView.swift`
- `ConnectionStore.swift`, `ConnectionManager.swift`, `ConnectionAuthorizer.swift`

Flow:
- Welcome-Screen bei leerem Connection-Set
- Add/Edit/Reauthorize Connection über zentrale Sheets
- Verwaltung mehrerer Verbindungen inkl. Custom Header / Zertifikat-Optionen

## 2.4 Detail-Seiten

Je Entität existieren eigene Detail-Views mit ViewModels:
- `AudiobookView` + `AudiobookViewModel`
- `PodcastView` + `PodcastViewModel`
- `EpisodeView` + `EpisodeViewModel`
- `SeriesView` + `SeriesViewModel`
- `PersonView` + `PersonViewModel`
- `CollectionView` + `CollectionViewModel`

Typische Bestandteile:
- Header
- Content-Listen / Subpages
- Toolbar-Aktionen
- gemeinsame Item-Actions (Queue, Download, Share, Progress etc.)

## 2.5 Playback

Verzeichnis `App/Playback/` enthält u. a.:
- `PlaybackViewModel`
- `PlaybackControls`, `PlaybackSlider`, `PlaybackRatePicker`
- `PlaybackQueue`
- `PlaybackSleepTimerButton`
- `PlaybackSkipButtons`
- `PlaybackAirPlayButton`

=> Deutet auf einen sehr ausgebauten Player mit Queue/Up-Next/Sleep-Timer/Route-Steuerung.

## 2.6 Settings

`App/Settings/SettingsView.swift` zeigt eine klare Informationsarchitektur:
- Appearance
- Playback
- Sleep Timer
- Connections
- Downloads
- Hidden Libraries
- CarPlay
- Tabs
- Advanced
- Support
- (Debug in DEBUG-Builds)

---

## 3) Screen-Inventar (aus Dateistruktur abgeleitet)

## 3.1 Top-Level Views (Auszug)
- `ContentView`
- `WelcomeView`
- `OfflineView`
- `SettingsView`
- `ConnectionManageView`
- `AudiobookView`
- `PodcastView`
- `EpisodeView`
- `SeriesView`
- `PersonView`
- `CollectionView`
- `MigrationView`
- `LoadingView`
- `ErrorView`

## 3.2 Panel-Views (Hauptnavigation)
- Audiobook: Home, Library, Series, Authors, Narrators, Genres, Tags, Bookmarks
- Podcast: Home, Library, Latest
- Collections/Playlists
- Downloaded
- Multi-Library Home
- Search

## 3.3 Sheet-Views (globale Modals)
- ListenNowSheet
- StatisticsSheet
- WhatsNewSheet
- DescriptionSheet
- GroupingConfigurationSheet
- CustomTabValueSheet
- CustomizeLibraryPanelSheet
- ConnectionAdd/Edit/Reauthorize
- EditCollection / CollectionMembershipEditor

---

## 4) Architektur der Original-App

## 4.1 Modul-Sicht (Swift-Dateien, grob)
- `App`: 216
- `ShelfPlayerKit`: 176
- `ShelfPlayback`: 14
- `WidgetExtension`: 9
- `ShelfPlayerMigration`: 5
- Tests: `ShelfPlayerKitTests`, `ShelfPlayerUITests`, `ShelfPlaybackTests`

## 4.2 App-Schicht (`App/`)
Schwerpunkte:
- `Navigation/` (App-State-Routing, Tabs, Deep-Link-Navigation)
- `Connection/` (Onboarding, Verwaltung, Auth-Flows)
- `Panels/` (Haupttab-Inhalte)
- `Detail/` + `Item/` (Entitäts- und Aktionen-Ebene)
- `Playback/` (Player UX)
- `Settings/` (Konfiguration)
- `Embassy/` (Spotlight, Intents, Kontext)
- `CarPlay/`

## 4.3 Core-Schicht (`ShelfPlayerKit/`)
Schwerpunkte:
- `Persistence/` (größter Block): lokale Modelle, Caches, Subsysteme
- `Network/`: API-Client, Endpunkte (`API+...`), Payloads, Konverter
- `Foundation/`: Domänenmodelle, Identifiers, Utility-Typen
- `Embassy/`: Intents/Automation-Objekte

## 4.4 Lifecycle-Hooks (`App/Lifecycle/ShelfPlayer.swift`)
- App-Launch-Hooks (Tips, Background Tasks, Dependency Registration)
- UI-Init-Hooks (ToS/Build-Handling, Cache-Invalidierung, Observer-Setup)
- Online-Init-Hook (Spotlight/Intents/Convenience-Download Scheduling)

=> Wichtig: App ist stark event- und background-task-getrieben, nicht nur „Screen + API Call“.

---

## 5) Was das für unser Android-Projekt bedeutet

## 5.1 Abgrenzung für MVP (empfohlen)
Nicht 1:1 nachbauen. In Android-MVP zuerst:
1. Connection + Auth robust
2. Library + Search (Basis)
3. Item-Details (Audiobook/Podcast/Episode minimal)
4. Player (Play/Pause, Seek, Chapter, Rate, Skip)
5. Progress Sync
6. Basis-Downloads

Später:
- Multi-Server/Library-Advanced
- automatische Convenience Downloads
- tiefere Integrationen (Android Auto, App Shortcuts, Widgets, etc.)

## 5.2 Konkrete strukturelle Leitplanken für Android
- Klare Schichten: `data` / `domain` / `ui`
- Feature-Pakete pro Flow (`connection`, `library`, `detail`, `player`, `settings`)
- frühe Definition zentraler App-States (ähnlich ContentView-States)
- frühe Definition Navigation-Contract (Tabs + Deep Link + Item Routing)

---

## 6) Mapping-Vorschlag iOS → Android (MVP)

- `WelcomeView` / Connection-Sheets → `ConnectionOnboardingScreen` + `ConnectionManageScreen`
- `TabRouter` + Panels → `MainScaffold` + BottomNav Tabs (Library, Search, Player, Settings)
- `Audiobook/Podcast/Episode View` → `ItemDetailScreen` (erst generisch, später spezialisiert)
- `PlaybackViewModel + Controls` → `PlayerViewModel + ExoPlayer Controller UI`
- `SettingsView` → mehrseitige Settings-Navigation

---

## 7) Offene Fragen vor der nächsten Implementierungsphase

1. Welche 4–5 Screens sind für unseren MVP *verbindlich* (Definition of Done)?
2. Wollen wir im MVP direkt Multi-Server oder zunächst Single-Server?
3. Welche iOS-Features sind explizit **Out of Scope** bis v0.2 (z. B. Widgets, Android Auto, Offline-Automation)?
4. Sollen wir ein eigenes Android-„TabRouter“-Äquivalent formal als Spec festschreiben?

---

## 8) Nächster Schritt (empfohlen)

Aufbauend auf dieser Referenz jetzt eine **konkrete, low-level MVP-Screen-Spec** erstellen mit:
- Screen-by-Screen Anforderungen
- State-Machine pro Screen
- Navigationsübergängen
- API/Repository-Contracts
- Akzeptanzkriterien pro Story

Damit gehen wir von „high-level Plan“ auf „implementierbare Stories“ runter.
