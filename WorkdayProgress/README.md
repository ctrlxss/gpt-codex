# Workday Progress

A small Android app that shows how far along your workday is — like a loading bar for your day.

## Features

- **Today screen** — animated progress bar with a shimmer sweep, big percentage, and a live
  `h:mm:ss` countdown until you're finished.
- **Patients left** — counts how many patients are still ahead of you, based on your remaining
  working time and a configurable slot length (default **20 minutes per patient**).
- **Mon–Fri schedule** — set work start/end and break start/end per weekday; days can be toggled
  off. Everything is saved on the device.
- Phase-aware: shows "work starts in…", pauses progress during your break ("break ends in…"),
  and celebrates when you're done. Weekends show a day-off screen.

## Install

Download `apk/workday-progress-v1.0.apk` onto your phone and open it
(you may need to allow "install from unknown sources"). Requires Android 8.0+.

## Build

```bash
export ANDROID_HOME=/path/to/android-sdk
gradle assembleRelease
# APK lands in app/build/outputs/apk/release/app-release.apk
```

Kotlin + Jetpack Compose, no third-party dependencies.

> Note: `keystore/workday.keystore` is a throwaway self-signing key (password in
> `app/build.gradle.kts`) so that updates install over the old version. Fine for personal
> sideloading — generate your own key before ever publishing to a store.
