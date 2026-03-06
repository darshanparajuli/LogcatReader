# Project

The Android project has four gradle modules: app, collections, logcat, and logger. The app module
contains most of the application code, and the logcat module contains all the logic for running
a logcat session and streaming logs.

In the app module, screens live in com.dp.logcatapp.ui.screens package and activities hosting these
screens live in com.dp.logcatapp.activities package. The app uses one activity per screen.

# Pull requests

When opening a pull request, include the following information:
```
# Summary of changes

<summary>

# Why 

<reason for making this change>

# Testing

<how are the changes getting tested>

# Demo

<any demo or screenshots showcasing the change, if applicable>

```

# Release

1. Suggest a version name based on recent changes (use semantics versioning) and confirm with the user
2. Create a new release branch with name `release-<version>`
3. Bump the version and versioncode numbers
4. Commit the change once the user either provides or agrees with the new version name
5. Run `./gradlew assembleRelease`
6. A release apk should've been generated in `app/build/outputs/apk/release`
7. Add a release tag in format `vX.X.X`
8. Push the changes (including the tag)
9. Create a new Github release with version name as the title
    * Add a brief summary of changes, any new contributors, and a link to the full changelog (commits range) in the release notes
    * Upload the apk to this release
