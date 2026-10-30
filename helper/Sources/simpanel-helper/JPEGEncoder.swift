import CoreImage
import Foundation

final class JPEGEncoder {
    private let context = CIContext(options: [.cacheIntermediates: false])
    private let colorSpace = CGColorSpace(name: CGColorSpace.sRGB)!

    func encode(_ surface: IOSurface, scale: Double, quality: Double) -> Data? {
        var image = CIImage(ioSurface: surface)
        if scale < 1 {
            image = image.applyingFilter(
                "CILanczosScaleTransform",
                parameters: [kCIInputScaleKey: scale, kCIInputAspectRatioKey: 1]
            )
        }
        let options = [
            CIImageRepresentationOption(
                rawValue: kCGImageDestinationLossyCompressionQuality as String
            ): quality,
        ]
        return context.jpegRepresentation(of: image, colorSpace: colorSpace, options: options)
    }
}
