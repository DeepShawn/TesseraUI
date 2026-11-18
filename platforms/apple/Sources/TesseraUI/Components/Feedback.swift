import SwiftUI
#if canImport(UIKit)
import UIKit
#endif

// MARK: - 提示消息

public struct TesseraToastMessage: Identifiable, Equatable, Sendable {
    public let id = UUID()
    public let text: String
    public let icon: TesseraIconName?
    public let actionTitle: String?

    public init(_ text: String, icon: TesseraIconName? = nil, actionTitle: String? = nil) {
        self.text = text
        self.icon = icon
        self.actionTitle = actionTitle
    }
}

public extension View {
    /// 提示消息：一块反色砖，从顶部落下（bouncy）；停留 3 秒（带操作 5 秒）；可上滑关掉
    func tesseraToast(_ message: Binding<TesseraToastMessage?>, onAction: (() -> Void)? = nil) -> some View {
        modifier(ToastModifier(message: message, onAction: onAction))
    }

    /// 对话框：面板从 0.96 缩放进场（smooth）；按钮是一组拼贴砖
    func tesseraDialog(isPresented: Binding<Bool>, title: String, message: String? = nil,
                       confirmTitle: String, cancelTitle: String? = "取消", destructive: Bool = false,
                       onConfirm: @escaping () -> Void) -> some View {
        modifier(DialogModifier(isPresented: isPresented, title: title, message: message, confirmTitle: confirmTitle,
                                cancelTitle: cancelTitle, destructive: destructive, onConfirm: onConfirm))
    }

    /// 底部抽屉：使用系统 sheet 的拖动与档位手势，外观换成 TesseraUI 样式
    func tesseraSheet<Content: View>(isPresented: Binding<Bool>, title: String? = nil,
                                     detents: Set<PresentationDetent> = [.medium, .large],
                                     @ViewBuilder content: @escaping () -> Content) -> some View {
        modifier(SheetModifier(isPresented: isPresented, title: title, detents: detents, sheetContent: content))
    }
}

struct SheetModifier<SheetContent: View>: ViewModifier {
    @Binding var isPresented: Bool
    let title: String?
    let detents: Set<PresentationDetent>
    let sheetContent: () -> SheetContent
    @Environment(\.tesseraShape) private var tesseraShape

    func body(content: Content) -> some View {
        let shape = tesseraShape
        return content.sheet(isPresented: $isPresented) {
            VStack(alignment: .leading, spacing: TesseraSpace.s4) {
                if let title { TesseraLabel(title, style: .title2).accessibilityAddTraits(.isHeader) }
                sheetContent()
                Spacer(minLength: 0)
            }
            .padding(.horizontal, TesseraSpace.s5)
            .padding(.top, TesseraSpace.s8)
            .frame(maxWidth: .infinity, alignment: .leading)
            .presentationDetents(detents)
            .presentationDragIndicator(.visible)
            .presentationCornerRadius(shape.radius(tile: TesseraRadius.s2xl, round: TesseraRound.panel))
            .presentationBackground(TesseraColor.bgElevated)
            // sheet 是新的视图层级，外观要再传进去
            .tesseraShape(shape)
        }
    }
}

struct ToastModifier: ViewModifier {
    @Binding var message: TesseraToastMessage?
    let onAction: (() -> Void)?
    @State private var dragY: CGFloat = 0
    @Environment(\.accessibilityReduceMotion) private var reduceMotion

    func body(content: Content) -> some View {
        content.overlay(alignment: .top) {
            if let m = message {
                TesseraToastView(message: m, onAction: { onAction?(); message = nil })
                    .offset(y: dragY)
                    .gesture(
                        DragGesture()
                            .onChanged { v in dragY = v.translation.height > 0 ? v.translation.height * 0.3 : v.translation.height }
                            .onEnded { v in
                                if v.translation.height < -24 || v.velocity.height < -TesseraMotion.flingVelocity { dismiss() }
                                else { withAnimation(TesseraMotion.bouncy) { dragY = 0 } }
                            }
                    )
                    .padding(.top, TesseraSpace.s4)
                    .transition(reduceMotion ? .opacity : .asymmetric(
                        insertion: .move(edge: .top).combined(with: .scale(scale: 0.9)).combined(with: .opacity),
                        removal: .opacity.animation(.easeIn(duration: TesseraMotion.durationFast))))
                    .task(id: m.id) {
                        try? await Task.sleep(for: .seconds(m.actionTitle == nil ? TesseraMotion.toastDuration : TesseraMotion.toastDurationWithAction))
                        if message?.id == m.id { dismiss() }
                    }
                    .onAppear {
                        #if canImport(UIKit)
                        UIAccessibility.post(notification: .announcement, argument: m.text)
                        #endif
                    }
            }
        }
        .animation(.tessera(TesseraMotion.bouncy, reduceMotion: reduceMotion), value: message)
    }

    private func dismiss() {
        withAnimation(.easeIn(duration: TesseraMotion.durationFast)) { message = nil }
        dragY = 0
    }
}

/// 提示消息的砖块本身
public struct TesseraToastView: View {
    let message: TesseraToastMessage
    let onAction: () -> Void
    @Environment(\.tesseraShape) private var tesseraShape

