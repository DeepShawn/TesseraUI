// swift-tools-version: 5.9
import PackageDescription

let package = Package(
    name: "TesseraUI",
    platforms: [.iOS(.v17), .macOS(.v14), .visionOS(.v1)],
    products: [
        .library(name: "TesseraUI", targets: ["TesseraUI"]),
    ],
    targets: [
        .target(name: "TesseraUI", path: "Sources/TesseraUI"),
    ]
)
