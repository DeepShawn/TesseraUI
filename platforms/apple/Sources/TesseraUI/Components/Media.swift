import SwiftUI
#if canImport(UIKit)
import UIKit
#elseif canImport(AppKit)
import AppKit
#endif

// MARK: - 轮播 Carousel

/// 轮播：系统分页滚动（跟手 + 惯性吸附）；圆点变长表示当前页，前后按钮可点。
/// autoplay 大于 0 时自动播放（秒），用户拖动后暂停 5 秒；“减弱动态效果”时不自动播放
public struct TesseraCarousel<Page: View>: View {
    let count: Int
    let height: CGFloat
    let peek: Bool
    let autoplay: Double
    let page: (Int) -> Page
    @State private var current: Int?
    @State private var lastTouch = Date.distantPast
    @Environment(\.tesseraShape) private var tesseraShape
    @Environment(\.accessibilityReduceMotion) private var reduceMotion
    @Environment(\.tesseraStaticRender) private var staticRender

    public init(count: Int, height: CGFloat = 200, peek: Bool = false, autoplay: Double = 0, initialPage: Int = 0,
                @ViewBuilder page: @escaping (Int) -> Page) {
        self.count = count
        self.height = height
        self.peek = peek
        self.autoplay = autoplay
        self.page = page
        self._current = State(initialValue: initialPage)
    }

    private var index: Int { current ?? 0 }

    private func go(_ i: Int) {
        lastTouch = .now
        withAnimation(.tessera(TesseraMotion.smooth, reduceMotion: reduceMotion)) { current = min(max(i, 0), count - 1) }
    }

    public var body: some View {
        VStack(spacing: TesseraSpace.s1) {
            ZStack {
                pager
                HStack {
                    CarouselNav(icon: .chevronLeft, label: "上一张", enabled: index > 0) { go(index - 1) }
                    Spacer()
                    CarouselNav(icon: .chevronRight, label: "下一张", enabled: index < count - 1) { go(index + 1) }
                }
                .padding(.horizontal, 12)
            }
            .frame(height: height)
            CarouselDots(count: count, index: index, round: tesseraShape.isRound) { go($0) }
        }
        .accessibilityElement(children: .contain)
        .accessibilityLabel("轮播")
        .task(id: autoplay) {
            guard autoplay > 0, !reduceMotion else { return }
            while !Task.isCancelled {
                try? await Task.sleep(nanoseconds: UInt64(autoplay * 1_000_000_000))
                if Date.now.timeIntervalSince(lastTouch) < 5 { continue }
                withAnimation(.tessera(TesseraMotion.smooth, reduceMotion: reduceMotion)) { current = index == count - 1 ? 0 : index + 1 }
            }
        }
    }

    private var pageShape: RoundedRectangle {
        RoundedRectangle(cornerRadius: tesseraShape.radius(tile: TesseraRadius.xl, round: TesseraRound.card), style: .continuous)
    }

    @ViewBuilder private var pager: some View {
        if staticRender {
            // 截图：只画当前页（以及露出的前后两页）
            GeometryReader { g in
                let inset: CGFloat = peek ? 32 : 0
                let w = g.size.width - inset * 2
                HStack(spacing: TesseraSpace.s3) {
                    ForEach(0..<count, id: \.self) { i in
                        page(i).frame(width: w, height: g.size.height).clipShape(pageShape)
                    }
                }
                .offset(x: inset - CGFloat(index) * (w + TesseraSpace.s3))
            }
            .clipped()
        } else {
            ScrollView(.horizontal, showsIndicators: false) {
                LazyHStack(spacing: TesseraSpace.s3) {
                    ForEach(0..<count, id: \.self) { i in
                        page(i)
                            .containerRelativeFrame(.horizontal)
                            .frame(maxHeight: .infinity)
                            .clipShape(pageShape)
                            .accessibilityElement(children: .combine)
                            .accessibilityLabel("第 \(i + 1) 张，共 \(count) 张")
                            .id(i)
                    }
                }
                .scrollTargetLayout()
            }
            .contentMargins(.horizontal, peek ? 32 : 0, for: .scrollContent)
            .scrollTargetBehavior(.viewAligned)
            .scrollPosition(id: $current)
            .simultaneousGesture(DragGesture(minimumDistance: 8).onChanged { _ in lastTouch = .now })
        }
    }
}

