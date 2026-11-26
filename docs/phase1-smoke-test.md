# Phase 1 driver smoke test

Nothing in this file has been run on a phone or an emulator. Run it yourself. The emulator is `emulator-5554`. From the emulator, the computer is `10.0.2.2`. `LAT` and `LNG` are the coordinates you choose.

```text
adb -s emulator-5554 shell input keyevent KEYCODE_WAKEUP
adb -s emulator-5554 shell am start -n ao.ridehailing.driver/.MainActivity
mosquitto_pub -h 127.0.0.1 -p 1883 -t driver/DRIVER_ID/account -m '{"type":"driver.online.rejected","messageId":"m1","payload":{"reason":"CHOICE_REQUIRED"}}'
mosquitto_pub -h 127.0.0.1 -p 1883 -t driver/DRIVER_ID/account -m '{"type":"ride.passenger_no_show.rejected","messageId":"m2","payload":{"reason":"TOO_FAR"}}'
mosquitto_pub -h 127.0.0.1 -p 1883 -t driver/DRIVER_ID/status -m '{"type":"driver.heading_home","messageId":"m3","payload":{}}'
mosquitto_pub -h 127.0.0.1 -p 1883 -t driver/DRIVER_ID/status -m '{"type":"driver.home_base_set","messageId":"m4","payload":{"lat":LAT,"lng":LNG}}'
```

Home base is chosen in the profile: the map, or the current GPS fix. Earnings no longer show the old sample totals.
