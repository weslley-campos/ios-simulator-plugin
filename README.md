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
  Sources/simpanel-spike/   milestone 1, the throwaway proof of the route
```

## Requirements

Apple silicon, Xcode 27.1 selected with `xcode-select`, and a booted iOS 27 simulator.

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
| Frames drawn while scrolling | about 190 a second, polled every millisecond |
| Taps and swipes | Land through `SimDeviceLegacyHIDClient` |
| macOS permission prompts | None |

Xcode 27.1 still ships the legacy HID client, so the helper doesn't need UniversalHID.
`IndigoHIDMessageForMouseNSEvent` now takes the screen size and builds the whole touch
message itself, and it drops a move that comes less than a frame after the previous one.
