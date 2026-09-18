# M1 Worklog

Raw material for the **Time and AI Reliance Distribution** table and the **AI Use
Reflection** in `M1_Doc.pdf`. Log entries as work happens, not at the end.

Deadline: **Mon 2026-09-21, 11pm**

## Time and AI Reliance

| ID | Task | Date | Hours | AI Reliance | Notes |
|----|------|------|-------|-------------|-------|
| 1 | Toolchain setup: Android Studio, SDK 35/36, Pixel_9 AVD, Docker + WSL2, fork + clone, config files, baseline build verified | 2026-09-08 → 09 | | 90% | Claude Opus 5 via Claude Code drove install/verify; manual steps were admin-elevated `wsl --install` and Android Studio GUI |
| 2 | Chunk 1: backend server-info APIs + 11 Jest tests, verified live in Docker | 2026-09-09 | | 85% | Claude wrote code and tests; the substantive call was NAT-aware public-IP resolution |
| 3 | Chunk 2: Button 1 UI, home screen, server-info screen, loading/error/retry states | 2026-09-09 | | 85% | Zero new Gradle deps; verified by driving the emulator and reading screenshots |
| 4 | Chunk 4: Button 2, back-end pixel relay + 16x16 Compose canvas, reconnect on both ends | 2026-09-09 | | 85% | Probing the upstream first changed the design; relay fidelity measured rather than assumed |
| 5 | Chunk 5: Button 3, countdown timer firing into a sprite-based Simon memory game, 4 original 8-bit sprites | 2026-09-09 | | 80% | Sprites hand-designed; three visual bugs only found by looking at screenshots |
| 6 | Package rename to com.noah.demo + Credential Manager Google Sign-In wiring | 2026-09-09 | | 85% | Renamed before OAuth registration to avoid re-registering the client |
| 7 | Button 3 revamp: CRT arcade presentation, phosphor-decay animation, pentatonic AudioTrack synth, difficulty levels; plus fixing an input-window crash found in Codex review | 2026-09-14 | | 50% | Plan reviewed by Codex before implementing; it caught a live IndexOutOfBounds that tapping during the post-round lead-in would trigger |
| | | | | | |

**Overall:** _(fill at submission)_

## AI Use Reflection: candidate tasks

Pick 2-3 at submission. Notes to draw on:

### 1. Environment setup and toolchain debugging
- **Tools:** Claude Code (Opus 5), agentic. It ran shell commands directly, read SDK state, installed packages.
- **Strategy:** Let it inspect actual machine state rather than describing it. It caught that
  "Sources for Android 35" is not "SDK Platform 35", and diagnosed a Gradle
  `IOException: The filename, directory name, or volume label syntax is incorrect`
  down to Java `Properties` escape handling in `local.properties`.
- **Advantage:** Verified each claim against the filesystem instead of guessing.
- **Disadvantage:** A heredoc it wrote silently collapsed `\\` to `\`, creating the
  very bug it then had to debug. Generated file content needs byte-level checking.

### 2. Backend API design under a deployment constraint
- **Task:** Implement M1's three server-info APIs.
- **Tools:** Claude Code (Opus 5), which wrote the routes, the tests, and ran them.
- **Strategy:** The non-obvious part was "server public IP". A naive
  `os.networkInterfaces()` returns the *private* address on a NAT'd cloud VM and
  would silently fail the spec once deployed. Settled on config-first
  (`SERVER_PUBLIC_IP`), then a memoized external reflector, then a local fallback.
- **Advantage:** Surfaced a failure mode that would not have appeared until the app
  was running on a cloud VM days later, and covered it with mocked tests for the
  reflector-down and reflector-erroring paths.
- **Disadvantage:** The first instinct is the plausible-looking `networkInterfaces()`
  answer. The NAT constraint had to be raised explicitly before the design changed.
  AI optimizes for code that runs locally, not code that survives deployment.

### 3. Verifying a relay instead of trusting it
- **Task:** Relay the course pixel stream (`wss://8.229.22.124`) through our back-end
  to the app without batching, delaying, or reformatting.
- **Tools:** Claude Code (Opus 5), which probed the upstream, wrote the relay, then wrote
  throwaway Node clients to measure it.
- **Strategy:** Probe before designing. The probe showed a valid Let's Encrypt cert
  (so no TLS bypass) that is *short-lived*, expiring mid-milestone, which means
  certificate pinning would have worked on day one and broken before the deadline.
  It also showed the ~5s inter-image pause that the canvas-clear logic keys on.
