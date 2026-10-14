# Baagbaan Boi (বাঘবান বই)

Baagbaan Boi is an agricultural management and direct messaging app for farmers and contractors, featuring real-time chat and audio/video calling.

## Calling Setup (ZegoCloud)

The calling feature utilizes ZegoCloud UIKit Prebuilt Call with offline push support.

### Configuring Credentials

To enable live audio and video calls, provide your ZegoCloud `AppID` and `AppSign`. The app checks `gradle.properties` (project or user home) and environment variables at build time:

1. **User Gradle Properties (Recommended for local dev):**
   Add your credentials to `~/.gradle/gradle.properties` so they are not committed to source control:
   ```properties
   ZEGO_APP_ID=YOUR_ZEGO_APP_ID
   ZEGO_APP_SIGN=YOUR_ZEGO_APP_SIGN
   ```

2. **Environment Variables (Recommended for CI/CD):**
   Set environment variables in your build environment:
   ```bash
   export ZEGO_APP_ID=YOUR_ZEGO_APP_ID
   export ZEGO_APP_SIGN=YOUR_ZEGO_APP_SIGN
   ```

3. **Project Gradle Properties:**
   You can also specify them in the project's `gradle.properties`:
   ```properties
   ZEGO_APP_ID=YOUR_ZEGO_APP_ID
   ZEGO_APP_SIGN=YOUR_ZEGO_APP_SIGN
   ```

> **Note:** `.env` is gitignored and the Secrets Gradle Plugin only manages Gemini API keys; `ZEGO_` keys are read directly by Gradle from Gradle properties or environment variables into `BuildConfig`. If these credentials are left empty or omitted, live calling features are gracefully disabled and a configuration dialog will guide you when placing a call.

## Building

```bash
gradle :app:assembleDebug
```
