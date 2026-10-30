# iOS Simulator Panel

An Android Studio plugin that shows a booted iOS simulator in a tool window, live and
interactive, the way Running Devices shows the Android emulator.

The spec is the [iOS Simulator Panel for Android Studio](https://claude.ai/artifact/JpKtxVex22wA1oJpdS7fHp)
doc.

## Layout

```
helper/     Swift package: the simpanel helper, which reads the simulator's framebuffer and
            injects input through CoreSimulator and SimulatorKit
  Sources/SimBridge/        Objective-C bridge to the private frameworks, loaded at runtime
  Sources/SimPanelKit/      the socket protocol: framing, messages, error codes, the socket
  Sources/simpanel-helper/  the helper the plugin starts, one per open panel
  Sources/simpanel-client/  a stand-in for the plugin, to drive the helper from a terminal
  Sources/simpanel-spike/   milestone 1, the throwaway proof of the route
  Tests/SimPanelKitTests/   protocol tests, run with `swift test`
plugin/     Gradle build of the Android Studio plugin, which bundles the helper in bin/
  protocol/    the same socket protocol, read and written over a JDK Unix socket channel
  simulators/  the device list, boot, paste and screenshots through `xcrun simctl`, and
               the orientation through `xcrun devicectl`
  helper/      HelperSession, which starts one helper, decodes its frames and sends input,
               and the policy for starting a new helper when one stops
  screen/      placing the screen and its device frame, zoom levels, turning the screen
               with the simulator and turning clicks into screen fractions
  keyboard/    the Mac keyboard as HID usages on the simulator's US layout
  settings/    the stream's frame rate and JPEG quality, from Advanced Settings
  shutdown/    asking to shut down the simulators the panel booted as the project closes
  ui/          the tool window: device picker, toolbar, the live screen, zoom controls
icons/      devicehub.svg, a 512 px icon in DeviceHub's style; the tool window's 20 px icon
            and its _dark variant are in plugin/src/main/resources/icons
```

## Requirements

Apple silicon, Xcode 27.1 selected with `xcode-select`, and a booted iOS 27 simulator. The
plugin targets Android Studio 2026.2.2 (build 262) and builds with JDK 25, because the
platform it compiles against is Java 25 bytecode.

## Plugin

```sh
cd plugin
./gradlew runIde      # a sandbox Android Studio with the plugin and its helper
./gradlew buildPlugin # build/distributions/simpanel-0.1.0.zip
./gradlew test
```

`androidStudioPath` in `gradle.properties` points at the Android Studio the plugin compiles
against and `runIde` starts; override it with `-PandroidStudioPath=...`. The build compiles
the helper in release mode and copies it into every sandbox and the zip. To use the plugin
in your own Android Studio, install the zip from Settings, Plugins, Install Plugin from
Disk.

The iOS Simulator tool window on the right lists the iOS simulators, newest runtime first,
and opens the booted one it showed last, or the first booted one. Picking a shut-down
simulator boots it. The panel shows one of four states:

- Busy, with a progress bar, while it lists the simulators, boots or shuts down one, or
  attaches.
- Live, showing the screen.
- Waiting, when no simulator is booted or the one shown shut down, with a link that boots
  it, or Open DeviceHub when there are none. It attaches as soon as that simulator boots,
  or another one does; one that was already booted doesn't take its place.
- Failed, saying why, with Retry and Open DeviceHub.

When the helper stops, or doesn't attach within 5 seconds, the panel starts a new one up to
three times, after 1, 2 and 4 seconds, then fails. A screen that hasn't drawn yet, as on a
simulator that just booted, gets 19 tries a second apart. A session that stayed live for 10
seconds starts the count over. Errors a new helper would hit again, such as a missing
device, fail at once.

- Clicks and drags on the screen become taps and swipes; the trackpad or wheel scrolls what
  lies under the pointer, as a finger dragging it.
- The panel draws the device around the screen; its buttons press when clicked.
- Beside the picker are Refresh and Shut Down. The controls below have Power, Volume Up and
  Down, Rotate Left and Right, Home, App Switcher, Take Screenshot, Paste and the Show
  Device Frame toggle. Rotating turns the simulator through `xcrun devicectl`, and the
  screen and its frame turn with it. While it's shown, the panel reads the orientation
  every 3 seconds, so a turn made in DeviceHub shows within 3 seconds.
- While the screen has focus, the Mac keyboard types on the simulator's US layout,
  including Option characters and accents through their dead keys. Characters the layout
  has no keys for are pasted. ⌘V pastes the Mac's clipboard, and ⌘A, ⌘C, ⌘X and ⌘Z edit text
  on the simulator; other ⌘ and Control shortcuts stay with the IDE.
- The floating pill at the bottom right zooms: Zoom In (⌘=), Zoom Out (⌘-), Actual Size
  (⌘.), one simulator pixel per display pixel, and Fit (⌘/), and shows the zoom. Its
  chevron folds it to the percentage. Past Fit the screen scrolls in the panel.
- Closing a project asks whether to shut down the simulators its panel opened that are
  still booted; Advanced Settings can make it always shut them down or keep them running.
- Advanced Settings, under iOS Simulator, also sets the stream's frame rate, up to 30 a
  second by default, and its JPEG quality, 70% by default. A change applies to the open
  screen at once.
- The gear after Shut Down opens the panel's settings menu. Its Show Frame Rate checkbox,
  off by default, paints how many frames arrived in the last second at the screen's top
  left.

`SIMPANEL_UDID=<udid> ./gradlew test` also runs the live test, which relaunches Settings on
that simulator, swipes it through a real helper and expects 25 decoded frames a second. It
measured 30.4 fps at 400 x 870. Run it on a simulator no panel is showing: a second helper's
touches don't land on a device another helper already drives.

## Helper

```sh
cd helper
swift build -c release
.build/release/simpanel-helper --udid <udid> --socket "$TMPDIR/simpanel-<udid>.sock" --verbose
```

The helper listens on the Unix socket, accepts one connection, attaches to the simulator and
sends `hello`, then streams frames until the plugin disconnects or the process that started
it exits. `--verbose` logs ticks, frames sent, encode and write time every half second to
stderr. A socket path can't be longer than 104 bytes, so keep it in `$TMPDIR`, not deeper.

To drive it without the plugin, run the client in a second terminal:

```sh
.build/release/simpanel-client --socket "$TMPDIR/simpanel-<udid>.sock" --out frames \
    --viewport 400x870 --seconds 2.5 --swipe 0.5,0.75,0.5,0.3
```

It sends the viewport, swipes or taps (`--tap x,y`) in screen fractions, saves the latest
frame to `frames/latest.jpg` and prints the frame rate across the scroll. `--scroll
x,y,dx,dy` scrolls, `--button home` presses a button and `--keys` presses HID usages, each
`S` (Shift) `A` (Option) or `C` (Command) prefixed, such as `S11,A8,C25`.

### Protocol

Every message is a 4-byte big-endian payload length, a 1-byte type, then the payload.
Frames are JPEG; everything else is JSON.

| Type | Direction | Payload |
|---|---|---|
| 1 `hello` | helper to plugin | `udid`, `name`, `width`, `height`, `orientation` |
| 2 `frame` | helper to plugin | JPEG, scaled to fit the viewport, never above full size |
| 4 `error` | helper to plugin | `code`, `message`; the helper exits after sending it |
| 5 `chrome` | helper to plugin | the device frame: its size, screen and corners, bezel and button PNGs |
| 16 `viewport` | plugin to helper | `width`, `height` in pixels |
| 17 `touch` | plugin to helper | `phase` (`down`, `move`, `up`), `x`, `y` as screen fractions |
| 18 `scroll` | plugin to helper | `x`, `y`, `dx`, `dy` as screen fractions; the helper drags a finger by them |
| 19 `key` | plugin to helper | `usage`, a HID keyboard usage, and `down` |
| 21 `button` | plugin to helper | `button` (`home`, `power`, `volume-up`, `volume-down`, `action`, `siri`) and `down` |
| 22 `settings` | plugin to helper | `maxFps`, 1 to 60, and `jpegQuality`, 0.1 to 1; the helper clamps both |

Error codes: `device_not_found`, `device_not_booted`, `framebuffer_unavailable`,
`hid_unavailable`, `xcode_not_found`, `unsupported_xcode` and `internal`. The helper also
sends `device_not_booted` when the simulator shuts down while it streams. Types 3 and 20
(`display`, `text`) are reserved and ignored today.

The framebuffer stays portrait when the simulator turns, so touches and scrolls are always
fractions of the portrait screen and the viewport is its upright size; the plugin turns the
frames for display and turns clicks back.

The helper polls the framebuffer at `maxFps`, 30 times a second by default, and sends a
frame only when the surface or its seed changed. Through the socket, at a 400 x 870
viewport in a release build:

| Check | Result |
|---|---|
| Frame rate while scrolling | 30 a second, median gap 33 ms, worst 40 ms |
| Encode, Lanczos scale and JPEG at 0.7 | 8 to 9 ms a frame |
| Socket write | 0.2 ms a frame, 52 KB average |
| Idle | No frames sent while the screen doesn't change |

## Spike

```sh
cd helper
swift build
.build/debug/simpanel-spike --udid <udid> --out frames --setup-tap 0.906,0.915
```

It attaches to the simulator, times JPEG encoding, scrolls the screen up and back down while
counting the frames the simulator draws, then taps `--tap` (Orbit's Home tab by default).
`--setup-tap` taps first, here Orbit's Settings tab, so there is a list to scroll.

Results on 2026-10-29 with Xcode 27.1 (27A9275) and an iPhone 18 Pro on iOS 27.0:

| Check | Result |
|---|---|
| Attach by UDID | 140 to 330 ms |
| Framebuffer | IOSurface, 1206 x 2622, BGRA |
| JPEG at full size | 4 to 7 ms a frame |
| JPEG at half size | 2 to 3 ms a frame |
| Frames drawn while scrolling | 41 to 57 a second, polled every millisecond |
| Taps and swipes | Land through `SimDeviceLegacyHIDClient` |
| macOS permission prompts | None |

Xcode 27.1 still ships the legacy HID client, so the helper doesn't need UniversalHID.
`IndigoHIDMessageForMouseNSEvent` now takes the screen size and builds the whole touch
message itself, and it drops a move that comes less than a frame after the previous one.
