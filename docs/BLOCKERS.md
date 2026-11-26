# Blockers Log

## Spec documents missing
- **Date:** 2026-10-26
- **Issue:** `docs/android_driver_app_prompt.md`, `docs/pflichtenheft_v1.0.pdf`, `docs/wireframe_driver_app.html`, and `docs/quarkus_backend_prompt.md` are not in this repository.
- **Proposed workaround:** Implement from the driver-app prompt pasted into the session. Mocks follow that contract (camelCase JSON, the error envelope, and the listed paths).
- **Status:** Continued. No PDF was invented.

## MQTT-00 — local Mosquitto round trip
- **Date:** 2026-10-26
- **Issue:** No Docker Mosquitto broker was started, so there is no live broker integration test.
- **Proposed workaround:** `MqttConnectionManagerTest` drives a fake gateway for connect, retry, will, and message forwarding.
- **Status:** Unit coverage stands in for the Docker round trip.

## MQTT-00 — broker address
- **Date:** 2026-11-18
- **Issue:** The broker host and port are not in the APK. The MQTT client stays disconnected until `adb shell am start -n ao.ridehailing.driver/.MainActivity -e mqtt_host "<ipv4>" -e mqtt_port "1883"` has been run once on that device.
- **Status:** The extras are stored and reused. Going online after that command uses the saved broker.

## MQTT-00 — certificate pin
- **Date:** 2026-10-26
- **Issue:** `MQTT_CERT_PIN` is empty. HiveMQ is connected with the platform TLS trust store via `sslWithDefaultConfig()`.
- **Proposed workaround:** Set `MQTT_CERT_PIN` when the Luanda broker certificate is available and pin it in the socket factory.
- **Status:** Open. TLS is still on for `ssl://` URLs.

## D-05 — Google Maps Navigation SDK
- **Date:** 2026-10-26
- **Issue:** The Navigation SDK is not on Maven Central.
- **Proposed workaround:** `NavigationPanel` shows the ETA and an external-maps action. `RouteClient` is the routing port. `MockRouteClient` returns a 600 second leg until OSRM is called from a signed build.
- **Status:** Continued with the port and the panel.

## D-07 — WebRTC peer connection
- **Date:** 2026-10-26
- **Issue:** `org.webrtc:google-webrtc` is not the artifact on this classpath. `io.github.webrtc-sdk:android` is present, but a two-emulator handshake was not run.
- **Proposed workaround:** `CallViewModel` publishes the MQTT call request, reads `{callId, signallingUrl, proxyNumber}`, and opens `WebSocketSignalling`. Media is a `WebRtcCall` stand-in. DTLS-SRTP stays inside libwebrtc when a real `PeerConnection` is attached. No `MediaRecorder` in the call or webrtc packages.
- **Status:** State machine and signalling handoff are tested. The handshake remains open.

## D-01 — WorkManager 5 second poll
- **Date:** 2026-10-26
- **Issue:** Periodic work cannot run every 5 seconds. The platform minimum is 15 minutes.
- **Proposed workaround:** `WorkOnboardingSchedule` enqueues a one-time worker with a 5 second delay. `OnboardingViewModel.poll` performs the status read while the screen is open.
- **Status:** Continued. The worker does not itself call the API.

## D-02 — device location
- **Date:** 2026-10-26
- **Issue:** This session does not read the device location provider.
- **Proposed workaround:** `IntervalPulse` fires every 5 seconds while online. The published point is `lat` 0, `lng` 0 until a real provider is passed into that block.
- **Status:** Subscription lifecycle is tested. Coordinates are a placeholder.

## Compose UI tests
- **Date:** 2026-10-26
- **Issue:** Document upload, chat send, and navigation panel were not executed as Compose instrumentation tests.
- **Proposed workaround:** ViewModel tests cover those flows. Screens are wired in `AppRoot`.
- **Status:** Open.

## ktlint and detekt
- **Date:** 2026-10-26
- **Issue:** Neither plugin is configured in this project.
- **Proposed workaround:** `./gradlew lint` is the static gate and reports zero errors.
- **Status:** Open. Android lint passed.
