# Condutor

Driver app for the Luanda ride-hailing pilot. Phase 1 is the project skeleton and phone OTP sign-in.

## Run

```
JAVA_HOME=/Library/Java/JavaVirtualMachines/temurin-21.jdk/Contents/Home ./gradlew :app:testDebugUnitTest :app:assembleDebug
```

`BASE_URL` comes from `gradle.properties` and is written into `BuildConfig`. The placeholder is `https://api.example.ao/v1/`. Do not put secrets in the repo. `local.properties` holds the SDK path and is gitignored.

There is no `google-services.json` yet. Firebase auto-init is removed so the process can start. The FCM token is requested only after Firebase is configured.
Yet google service is not needed
## Phase 1

Phone entry accepts an Angolan mobile number (`9` plus 8 digits, with or without `+244`). OTP verify stores the access token in `EncryptedSharedPreferences` and opens the home stub. Other routes exist as titles only.

Communicates with passenger and needs a MQTT broker to run
