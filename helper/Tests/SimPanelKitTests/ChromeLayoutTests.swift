import CoreGraphics
import Testing
@testable import SimPanelKit

struct ChromeLayoutTests {
    /// Xcode's phone12 chrome, which draws the iPhone 18 Pro Max.
    private let phone12 = DeviceChrome(
        images: DeviceChrome.Images(
            topLeft: "Phone TL", top: "Phone Top", topRight: "Phone TR", right: "Phone Right",
            bottomRight: "Phone BR", bottom: "Phone Base", bottomLeft: "Phone BL", left: "Phone Left",
            screen: "Screen",
            sizing: DeviceChrome.Sizing(leftWidth: 18, rightWidth: 18, topHeight: 18, bottomHeight: 18),
            devicePadding: DeviceChrome.Insets(top: 0, left: 9, bottom: 0, right: 9)
        ),
        inputs: [
            Self.input("action", image: "Mute BTN", anchor: "left", x: 8, y: 180),
            Self.input("volume-up", image: "Vol BTN", anchor: "left", x: 8, y: 268),
            Self.input("volume-down", image: "Vol BTN", anchor: "left", x: 8, y: 354),
            Self.input("power", image: "X_Power BTN", anchor: "right", x: -8, y: 293),
        ]
    )

    private let phone12Sizes: [String: CGSize] = [
        "Phone TL": CGSize(width: 110, height: 110), "Phone TR": CGSize(width: 110, height: 110),
        "Phone BL": CGSize(width: 110, height: 110), "Phone BR": CGSize(width: 110, height: 110),
        "Phone Top": CGSize(width: 1, height: 110), "Phone Base": CGSize(width: 1, height: 110),
        "Phone Left": CGSize(width: 110, height: 1), "Phone Right": CGSize(width: 110, height: 1),
        "Screen": CGSize(width: 1, height: 1),
        "Mute BTN": CGSize(width: 16, height: 34), "Vol BTN": CGSize(width: 16, height: 64),
        "X_Power BTN": CGSize(width: 16, height: 101),
    ]

    private static func input(
        _ name: String, type: String = "button", image: String, anchor: String,
        align: String = "leading", x: Double, y: Double
    ) -> DeviceChrome.Input {
        DeviceChrome.Input(
            name: name, type: type, image: image, imageDown: "\(image) Dn", onTop: false,
            anchor: anchor, align: align,
            offsets: DeviceChrome.Offsets(normal: DeviceChrome.Offset(x: x, y: y))
        )
    }

    private func layOut(_ chrome: DeviceChrome, sizes: [String: CGSize]? = nil) -> ChromeLayout {
        ChromeLayout(
            chrome: chrome,
            screen: CGSize(width: 440, height: 956),
            imageSizes: sizes ?? phone12Sizes
        )
    }

    @Test func wrapsTheBodyAroundTheScreenWithRoomForTheButtons() {
        let layout = layOut(phone12)

        #expect(layout.size == CGSize(width: 494, height: 992))
        #expect(layout.body == CGRect(x: 9, y: 0, width: 476, height: 992))
        #expect(layout.screen == CGRect(x: 27, y: 18, width: 440, height: 956))
    }

    @Test func stretchesTheEdgesBetweenTheCornersAndFillsTheMiddle() {
        let pieces = Dictionary(uniqueKeysWithValues: layOut(phone12).pieces.map { ($0.image, $0.frame) })

        #expect(pieces["Screen"] == CGRect(x: 119, y: 110, width: 256, height: 772))
        #expect(pieces["Phone Top"] == CGRect(x: 119, y: 0, width: 256, height: 110))
        #expect(pieces["Phone Right"] == CGRect(x: 375, y: 110, width: 110, height: 772))
        #expect(pieces["Phone BR"] == CGRect(x: 375, y: 882, width: 110, height: 110))
    }

    @Test func putsEachButtonsInnerEdgeItsOffsetFromTheBodysEdge() {
        let buttons = layOut(phone12).buttons

        #expect(buttons.map(\.button) == [.action, .volumeUp, .volumeDown, .power])
        #expect(buttons.map(\.frame) == [
            CGRect(x: 1, y: 180, width: 16, height: 34),
            CGRect(x: 1, y: 268, width: 16, height: 64),
            CGRect(x: 1, y: 354, width: 16, height: 64),
            CGRect(x: 477, y: 293, width: 16, height: 101),
        ])
        #expect(buttons[0].pressedImage == "Mute BTN Dn")
    }

    @Test func measuresATopButtonFromTheCornerItsAlignedTo() throws {
        let tablet = DeviceChrome(
            images: phone12.images,
            inputs: [Self.input("power", image: "X_Power BTN", anchor: "top", align: "trailing", x: -65, y: 8)]
        )

        let power = try #require(layOut(tablet).buttons.first)

        #expect(power.frame == CGRect(x: 485 - 65 - 16, y: 8 - 101, width: 16, height: 101))
    }

    @Test func leavesOutSwitchesAndButtonsItCannotPress() {
        let chrome = DeviceChrome(
            images: phone12.images,
            inputs: [
                Self.input("action", type: "switch", image: "Mute BTN", anchor: "left", x: 8, y: 180),
                Self.input("camera", image: "Vol BTN", anchor: "right", x: -8, y: 400),
            ]
        )

        #expect(layOut(chrome).buttons.isEmpty)
    }
}
