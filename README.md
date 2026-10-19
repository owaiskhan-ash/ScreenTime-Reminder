# Screen Time Reminder

A simple Android app by [Just Simple Apps](https://www.justsimpleapps.online/).
It reads the phone's own screen-time record, watches continuous sessions, and
sends one calm nudge to step away.

- Live screen time, unlocks and top apps, straight from `UsageStatsManager`
- Break reminders after 30 / 45 / 60 / 90 minutes of continuous use
- Daily limit, seven-day history and a five-minute break timer
- Pure Light and Deep Dark themes
- Free. No ads. No tracking. No account. Nothing leaves the phone.

## Build

```bash
./gradlew assembleDebug
```

The APK lands in `app/build/outputs/apk/debug/`. GitHub Actions also builds it on
every push and attaches it to the rolling [`apk-latest`](https://github.com/owaiskhan-ash/ScreenTime-Reminder/releases/tag/apk-latest) release.

## Permissions

- **Usage access** — required to read screen time. Android opens this settings page; the app never sees content, messages or app data.
- **Notifications** — for break reminders.
- **Foreground service** — a quiet ongoing notice keeps the monitor alive while the app is closed. Data stays on the device.

The `preview/` folder contains a dependency-free browser prototype of the same
interface, used for design review.