private struct CarouselNav: View {
    let icon: TesseraIconName
    let label: String
    let enabled: Bool
    let action: () -> Void
    @Environment(\.tesseraShape) private var tesseraShape

    var body: some View {
        let shape = RoundedRectangle(cornerRadius: tesseraShape.radius(tile: TesseraRadius.md), style: .continuous)
        Button(action: action) {
            TesseraIcon(icon, size: 20).foregroundStyle(TesseraColor.textPrimary)
                .frame(width: TesseraSize.touchMin, height: TesseraSize.touchMin)
                .background(TesseraColor.bgElevated.opacity(0.88), in: shape)
                .tesseraShadow(.level1)
        }
        .buttonStyle(.plain)
        .opacity(enabled ? 1 : 0)
        .allowsHitTesting(enabled)
        .accessibilityHidden(!enabled)
        .accessibilityLabel(label)
    }
}

/// 圆点只是视觉指示（读屏通过页面名称与前后按钮操作），点按也能跳页
private struct CarouselDots: View {
    let count: Int
    let index: Int
    let round: Bool
    let go: (Int) -> Void
    @Environment(\.accessibilityReduceMotion) private var reduceMotion

    var body: some View {
        HStack(spacing: 6) {
            ForEach(0..<count, id: \.self) { i in
                let on = i == index
                RoundedRectangle(cornerRadius: round ? 4 : 3, style: .continuous)
                    .fill(on ? TesseraColor.textPrimary : TesseraColor.fillControlOff)
                    .frame(width: on ? 22 : 8, height: 8)
                    .frame(height: 24)
                    .contentShape(Rectangle())
                    .onTapGesture { go(i) }
            }
        }
        .animation(.tessera(TesseraMotion.snappy, reduceMotion: reduceMotion), value: index)
        .frame(maxWidth: .infinity)
        .accessibilityHidden(true)
    }
}

// MARK: - 图片预览 ImageViewer

public extension View {
    /// 全屏图片预览：双指 / 双击缩放，放大后拖动平移；未放大时下滑关闭（背景随之变淡）、左右滑切换
    func tesseraImageViewer(isPresented: Binding<Bool>, images: [Image], descriptions: [String] = [], startIndex: Int = 0) -> some View {
        #if os(iOS)
        fullScreenCover(isPresented: isPresented) {
            TesseraImageViewer(images: images, descriptions: descriptions, startIndex: startIndex) { isPresented.wrappedValue = false }
                .presentationBackground(.clear)
        }
        #else
        sheet(isPresented: isPresented) {
            TesseraImageViewer(images: images, descriptions: descriptions, startIndex: startIndex) { isPresented.wrappedValue = false }
                .frame(minWidth: 720, minHeight: 520)
        }
        #endif
    }
}

/// 图片预览的内容（不含弹出窗口），截图测试也用它
public struct TesseraImageViewer: View {
    let images: [Image]
    let descriptions: [String]
    let onDismiss: () -> Void
    @State private var index: Int
    @State private var scale: CGFloat = 1
    @State private var pinchBase: CGFloat = 1
    @State private var offset: CGSize = .zero
    @State private var dragBase: CGSize = .zero
    @Environment(\.tesseraShape) private var tesseraShape
    @Environment(\.accessibilityReduceMotion) private var reduceMotion

    public init(images: [Image], descriptions: [String] = [], startIndex: Int = 0, onDismiss: @escaping () -> Void) {
        self.images = images
        self.descriptions = descriptions
        self.onDismiss = onDismiss
        self._index = State(initialValue: min(max(startIndex, 0), max(images.count - 1, 0)))
    }

    private var spring: Animation { .tessera(TesseraMotion.smooth, reduceMotion: reduceMotion) }

