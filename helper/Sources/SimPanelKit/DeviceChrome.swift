import Foundation

/// The parts of a device chrome's `chrome.json` that draw a device around its screen.
public struct DeviceChrome: Decodable, Equatable, Sendable {
    public let images: Images
    public let inputs: [Input]

    public init(images: Images, inputs: [Input]) {
        self.images = images
        self.inputs = inputs
    }

    /// The body as a nine-slice: corners at their own size, edges stretched between them and the
    /// screen image filling the middle.
    public struct Images: Decodable, Equatable, Sendable {
        public let topLeft: String
        public let top: String
        public let topRight: String
        public let right: String
        public let bottomRight: String
        public let bottom: String
        public let bottomLeft: String
        public let left: String
        public let screen: String
        /// How far the body reaches past the screen on each side.
        public let sizing: Sizing
        /// Room left around the body for the buttons that stick out of it.
        public let devicePadding: Insets

        public init(
            topLeft: String, top: String, topRight: String, right: String, bottomRight: String,
            bottom: String, bottomLeft: String, left: String, screen: String, sizing: Sizing,
            devicePadding: Insets
        ) {
            self.topLeft = topLeft
            self.top = top
            self.topRight = topRight
            self.right = right
            self.bottomRight = bottomRight
            self.bottom = bottom
            self.bottomLeft = bottomLeft
            self.left = left
            self.screen = screen
            self.sizing = sizing
            self.devicePadding = devicePadding
        }
    }

    public struct Sizing: Decodable, Equatable, Sendable {
        public let leftWidth: Double
        public let rightWidth: Double
        public let topHeight: Double
        public let bottomHeight: Double

        public init(leftWidth: Double, rightWidth: Double, topHeight: Double, bottomHeight: Double) {
            self.leftWidth = leftWidth
            self.rightWidth = rightWidth
            self.topHeight = topHeight
            self.bottomHeight = bottomHeight
        }
    }

    public struct Insets: Decodable, Equatable, Sendable {
        public let top: Double
        public let left: Double
        public let bottom: Double
        public let right: Double

        public init(top: Double, left: Double, bottom: Double, right: Double) {
            self.top = top
            self.left = left
            self.bottom = bottom
            self.right = right
        }
    }

    /// A button, switch or crown on the body's edge.
    public struct Input: Decodable, Equatable, Sendable {
        public let name: String
        public let type: String
        public let image: String?
        public let imageDown: String?
        public let onTop: Bool?
        /// The edge it sits on: left, right, top or bottom.
        public let anchor: String
        /// Where along that edge its offset counts from: leading, trailing or center.
        public let align: String
        public let offsets: Offsets

        public init(
            name: String, type: String, image: String?, imageDown: String?, onTop: Bool?,
            anchor: String, align: String, offsets: Offsets
        ) {
            self.name = name
            self.type = type
            self.image = image
            self.imageDown = imageDown
            self.onTop = onTop
            self.anchor = anchor
            self.align = align
            self.offsets = offsets
        }
    }

    public struct Offsets: Decodable, Equatable, Sendable {
        public let normal: Offset

        public init(normal: Offset) {
            self.normal = normal
        }
    }

    public struct Offset: Decodable, Equatable, Sendable {
        public let x: Double
        public let y: Double

        public init(x: Double, y: Double) {
            self.x = x
            self.y = y
        }
    }
}
