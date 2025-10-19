# Logcat Reader

### What?

Logcat Reader is an android app tool that allows access to messages from applications and services
running on an Android device. Most Android developers depend heavily on logcat (and adb) during
development to debug their applications.

Logcat Reader merges different logging information available from different Android subsystems.
This app was developed to help simplify the processes involved for developers and app testers in
getting android log information.

Logcat Reader provides following log information:

#### Priority (this indicates the severity of a log message):

* Verbose
* Debug
* Info
* Warning
* Error
* Assert

#### Tag

This identifies the software component that generated the message, which is being used to filter
messages.

#### The actual message

A new line character is added to the end of every messages.

### Goal

The primary goal of Logcat Reader is to allow viewing device logs directly on the phone. It is
completely up to the user what they want to use these logs for.

### Thank you

Thank you for using Logcat Reader, for any questions check out FAQ file on this repository.