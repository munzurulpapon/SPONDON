# SPONDON – Smart City App (Android)

Native Java Android app (Firebase + osmdroid).

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

`google-services.json` and `local.properties` are git-ignored – never commit them.
