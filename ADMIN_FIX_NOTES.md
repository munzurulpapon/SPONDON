# Admin fixes — Publish Job & Monitor Citizen Service Requests

## What was wrong (from the code)
**Monitor Citizen Service Requests** (`AdminServiceRequestsActivity`)
- Query used `orderBy("createdAt")` — silently drops every request without that field and is the
  first query to fail when rules differ. (`ProviderRequestsActivity` had already dropped it.)
- Fields were read with `getString()`, which throws on any non-text value and takes the screen down.
- Any failure showed only a generic toast, hiding the real reason (e.g. `PERMISSION_DENIED`).

**Publish Job** (`ManageJobsActivity`) / **Jobs Opportunities** (`JobsActivity`)
- Publish gave no feedback when offline (Firestore queues the write; button stayed disabled forever)
  and a retry could create duplicate postings.
- `JobsActivity` swallowed listener errors silently (a permission problem looked like "job never
  appeared"), read fields unsafely, and showed new jobs in arbitrary order / blank descriptions.
- `jobs` rules let *any* signed-in user write jobs.

## What changed
- `FirestoreUi.java` (new): never-throwing field readers, readable Firestore error messages, online check.
- `AdminServiceRequestsActivity` + `activity_admin_service_requests.xml`: no `orderBy`, device-side
  newest-first sort, safe parsing, on-screen error with the real reason + **Retry** button, offline-copy
  indicator, provider/time shown, failure feedback on status override.
- `ManageJobsActivity`: online/sign-in check, one stable document id per posting (retry = same doc,
  no duplicates), 20 s watchdog so the button never stays stuck, real error text; candidate list no
  longer uses `orderBy` and shows errors.
- `JobsActivity`: errors surfaced, safe parsing, newest published job first, blank fields get readable defaults.
- `firestore.rules`: `isAdmin()` helper (`users/{uid}.role == 'Admin'`); `jobs` are readable by any
  signed-in user, but only admins may create/update/delete (with title/company/location required).
  `service_requests` rules are unchanged (providers query the whole pool, so reads must stay open to
  signed-in users) and are now documented.
- `firestore-rules-tests/admin_jobs_requests.test.js` + `npm run test:admin` script.

## You must do: publish the rules
The app cannot change server rules. Until the rules in the Firebase project match `firestore.rules`,
the live project may still deny these actions.
- Console: Firestore Database -> Rules -> paste `firestore.rules` -> Publish, **or**
- CLI (project root): `firebase deploy --only firestore:rules --project spondon-a07ec`

## Build
`gradlew.bat assembleDebug` (Windows) or Android Studio: Build > Build Bundle(s)/APK(s) > Build APK(s).
Output: `app/build/outputs/apk/debug/app-debug.apk`.

## What was and was not verified (authoring environment had no Android SDK / Gradle / Firebase access)
- Verified: all XML parses; every `R.id` used exists; changed Java compiles with `javac --release 8`
  against the Android API 32 jar using hand-written stubs of the Firebase/AndroidX classes (0 errors,
  0 project warnings); helper logic unit-tested.
- NOT verified: real Gradle build / APK, behaviour against the live Firebase project, and the Firestore
  rules (emulator unavailable). Run `cd firestore-rules-tests && npm install && npm run test:admin`.
