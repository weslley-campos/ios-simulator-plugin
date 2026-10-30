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
icons/      devicehub.svg, a 512 px icon in DeviceHub's style, and toolWindow.svg with its
            _dark variant, the 16 px glyph for the tool window
```

## Requirements

Apple silicon, Xcode 27.1 selected with `xcode-select`, and a booted iOS 27 simulator.

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
frame to `frames/latest.jpg` and prints the frame rate across the scroll.

### Protocol

Every message is a 4-byte big-endian payload length, a 1-byte type, then the payload.
Frames are JPEG; everything else is JSON.

| Type | Direction | Payload |
|---|---|---|
| 1 `hello` | helper to plugin | `udid`, `name`, `width`, `height`, `orientation` |
| 2 `frame` | helper to plugin | JPEG, scaled to fit the viewport, never above full size |
| 4 `error` | helper to plugin | `code`, `message`; the helper exits after sending it |
| 16 `viewport` | plugin to helper | `width`, `height` in pixels |
| 17 `touch` | plugin to helper | `phase` (`down`, `move`, `up`), `x`, `y` as screen fractions |

Error codes: `device_not_found`, `device_not_booted`, `framebuffer_unavailable`,
`hid_unavailable`, `xcode_not_found`, `unsupported_xcode` and `internal`. Types 3, 18 to 22
(`display`, `scroll`, `key`, `text`, `button`, `settings`) are reserved for later milestones
and ignored today.

The helper polls the framebuffer 30 times a second and sends a frame only when the surface
or its seed changed. Through the socket, at a 400 x 870 viewport in a release build:

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
