<img src="/app/playstore_images/launcher_icon.png" width="192px" />

# Logcat Reader

A simple app for viewing logs on an android device.

<a href="https://f-droid.org/packages/com.dp.logcatapp/" target="_blank">
<img src="https://f-droid.org/badge/get-it-on.png" alt="Get it on F-Droid" height="80"/></a>
<a href='https://play.google.com/store/apps/details?id=com.dp.logcatapp'><img alt='Get it on Google Play' src='https://play.google.com/intl/en_us/badges/images/generic/en_badge_web_generic.png' height="80"/></a>

## Features

* Nice UI (open an issue if you disagree :P)
* Record, save, share or export `logcat` logs
* Filter logs by app (or package name), tag, message, priority, pid, tid, date, time, etc.
    * Message, tag, and package name support regex
* Robust search functionality with highlighting and regex support
* Various display options (compact mode, for example)

## Screenshots

<img src="/app/playstore_images/screenshots/dark_mode.png" width="300px" /> <img src="/app/playstore_images/screenshots/light_mode.png" width="300px" />
<img src="/app/playstore_images/screenshots/search.png" width="300px" /> <img src="/app/playstore_images/screenshots/compact_view.png" width="300px" />

## Pre-requisite

Use ADB to grant `android.permission.READ_LOGS` to Logcat Reader.

```sh
adb shell "pm grant com.dp.logcatapp android.permission.READ_LOGS && am force-stop com.dp.logcatapp"
```

## Contributing

Pull requests are welcome! Please
use [Square's code style](https://github.com/square/java-code-styles) for formatting. 🙏
