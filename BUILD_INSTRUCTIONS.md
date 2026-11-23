# Build and installation

Install JDK 17 and Android SDK Platform 35 + build tools, accept SDK licenses, and set `ANDROID_HOME` or create `local.properties` with `sdk.dir=/path/to/sdk`. Android Studio: open the repository and sync Gradle. VS Code: use a JDK 17 terminal with Android SDK installed.

```sh
./gradlew assembleDebug
adb install app/build/outputs/apk/debug/app-debug.apk
./gradlew assembleRelease
```

Release currently produces an **unsigned** APK at `app/build/outputs/apk/release/`. Configure an organization-controlled Gradle signingConfig using secrets outside source control and use a secure CI signing workflow before distribution. Never commit keys or passwords. Release build success does not imply production readiness.

The Gradle wrapper targets Gradle 8.9. Downloading dependencies needs access to Google Maven, Maven Central, and Gradle distributions. These hosts and the JDK/SDK are not available in this sandbox, so APK compilation cannot be verified here.

For on-device discovery, install on a real Android phone connected to a private IPv4 Wi-Fi subnet. The Android emulator's virtual network may not expose other LAN devices. `ACCESS_NETWORK_STATE` and `INTERNET` are needed for link data and local TCP sockets; location permission is not requested, so SSID is unavailable.
