# Cyberspace

![GitHub License](https://img.shields.io/github/license/IamAndelib/cyberspace-apk?style=flat-square&logoColor=white&labelColor=black&color=white)
![GitHub tag](https://img.shields.io/github/v/tag/IamAndelib/cyberspace-apk?style=flat-square&logoColor=white&labelColor=black&color=white)

Android client for [beta.cyberspace.online](https://beta.cyberspace.online).

## Download

[<img src="https://img.shields.io/badge/Download%20APK-Latest%20Release-white?style=for-the-badge&logoColor=black&labelColor=black" height="48">](https://github.com/IamAndelib/cyberspace-apk/releases/latest)

> Enable **Install from unknown sources** in your Android settings before installing.

## Features

- Full-screen WebView with edge-to-edge display
- Pull-to-refresh with loading spinner
- Smooth fade-in on cold start (no blank screen flash)
- Loading indicator on every navigation
- Offline detection with retry page
- File upload and download support
- GPU-accelerated rendering

## Installation

Download the latest APK from the [Releases](https://github.com/IamAndelib/cyberspace-apk/releases/latest) page and install it on your Android device (API 21+).

## Building

Requires Java 17 and Android SDK.

```bash
git clone https://github.com/IamAndelib/cyberspace-apk.git
cd cyberspace-apk
export JAVA_HOME=/usr/lib/jvm/java-17-openjdk
./gradlew assembleRelease
```

## License

[MIT](./LICENSE)
