// swift-tools-version:6.0
import PackageDescription

let package = Package(
    name: "simpanel-helper",
    platforms: [.macOS(.v15)],
    products: [
        .executable(name: "simpanel-helper", targets: ["simpanel-helper"]),
        .executable(name: "simpanel-client", targets: ["simpanel-client"]),
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
        .target(
            name: "SimPanelKit",
            dependencies: ["SimBridge"]
        ),
        .executableTarget(
            name: "simpanel-helper",
            dependencies: ["SimBridge", "SimPanelKit"]
        ),
        .executableTarget(
            name: "simpanel-client",
            dependencies: ["SimPanelKit"]
        ),
        .executableTarget(
            name: "simpanel-spike",
            dependencies: ["SimBridge"]
        ),
        .testTarget(
            name: "SimPanelKitTests",
            dependencies: ["SimPanelKit"]
        ),
    ]
)
