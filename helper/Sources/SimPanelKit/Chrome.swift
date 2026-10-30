import CoreGraphics
import Foundation

/// The device drawn around its screen: sizes in points from the top left, images as PNGs at
/// `scale` pixels a point.
public struct Chrome: Codable, Equatable, Sendable {
    public let width: Double
    public let height: Double
    public let scale: Double
    public let screen: ChromeRect
    /// How round the screen's corners are, so the plugin can clip the frames to them.
    public let screenCornerRadius: Double
    /// The body, with the screen cut out of it.
    public let bezel: Data
    public let buttons: [ChromeButton]

    public init(
        width: Double, height: Double, scale: Double, screen: ChromeRect, screenCornerRadius: Double,
        bezel: Data, buttons: [ChromeButton]
    ) {
        self.width = width
        self.height = height
        self.scale = scale
        self.screen = screen
        self.screenCornerRadius = screenCornerRadius
        self.bezel = bezel
        self.buttons = buttons
    }
}

public struct ChromeButton: Codable, Equatable, Sendable {
    public let button: DeviceButton
    public let frame: ChromeRect
    /// Drawn over the body instead of under it.
    public let onTop: Bool
    public let image: Data
    public let pressedImage: Data

    public init(button: DeviceButton, frame: ChromeRect, onTop: Bool, image: Data, pressedImage: Data) {
        self.button = button
        self.frame = frame
        self.onTop = onTop
        self.image = image
        self.pressedImage = pressedImage
    }
}

public struct ChromeRect: Codable, Equatable, Sendable {
    public let x: Double
    public let y: Double
    public let width: Double
    public let height: Double

    public init(_ rect: CGRect) {
        self.x = rect.minX
        self.y = rect.minY
        self.width = rect.width
        self.height = rect.height
    }
}