    private func zoom(to s: CGFloat) {
        withAnimation(spring) {
            scale = min(max(s, 1), 5)
            if scale == 1 { offset = .zero }
            pinchBase = scale
            dragBase = offset
        }
    }

    private func show(_ i: Int) {
        guard !images.isEmpty else { return }
        index = (i + images.count) % images.count
        scale = 1; pinchBase = 1; offset = .zero; dragBase = .zero
    }

    public var body: some View {
        let fade = max(0.3, 1 - abs(offset.height) / 1200)
        ZStack {
            Color.black.opacity(0.92 * fade).ignoresSafeArea()
            if !images.isEmpty { picture }
            VStack {
                topBar
                Spacer()
                tools
            }
            .padding(16)
        }
        .accessibilityElement(children: .contain)
        .accessibilityLabel("图片预览")
        .accessibilityAction(.escape) { onDismiss() }
    }

    private var picture: some View {
        images[index]
            .resizable()
            .scaledToFit()
            .clipShape(RoundedRectangle(cornerRadius: tesseraShape.radius(tile: TesseraRadius.md, round: TesseraRound.row), style: .continuous))
            .padding(.horizontal, 16)
            .padding(.vertical, 64)
            .scaleEffect(scale)
            .offset(offset)
            .accessibilityLabel(descriptions.indices.contains(index) ? descriptions[index] : "图片")
            .gesture(MagnifyGesture()
                .onChanged { v in scale = min(max(pinchBase * v.magnification, 0.6), 6) }
                .onEnded { _ in zoom(to: scale) })
            .simultaneousGesture(DragGesture()
                .onChanged { v in
                    if scale > 1.01 {
                        offset = CGSize(width: dragBase.width + v.translation.width, height: dragBase.height + v.translation.height)
                    } else {
                        // 未放大：竖向跟手（用于关闭），横向带阻尼（用于切换）
                        offset = CGSize(width: v.translation.width * 0.4, height: v.translation.height)
                    }
                }
                .onEnded { v in
                    if scale > 1.01 { dragBase = offset; return }
                    if abs(v.translation.height) > 120 || abs(v.predictedEndTranslation.height) > 400 { onDismiss(); return }
                    if abs(v.translation.width) > 80 && images.count > 1 { show(v.translation.width < 0 ? index + 1 : index - 1); return }
                    withAnimation(spring) { offset = .zero }
                })
            .onTapGesture(count: 2) { zoom(to: scale > 1.01 ? 1 : 2.5) }
    }

    private var topBar: some View {
        HStack {
            Text(images.count > 1 ? "\(index + 1) / \(images.count)" : (descriptions.first ?? ""))
                .font(.system(size: 14, weight: .semibold)).foregroundStyle(.white).monospacedDigit()
            Spacer()
            ViewerButton(icon: .close, label: "关闭", shape: RoundedRectangle(cornerRadius: tesseraShape.radius(tile: TesseraRadius.md), style: .continuous), filled: true, action: onDismiss)
        }
    }

    private var tools: some View {
        let round = tesseraShape.isRound
        var list: [(TesseraIconName, String, () -> Void)] = []
        if images.count > 1 { list.append((.chevronLeft, "上一张", { show(index - 1) })) }
        list.append((.zoomOut, "缩小", { zoom(to: scale / 1.5) }))
        list.append((.zoomIn, "放大", { zoom(to: scale * 1.5) }))
        if images.count > 1 { list.append((.chevronRight, "下一张", { show(index + 1) })) }
        return HStack(spacing: round ? 0 : TesseraTile.grout) {
            ForEach(Array(list.enumerated()), id: \.offset) { i, t in
                ViewerButton(icon: t.0, label: t.1, shape: round ? tileShape(.single, outer: 22) : tileShape(.of(i, count: list.count), outer: 14), filled: !round, action: t.2)
            }
        }
        .padding(round ? 4 : 0)
        .background(round ? Color.white.opacity(0.12) : .clear, in: Capsule())
    }
}

