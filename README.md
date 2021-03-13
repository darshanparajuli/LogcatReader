<img src="/app/playstore_images/launcher_icon.png" width="192px" />

# Logcat Reader [![Build Status](https://travis-ci.org/darshanparajuli/LogcatReader.svg?branch=master)](https://travis-ci.org/darshanparajuli/LogcatReader)

A simple app for viewing logs on an android device.

<a href="https://f-droid.org/packages/com.dp.logcatapp/" target="_blank">
<img src="https://f-droid.org/badge/get-it-on.png" alt="Get it on F-Droid" height="80"/></a>
<a href='https://play.google.com/store/apps/details?id=com.dp.logcatapp'><img alt='Get it on Google Play' src='https://play.google.com/intl/en_us/badges/images/generic/en_badge_web_generic.png' height="80"/></a>

## Screenshots
<img src="/app/playstore_images/screenshots/screenshot-1.png" width="300px" /> <img src="/app/playstore_images/screenshots/screenshot-2.png" width="300px" />

## Usage

Use ADB to grant `android.permission.READ_LOGS` to LogCatReader.

```sh
adb shell "pm grant com.dp.logcatapp android.permission.READ_LOGS && am force-stop com.dp.logcatapp"
```

## Contributing

Pull requests are welcome! Please use [Square's code style](https://github.com/square/java-code-styles) for formatting. 🙏
