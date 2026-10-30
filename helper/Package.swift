// swift-tools-version:6.0
import PackageDescription

let package = Package(
    name: "simpanel-helper",
    platforms: [.macOS(.v14)],
    products: [
        .executable(name: "simpanel-spike", targets: ["simpanel-spike"]),
    ],
    targets: [
        .target(
            name: "SimBridge",
            linkerSettings: [
                .linkedFramework("Foundation"),
                .linkedFramework("IOSurface"),
            ]
        ),
        .executableTarget(
            name: "simpanel-spike",
            dependencies: ["SimBridge"]
        ),
    ]
)
