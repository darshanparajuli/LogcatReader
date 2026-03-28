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

# Release guide

1. Suggest a version name based on recent changes (use semantics versioning) and confirm with the
   user
2. Bump the version and versioncode numbers, and commit the change
3. Add a release tag in format `vX.X.X`
4. Run `./gradlew assembleRelease`
5. A release apk should've been generated in `app/build/outputs/apk/release`
6. Push release branch (including the tag) after confirming with the user
7. Create a new GitHub release with version name as the title after confirming with the user
    * Add a brief summary of changes, any new contributors, and a link to the full changelog (
      commits range) in the release notes
    * Upload the apk to this release
