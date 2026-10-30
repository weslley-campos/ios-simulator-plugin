/// The scale that fits a frame inside the viewport, keeping its aspect and never enlarging it.
public func frameScale(width: Int, height: Int, viewport: Viewport?) -> Double {
    guard let viewport, viewport.width > 0, viewport.height > 0, width > 0, height > 0 else {
        return 1
    }
    let fit = min(Double(viewport.width) / Double(width), Double(viewport.height) / Double(height))
    return min(1, fit)
}
