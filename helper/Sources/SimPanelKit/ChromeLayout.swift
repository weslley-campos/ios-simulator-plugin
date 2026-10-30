import CoreGraphics
import Foundation

/// Where each piece of a device chrome goes around a screen, in points from the top left.
public struct ChromeLayout: Equatable, Sendable {
    public struct Piece: Equatable, Sendable {
        public let image: String
        public let frame: CGRect
    }

    public struct Button: Equatable, Sendable {
        public let button: DeviceButton
        public let image: String
        public let pressedImage: String
        public let onTop: Bool
        public let frame: CGRect
    }

    public let size: CGSize
    public let body: CGRect
    public let screen: CGRect
    /// The body's nine slices, in the order they're drawn.
    public let pieces: [Piece]
    /// The buttons this version can press; switches, crowns and unknown buttons are left out.
    public let buttons: [Button]

    /// Lays [chrome] out around a screen of [screen] points, given each image's size in points.
    public init(chrome: DeviceChrome, screen: CGSize, imageSizes: [String: CGSize]) {
        let images = chrome.images
        let sizing = images.sizing
        let padding = images.devicePadding
        let body = CGRect(
            x: padding.left,
            y: padding.top,
            width: sizing.leftWidth + screen.width + sizing.rightWidth,
            height: sizing.topHeight + screen.height + sizing.bottomHeight
        )
        func size(_ name: String) -> CGSize { imageSizes[name] ?? .zero }

        let topLeft = CGRect(origin: body.origin, size: size(images.topLeft))
        let topRightSize = size(images.topRight)
        let topRight = CGRect(
            origin: CGPoint(x: body.maxX - topRightSize.width, y: body.minY),
            size: topRightSize
        )
        let bottomLeftSize = size(images.bottomLeft)
        let bottomLeft = CGRect(
            origin: CGPoint(x: body.minX, y: body.maxY - bottomLeftSize.height),
            size: bottomLeftSize
        )
        let bottomRightSize = size(images.bottomRight)
        let bottomRight = CGRect(
            origin: CGPoint(x: body.maxX - bottomRightSize.width, y: body.maxY - bottomRightSize.height),
            size: bottomRightSize
        )
        let top = CGRect(
            x: topLeft.maxX, y: body.minY,
            width: topRight.minX - topLeft.maxX, height: size(images.top).height
        )
        let bottomHeight = size(images.bottom).height
        let bottom = CGRect(
            x: bottomLeft.maxX, y: body.maxY - bottomHeight,
            width: bottomRight.minX - bottomLeft.maxX, height: bottomHeight
        )
        let left = CGRect(
            x: body.minX, y: topLeft.maxY,
            width: size(images.left).width, height: bottomLeft.minY - topLeft.maxY
        )
        let rightWidth = size(images.right).width
        let right = CGRect(
            x: body.maxX - rightWidth, y: topRight.maxY,
            width: rightWidth, height: bottomRight.minY - topRight.maxY
        )
        let middle = CGRect(
            x: left.maxX, y: top.maxY,
            width: right.minX - left.maxX, height: bottom.minY - top.maxY
        )

        self.size = CGSize(
            width: padding.left + body.width + padding.right,
            height: padding.top + body.height + padding.bottom
        )
        self.body = body
        self.screen = CGRect(
            origin: CGPoint(x: body.minX + sizing.leftWidth, y: body.minY + sizing.topHeight),
            size: screen
        )
        self.pieces = [
            Piece(image: images.screen, frame: middle),
            Piece(image: images.top, frame: top),
            Piece(image: images.bottom, frame: bottom),
            Piece(image: images.left, frame: left),
            Piece(image: images.right, frame: right),
            Piece(image: images.topLeft, frame: topLeft),
            Piece(image: images.topRight, frame: topRight),
            Piece(image: images.bottomLeft, frame: bottomLeft),
            Piece(image: images.bottomRight, frame: bottomRight),
        ]
        self.buttons = chrome.inputs.compactMap { input in
            guard input.type == "button", let button = DeviceButton(rawValue: input.name),
                  let image = input.image,
                  let frame = Self.frame(of: input, size: size(image), on: body)
            else { return nil }
            return Button(
                button: button,
                image: image,
                pressedImage: input.imageDown ?? image,
                onTop: input.onTop ?? false,
                frame: frame
            )
        }
    }

    /// The offset puts the button's inner edge, the one facing the body, that far from the body's
    /// edge, and its position along the edge that far from where [align] says.
    private static func frame(of input: DeviceChrome.Input, size: CGSize, on body: CGRect) -> CGRect? {
        let offset = input.offsets.normal
        func along(start: CGFloat, end: CGFloat, length: CGFloat, by distance: CGFloat) -> CGFloat? {
            switch input.align {
            case "leading": start + distance
            case "trailing": end + distance - length
            case "center": (start + end) / 2 + distance - length / 2
            default: nil
            }
        }
        let alongY = along(start: body.minY, end: body.maxY, length: size.height, by: offset.y)
        let alongX = along(start: body.minX, end: body.maxX, length: size.width, by: offset.x)
        let origin: CGPoint? = switch input.anchor {
        case "left": alongY.map { CGPoint(x: body.minX + offset.x - size.width, y: $0) }
        case "right": alongY.map { CGPoint(x: body.maxX + offset.x, y: $0) }
        case "top": alongX.map { CGPoint(x: $0, y: body.minY + offset.y - size.height) }
        case "bottom": alongX.map { CGPoint(x: $0, y: body.maxY + offset.y) }
        default: nil
        }
        return origin.map { CGRect(origin: $0, size: size) }
    }
}
