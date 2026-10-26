# Engineering Decisions Log

## MQTT library choice — HiveMQ vs Eclipse Paho
- **Date:** 2026-10-26
- **Decision:** HiveMQ MQTT Client `com.hivemq:hivemq-mqtt-client:1.3.3`
- **Rationale:** The 1.3 line resolves from Maven Central and is maintained. The Android Paho service is a poor fit for Android 14 foreground-service rules.
- **Alternatives considered:** Eclipse Paho Android service 1.1.1, Eclipse Paho Java client
- **Trade-offs:** HiveMQ brings Netty. Duplicate `META-INF/INDEX.LIST` entries are excluded in packaging.

## WebRTC signalling on WebSocket, not MQTT
- **Date:** 2026-10-26
- **Decision:** SDP and ICE stay on `wss://api.example.ao/v1/calls/{callId}/signal`.
- **Rationale:** Signalling is an ordered request/response stream. MQTT pub/sub is a poor fit. Call setup, end, and quality stay on MQTT.
- **Alternatives considered:** `ride/{rideId}/call/{callId}/signal` on MQTT

## JSON field names
- **Date:** 2026-10-26
- **Decision:** Moshi models use camelCase, matching Jackson on the Quarkus side. There is no snake_case in the API models.
- **Rationale:** The backend prompt names camelCase as the contract.

## Topic names used at runtime
- **Date:** 2026-10-26
- **Decision:** `TopicMapper` keeps the ride-scoped taxonomy (`ride/{rideId}/offer`, `ride/{rideId}/location`, and the rest). Online mode subscribes to `driver/{driverId}/ride-offers` and `driver/{driverId}/demand-zones`, and publishes location to `driver/{driverId}/location`, because D-02 and D-03 name those topics.
- **Rationale:** The later requirements override the foundation topic for those three flows. Both patterns exist in code.

## Error envelope parsing
- **Date:** 2026-10-26
- **Decision:** `ApiErrorParser` reads `error.message` with a small scanner.
- **Rationale:** Android's `org.json` stub does not parse in JVM unit tests. The UI still receives `error.message`.

## Onboarding poll interval
- **Date:** 2026-10-26
- **Decision:** A 5 second one-time WorkManager request plus an in-screen poll.
- **Rationale:** Periodic work cannot be scheduled every 5 seconds.

## Mocks
- **Date:** 2026-10-26
- **Decision:** Hilt binds the mock APIs. `USE_MOCK_API` is true in `gradle.properties`.
- **Rationale:** The Quarkus service is not in this repo. The mock shapes follow the pasted contract.

## Amounts
- **Date:** 2026-10-26
- **Decision:** `AoaFormat` renders `Kz 13.850,00`. Commission uses the same format with a leading minus.
- **Rationale:** The pilot is Angola, not euro.

## Navigation and calls
- **Date:** 2026-10-26
- **Decision:** In-app navigation is a panel plus an external maps action. Audio calls stop at the signalling handoff described in `docs/BLOCKERS.md`.
- **Rationale:** The Maps Navigation SDK and a two-emulator WebRTC lab are not available here.

## One verified snapshot
- **Date:** 2026-10-26
- **Decision:** MQTT-00 and D-01 through D-15 share one commit. The status file records that SHA on every row.
- **Rationale:** Room, Hilt, navigation, and strings are shared. Splitting them after the fact would produce commits that do not build.