private struct ViewerButton<S: InsettableShape>: View {
    let icon: TesseraIconName
    let label: String
    let shape: S
    let filled: Bool
    let action: () -> Void

    var body: some View {
        Button(action: action) {
            TesseraIcon(icon, size: 20).foregroundStyle(.white)
                .frame(width: TesseraSize.touchMin, height: TesseraSize.touchMin)
                .background(filled ? Color.white.opacity(0.12) : .clear, in: shape)
                .contentShape(shape)
        }
        .buttonStyle(.plain)
        .accessibilityLabel(label)
    }
}

// MARK: - 代码块 CodeBlock

/// 代码块：凹槽底色，等宽字体，长行在块内横向滚动；复制后图标变成勾
public struct TesseraCodeBlock: View {
    let code: String
    let language: String?
    let lineNumbers: Bool
    @State private var copied = false
    @Environment(\.tesseraShape) private var tesseraShape
    @Environment(\.tesseraStaticRender) private var staticRender

    public init(_ code: String, language: String? = nil, lineNumbers: Bool = false) {
        self.code = code
        self.language = language
        self.lineNumbers = lineNumbers
    }

    private var trimmed: String {
        var s = code
        while s.hasSuffix("\n") { s.removeLast() }
        return s
    }

    private func copy() {
        #if canImport(UIKit)
        UIPasteboard.general.string = trimmed
        #elseif canImport(AppKit)
        NSPasteboard.general.clearContents()
        NSPasteboard.general.setString(trimmed, forType: .string)
        #endif
        copied = true
        Task { @MainActor in
            try? await Task.sleep(nanoseconds: 1_600_000_000)
            copied = false
        }
    }

    public var body: some View {
        let round = tesseraShape.isRound
        let shape = RoundedRectangle(cornerRadius: tesseraShape.radius(tile: TesseraRadius.md, round: TesseraRound.row), style: .continuous)
        VStack(spacing: 0) {
            HStack {
                Text(language ?? "").font(.system(size: 12, weight: .medium, design: .monospaced)).foregroundStyle(TesseraColor.textSecondary)
                Spacer()
                Button(action: copy) {
                    TesseraIcon(copied ? .check : .copy, size: 18)
                        .foregroundStyle(copied ? TesseraColor.successText : TesseraColor.textSecondary)
                        .frame(width: TesseraSize.touchMin, height: TesseraSize.touchMin)
                        .contentShape(Rectangle())
                }
                .buttonStyle(.plain)
                .accessibilityLabel(copied ? "已复制" : "复制代码")
            }
            .padding(.leading, round ? 18 : 16)
            .overlay(alignment: .bottom) { if !round { TesseraColor.borderSubtle.frame(height: 1) } }
            codeBody
        }
        .background { Color.clear.tesseraTile(TesseraColor.fillDefault, style: .sunken, shape: shape) }
        .clipShape(shape)
    }

    @ViewBuilder private var codeBody: some View {
        let lines = trimmed.components(separatedBy: "\n")
        let text = HStack(alignment: .top, spacing: 12) {
            if lineNumbers {
                // 行号与代码用同一种文字排版（一整段），行高才完全一致
                Text(lines.indices.map { "\($0 + 1)" }.joined(separator: "\n"))
                    .multilineTextAlignment(.trailing)
                    .foregroundStyle(TesseraColor.textTertiary)
                    .frame(minWidth: 22, alignment: .trailing)
                    .accessibilityHidden(true)
            }
            Text(trimmed).foregroundStyle(TesseraColor.textPrimary).textSelection(.enabled)
        }
        .font(.system(size: 13, design: .monospaced))
        .lineSpacing(5)
        .fixedSize()
        .padding(.horizontal, tesseraShape.isRound ? 18 : 16)
        .padding(.vertical, 14)
        if staticRender {
            LeadingClipLayout { text }.clipped()
        } else {
            ScrollView(.horizontal, showsIndicators: false) { text }
                .frame(maxWidth: .infinity, alignment: .leading)
        }
    }
}
