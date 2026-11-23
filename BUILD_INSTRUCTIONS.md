# Build and install

Install JDK 17 and Android SDK Platform 35 + build tools; accept SDK licenses. Set `ANDROID_HOME` or `local.properties` with `sdk.dir=/path/to/sdk`. Open the repository in Android Studio or use a VS Code terminal with the toolchain installed.

```sh
./gradlew assembleDebug
adb install app/build/outputs/apk/debug/app-debug.apk
./gradlew assembleRelease
```

Release generates an unsigned APK. Configure signing using organization-managed secrets outside source control before distribution. No real signing keys are provided.

A real phone on a private IPv4 Wi-Fi subnet is needed for meaningful discovery; an emulator may not see LAN devices. No location permission is requested, so SSID is unavailable. This sandbox lacks Java and the Android SDK and cannot access Gradle/Android artifact hosts; APK compilation could not be verified here.

## Windows / Visual Studio Code (PowerShell)

1. Install **JDK 17** and Android Studio (for the SDK; you may still write code in VS Code). In Android Studio's SDK Manager, install **Android SDK Platform 35**, **Android SDK Build-Tools**, **Platform-Tools**, and **Command-line Tools**. Accept licenses in SDK Manager. Internet access to Google Maven, Maven Central, and Gradle distributions is needed for the first build.
2. Set `JAVA_HOME` to your JDK 17 folder and add `%JAVA_HOME%\bin` to PATH. Restart VS Code. Verify with `java -version` (it should report 17).
3. In VS Code, **File → Open Folder → `C:\InfraGuard-main`**, open a PowerShell terminal, and run:

```powershell
cd C:\InfraGuard-main
java -version
Test-Path .\gradle\wrapper\gradle-wrapper.jar
```

4. If Android Studio installed the SDK in the default location, create an untracked `local.properties` file in `C:\InfraGuard-main` with this single line (substitute your Windows username if necessary):

```properties
sdk.dir=C:/Users/YOUR_USERNAME/AppData/Local/Android/Sdk
```

Alternatively set `ANDROID_HOME` to the SDK directory. Then run:

```powershell
.\gradlew.bat assembleDebug
```

The debug APK should be at `C:\InfraGuard-main\app\build\outputs\apk\debug\app-debug.apk`. You can copy it to a phone and install it after allowing installation from your file manager, or use USB debugging and:

```powershell
& "$env:LOCALAPPDATA\Android\Sdk\platform-tools\adb.exe" install -r .\app\build\outputs\apk\debug\app-debug.apk
```

If PowerShell cannot find the launcher, check that you downloaded a revision containing `gradlew.bat`; older ZIP archives do not include it. If Gradle says the SDK is missing, fix `local.properties`; if it says Java is missing/wrong, fix `JAVA_HOME`. If Gradle cannot download dependencies, check internet/proxy access. **The APK has not been compiled in this sandbox**, so if a Kotlin compilation error occurs, share the complete first error and it can be fixed.
