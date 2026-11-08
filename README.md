# SPONDON – Smart City App (Android)

Native Java Android app (Firebase + osmdroid).

SPONDON is a smart city "super app" for citizens, service providers and city administrators.
It brings civic reporting, emergency SOS, city services, healthcare, tourism, jobs and
road/vehicle services into one place, with role-based dashboards.

---

## Setup (required before building)

Secrets are **not** stored in this repository. After cloning:

1. **Firebase config** – download `google-services.json` from your Firebase console
   (Project settings → Your apps → Android) and place it at `app/google-services.json`.
   A placeholder is provided in `app/google-services.json.example`.
2. **MapTiler API key** – create `local.properties` in the project root
   (see `local.properties.example`) and add:

   ```
   MAPTILER_API_KEY=your_key_here
   ```

   (An environment variable named `MAPTILER_API_KEY` also works, e.g. for CI.)
3. Open in Android Studio and run.

`google-services.json` and `local.properties` are git-ignored – **never commit them**.

> If Gradle fails with an invalid Java home error, open `gradle.properties` and remove or
> edit the `org.gradle.java.home=...` line so it points to a JDK on your machine.

---

## Features

| Area | What it does |
|---|---|
| **Authentication & roles** | Email/password login, Citizen and Service Provider registration, Admin role, auto-login routing from the splash screen |
| **Civic issue reporting** | Report city problems with category, location, and photo; track your reports; admin review and status updates |
| **Emergency SOS** | Emergency requests with admin monitoring and management |
| **City map** | osmdroid map with MapTiler tiles (light/dark), place search, and OSRM-based routing |
| **Public projects** | Public infrastructure projects with admin-managed progress updates |
| **City services** | Browse electricians, plumbers, cleaners and more; book a service; provider workspace; request history and ratings; admin oversight |
| **Healthcare** | Hospitals, clinics, pharmacies and emergency hotlines; blood donor directory and registration |
| **Tourism** | Landmarks and heritage places with offline bookmarks and route launch |
| **Jobs** | Job listings, applications, application tracking; admin job publishing and applicant review |
| **Vehicle & road** | Vehicle service directory, parking information, road alerts, pothole reporting shortcut |
| **Notifications** | Firebase Cloud Messaging with in-app notification center |
| **Offline support** | Local SQLite storage alongside Cloud Firestore sync |

Some directories (hospitals, parking, road alerts, etc.) use clearly labelled **demo data**.

---

## Tech stack

- **Language / UI:** Java 8, XML layouts, View Binding
- **Backend:** Firebase Authentication, Cloud Firestore, Realtime Database, Storage, Cloud Messaging
- **Maps:** [osmdroid](https://github.com/osmdroid/osmdroid) with MapTiler tiles, OSRM routing
- **Local storage:** SQLite
- **Build:** Gradle 7.3.3, Android Gradle Plugin 7.2.0-rc02
- **SDK:** minSdk 21, targetSdk / compileSdk 32

---

## Project structure

```
app/                      Android application module
  src/main/java/...       Activities, services, helpers
  src/main/res/           Layouts, drawables, values
  google-services.json.example   Template for your Firebase config
firestore.rules           Firestore security rules
firestore-rules-tests/    Security rules tests (Node + Firebase emulator)
local.properties.example  Template for local secrets
```

---

## Firestore security rules tests

Requires Node.js and the Firebase CLI.

```bash
cd firestore-rules-tests
npm install
npm test            # rewards rules
npm run test:admin  # admin / jobs / service-request rules
```

---

## Security notes

- Do not commit `google-services.json`, `local.properties`, keystores or `.env` files.
- Restrict your Firebase API key to this app (package name + SHA-1) in Google Cloud Console.
- If a key is ever exposed, rotate it immediately.

---

## License

Add a license of your choice here.
