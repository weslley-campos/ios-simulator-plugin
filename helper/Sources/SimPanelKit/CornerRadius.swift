/// The radius, in points, of the circle through the point where a rounded corner crosses its
/// diagonal, from the coverage of the pixels along that diagonal, starting at the corner.
public func cornerRadius(diagonal coverage: [UInt8], scale: Double) -> Double {
    guard scale > 0, let inside = coverage.firstIndex(where: { $0 >= 128 }) else { return 0 }
    let crossing = (Double(inside) + 0.5) / scale
    return crossing / (1 - 1 / 2.squareRoot())
}
