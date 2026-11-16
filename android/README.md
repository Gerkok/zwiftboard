# ZwiftBoard Android (experimental)

This native Android app scans for both Zwift Click V2 pods, keeps GATT sessions
active in a foreground service, and forwards right-pod steering and shifting
events to an experimental MyWhoosh Link TCP server.

## Run

Open the `android` directory in Android Studio, allow the Gradle sync to finish,
and run the `app` configuration on an Android 8.0+ tablet with BLE. Or build
from PowerShell at the repository root:

```powershell
.\android\gradlew.bat -p android testDebugUnitTest assembleDebug
```

The APK is written to
`android\app\build\outputs\apk\debug\app-debug.apk`. Tap **Start background
bridge**, grant Bluetooth permissions, and turn on both Click pods. The ongoing
notification indicates that the foreground service is active. The app must be
granted Bluetooth access and must not be battery-restricted by the tablet
vendor.

The starter mapping is:

| Click V2 button | SINGLE | DOUBLE |
| --- | --- | --- |
| `A` / `Z` | Steer right / left while pressed | Steer right / left while pressed |
| `B` | Shift up | Shift up |
| `PLUS` | Shift down | Shift up |
| `Y` | U-turn | U-turn |
| `LEFT` / `RIGHT` | — | Steer left / right while pressed |
| `MIN` | — | Shift down |
| `UP` / `DOWN` | — | Not supported by the current Link controls |

The app automatically switches from SINGLE to DOUBLE once both pods are
connected and the LEFT pod is confirmed unlocked by post-challenge button data.
The switch happens once per bridge run and is not reverted if a pod disconnects.
Use the in-app mode button to switch manually at any time. `LEFT` and `RIGHT`
are kept connected because the Click V2 is a paired device. Mode changes apply
immediately without reconnecting.

The app also shows each pod's connection state, LEFT pod unlock state, and
the latest button press from either pod. LEFT unlock is shown as **Unlocked**
only after button data arrives following a challenge; otherwise the display
distinguishes unknown, locked, and an unlock response awaiting button data.
RIGHT does not require the Zwift unlock.

## MyWhoosh Link status

The app listens on TCP port `21587`, sends the observed `ConnectedDevicesList`
handshake when MyWhoosh connects, and sends JSON `Controls` messages. This
implements the public protocol observations in
[`shotah/zwift-to-mywhoosh-rust`](https://github.com/shotah/zwift-to-mywhoosh-rust/blob/main/troubleshooting/MYWHOOSH_LINK_PROTOCOL.md).

**Endpoint discovery/registration is not implemented or verified.** MyWhoosh
may need to register the Link endpoint through its official Link app/backend
before it connects to this server. The UI can show that the TCP server is
listening, but that does not mean MyWhoosh has connected. The Link app must not
be running at the same time if it owns the same port or endpoint.

## Protocol and hardware caveats

- Both pods must be available to this app; close Zwift, Companion, or any other
  app that already has a BLE connection to either pod.
- The LEFT pod may require the unlock performed through the official Zwift app.
- The app uses the known Click V2 handshake, 3-second keepalive, and 55-second
  idle reset behavior from the Windows project. The Android BLE lifecycle and
  idle-reset behavior still require verification on real hardware.
- The app does not emulate Android keyboard input or use Accessibility. It
  sends MyWhoosh Link control messages.

## Tests

Run `.\gradlew.bat testDebugUnitTest` from the `android` directory. Unit tests cover Click button-frame decoding, the LEFT unlock-state transitions,
and the MyWhoosh control-message mapping.
