# Android Security Inspector

A local, on-device security analysis app for Android. It inspects the device it's
running on and produces a report — it never attempts to root, bypass platform
security, or interfere with other apps.

## Getting a ready-to-install APK without Android Studio

This project includes a GitHub Actions workflow (`.github/workflows/build-apk.yml`)
that builds a debug APK in the cloud — no local Android SDK or Android Studio needed.

1. Create a new repository on [github.com](https://github.com) (public or private —
   private repos still get free Actions minutes).
2. Push this project's contents to it:
   ```
   cd AndroidSecurityInspector
   git init
   git add .
   git commit -m "Initial commit"
   git branch -M main
   git remote add origin https://github.com/<your-username>/<your-repo>.git
   git push -u origin main
   ```
3. On GitHub, go to the **Actions** tab of your repo. A "Build Debug APK" run
   should start automatically (or click "Run workflow" to trigger it manually).
4. Once it finishes (a few minutes), open the completed run and scroll to
   **Artifacts** — download `android-security-inspector-debug-apk`. It's a zip
   containing `app-debug.apk`.
5. Transfer that APK to your Android phone (e.g. via a link to yourself, Drive,
   email) and tap it to install. You'll need to allow "install unknown apps"
   for whatever app you use to open it, the first time.

This produces a **debug build** — fine for personal/sideloaded use, not signed
for Play Store distribution.

## Opening the project in Android Studio (alternative)


1. Open Android Studio (Iguana/2023.2+ recommended, matching AGP 8.5.x).
2. **File → Open** and select this folder (`AndroidSecurityInspector/`).
3. Let Gradle sync. If the wrapper JAR (`gradle/wrapper/gradle-wrapper.jar`) is
   missing (it is not included in this delivery since it's a binary and this
   environment doesn't have network access to Gradle's distribution servers),
   Android Studio will offer to regenerate it automatically. Alternatively, run:
   ```
   gradle wrapper --gradle-version 8.7
   ```
   from a machine with Gradle installed, from the project root.
4. Build & run on a device or emulator running **API 26+**.

## Architecture

Clean Architecture + MVVM, organized by layer:

```
domain/            Pure Kotlin — models, repository interfaces, use cases
data/
  source/          Thin wrappers around Android APIs (DeviceInfoProvider,
                    SecurityCheckProvider, InstalledAppsProvider, FdListingParser)
  repository/       Repository implementations, map data ↔ domain models
  local/            Room database, entities, DAO, JSON (de)serialization
presentation/
  dashboard/        Main score/risk dashboard
  filescan/         Paste/upload + FD-listing classification
  report/           Full report, search/filter, PDF/JSON/CSV export
  history/          Scan history + score trend chart
  components/       Shared composables (ScoreGauge, RiskBadge, finding cards…)
  theme/            Material 3 dark/light cybersecurity theme
  navigation/       NavHost + bottom navigation
di/                 Hilt modules (Database, Repository bindings)
util/               ReportExporter (PDF via PdfDocument, JSON, CSV)
```

State flows one way: UI observes `StateFlow` from a `@HiltViewModel`, which calls
a `UseCase`, which calls a `Repository` interface, implemented against a `data/source`
class that talks to Android.

## What each security check actually does

Every check uses a **documented, public Android API** — no root, no hidden/reflection
APIs, no shelling out to `su`. Findings are explicitly labeled `INFORMATIONAL` or
`HEURISTIC` (see `domain/model/SecurityFinding.kt`) so the UI and reports never present
a heuristic signal as proof of compromise.

| Check | API used | Notes |
|---|---|---|
| Root indicators | `File.exists()` on well-known paths, `PackageManager` for known root-manager packages, `Build.TAGS` | Heuristic only |
| Bootloader status | — | **Not available** via any public API; the app reports this limitation transparently instead of guessing |
| Developer Options | `Settings.Global.DEVELOPMENT_SETTINGS_ENABLED` | Documented key |
| USB debugging | `Settings.Global.ADB_ENABLED` | Documented key |
| Unknown sources | `PackageManager.canRequestPackageInstalls()` (API 26+, own app only) or `Settings.Secure.INSTALL_NON_MARKET_APPS` (below API 26) | Per-app model on modern Android is a genuine platform limitation, explained in-app |
| Installed apps | `PackageManager.getInstalledPackages()` | Requires `QUERY_ALL_PACKAGES` on API 30+ (declared with justification in the manifest) |
| Accessibility services | `AccessibilityManager.getEnabledAccessibilityServiceList()` | |
| Device admin apps | `DevicePolicyManager.getActiveAdministrators()` | |
| VPN / active network | `ConnectivityManager` + `NetworkCapabilities` | |
| Package integrity | `PackageManager.getInstalledPackages(GET_SIGNATURES)` | Presence check only, not cryptographic verification against a baseline |

## File / log analysis feature

The "File Analysis" tab lets you paste or upload diagnostic text you've already
collected yourself (e.g. `adb shell ls -l /proc/<pid>/fd` run from your own machine).
`FdListingParser` classifies each line into one of the categories from the spec
(Standard Streams, APK Files, Framework Libraries, Binder, HwBinder, Pipes, Shared
Memory, EventFD, EventPoll, Inotify, Sockets, Overlay Packages, Kernel Interfaces,
Unknown). This is a pure text classifier — the app never reads `/proc` itself or
inspects other running processes.

## Reports & export

`ReportExporter` (in `util/`) generates:
- **PDF** via `android.graphics.pdf.PdfDocument` (no third-party PDF library)
- **JSON** via `kotlinx.serialization`
- **CSV** via plain buffered file writing

Files are written to the app's own external-files "reports" directory and shared
out through a `FileProvider` content URI — the app never writes to shared/public
storage directly.

## Testing

Unit tests live under `app/src/test/`:
- `RiskLevelTest` — score → risk level boundaries
- `RunSecurityScanUseCaseTest` — scoring math (deductions, floor at 0, positive
  findings never penalized)
- `FdListingParserTest` — line classification for each FD category

Run with: `./gradlew test`

## Permissions declared

Only two, both explained inline in `AndroidManifest.xml`:
- `ACCESS_NETWORK_STATE` / `ACCESS_WIFI_STATE` — read-only network/VPN status
- `QUERY_ALL_PACKAGES` — required on API 30+ to enumerate installed apps for the
  security check; the app only reads public package metadata and never starts,
  modifies, or interferes with another app

No root, device-admin, accessibility-service, or usage-access permissions are
requested by this app for itself.

## Known limitations (by design)

- Bootloader lock state has no public API — reported as a limitation, not guessed.
- "Unknown sources" can only be checked for *this app's own* install permission on
  API 26+, since Android moved this to a per-app model.
- Root/heuristic checks are signals, not proof — the UI always labels them as such.
