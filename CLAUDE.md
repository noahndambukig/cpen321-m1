# CPEN 321 M1: individual milestone

Fork of the course template `UBC-CPEN-V-321/W2026`. **Due Mon 2026-09-21, 11pm.**
This milestone is **individual work**. Collaboration is prohibited.

Course docs live in `../cpen321/docs/` (M1.pdf, Project_Description.pdf,
Submission_Guidelines.pdf). Read them before changing scope.

## Hard constraints (from Project Description §2)

- Frontend: **Kotlin, native Android, Jetpack Compose**. No Views, no Flutter/RN/Expo
- Backend: **Node.js + TypeScript**, deployed to a cloud provider
- DB (if used): MySQL or MongoDB only
- **Firebase / Supabase / Amplify / Parse are forbidden** for auth, DB, or any major
  functionality. Only Firebase push notifications are exempt. Implement Google
  Sign-In directly, never via Firebase Auth.
- Toolchain is pinned in `frontend/gradle/libs.versions.toml`. **Do not bump it**:
  AGP 8.13.2, Kotlin 2.1.21, compileSdk/targetSdk 35, minSdk 26.
- APK must run on a **Pixel 9 emulator, API 36**, and on a physical device.

## What M1 requires

Three buttons, each working independently:

1. **Login + server info**: Google/Facebook auth, then over **HTTPS** display:
   server public IP, client IP, server local time, client local time (both
   `hh:mm:ss GMT+hh:mm`), your name from a backend API, and the signed-in user's
   name. Backend exposes three APIs: server IP, server time, your name.
   *Marks are deducted for a messy or unclear screen.*
2. **Live updates**: backend connects to `wss://8.229.22.124`, relays each
   `{"x","y","color"}` pixel to the app over your own websocket with no batching,
   delay, or reformatting. App paints a 16×16 grid live. ~15s per image, 5s pause.
3. **Timer + surprise**: user-set minutes/seconds; on fire, do something
   interesting. Creativity is explicitly graded.

## Environment

| | |
|---|---|
| SDK | `C:/Users/NoahNdambuki/AppData/Local/Android/Sdk` |
| AVD | `Pixel_9`, API 36, google_apis_playstore, x86_64 |
| JDK | Gradle self-provisions 17 (`gradle/gradle-daemon-jvm.properties`) |
| `JAVA_HOME` for gradlew | `C:\Program Files\Android\Android Studio\jbr` |
| Docker | required by `scripts/run-backend.ps1`; WSL2 backend |
| Package | `com.noah.demo` |
| Debug SHA-1 | `9B:6A:52:6A:9F:5D:4F:92:1B:BA:4B:9D:8E:F5:36:6E:3A:B2:C2:94` |

## Button 2 relay: settled design

Probe of `wss://8.229.22.124` (2026-09-09): live, 387 messages in 25s, payload
exactly `{"x":int,"y":int,"color":"#hex"}`, gaps min 19ms / median 51ms / max 5052ms.
The ~5s max gap is the pause between images.

- **One shared upstream connection**, fanned out to every app client. Do not open a
  connection per client against a shared course server.
- **Forward each frame verbatim.** The spec forbids batching, delaying or
  reformatting, so the relay must not parse and re-serialize.
- **Reconnect with exponential backoff**; the server must stay up until grades post.
- **Grid state lives in the app**, not the back-end: the relay stays stateless.
- **Clear the grid on a gap > 2s**, which is the only available signal for "next
  image starting".
- **Late joiners see a partial image.** Accepted: caching the grid server-side would
  contradict "relay immediately".
- Render with a Compose **Canvas**, not 256 composables.

TLS: the upstream presents a **valid Let's Encrypt certificate** and strict
verification passes, so no `rejectUnauthorized: false` is needed. It is a 6-day short-lived cert
(Sep 7 to Sep 13 at time of writing), so **never pin the certificate or its
fingerprint**; it rotates during the milestone.

## Gotchas

- **`sdk.dir` in `local.properties` must use forward slashes.** Java's `Properties`
  parser treats `\U`, `\N` etc. as escape sequences and silently strips them,
  producing `IOException: The filename, directory name, or volume label syntax is
  incorrect` from `SdkLocator.validateSdkPath`.
- `scripts/run-frontend.ps1` is **emulator-only**: it matches `adb devices` against
  `^emulator-\d+` and boots the AVD named `Pixel_9`. For a physical device call
  `./gradlew installDebug` directly.
- `gradlew` needs `java` on PATH for its own launcher even though the daemon uses
  its self-provisioned JDK 17.
- `sdkmanager` is deprecated; package paths now use `/` not `;`
  (`platforms/android-35`).
- Config files `backend/.env` and `frontend/local.properties` are gitignored, so their
  contents must be documented in `M1_Doc.pdf` for the TAs.

## Button 3 presentation

The Button 3 screens (`ui/game/`, `SimonScreen.kt`, `TimerScreen.kt`) deliberately
**bypass the Material colour scheme** and define their own palette in
`ui/game/ArcadeTheme.kt`. This is not an oversight: `ui/theme/Theme.kt` enables
`dynamicColor`, which derives the palette from the device wallpaper, so without this
the game would look different on every device. Do not "fix" these screens back onto
MaterialTheme.

`CrtSurface` draws full-bleed behind the system bars and applies insets to its own
content, so `MainActivity` passes no modifier to `TimerRoute`. Overlays use
`drawWithContent`, not `drawBehind`, which paints under the child.

Input is locked (`phase = Showing(null)`) the instant a round completes, and the tap
handler bounds-checks `expectedIndex`. Both are load-bearing: without them a tap during
the post-round lead-in indexes past the end of the sequence and crashes.

## Conventions

- Keep the template's directory structure intact; graders run `scripts/`.
- Working version always on `main`.
- Log every work session in `WORKLOG.md`. It feeds the required time and
  AI-reliance table in `M1_Doc.pdf`.
