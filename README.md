# Noah_Demo: CPEN 321 M1

Android (Kotlin/Compose) app with a Node.js/TypeScript back-end.

The deployed back-end is already running, so **you do not need to deploy anything to
exercise the app**. Installing the APK is enough. Instructions for running your own
copy of the back-end are below.

| | |
|---|---|
| Deployed back-end | `https://3.218.47.84.sslip.io` (AWS EC2, `us-east-1`) |
| App package | `com.noah.demo` |
| Emulator target | Pixel 9, API 36 |

The back-end presents a real Let's Encrypt certificate, so no certificate needs to be
installed or trusted on the device.

## Frontend Setup

**Prerequisites**
- Android Studio with **SDK Platform 35** (compile target) and an **API 36** system
  image (Google APIs or Google Play) for the Pixel 9 emulator
- An AVD named **`Pixel_9`** if you intend to use `scripts/run-frontend.[sh|ps1]`,
  which looks that name up exactly
- `java` on `PATH` for the Gradle launcher. Gradle downloads its own JDK 17 for the
  build itself (pinned in `frontend/gradle/gradle-daemon-jvm.properties`), so any
  recent JDK will do here, and Android Studio's bundled JBR works.

**Configuration.** Create `frontend/local.properties`:

```properties
sdk.dir=C:/Users/<you>/AppData/Local/Android/Sdk
API_BASE_URL=https://3.218.47.84.sslip.io
GOOGLE_CLIENT_ID=602626048108-tmthf8i1p2mjbg53gtacc97cu0jn79md.apps.googleusercontent.com
```

`sdk.dir` **must use forward slashes** on Windows. Java's `Properties` parser treats
`\U`, `\N` and similar as escape sequences and silently corrupts the path, producing a
confusing `IOException: The filename, directory name, or volume label syntax is
incorrect` from the Android Gradle plugin.

`GOOGLE_CLIENT_ID` is the **Web** OAuth client ID. Credential Manager takes it as its
`serverClientId`; the Android OAuth client is a separate registration that authorises
the signing certificate and is never referenced in code.

**Build and run**

```bash
cd frontend
./gradlew installDebug        # builds, installs to a connected device or emulator
```

`installDebug` installs but does not launch. Open **Noah_Demo** from the launcher
afterwards. With both an emulator and a phone attached, set `ANDROID_SERIAL` to pick
one.

To build a signed release APK you additionally need the keystore and these entries in
`local.properties` (see `M1_Doc.pdf` for the values):

```properties
RELEASE_STORE_FILE=release.keystore
RELEASE_STORE_PASSWORD=<see M1_Doc.pdf>
RELEASE_KEY_ALIAS=noahdemo
RELEASE_KEY_PASSWORD=<see M1_Doc.pdf>
```

## Backend Setup

Only needed if you want to run the back-end yourself; the deployed one is live.

**Prerequisites:** Docker with Compose v2.24+, and `curl`.

Create `backend/.env` from `backend/.env.example`:

```properties
PORT=3000
NODE_ENV=development
MONGODB_URI=mongodb://localhost:27017/cpen321
OWNER_FIRST_NAME=Noah
OWNER_LAST_NAME=Ndambuki
SERVER_PUBLIC_IP=
PIXEL_STREAM_URL=wss://8.229.22.124
GOOGLE_CLIENT_ID=602626048108-tmthf8i1p2mjbg53gtacc97cu0jn79md.apps.googleusercontent.com
JWT_SECRET=<any random string>
```

Leave `SERVER_PUBLIC_IP` blank locally. The server then asks an external reflector
for its public address. Set it explicitly when deploying, because a cloud VM behind
NAT cannot see its own public address on any interface.

```bash
./scripts/run-backend.sh        # or .ps1 on Windows
```

The script starts `docker compose` and waits for `http://localhost:3000/health`.
Stop with `docker compose down`.

Point the app at a local back-end by setting `API_BASE_URL=http://10.0.2.2:3000` in
`local.properties` (`10.0.2.2` is the emulator's alias for the host) and rebuilding.
Note that this is plain HTTP, which the release manifest does not permit. Use the
deployed HTTPS back-end to exercise the app as submitted.

### APIs

| Method | Path | Returns |
|---|---|---|
| GET | `/health` | `{"status":"ok"}` |
| GET | `/api/server-ip` | `{"ip":"3.218.47.84"}` |
| GET | `/api/server-time` | `{"time":"hh:mm:ss GMT+hh:mm"}` |
| GET | `/api/name` | `{"firstName":"Noah","lastName":"Ndambuki"}` |
| GET | `/api/client-ip` | `{"ip":"<caller address>"}` |
| WS | `/ws/pixels` | relayed pixel stream |

`/api/client-ip` is an extra beyond the three M1 requires; it lets the app show the
caller's address without a device permission.

## Additional Setup

**Google Sign-In.** If sign-in reports no usable account on a slow emulator, give the
AVD more RAM (Device Manager - Edit - Additional settings) and try again. Android's
CredentialManager waits about three seconds for a provider, and a constrained emulator
can take longer than that; the app already retries once automatically.

The OAuth consent screen is published to production, so **any Google account can sign
in**. No account needs to be pre-authorised. You may see a "Google hasn't verified
this app" interstitial; choose **Advanced → Go to Noah_Demo (unsafe)**. That is
expected for an unverified app, and verification is only required for sensitive
scopes. This app requests only `email`, `profile` and `openid`.

**Deployment.** `docker-compose.yml` is unchanged from the course template and runs
the back-end alone. The deployed host additionally applies `docker-compose.prod.yml`,
which puts Caddy in front to terminate TLS:

```bash
docker compose -f docker-compose.yml -f docker-compose.prod.yml up --build -d
```

Caddy obtains and renews a Let's Encrypt certificate for `3.218.47.84.sslip.io`
automatically. `sslip.io` resolves any embedded IP address back to itself, which
supplies a hostname for certificate issuance without owning a domain. Port 80 must be
reachable for the ACME HTTP-01 challenge and port 443 for traffic.