    public init(message: TesseraToastMessage, onAction: @escaping () -> Void = {}) {
        self.message = message
        self.onAction = onAction
    }

    public var body: some View {
        // 拼贴：一块反色砖；圆融：一颗反色胶囊
        let shape = RoundedRectangle(cornerRadius: tesseraShape.radius(tile: TesseraRadius.lg), style: .continuous)
        HStack(spacing: TesseraSpace.s3) {
            if let icon = message.icon { TesseraIcon(icon) }
            TesseraLabel(message.text, style: .bodySm, color: TesseraColor.textInverse).lineLimit(2)
            if let action = message.actionTitle {
                Button(action: onAction) { Text(action).font(.system(size: 14, weight: .semibold)).underline() }
                    .buttonStyle(.plain)
                    .frame(minHeight: TesseraSize.touchMin)
                    .padding(.horizontal, TesseraSpace.s2)
            }
        }
        .foregroundStyle(TesseraColor.textInverse)
        .padding(.leading, tesseraShape == .round ? TesseraSpace.s5 : TesseraSpace.s4)
        .padding(.trailing, message.actionTitle == nil ? TesseraSpace.s5 : TesseraSpace.s2)
        .frame(minHeight: 48)
        .tesseraTile(TesseraColor.bgInverse, style: .solid, shape: shape)
        .tesseraShadow(.level2)
        .frame(maxWidth: 480)
        .padding(.horizontal, TesseraSpace.s4)
    }
}

// MARK: - 对话框

struct DialogModifier: ViewModifier {
    @Binding var isPresented: Bool
    let title: String
    let message: String?
    let confirmTitle: String
    let cancelTitle: String?
    let destructive: Bool
    let onConfirm: () -> Void
    @Environment(\.accessibilityReduceMotion) private var reduceMotion

    func body(content: Content) -> some View {
        content.overlay {
            ZStack {
                if isPresented {
                    TesseraColor.bgScrim.ignoresSafeArea()
                        .onTapGesture { isPresented = false }
                        .transition(.opacity.animation(.easeOut(duration: TesseraMotion.durationNormal)))
                    TesseraDialogPanel(title: title, message: message, confirmTitle: confirmTitle, cancelTitle: cancelTitle, destructive: destructive,
                                       onCancel: { isPresented = false }, onConfirm: { isPresented = false; onConfirm() })
                        .transition(reduceMotion ? .opacity : .asymmetric(
                            insertion: .scale(scale: 0.96).combined(with: .opacity),
                            removal: .scale(scale: 0.98).combined(with: .opacity).animation(.easeIn(duration: TesseraMotion.durationFast))))
                        .accessibilityAddTraits(.isModal)
                }
            }
            .animation(.tessera(TesseraMotion.smooth, reduceMotion: reduceMotion), value: isPresented)
        }
    }
}

/// 对话框面板（不含遮罩），也可用于自定义弹层
public struct TesseraDialogPanel: View {
    let title: String
    let message: String?
    let confirmTitle: String
    let cancelTitle: String?
    let destructive: Bool
    let onCancel: () -> Void
    let onConfirm: () -> Void
    @Environment(\.tesseraShape) private var tesseraShape

    public init(title: String, message: String? = nil, confirmTitle: String, cancelTitle: String? = "取消", destructive: Bool = false,
                onCancel: @escaping () -> Void = {}, onConfirm: @escaping () -> Void = {}) {
        self.title = title
        self.message = message
        self.confirmTitle = confirmTitle
        self.cancelTitle = cancelTitle
        self.destructive = destructive
        self.onCancel = onCancel
        self.onConfirm = onConfirm
    }

    public var body: some View {
        let round = tesseraShape == .round
        let shape = RoundedRectangle(cornerRadius: tesseraShape.radius(tile: TesseraRadius.s2xl, round: TesseraRound.panel), style: .continuous)
        VStack(alignment: .leading, spacing: 0) {
            TesseraLabel(title, style: .title2).accessibilityAddTraits(.isHeader)
            if let message { TesseraLabel(message, style: .bodySm, color: TesseraColor.textSecondary).padding(.top, TesseraSpace.s2) }
            // 拼贴：一组拼贴砖；圆融：两颗独立的胶囊，间距 8
            HStack(spacing: tesseraShape.pillGap) {
                if let cancelTitle {
                    Button(cancelTitle, action: onCancel).buttonStyle(.tessera(.secondary, size: .lg, position: round ? nil : .first, fullWidth: true))
                }
                Button(confirmTitle, action: onConfirm)
                    .buttonStyle(.tessera(destructive ? .danger : .primary, size: .lg, position: round ? nil : (cancelTitle == nil ? .single : .last), fullWidth: true))
            }
            .padding(.top, TesseraSpace.s6)
        }
        .padding(TesseraSpace.s6)
        .frame(maxWidth: 440)
        .background(TesseraColor.bgElevated, in: shape)
        .overlay { shape.strokeBorder(round ? .clear : TesseraColor.borderSurface, lineWidth: 1) }
        .tesseraShadow(.level3)
        .padding(TesseraSpace.s4)
    }
}