- **Advantage:** The first fidelity test reported "BYTE-IDENTICAL: false" and looked
  like a bug in the relay. Testing the *assumption* instead revealed the course server
  sends an independent randomized stream per connection (1/256 positional match
  between two upstream connections), so the comparison was invalid by construction.
  The real test, two clients on our relay, passed 311/311 with 1ms skew.
- **Disadvantage:** That first test was AI-written and confidently wrong in its
  premise. A plausible-looking test that measures the wrong thing is more dangerous
  than no test, because a "false" result invites you to "fix" working code.

**Was AI used to prepare M1_Doc.pdf?** _(answer at submission)_

## Facts needed for M1_Doc.pdf

| Field | Value |
|-------|-------|
| Repo | https://github.com/noahndambukig/cpen321-m1 |
| Commit SHA (main, at submission) | `933244d64984c4713e038d09e6d9ccc8e694faf7` |
| Backend public IP / domain | `3.218.47.84`, `https://3.218.47.84.sslip.io` (AWS EC2 t3.micro, us-east-1, Elastic IP) |
| Physical device (make + model) | _TBD_ |
| Release keystore SHA-1 | `A4:1F:86:42:EE:19:7A:A1:C7:A5:42:6D:63:CE:DD:A4:AD:D4:47:54` (alias `noahdemo`, password in `M1_Doc.md`) |
| Debug keystore SHA-1 | `9B:6A:52:6A:9F:5D:4F:92:1B:BA:4B:9D:8E:F5:36:6E:3A:B2:C2:94` |
| App name / APK | Noah_Demo → `M1_Noah_Demo.apk` |
| App package | `com.noah.demo` |
| Button 3 description | User sets minutes/seconds; on zero the app drops into a Simon-style memory game played with four original 8-bit sprites (alien, ghost, mushroom, star) instead of colours, tuned to a C major pentatonic scale. Easy/Medium/Hard control replay speed, each with its own best score. Presented as a CRT arcade cabinet. |
| Limitations | _TBD_ |

### Deployment (AWS)

- **EC2** t3.micro, Ubuntu 24.04, us-east-1, **Elastic IP 3.218.47.84** (elastic so the
  address cannot change, because `API_BASE_URL` is compiled into the APK).
- Security group: 22 from a single IP, 80 and 443 from anywhere. 80 is required for
  the Let's Encrypt HTTP-01 challenge, 443 serves the API and the websocket.
- 2 GB swap added; the box has only 911 MB RAM and runs mongo + node + caddy.
- **Caddy** terminates TLS (`Caddyfile`, `docker-compose.prod.yml`) and reverse-proxies
  to the backend. It obtains and renews a real Let's Encrypt certificate for
  `3.218.47.84.sslip.io` automatically. sslip.io resolves the embedded IP, which
  gives Let's Encrypt a hostname to issue against. No self-signed cert and therefore
  no Android `network_security_config` is needed.
- Deploy: `docker compose -f docker-compose.yml -f docker-compose.prod.yml up --build -d`
- `docker-compose.yml` is unchanged from the template so the TA local flow still works.

### Secrets / setup steps TAs need
- `backend/.env`: from `.env.example`; `PORT`, `MONGODB_URI`, `GOOGLE_CLIENT_ID`, `JWT_SECRET`
- `frontend/local.properties`: `sdk.dir`, `API_BASE_URL`, `GOOGLE_CLIENT_ID`
- **`sdk.dir` must use forward slashes** (`C:/Users/...`). Java's `Properties` parser
  treats `\U`, `\N` etc. as escapes and silently corrupts the path.

## Google OAuth setup (project 602626048108 "CPEN 321 A1")

**Two** OAuth clients are required, in the same project:

| Type | Purpose | Value |
|------|---------|-------|
| Android | Proves the APK is ours via package + signing cert. Never used in code. | `com.noah.demo` + debug SHA-1 |
| **Web** | The audience the ID token is issued for. This is the one the app uses. | `GOOGLE_CLIENT_ID` in both config files |

Passing the **Android** client ID as Credential Manager's `serverClientId` fails with
`[28444] Developer console is not set up correctly`. The consent screens still
appear and only the final token issuance fails, so the error points nowhere useful.
Cost us a debugging cycle; the Web client ID is the one that goes in config.

## Known limitations (draft for M1_Doc.pdf)

