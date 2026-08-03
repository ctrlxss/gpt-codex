# Core System

An Android app meant to grow into one place for everything — starting with your workday.

## Modules

### Workday progress

- **Today screen** — animated progress bar with a shimmer sweep, big percentage, and a live
  `h:mm:ss` countdown until you're finished.
- **Patients left** — counts how many patients are still ahead of you, based on your remaining
  working time and a configurable slot length (default **20 minutes per patient**).
- **Week planner** — a calendar-style Mon–Fri grid (time axis on the left) with a red "now" line
  showing the current time. Each day holds any number of work blocks: tap free space to add one,
  tap a block to edit — start and end are typed into simple time fields (8:00, 0800, 14:30 all
  work). Gaps between blocks are breaks — so "morning block, 3 h break, 2 patients, 1 h break,
  3 patients" works naturally. Everything is saved on device.
- Phase-aware: "work starts in…", live progress while working, paused progress between blocks
  ("break — next block in…"), and a done state. Days without blocks show a day-off screen.

### Attendance & hours

- **I'm sick today** button — marks the day as a sick day (tap again to undo).
- **Clock out** button — clock-in is automatic from your schedule; press clock out when you go
  home (early or on time) and the day's hours are counted up to that moment.
- **Hours & attendance** screen — per month: hours worked vs planned, days worked, sick days,
  and early leaves, with a browsable month-by-month list of special days.

### Coming soon

The side menu is the home for future modules: reminders, tasks, plans & ideas.

## Install

Download `apk/core-system-v3.0.apk` onto your phone and open it
(you may need to allow "install from unknown sources"). Requires Android 8.0+.
Installs as an update over the previous "Workday" version.

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
