# TV Remote — Android app

A real Android app (Kotlin + Jetpack Compose) that turns your phone into a
remote for an **Android TV / Google TV** device on the same Wi-Fi network —
built from the provided `tv-remote.html` design.

## How it actually controls the TV

There's no "universal remote API" for arbitrary smart TVs, so this app uses
the same mechanism real ADB-based remote apps use:

1. Android TV / Google TV boxes (Chromecast with Google TV, Nvidia Shield,
   Mi Box, Google TV Streamer, most Android-based smart TVs) expose
   **Settings → System → Developer options → Network debugging**. Turning
   this on opens the standard ADB daemon on `tcp/5555` — no root needed.
2. The app connects straight to that port using
   [`dadb`](https://github.com/mobile-dev-inc/dadb), a pure-Kotlin ADB
   client — no `adb` binary, no PC, no ADB server.
3. Every button (power, back, home, volume, the touchpad, app shortcuts…)
   runs a real `input keyevent` / `am start` / `monkey` shell command on the
   TV, exactly like the official Android TV remote app does.
4. The first time you connect, the TV will show an **"Allow debugging?"**
   pop-up with a fingerprint — accept it once per TV, same as `adb connect`
   on a PC.

### One-time setup on the TV
1. Settings → System → About → tap "Build" ~7 times to unlock Developer
   options.
2. Settings → System → Developer options → turn on **Network debugging**
   (also called "ADB debugging" on some boxes).
3. Note the TV's IP address (Settings → Network) — or just let the app scan
   for it, see below.

### In the app
- Tap the TV name at the top → the app scans your phone's Wi-Fi subnet for
  any device with port 5555 open and lists it.
- Tap the TV you want → accept the pairing prompt on the TV screen (first
  time only) → you're connected.

## What's real vs. approximated
- Power, back, home, recents, menu, volume, mute, app shortcuts (YouTube /
  Netflix / Prime Video / Settings) → **real ADB key events / intents.**
- Touchpad → drags are converted into DPAD steps (works on every Android TV
  launcher and app); this is the same approach most third-party remote apps
  use, since Android TV has no generic mouse-pointer protocol over ADB.
- Mic / Assistant → opens the voice assistant intent; TV must have Google
  Assistant configured to actually listen.
- App package names (`com.netflix.ninja`, `com.google.android.youtube.tv`,
  `com.amazon.amazonvideo.livingroom`) are the common Android TV package
  IDs — if a TV uses a different build, that shortcut just won't launch
  anything (safe no-op).

## Project structure
```
app/src/main/java/com/tvremote/app/
├── MainActivity.kt
├── RemoteViewModel.kt        # app state + button actions
├── adb/
│   ├── AdbRemoteClient.kt    # talks real ADB protocol to the TV
│   └── TvDiscovery.kt        # scans Wi-Fi subnet for port 5555
└── ui/
    ├── RemoteScreen.kt
    ├── components/           # TopBar, VolumeSlider, AppsRow, Touchpad, BottomControls
    └── theme/                # colors ported from the original design
```

## Build it
1. Open this folder in **Android Studio** (Iguana or newer) — it will fetch
   the Gradle wrapper and sync automatically.
2. Run on a phone (minSdk 26 / Android 8+) on the **same Wi-Fi** as the TV.
3. Turn on Network debugging on the TV (above), tap the TV name in the app,
   pick your TV, accept the prompt on the TV.

## Push this to your own GitHub repo
This folder is already a git repo with an initial commit. To push it:

```bash
cd TVRemoteApp
# create an empty repo on GitHub first (no README/.gitignore, so it stays empty), then:
git remote add origin https://github.com/<your-username>/<your-repo>.git
git branch -M main
git push -u origin main
```

If you use SSH instead of HTTPS, use
`git@github.com:<your-username>/<your-repo>.git` for the remote URL.

## Known limitations
- Network debugging must be manually enabled on the TV; there's no way
  around that for any app that doesn't ship as an official manufacturer
  remote (Samsung/LG use different, proprietary protocols not covered here).
- The subnet scan checks all 254 addresses on your phone's /24 — fast on
  most home routers, but works only if the phone and TV share the same
  Wi-Fi subnet (true for basically all home networks).
