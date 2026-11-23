# Build and install

Install JDK 17 and Android SDK Platform 35 + build tools; accept SDK licenses. Set `ANDROID_HOME` or `local.properties` with `sdk.dir=/path/to/sdk`. Open the repository in Android Studio or use a VS Code terminal with the toolchain installed.

```sh
./gradlew assembleDebug
adb install app/build/outputs/apk/debug/app-debug.apk
./gradlew assembleRelease
```

Release generates an unsigned APK. Configure signing using organization-managed secrets outside source control before distribution. No real signing keys are provided.

A real phone on a private IPv4 Wi-Fi subnet is needed for meaningful discovery; an emulator may not see LAN devices. No location permission is requested, so SSID is unavailable. This sandbox lacks Java and the Android SDK and cannot access Gradle/Android artifact hosts; APK compilation could not be verified here.
