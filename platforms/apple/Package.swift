// swift-tools-version: 5.9
import PackageDescription

let package = Package(
    name: "TesseraUI",
    defaultLocalization: "zh-Hans",
    platforms: [.iOS(.v17), .macOS(.v14), .visionOS(.v1)],
    products: [
        .library(name: "TesseraUI", targets: ["TesseraUI"]),
    ],
    targets: [
        .target(
            name: "TesseraUI",
            path: "Sources/TesseraUI",
            resources: [.process("Resources")]
        ),
        // macOS 示例 App：swift run TesseraSample
        .executableTarget(name: "TesseraSample", dependencies: ["TesseraUI"], path: "Sources/TesseraSample"),
        // 截图测试（macOS）：swift test，图片写到 .build/snapshots
        .testTarget(name: "TesseraUITests", dependencies: ["TesseraUI"], path: "Tests/TesseraUITests"),
    ]
)
