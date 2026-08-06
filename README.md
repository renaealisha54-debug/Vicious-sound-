# Vicious Sound

Part of the [Vicious](https://github.com/renaealisha54-debug) ecosystem.

Audio sonification module for system health monitoring. Vicious Sound listens for
periodic "heartbeats" from a running process and generates a continuous tone that
shifts in real time to reflect its current state — turning silent failures into
something you can hear.

## How it works

- **Watchdog** — tracks the time since the last heartbeat and the reported loop
  speed, classifying state as `HEALTHY`, `WARNING`, `CRITICAL`, or `CRASHED`.
- **AudioEngine** — synthesizes a live waveform on a background thread based on
  the current state:
  - `HEALTHY` — a clean sine tone at the base frequency
  - `WARNING` — a raised-pitch sine tone, pulsing faster
  - `CRITICAL` — a blended square wave + noise, pulsing rapidly
  - `CRASHED` — a descending sine sweep from high to low pitch
- **MainActivity** — a simple Compose UI for manually triggering each state and
  running a full self-test sequence through all four.

## Requirements

- Android Studio (Kotlin, Jetpack Compose)
- minSdk 26, targetSdk / compileSdk 34
- JDK 17

## Building

```
./gradlew assembleDebug
```

## Status

Early-stage / self-test build. Core watchdog and audio engine are functional;
integration with other Vicious modules for real heartbeat signals is in progress.

## License

Licensed under the GNU General Public License v3.0 — see [LICENSE](LICENSE).

Copyright (c) 2026 Alisha Bevis

## License

Copyright (c) 2026 Alisha Bevis. All rights reserved.
