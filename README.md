
# ClearKeep Android

Clearkeep is a free and open source software application for Android, iOS, that employs end-to-end encryption (E2EE) to keep communications safe.

Communications can be made between either individuals or a group, allowing users to send end-to-end encrypted text, picture, as well as engage in live video and audio calls.

Default end-to-end encryption scaling to thousands of users per room. We protects against accidental leaks, eavesdroppers and third party interference by ensuring that by default the server cannot see your conversations.

## What works

- Multi-server peer, group chat and call
- End-to-end message encryption
- Upload files and images to chat
- User status and avatar
- Social account login
- SRP protocol implementation
- Multi-device support
- 2 Factors Authorization (2FA) via phone number

## Prerequisites

- Git
- [Android Studio Android Studio Arctic Fox | 2020.3.1 Canary 10](https://developer.android.com/studio/archive)
- Android SDK Platform 30 (Android 11)
- JDK 8 or higher
- Android OS from Android 8

## Build & Run

1. Git clone source code    
   `git clone git@github.com:ClearKeep/ck-android.git`  
   or  
   `git clone https://github.com/ClearKeep/ck-android.git`
2. Using Android Studio import this project
3. Run app in Android Studio

## Dependencies

- Android Jetpack (Compose, Hilt, Room, Architecture Components)
- Kotlin Coroutines
- Protobuf Java Lite
- [Firebase Cloud Messaging](https://firebase.google.com/docs/cloud-messaging) for push notifications
- [CSRP](https://github.com/cocagne/csrp) for SRP protocol implementation
- [Janus Gateway Android](https://github.com/benwtrent/janus-gateway-android) for video and voice call
- [Signal Protocol Library Java](https://github.com/signalapp/libsignal-protocol-java) for end-to-end message encryption