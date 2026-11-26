# Phase 1 driver report

`./gradlew :app:testDebugUnitTest --tests ao.ridehailing.driver.phase1.Phase1UseCaseTest` passed: 5 tests, 0 failures. These tests cover the phase 1 rules the driver app can decide without a device. The screens that publish the messages are not all wired, and this report does not count those screens as done.

## Use cases covered by the passing test

1. The account JSON from `GET /v1/drivers/{id}/account` is read as debt, ceiling, choice, block, `pickupWaitMinutes`, and `noShowMetres`. The app does not invent those numbers.
2. Going online is blocked locally when the account says `CHOICE_REQUIRED`, `BLOCKED`, or `DEBT_LIMIT`. A rejection reason maps to one sentence. Nothing in this code retries.
3. Cash received is the envelope `ride.payment.cash_received` with `rideId`, `driverId`, `passengerId`, and the fare string only. Pay online sends `{"amount":"<debt>"}`. A choice sends `{"choice":"PAY_ONLINE"}`.
4. The still timer adds time only while both ends of a sample stay within `noShowMetres` of the pickup. The button enables only after `pickupWaitMinutes` of that time.
5. Heading home publishes `driver.heading_home` with an empty payload. Home base publishes `driver.home_base_set` with the latitude and longitude the caller supplies. A second set is refused.

The payment screen no longer shows Visa. Multicaixa Express and Dinheiro stay. The Multicaixa status flow is not built.

## MQTT the tests lock

`driver/{id}/account` is the topic the app subscribes to for `driver.account.updated`, `driver.online.rejected`, and `ride.passenger_no_show.rejected`.

```json
{"type":"ride.payment.cash_received","messageId":"m1","payload":{"rideId":"R1","driverId":"D1","passengerId":"P1","fare":"13850.00"}}
```

```json
{"type":"driver.heading_home","messageId":"m6","payload":{}}
```

```json
{"type":"driver.home_base_set","messageId":"m7","payload":{"lat":-8.84,"lng":13.23}}
```

The too-far sentence the app shows is: “Está demasiado longe do local de embarque. O não-comparecimento não foi registado.”

## REST

`GET /v1/drivers/{id}/account` with the existing Bearer token. `POST /v1/drivers/{id}/debt/choice`. `POST /v1/drivers/{id}/debt/payments` for the current debt. The client methods for those two posts are not in `ApiService` yet.

## Configuration

Pickup wait and no-show distance are fields on the account payload. They are not in `BuildConfig` or `gradle.properties`.

## Not done

- FR-01 through FR-55 are not all implemented. FR-10 stays removed.
- Going online subscribes to `driver/{id}/account`. Cash received for method `CASH` publishes `ride.payment.cash_received`. Profile settings offer the map or the current GPS fix for the home base. A later home base replaces the earlier one. The earnings page no longer shows the sample week total.
- The still timer and the strike warning are covered by the unit tests and the Portuguese string resources. They are not exercised on a device.
- Address geocoding is not built. The real AGT client is not called. The voice connection of a call is not built.
- Nothing has been run on a phone. The commands are in `docs/phase1-smoke-test.md`.
