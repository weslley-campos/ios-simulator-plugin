Run your iOS app in Android Studio, right next to the code. iOS Simulator docks a booted simulator in a tool window, live and interactive, the way Running Devices shows the Android emulator.

Made for Kotlin Multiplatform and Compose Multiplatform projects: test the iOS side without switching windows.

### Features

- **Live screen** at up to 60 fps, inside the device frame.
- **Touch**: click to tap, drag to swipe, scroll with the trackpad or mouse wheel.
- **Keyboard**: type straight into the simulator. `⌘V` pastes your Mac clipboard. Show or hide the on-screen keyboard from the toolbar.
- **Hardware buttons**: Power, Volume, Home and App Switcher, from the toolbar or the device frame.
- **Rotate, screenshot and zoom** from the toolbar.
- **Device picker**: lists every simulator and boots the one you pick.
- **Follows your runs**: running or debugging an iOS app opens the panel on that simulator.
- **Recovers on its own** when the simulator restarts or the connection drops.

### Getting started

1. Open the **iOS Simulator** tool window on the right.
2. Pick a simulator. It boots if it isn't running.
3. Click, type and scroll as you would on a device.

Change the frame rate, image quality and what happens on project close under **Settings → Advanced Settings → iOS Simulator**, or from the ⚙ menu in the panel's toolbar.

### Requirements

- A Mac with Apple silicon
- Xcode 27.1, selected with `xcode-select`
- An iOS 26.5 or iOS 27 simulator runtime

[Source code and issues on GitHub](https://github.com/weslley-campos/ios-simulator-plugin)