- ESLint cannot run locally: the template's `.eslintrc.json` extends `standard` and
  `plugin:node`, which are absent from `backend/package.json`. That config must not
  be modified (Project Description section 7) and Codacy supplies its own plugins
  server-side. Not required until CP1.
- The container runs UTC, so server time reads `GMT+00:00`. Valid per the spec; set
  `TZ` in `docker-compose.yml` if a non-zero offset is preferred for the demo.
- Server and client clocks can differ by ~1s on screen (e.g. `22:52:24 GMT+00:00`
  vs `15:52:23 GMT-07:00`). Both are truncated to whole seconds and the emulator's
  clock drifts slightly from the host; the two values are captured a round-trip
  apart by design, since each side reports its own local time.

- **Google Sign-In can time out on an underpowered emulator.** Android's
  CredentialManager waits roughly three seconds for a credential provider; on a
  2 GB Pixel 9 AVD that had been running for days, Play Services answered at ~5s,
  so the framework cancelled first and the app saw a spurious "no credential"
  error. The app now retries once automatically, which resolved it. If sign-in
  still fails on a constrained emulator, allocate more RAM to the AVD
  (Device Manager - Edit - Additional settings) and retry.

## Infrastructure notes

- Root volume grown 8 GB → 20 GB on 2026-09-12 after the 8 GB filled, which killed
  mongo (exit 14) and blocked `git fetch`. Now 19 GB filesystem, 36% used.
- `docker-compose.prod.yml` adds `restart: unless-stopped` to **backend** (the
  template sets none, so a host reboot would have left it down) and caps container
  logs at 10 MB × 3 so logs cannot fill the disk again.
- TLS certificate expires **2026-12-09**; Caddy renews automatically while the
  instance runs.

## Pre-submission checklist (deferred work: do not lose these)

- [x] **OAuth consent screen published to production** (2026-09-12). Any Google
      account can now sign in, so graders use their own and the new-device challenge
      that blocks the test account no longer matters. Required a homepage and privacy
      policy URL, both served from GitHub Pages in `docs/`, plus registering
      `noahndambukig.github.io` under Authorised domains.
      **Do not upload an app logo**: that would force Google verification review.
- [ ] **Redeploy the server from git after the final commit.** The running code was
      copied over scp (since we are holding a single commit to the end), so the
      instance is not currently built from a pushed revision. After pushing, run
      `git pull` on the box and rebuild so the repo genuinely reproduces production.
- [ ] Certificate expires **2026-12-09**; Caddy auto-renews, but the instance must
      stay running. Do not stop it before grades post.
- [ ] Tear down after grades: terminate the instance **and release the Elastic IP**
      (an unattached Elastic IP bills hourly).
- [ ] Record the physical device manufacturer + model.
- [ ] Fill in hours and the Overall row in the time table above.
- [ ] Write `M1_Group.pdf`: student IDs, first and last names of all four members.
- [ ] Write `M1_ProjectIdea.pdf`. It needs the group repo to exist as a renamed public
      fork with TA write access. **Noah's fork slot is used by `cpen321-m1`, so a
      teammate or an org must own the group fork.**
- [ ] Export `M1_Doc.md` to PDF once the [FILL IN] items are complete.

## Chunk status

| Chunk | Description | State | Verified by |
|-------|-------------|-------|-------------|
| 0 | Toolchain + repo + baseline build | DONE | `/health` 200, `assembleDebug` SUCCESS |
| 1 | Backend: 3 APIs | **DONE** | 11/11 Jest pass; live curl on all 4 endpoints; `run-backend.ps1` green |
| 2 | Button 1 UI (pre-auth) | **DONE** | Emulator screenshots: home, all 6 fields, error state, retry recovery, back nav |
| 3 | Google Sign-In | **DONE** | Debug and **release** builds both complete the full consent flow; release SHA-1 registered 2026-09-12 |
| 4 | Button 2: websocket relay + grid | **DONE** | 311/311 fan-out fidelity, 1ms skew; emulator showed 256/256 pixel image; reconnect verified |
| 5 | Button 3: timer + surprise | **DONE** | Timer setup/countdown/fire verified; logcat proved correct+wrong tap handling; reached Round 2 / Best 2 |
| 6 | Cloud deploy + HTTPS | **DONE** | All 5 endpoints 200 over HTTPS; wss 293 frames 0 errors; verified on emulator against EC2 |
| 7 | Release APK + docs | **DONE** | Release APK signed, sign-in verified in release build, README + M1_Doc drafted |
