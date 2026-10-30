import CoreGraphics
import Foundation
import ImageIO
import SimPanelKit
import UniformTypeIdentifiers

private let ChromeDirectory = URL(fileURLWithPath: "/Library/Developer/DeviceKit/Chrome")

struct ChromeUnavailable: Error, CustomStringConvertible {
    let description: String
}

/// Draws a device around its screen from the artwork Xcode's Simulator uses: the device type
/// names a chrome bundle, whose PDFs make the body and buttons, and a mask for the screen's corners.
enum ChromeRenderer {
    /// [screen] is in points; the images come out at [scale] pixels a point.
    static func render(deviceTypePath: String, screen: CGSize, scale: Double) throws -> Chrome {
        let resources = URL(fileURLWithPath: deviceTypePath).appendingPathComponent("Contents/Resources")
        let profile = try plist(resources.appendingPathComponent("profile.plist"))
        guard let identifier = profile["chromeIdentifier"] as? String else {
            throw ChromeUnavailable(description: "\(deviceTypePath) names no chrome")
        }
        let chromeResources = try chromeBundle(identifier)
        let chrome = try JSONDecoder().decode(
            DeviceChrome.self,
            from: Data(contentsOf: chromeResources.appendingPathComponent("chrome.json"))
        )
        var pages: [String: CGPDFPage] = [:]
        func page(_ name: String) -> CGPDFPage? {
            if let page = pages[name] { return page }
            let url = chromeResources.appendingPathComponent("\(name).pdf")
            let page = CGPDFDocument(url as CFURL)?.page(at: 1)
            pages[name] = page
            return page
        }
        let images = chrome.images
        let names = [images.topLeft, images.top, images.topRight, images.right, images.bottomRight,
                     images.bottom, images.bottomLeft, images.left, images.screen]
            + chrome.inputs.flatMap { [$0.image, $0.imageDown].compactMap { $0 } }
        let sizes = Dictionary(
            names.compactMap { name in page(name).map { (name, $0.getBoxRect(.mediaBox).size) } },
            uniquingKeysWith: { size, _ in size }
        )
        let layout = ChromeLayout(chrome: chrome, screen: screen, imageSizes: sizes)
        let mask = (profile["framebufferMask"] as? String).flatMap { name in
            CGPDFDocument(resources.appendingPathComponent("\(name).pdf") as CFURL)?.page(at: 1)
        }

        let bezel = try png(size: layout.size, scale: scale) { context in
            for piece in layout.pieces {
                if let page = page(piece.image) { draw(page, in: piece.frame, on: context) }
            }
            if let mask {
                context.setBlendMode(.destinationOut)
                draw(mask, in: layout.screen, on: context)
            } else {
                context.clear(layout.screen)
            }
        }
        let buttons = try layout.buttons.compactMap { button -> ChromeButton? in
            guard let image = page(button.image) else { return nil }
            let pressed = page(button.pressedImage) ?? image
            let bounds = CGRect(origin: .zero, size: button.frame.size)
            return ChromeButton(
                button: button.button,
                frame: ChromeRect(button.frame),
                onTop: button.onTop,
                image: try png(size: bounds.size, scale: scale) { draw(image, in: bounds, on: $0) },
                pressedImage: try png(size: bounds.size, scale: scale) { draw(pressed, in: bounds, on: $0) }
            )
        }
        let screenCornerRadius = try mask.map { try cornerRadius(of: $0, screen: layout.screen.size, scale: scale) }
        return Chrome(
            width: layout.size.width,
            height: layout.size.height,
            scale: scale,
            screen: ChromeRect(layout.screen),
            screenCornerRadius: screenCornerRadius ?? 0,
            bezel: bezel,
            buttons: buttons
        )
    }

    private static func plist(_ url: URL) throws -> [String: Any] {
        let data = try Data(contentsOf: url)
        guard let plist = try PropertyListSerialization.propertyList(from: data, format: nil) as? [String: Any] else {
            throw ChromeUnavailable(description: "\(url.path) isn't a dictionary")
        }
        return plist
    }

