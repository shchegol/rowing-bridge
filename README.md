# Rowing Bridge

Android app that connects to a BLE FTMS rowing machine (and, separately, a Garmin watch's broadcast heart rate) and writes a real `.FIT` activity file — bypassing a Garmin Connect IQ limitation where Garmin Connect silently discards app-supplied values for standard metrics (distance, cadence, power, etc.) written via `ActivityRecording.Session`/`FitContributor`.

## What it does

- Scans for and connects to a BLE FTMS rower (Bluetooth SIG Fitness Machine Service), parsing the full Rower Data characteristic.
- Optionally connects to a Garmin watch's "Broadcast Heart Rate" (standard BLE Heart Rate Service) for live and recorded heart rate.
- Records a workout (Start/Pause/Resume/Save/Discard) with self-computed averages/maxima where the rower doesn't report its own.
- Encodes a proper `.FIT` file via the official [Garmin FIT SDK](https://developer.garmin.com/fit/overview/) (`com.garmin:fit`) and saves it to Downloads, ready for manual import into Garmin Connect.
- Optional Strava upload (manual button, OAuth2) — currently disabled pending a paid Strava API plan; wire real credentials into `local.properties` to enable it.
- Settings: light/dark/system theme, in-app language override (English/Русский/Español).

## Building

Requires Android Studio, JDK 17, and the Android SDK. Open the project and sync Gradle. See `local.properties` for the (optional, currently unused) Strava API credential placeholders.
