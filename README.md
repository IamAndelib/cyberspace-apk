# Cyberspace Android

Android client for [beta.cyberspace.online](https://beta.cyberspace.online).

## Download

**[⬇ Download Cyberspace-v1.3.apk](https://github.com/IamAndelib/cyberspace-apk/releases/download/v1.3/Cyberspace-v1.3.apk)**

Or browse all releases → [Releases](https://github.com/IamAndelib/cyberspace-apk/releases)

> Enable **Install from unknown sources** in your device settings before installing.

---

## Features

- Full-screen WebView wrapper for beta.cyberspace.online
- Edge-to-edge display (draws behind status bar and navigation bar)
- Pull-to-refresh with loading spinner
- Offline detection with a retry page
- File upload and download support
- Hardware-accelerated rendering

## Requirements

- Android 5.0 (API 21) or higher

## Release history

| Version | APK | Notes |
|---------|-----|-------|
| v1.3 | [Cyberspace-v1.3.apk](https://github.com/IamAndelib/cyberspace-apk/releases/download/v1.3/Cyberspace-v1.3.apk) | Startup speed & navigation feel improvements |
| v1.2 | [Cyberspace-v1.2.apk](https://github.com/IamAndelib/cyberspace-apk/releases/download/v1.2/Cyberspace-v1.2.apk) | Performance and correctness improvements |
| v1.1 | [Cyberspace-v1.1.apk](https://github.com/IamAndelib/cyberspace-apk/releases/download/v1.1/Cyberspace-v1.1.apk) | Initial release |

## Build from source

Requires Java 17 and Android SDK.

```bash
git clone https://github.com/IamAndelib/cyberspace-apk.git
cd cyberspace-apk
export JAVA_HOME=/usr/lib/jvm/java-17-openjdk
./gradlew assembleRelease
```