    private static func chromeBundle(_ identifier: String) throws -> URL {
        let bundles = (try? FileManager.default.contentsOfDirectory(
            at: ChromeDirectory,
            includingPropertiesForKeys: nil
        )) ?? []
        for url in bundles where url.pathExtension == "devicechrome" {
            if let bundle = Bundle(url: url), bundle.bundleIdentifier == identifier, let resources = bundle.resourceURL {
                return resources
            }
        }
        throw ChromeUnavailable(description: "no \(identifier) in \(ChromeDirectory.path)")
    }

    /// Draws a PDF page stretched into [rect], given in points from the top left.
    private static func draw(_ page: CGPDFPage, in rect: CGRect, on context: CGContext) {
        let box = page.getBoxRect(.mediaBox)
        guard box.width > 0, box.height > 0 else { return }
        context.saveGState()
        // drawPDFPage doesn't clip to the page, and some chromes, such as phone11's, draw past it.
        context.clip(to: rect)
        context.translateBy(x: rect.minX, y: rect.minY)
        context.scaleBy(x: rect.width / box.width, y: rect.height / box.height)
        // PDFs draw from the bottom left, so flip the page inside its own box.
        context.translateBy(x: 0, y: box.height)
        context.scaleBy(x: 1, y: -1)
        context.translateBy(x: -box.minX, y: -box.minY)
        context.drawPDFPage(page)
        context.restoreGState()
    }

    /// Measures the mask's top left corner, drawn on its own.
    private static func cornerRadius(of mask: CGPDFPage, screen: CGSize, scale: Double) throws -> Double {
        let corner = CGSize(width: screen.width / 2, height: screen.height / 2)
        let context = try bitmap(size: corner, scale: scale) {
            draw(mask, in: CGRect(origin: .zero, size: screen), on: $0)
        }
        guard let data = context.data else { return 0 }
        let pixels = data.assumingMemoryBound(to: UInt8.self)
        let alpha = (0..<min(context.width, context.height)).map { pixels[$0 * context.bytesPerRow + $0 * 4 + 3] }
        return SimPanelKit.cornerRadius(diagonal: alpha, scale: scale)
    }

    /// A transparent RGBA bitmap of [size] points, with the origin at the top left, once [draw]
    /// has drawn on it.
    private static func bitmap(size: CGSize, scale: Double, draw: (CGContext) -> Void) throws -> CGContext {
        let width = Int((size.width * scale).rounded(.up))
        let height = Int((size.height * scale).rounded(.up))
        guard width > 0, height > 0, let context = CGContext(
            data: nil, width: width, height: height, bitsPerComponent: 8, bytesPerRow: 0,
            space: CGColorSpace(name: CGColorSpace.sRGB)!,
            bitmapInfo: CGImageAlphaInfo.premultipliedLast.rawValue
        ) else {
            throw ChromeUnavailable(description: "can't draw \(width) x \(height)")
        }
        context.translateBy(x: 0, y: CGFloat(height))
        context.scaleBy(x: scale, y: -scale)
        draw(context)
        return context
    }

    private static func png(size: CGSize, scale: Double, draw: (CGContext) -> Void) throws -> Data {
        let context = try bitmap(size: size, scale: scale, draw: draw)
        let data = NSMutableData()
        guard let image = context.makeImage(),
              let destination = CGImageDestinationCreateWithData(data, UTType.png.identifier as CFString, 1, nil)
        else {
            throw ChromeUnavailable(description: "can't encode \(context.width) x \(context.height)")
        }
        CGImageDestinationAddImage(destination, image, nil)
        guard CGImageDestinationFinalize(destination) else {
            throw ChromeUnavailable(description: "can't encode \(context.width) x \(context.height)")
        }
        return data as Data
    }
}
